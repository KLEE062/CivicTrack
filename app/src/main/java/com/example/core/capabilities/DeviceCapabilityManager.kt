package com.example.core.capabilities

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

enum class PermissionAccessState {
  GRANTED,
  DENIED_CAN_REQUEST,
  PERMANENTLY_DENIED,
}

enum class LocationPermissionState {
  FINE_GRANTED,
  COARSE_ONLY_GRANTED,
  DENIED_CAN_REQUEST,
  PERMANENTLY_DENIED,
}

sealed interface BiometricCapabilityState {
  data class Available(
    val strengthLabel: String,
    val supportsDeviceCredentialFallback: Boolean,
    val allowedAuthenticators: Int,
  ) : BiometricCapabilityState

  data object NoHardware : BiometricCapabilityState
  data object HardwareUnavailable : BiometricCapabilityState
  data object NoneEnrolled : BiometricCapabilityState
  data object SecurityUpdateRequired : BiometricCapabilityState
  data object Unsupported : BiometricCapabilityState
}

data class DeviceCompatibilityInfo(
  val deviceModel: String,
  val androidVersionLabel: String,
  val hasGpsHardware: Boolean,
  val hasNetworkLocation: Boolean,
  val hasCameraHardware: Boolean,
  val cameraCount: Int,
  val hasFingerprintSensor: Boolean,
  val hasFaceHardware: Boolean,
  val hasIrisHardware: Boolean,
)

data class DeviceCapabilitySnapshot(
  val locationPermission: LocationPermissionState,
  val locationServicesEnabled: Boolean,
  val cameraPermission: PermissionAccessState,
  val cameraAvailable: Boolean,
  val biometricCapability: BiometricCapabilityState,
  val networkAvailable: Boolean,
  val compatibility: DeviceCompatibilityInfo,
) {
  val isLocationReady: Boolean
    get() =
      (locationPermission == LocationPermissionState.FINE_GRANTED ||
        locationPermission == LocationPermissionState.COARSE_ONLY_GRANTED) &&
        locationServicesEnabled

  val isCameraReady: Boolean
    get() = cameraAvailable && cameraPermission == PermissionAccessState.GRANTED
}

interface DeviceCapabilityChecker {
  fun inspectCapabilities(
    activity: Activity?,
    locationPreviouslyRequested: Boolean,
    cameraPreviouslyRequested: Boolean,
  ): DeviceCapabilitySnapshot

  fun isLocationServicesEnabled(): Boolean
  fun hasCameraHardware(): Boolean
  fun checkBiometricAvailability(allowDeviceCredentialFallback: Boolean = true): BiometricCapabilityState
  fun isNetworkConnected(): Boolean
  fun buildAppSettingsIntent(): Intent
  fun buildLocationSettingsIntent(): Intent
  fun buildBiometricEnrollIntent(): Intent
  fun userMessageForBiometricState(state: BiometricCapabilityState): String
  fun userMessageForLocationIssue(
    permission: LocationPermissionState,
    servicesEnabled: Boolean,
  ): String
  fun userMessageForCameraIssue(
    available: Boolean,
    permission: PermissionAccessState,
  ): String
}

class AndroidDeviceCapabilityManager(private val context: Context) : DeviceCapabilityChecker {

  override fun inspectCapabilities(
    activity: Activity?,
    locationPreviouslyRequested: Boolean,
    cameraPreviouslyRequested: Boolean,
  ): DeviceCapabilitySnapshot {
    val locationPerm = inspectLocationPermission(activity, locationPreviouslyRequested)
    val cameraPerm = inspectSinglePermission(Manifest.permission.CAMERA, activity, cameraPreviouslyRequested)
    val cameraAvailable = hasCameraHardware()
    val locationEnabled = isLocationServicesEnabled()
    val biometricState = checkBiometricAvailability(allowDeviceCredentialFallback = true)
    val networkAvailable = isNetworkConnected()

    return DeviceCapabilitySnapshot(
      locationPermission = locationPerm,
      locationServicesEnabled = locationEnabled,
      cameraPermission = cameraPerm,
      cameraAvailable = cameraAvailable,
      biometricCapability = biometricState,
      networkAvailable = networkAvailable,
      compatibility = inspectCompatibility(),
    )
  }

  private fun inspectLocationPermission(
    activity: Activity?,
    previouslyRequested: Boolean,
  ): LocationPermissionState {
    val fineGranted =
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    if (fineGranted) return LocationPermissionState.FINE_GRANTED

    val coarseGranted =
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    if (coarseGranted) return LocationPermissionState.COARSE_ONLY_GRANTED

    val showFineRationale =
      activity != null &&
        ActivityCompat.shouldShowRequestPermissionRationale(
          activity,
          Manifest.permission.ACCESS_FINE_LOCATION,
        )
    val showCoarseRationale =
      activity != null &&
        ActivityCompat.shouldShowRequestPermissionRationale(
          activity,
          Manifest.permission.ACCESS_COARSE_LOCATION,
        )

    return if (previouslyRequested && activity != null && !showFineRationale && !showCoarseRationale) {
      LocationPermissionState.PERMANENTLY_DENIED
    } else {
      LocationPermissionState.DENIED_CAN_REQUEST
    }
  }

  private fun inspectSinglePermission(
    permission: String,
    activity: Activity?,
    previouslyRequested: Boolean,
  ): PermissionAccessState {
    val granted =
      ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    if (granted) return PermissionAccessState.GRANTED

    val showRationale =
      activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)

    return if (previouslyRequested && activity != null && !showRationale) {
      PermissionAccessState.PERMANENTLY_DENIED
    } else {
      PermissionAccessState.DENIED_CAN_REQUEST
    }
  }

  override fun isLocationServicesEnabled(): Boolean {
    val locationManager =
      context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      locationManager.isLocationEnabled
    } else {
      @Suppress("DEPRECATION")
      val mode =
        Settings.Secure.getInt(
          context.contentResolver,
          Settings.Secure.LOCATION_MODE,
          Settings.Secure.LOCATION_MODE_OFF,
        )
      mode != Settings.Secure.LOCATION_MODE_OFF
    }
  }

  override fun hasCameraHardware(): Boolean {
    val pm = context.packageManager
    if (!pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
      return false
    }
    return try {
      val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
      cameraManager?.cameraIdList?.isNotEmpty() == true
    } catch (_: Exception) {
      false
    }
  }

  override fun checkBiometricAvailability(allowDeviceCredentialFallback: Boolean): BiometricCapabilityState {
    val biometricManager = BiometricManager.from(context)

    val strongOnly = biometricManager.canAuthenticate(BIOMETRIC_STRONG)
    if (strongOnly == BiometricManager.BIOMETRIC_SUCCESS) {
      val authenticators =
        if (allowDeviceCredentialFallback && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        } else {
          BIOMETRIC_STRONG
        }
      return BiometricCapabilityState.Available(
        strengthLabel = "Class 3 (Strong Biometric)",
        supportsDeviceCredentialFallback =
          allowDeviceCredentialFallback && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
        allowedAuthenticators = authenticators,
      )
    }

    val weakAuthenticators =
      if (allowDeviceCredentialFallback && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        BIOMETRIC_WEAK or DEVICE_CREDENTIAL
      } else {
        BIOMETRIC_WEAK
      }

    val weakResult = biometricManager.canAuthenticate(weakAuthenticators)
    return when (weakResult) {
      BiometricManager.BIOMETRIC_SUCCESS ->
        BiometricCapabilityState.Available(
          strengthLabel = "Class 2 (Standard Biometric)",
          supportsDeviceCredentialFallback =
            allowDeviceCredentialFallback && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P,
          allowedAuthenticators = weakAuthenticators,
        )
      BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricCapabilityState.NoneEnrolled
      BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricCapabilityState.NoHardware
      BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricCapabilityState.HardwareUnavailable
      BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
        BiometricCapabilityState.SecurityUpdateRequired
      else -> BiometricCapabilityState.Unsupported
    }
  }

  override fun isNetworkConnected(): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
      ?: return false
    val activeNetwork = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }

  private fun inspectCompatibility(): DeviceCompatibilityInfo {
    val pm = context.packageManager
    val cameraCount =
      try {
        (context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager)?.cameraIdList?.size ?: 0
      } catch (_: Exception) {
        0
      }

    return DeviceCompatibilityInfo(
      deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
      androidVersionLabel = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
      hasGpsHardware = pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS),
      hasNetworkLocation = pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_NETWORK),
      hasCameraHardware = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) && cameraCount > 0,
      cameraCount = cameraCount,
      hasFingerprintSensor = pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT),
      hasFaceHardware =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
          pm.hasSystemFeature(PackageManager.FEATURE_FACE),
      hasIrisHardware =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
          pm.hasSystemFeature(PackageManager.FEATURE_IRIS),
    )
  }

  override fun buildAppSettingsIntent(): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
      data = Uri.fromParts("package", context.packageName, null)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

  override fun buildLocationSettingsIntent(): Intent =
    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

  override fun buildBiometricEnrollIntent(): Intent {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
        putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    } else {
      Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    }
  }

  override fun userMessageForBiometricState(state: BiometricCapabilityState): String =
    when (state) {
      is BiometricCapabilityState.Available ->
        "Device biometric authentication ready (${state.strengthLabel})."
      BiometricCapabilityState.NoHardware ->
        "This device does not have compatible biometric hardware. Use your account credentials instead."
      BiometricCapabilityState.HardwareUnavailable ->
        "Biometric sensor is temporarily busy or unavailable. Try again shortly."
      BiometricCapabilityState.NoneEnrolled ->
        "No fingerprint, face, or screen lock credential is enrolled on this device. Open Device Security Settings to enroll one."
      BiometricCapabilityState.SecurityUpdateRequired ->
        "A system security update is required before biometric authentication can be used."
      BiometricCapabilityState.Unsupported ->
        "Biometric authentication is not supported on this Android configuration."
    }

  override fun userMessageForLocationIssue(
    permission: LocationPermissionState,
    servicesEnabled: Boolean,
  ): String =
    when {
      permission == LocationPermissionState.PERMANENTLY_DENIED ->
        "Location permission was permanently denied. Enable Precise Location in Android App Settings to verify workplace attendance."
      permission == LocationPermissionState.DENIED_CAN_REQUEST ->
        "Location permission is required at check-in to verify you are inside your workplace geofence."
      !servicesEnabled ->
        "Device Location Services are turned off. Enable GPS / Location in Android Settings."
      permission == LocationPermissionState.COARSE_ONLY_GRANTED ->
        "Approximate location is granted, but Precise Location is recommended for accurate workplace geofencing."
      else -> "Location is ready for attendance verification."
    }

  override fun userMessageForCameraIssue(
    available: Boolean,
    permission: PermissionAccessState,
  ): String =
    when {
      !available ->
        "No active camera hardware was detected on this device. You can still upload images via the Android Photo Picker."
      permission == PermissionAccessState.PERMANENTLY_DENIED ->
        "Camera permission was permanently denied. Enable Camera access in Android App Settings to capture photos."
      permission == PermissionAccessState.DENIED_CAN_REQUEST ->
        "Camera permission is required when you choose to take a profile photo or scan a document."
      else -> "Camera is ready for user-initiated capture."
    }
}
