package com.example

import com.example.core.camera.ImageProcessor
import com.example.core.location.CapturedLocation
import com.example.core.location.GeofenceEvaluationResult
import com.example.core.location.GeofenceEvaluator
import com.example.data.model.LocationVerificationOutcome
import com.example.data.model.WorkplaceDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  private val workplace =
    WorkplaceDto(
      id = "wp-1",
      name = "Civic Annex",
      latitude = 37.7749,
      longitude = -122.4194,
      geofenceRadiusMeters = 150.0,
      maxLocationAccuracyMeters = 50.0,
    )

  @Test
  fun `geofence verifies location inside radius with acceptable accuracy`() {
    val inside =
      CapturedLocation(
        latitude = 37.7752,
        longitude = -122.4194,
        accuracyMeters = 15.0,
        timestampMillis = 1_763_467_200_000L,
        provider = "fused",
      )
    val result = GeofenceEvaluator.evaluate(inside, workplace)
    assertTrue(result is GeofenceEvaluationResult.Verified)
    assertEquals(LocationVerificationOutcome.VERIFIED, result.outcome)
  }

  @Test
  fun `geofence rejects location with insufficient accuracy`() {
    val inaccurate =
      CapturedLocation(
        latitude = 37.7749,
        longitude = -122.4194,
        accuracyMeters = 120.0,
        timestampMillis = 1_763_467_200_000L,
        provider = "network",
      )
    val result = GeofenceEvaluator.evaluate(inaccurate, workplace)
    assertTrue(result is GeofenceEvaluationResult.InsufficientAccuracy)
    assertEquals(LocationVerificationOutcome.INSUFFICIENT_ACCURACY, result.outcome)
  }

  @Test
  fun `image processor enforces allowed MIME types`() {
    assertTrue(ImageProcessor.isSupportedMimeType("image/jpeg"))
    assertTrue(ImageProcessor.isSupportedMimeType("image/png"))
    assertTrue(ImageProcessor.isSupportedMimeType("image/webp"))
    assertFalse(ImageProcessor.isSupportedMimeType("application/pdf"))
    assertFalse(ImageProcessor.isSupportedMimeType("application/octet-stream"))
  }
}
