package com.example.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import com.example.core.capabilities.DeviceCapabilityChecker
import com.example.core.capabilities.LocationPermissionState
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.Instant
import kotlin.coroutines.resume

data class CapturedLocation(
  val latitude: Double,
  val longitude: Double,
  val accuracyMeters: Double,
  val timestampMillis: Long,
  val provider: String,
) {
  val timestampIso: String
    get() = Instant.ofEpochMilli(timestampMillis).toString()
}

sealed interface LocationFetchResult {
  data class Success(val location: CapturedLocation) : LocationFetchResult
  data class PermissionDenied(val isPermanent: Boolean) : LocationFetchResult
  data object ServicesDisabled : LocationFetchResult
  data class Unavailable(val reason: String) : LocationFetchResult
}

interface AttendanceLocationProvider {
  suspend fun fetchCurrentLocation(
    permissionState: LocationPermissionState,
  ): LocationFetchResult
}

class FusedAttendanceLocationProvider(
  private val context: Context,
  private val capabilityChecker: DeviceCapabilityChecker,
) : AttendanceLocationProvider {

  @SuppressLint("MissingPermission")
  override suspend fun fetchCurrentLocation(
    permissionState: LocationPermissionState,
  ): LocationFetchResult {
    if (
      permissionState == LocationPermissionState.DENIED_CAN_REQUEST ||
        permissionState == LocationPermissionState.PERMANENTLY_DENIED
    ) {
      return LocationFetchResult.PermissionDenied(
        isPermanent = permissionState == LocationPermissionState.PERMANENTLY_DENIED
      )
    }

    if (!capabilityChecker.isLocationServicesEnabled()) {
      return LocationFetchResult.ServicesDisabled
    }

    val priority =
      if (permissionState == LocationPermissionState.FINE_GRANTED) {
        Priority.PRIORITY_HIGH_ACCURACY
      } else {
        Priority.PRIORITY_BALANCED_POWER_ACCURACY
      }

    val fusedResult = tryFusedLocation(priority)
    if (fusedResult != null) {
      return LocationFetchResult.Success(fusedResult)
    }

    val fallbackResult = tryLocationManagerFallback(permissionState)
    return if (fallbackResult != null) {
      LocationFetchResult.Success(fallbackResult)
    } else {
      LocationFetchResult.Unavailable(
        "Unable to obtain a current GPS or network location fix. Move near an open window or verify GPS signal."
      )
    }
  }

  @SuppressLint("MissingPermission")
  private suspend fun tryFusedLocation(priority: Int): CapturedLocation? =
    suspendCancellableCoroutine { continuation ->
      try {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()
        continuation.invokeOnCancellation { cts.cancel() }

        val request =
          CurrentLocationRequest.Builder()
            .setPriority(priority)
            .setDurationMillis(10_000L)
            .setMaxUpdateAgeMillis(15_000L)
            .build()

        fusedClient
          .getCurrentLocation(request, cts.token)
          .addOnSuccessListener { location: Location? ->
            if (continuation.isActive) {
              continuation.resume(location?.toCapturedLocation("fused"))
            }
          }
          .addOnFailureListener {
            if (continuation.isActive) {
              continuation.resume(null)
            }
          }
      } catch (_: SecurityException) {
        if (continuation.isActive) continuation.resume(null)
      } catch (_: Exception) {
        if (continuation.isActive) continuation.resume(null)
      }
    }

  @SuppressLint("MissingPermission")
  private suspend fun tryLocationManagerFallback(
    permissionState: LocationPermissionState,
  ): CapturedLocation? =
    suspendCancellableCoroutine { continuation ->
      try {
        val lm =
          context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: run {
              if (continuation.isActive) continuation.resume(null)
              return@suspendCancellableCoroutine
            }

        val provider =
          when {
            permissionState == LocationPermissionState.FINE_GRANTED &&
              lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
              LocationManager.NETWORK_PROVIDER
            else -> null
          }

        if (provider == null) {
          if (continuation.isActive) continuation.resume(null)
          return@suspendCancellableCoroutine
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
          val cancellationSignal = CancellationSignal()
          continuation.invokeOnCancellation { cancellationSignal.cancel() }
          lm.getCurrentLocation(
            provider,
            cancellationSignal,
            ContextCompat.getMainExecutor(context),
          ) { location ->
            if (continuation.isActive) {
              continuation.resume(location?.toCapturedLocation(provider))
            }
          }
        } else {
          @Suppress("DEPRECATION")
          val lastKnown = lm.getLastKnownLocation(provider)
          val freshEnough =
            lastKnown != null && (System.currentTimeMillis() - lastKnown.time) <= 60_000L
          if (continuation.isActive) {
            continuation.resume(
              if (freshEnough) lastKnown?.toCapturedLocation(provider) else null
            )
          }
        }
      } catch (_: SecurityException) {
        if (continuation.isActive) continuation.resume(null)
      } catch (_: Exception) {
        if (continuation.isActive) continuation.resume(null)
      }
    }

  private fun Location.toCapturedLocation(source: String): CapturedLocation =
    CapturedLocation(
      latitude = latitude,
      longitude = longitude,
      accuracyMeters = if (hasAccuracy()) accuracy.toDouble() else 999.0,
      timestampMillis = if (time > 0L) time else System.currentTimeMillis(),
      provider = provider ?: source,
    )
}
