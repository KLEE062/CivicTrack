package com.example.core.location

import com.example.data.model.LocationVerificationOutcome
import com.example.data.model.WorkplaceDto
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

sealed interface GeofenceEvaluationResult {
  val outcome: LocationVerificationOutcome
  val distanceMeters: Double?
  val permittedRadiusMeters: Double
  val failureReason: String?

  data class Verified(
    val location: CapturedLocation,
    override val distanceMeters: Double,
    override val permittedRadiusMeters: Double,
  ) : GeofenceEvaluationResult {
    override val outcome = LocationVerificationOutcome.VERIFIED
    override val failureReason: String? = null
  }

  data class OutsideGeofence(
    val location: CapturedLocation,
    override val distanceMeters: Double,
    override val permittedRadiusMeters: Double,
  ) : GeofenceEvaluationResult {
    override val outcome = LocationVerificationOutcome.OUTSIDE_GEOFENCE
    override val failureReason: String =
      "Device is %.1fm from workplace (permitted radius: %.0fm).".format(
        distanceMeters,
        permittedRadiusMeters,
      )
  }

  data class InsufficientAccuracy(
    val location: CapturedLocation,
    val maxAllowedAccuracyMeters: Double,
    override val distanceMeters: Double,
    override val permittedRadiusMeters: Double,
  ) : GeofenceEvaluationResult {
    override val outcome = LocationVerificationOutcome.INSUFFICIENT_ACCURACY
    override val failureReason: String =
      "Location accuracy ±%.1fm exceeds workplace threshold of ±%.0fm.".format(
        location.accuracyMeters,
        maxAllowedAccuracyMeters,
      )
  }
}

object GeofenceEvaluator {

  private const val EARTH_RADIUS_METERS = 6_371_000.0

  fun calculateDistanceMeters(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double,
  ): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val originLatRad = Math.toRadians(lat1)
    val targetLatRad = Math.toRadians(lat2)

    val a =
      sin(dLat / 2).pow(2.0) +
        cos(originLatRad) * cos(targetLatRad) * sin(dLon / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return EARTH_RADIUS_METERS * c
  }

  fun evaluate(
    capturedLocation: CapturedLocation,
    workplace: WorkplaceDto,
  ): GeofenceEvaluationResult {
    val distance =
      calculateDistanceMeters(
        lat1 = capturedLocation.latitude,
        lon1 = capturedLocation.longitude,
        lat2 = workplace.latitude,
        lon2 = workplace.longitude,
      )

    if (capturedLocation.accuracyMeters > workplace.maxLocationAccuracyMeters) {
      return GeofenceEvaluationResult.InsufficientAccuracy(
        location = capturedLocation,
        maxAllowedAccuracyMeters = workplace.maxLocationAccuracyMeters,
        distanceMeters = distance,
        permittedRadiusMeters = workplace.geofenceRadiusMeters,
      )
    }

    return if (distance <= workplace.geofenceRadiusMeters) {
      GeofenceEvaluationResult.Verified(
        location = capturedLocation,
        distanceMeters = distance,
        permittedRadiusMeters = workplace.geofenceRadiusMeters,
      )
    } else {
      GeofenceEvaluationResult.OutsideGeofence(
        location = capturedLocation,
        distanceMeters = distance,
        permittedRadiusMeters = workplace.geofenceRadiusMeters,
      )
    }
  }
}
