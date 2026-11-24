package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AttendanceSubmissionStatus
import com.example.data.model.BiometricStepStatus
import com.example.data.model.GeofenceStepStatus
import com.example.ui.RationaleType
import com.example.ui.theme.LocalCivicStatusPalette

@Composable
fun SupabaseConfigBanner(modifier: Modifier = Modifier) {
  val palette = LocalCivicStatusPalette.current
  Card(
    modifier = modifier.fillMaxWidth().testTag("supabase_config_banner"),
    colors = CardDefaults.cardColors(containerColor = palette.warningContainer),
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(
        imageVector = Icons.Filled.Info,
        contentDescription = "Configuration notice",
        tint = palette.warning,
      )
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = "Supabase Backend Credentials Required",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text =
            "Set SUPABASE_URL and SUPABASE_ANON_KEY in the AI Studio Secrets panel to enable employee sign-in, attendance_records, location_verifications, and Storage uploads. All hardware and capability diagnostics below remain live.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
fun PermissionRationaleDialog(
  rationaleType: RationaleType,
  onAllow: () -> Unit,
  onDismiss: () -> Unit,
) {
  val isLocation = rationaleType == RationaleType.LOCATION
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = if (isLocation) Icons.Filled.LocationOn else Icons.Filled.PhotoCamera,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
      )
    },
    title = {
      Text(
        text =
          stringResource(
            if (isLocation) R.string.location_rationale_title else R.string.camera_rationale_title
          )
      )
    },
    text = {
      Text(
        text =
          stringResource(
            if (isLocation) R.string.location_rationale_body else R.string.camera_rationale_body
          )
      )
    },
    confirmButton = {
      Button(
        onClick = onAllow,
        modifier = Modifier.testTag("rationale_allow_button"),
      ) {
        Text(stringResource(R.string.allow_permission))
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("rationale_dismiss_button"),
      ) {
        Text(stringResource(R.string.not_now))
      }
    },
  )
}

@Composable
fun PermissionBlockedBanner(
  message: String,
  requiresLocationSettings: Boolean,
  onOpenSettings: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalCivicStatusPalette.current
  Card(
    modifier = modifier.fillMaxWidth().testTag("permission_blocked_banner"),
    colors = CardDefaults.cardColors(containerColor = palette.errorContainer),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.Warning,
          contentDescription = "Action needed",
          tint = palette.error,
        )
        Text(
          text = if (requiresLocationSettings) "Location Services Disabled" else "Permission Required",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
          onClick = onOpenSettings,
          modifier = Modifier.testTag("open_system_settings_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.Settings,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(Modifier.width(6.dp))
          Text(
            stringResource(
              if (requiresLocationSettings) R.string.open_location_settings
              else R.string.open_app_settings
            )
          )
        }
        TextButton(onClick = onDismiss) { Text("Dismiss") }
      }
    }
  }
}

@Composable
fun ThreeStagePipelineCard(
  biometricStep: BiometricStepStatus,
  geofenceStep: GeofenceStepStatus,
  submissionStep: AttendanceSubmissionStatus,
  onRetrySync: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalCivicStatusPalette.current

  Card(
    modifier = modifier.fillMaxWidth().testTag("verification_pipeline_card"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
        text = "Verification Pipeline",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = "Device biometric unlock, workplace geofence check, and attendance sync are tracked independently.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

      // Operation 1: Device Biometric Authentication
      val bioVisual =
        when (biometricStep) {
          BiometricStepStatus.Idle ->
            StepVisual("Pending check-in action", MaterialTheme.colorScheme.onSurfaceVariant, Icons.Filled.Fingerprint, false)
          BiometricStepStatus.NotRequired ->
            StepVisual("Not required by workplace policy", MaterialTheme.colorScheme.tertiary, Icons.Filled.Fingerprint, false)
          BiometricStepStatus.Prompting ->
            StepVisual("Waiting for OS BiometricPrompt...", MaterialTheme.colorScheme.primary, Icons.Filled.Fingerprint, true)
          is BiometricStepStatus.LocalDeviceAuthenticated ->
            StepVisual(
              "OS-local check passed (${biometricStep.methodLabel}) — not central identity proof",
              palette.verified,
              Icons.Filled.CheckCircle,
              false,
            )
          is BiometricStepStatus.Cancelled ->
            StepVisual(biometricStep.message, palette.warning, Icons.Filled.Warning, false)
          is BiometricStepStatus.Unavailable ->
            StepVisual(biometricStep.message, palette.error, Icons.Filled.Error, false)
          is BiometricStepStatus.Error ->
            StepVisual(biometricStep.message, palette.error, Icons.Filled.Error, false)
        }
      PipelineStepRow(
        title = stringResource(R.string.step_biometric_title),
        detail = bioVisual.detail,
        tint = bioVisual.tint,
        icon = bioVisual.icon,
        inProgress = bioVisual.inProgress,
        tag = "pipeline_step_biometric",
      )

      // Operation 2: Workplace Geofence Verification
      val geoVisual =
        when (geofenceStep) {
          GeofenceStepStatus.Idle ->
            StepVisual("Waiting to capture GPS coordinates", MaterialTheme.colorScheme.onSurfaceVariant, Icons.Filled.GpsFixed, false)
          GeofenceStepStatus.AcquiringLocation ->
            StepVisual("Acquiring current device coordinates...", MaterialTheme.colorScheme.primary, Icons.Filled.GpsFixed, true)
          is GeofenceStepStatus.Verified ->
            StepVisual(
              "Verified: %.1fm from workplace (radius %.0fm, ±%.1fm accuracy)".format(
                geofenceStep.distanceMeters,
                geofenceStep.permittedRadiusMeters,
                geofenceStep.accuracyMeters,
              ),
              palette.verified,
              Icons.Filled.CheckCircle,
              false,
            )
          is GeofenceStepStatus.OutsideGeofence ->
            StepVisual(
              "Outside geofence: %.1fm away (max %.0fm), ±%.1fm accuracy".format(
                geofenceStep.distanceMeters,
                geofenceStep.permittedRadiusMeters,
                geofenceStep.accuracyMeters,
              ),
              palette.error,
              Icons.Filled.GpsOff,
              false,
            )
          is GeofenceStepStatus.InsufficientAccuracy ->
            StepVisual(
              "Insufficient GPS accuracy: ±%.1fm (required ≤ %.0fm)".format(
                geofenceStep.accuracyMeters,
                geofenceStep.maxAllowedMeters,
              ),
              palette.warning,
              Icons.Filled.Warning,
              false,
            )
          is GeofenceStepStatus.LocationUnavailable ->
            StepVisual(geofenceStep.reason, palette.error, Icons.Filled.GpsOff, false)
          is GeofenceStepStatus.PermissionDenied ->
            StepVisual(
              if (geofenceStep.isPermanent) "Location permission permanently denied"
              else "Location permission denied",
              palette.error,
              Icons.Filled.GpsOff,
              false,
            )
          GeofenceStepStatus.ServicesDisabled ->
            StepVisual("Device location services are disabled", palette.error, Icons.Filled.GpsOff, false)
        }
      PipelineStepRow(
        title = stringResource(R.string.step_geofence_title),
        detail = geoVisual.detail,
        tint = geoVisual.tint,
        icon = geoVisual.icon,
        inProgress = geoVisual.inProgress,
        tag = "pipeline_step_geofence",
      )

      // Operation 3: Attendance Submission to Supabase
      val syncVisual =
        when (submissionStep) {
          AttendanceSubmissionStatus.Idle ->
            StepVisual("Ready to record attendance & location_verifications", MaterialTheme.colorScheme.onSurfaceVariant, Icons.Filled.CloudDone, false)
          AttendanceSubmissionStatus.Submitting ->
            StepVisual("Recording attendance & location verification in Supabase...", MaterialTheme.colorScheme.primary, Icons.Filled.CloudDone, true)
          is AttendanceSubmissionStatus.Recorded ->
            StepVisual(
              "${submissionStep.checkType.displayLabel} recorded (${submissionStep.geofenceOutcome.apiValue}) • ID ${submissionStep.attendanceRecordId.take(8)}",
              palette.verified,
              Icons.Filled.CloudDone,
              false,
            )
          is AttendanceSubmissionStatus.NetworkError ->
            StepVisual(submissionStep.message, palette.error, Icons.Filled.CloudOff, false)
          is AttendanceSubmissionStatus.BackendError ->
            StepVisual(submissionStep.message, palette.error, Icons.Filled.Error, false)
        }
      PipelineStepRow(
        title = stringResource(R.string.step_attendance_title),
        detail = syncVisual.detail,
        tint = syncVisual.tint,
        icon = syncVisual.icon,
        inProgress = syncVisual.inProgress,
        tag = "pipeline_step_submission",
      )

      if (submissionStep is AttendanceSubmissionStatus.NetworkError) {
        OutlinedButton(
          onClick = onRetrySync,
          modifier = Modifier.fillMaxWidth().testTag("retry_attendance_sync_button"),
        ) {
          Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(8.dp))
          Text("Retry Attendance Sync")
        }
      }
    }
  }
}

private data class StepVisual(
  val detail: String,
  val tint: Color,
  val icon: ImageVector,
  val inProgress: Boolean,
)

@Composable
private fun PipelineStepRow(
  title: String,
  detail: String,
  tint: Color,
  icon: ImageVector,
  inProgress: Boolean,
  tag: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth().testTag(tag),
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Box(
      modifier =
        Modifier.size(32.dp).clip(CircleShape).background(tint.copy(alpha = 0.14f)),
      contentAlignment = Alignment.Center,
    ) {
      if (inProgress) {
        CircularProgressIndicator(
          modifier = Modifier.size(18.dp),
          strokeWidth = 2.dp,
          color = tint,
        )
      } else {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = tint,
          modifier = Modifier.size(18.dp),
        )
      }
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = detail,
        style = MaterialTheme.typography.bodyMedium,
        color = tint,
      )
    }
  }
}

@Composable
fun StatusPill(
  label: String,
  containerColor: Color,
  contentColor: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    color = containerColor,
    shape = CircleShape,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = contentColor,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}
