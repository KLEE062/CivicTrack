package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.capabilities.BiometricCapabilityState
import com.example.core.capabilities.LocationPermissionState
import com.example.core.capabilities.PermissionAccessState
import com.example.ui.CivicTrackUiState
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalCivicStatusPalette

@Composable
fun DeviceSecuritySettingsScreen(
  uiState: CivicTrackUiState,
  onRequestLocationPermission: () -> Unit,
  onRequestCameraPermission: () -> Unit,
  onOpenAppSettings: () -> Unit,
  onOpenLocationSettings: () -> Unit,
  onOpenBiometricEnroll: () -> Unit,
  onToggleBiometricLogin: (Boolean) -> Unit,
  onTestBiometricPrompt: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val snapshot = uiState.capabilities
  val palette = LocalCivicStatusPalette.current

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        Text(
          text = "Device & Security Capabilities",
          style = MaterialTheme.typography.headlineMedium,
        )
        Text(
          text = "Inspect hardware availability, manage runtime permissions, and configure local biometric unlock.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      // 1. Location Permission & Services Card
      item {
        val locPerm = snapshot?.locationPermission ?: LocationPermissionState.DENIED_CAN_REQUEST
        val servicesOn = snapshot?.locationServicesEnabled ?: false
        val locLabel =
          when (locPerm) {
            LocationPermissionState.FINE_GRANTED -> "Precise Granted"
            LocationPermissionState.COARSE_ONLY_GRANTED -> "Approximate Only"
            LocationPermissionState.DENIED_CAN_REQUEST -> "Not Granted"
            LocationPermissionState.PERMANENTLY_DENIED -> "Permanently Denied"
          }
        val locHealthy =
          (locPerm == LocationPermissionState.FINE_GRANTED ||
            locPerm == LocationPermissionState.COARSE_ONLY_GRANTED) && servicesOn

        Card(
          modifier = Modifier.fillMaxWidth().testTag("settings_location_card"),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Location & Geofencing", style = MaterialTheme.typography.titleLarge)
              }
              StatusPill(
                label = locLabel,
                containerColor = if (locHealthy) palette.verifiedContainer else palette.warningContainer,
                contentColor = if (locHealthy) palette.verified else palette.warning,
              )
            }

            Text(
              text =
                "Location Services: ${if (servicesOn) "Enabled" else "Disabled"} • Foreground only (no background tracking).",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              if (locPerm == LocationPermissionState.DENIED_CAN_REQUEST) {
                Button(
                  onClick = onRequestLocationPermission,
                  modifier = Modifier.testTag("settings_request_location_button"),
                ) {
                  Text("Grant Location")
                }
              } else if (locPerm == LocationPermissionState.PERMANENTLY_DENIED) {
                Button(
                  onClick = onOpenAppSettings,
                  modifier = Modifier.testTag("settings_open_app_settings_location"),
                ) {
                  Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(6.dp))
                  Text(stringResource(R.string.open_app_settings))
                }
              }
              if (!servicesOn) {
                OutlinedButton(
                  onClick = onOpenLocationSettings,
                  modifier = Modifier.testTag("settings_open_location_settings_button"),
                ) {
                  Text(stringResource(R.string.open_location_settings))
                }
              }
            }
          }
        }
      }

      // 2. Camera Hardware & Permission Card
      item {
        val camAvailable = snapshot?.cameraAvailable ?: false
        val camPerm = snapshot?.cameraPermission ?: PermissionAccessState.DENIED_CAN_REQUEST
        val camLabel =
          when {
            !camAvailable -> "No Hardware"
            camPerm == PermissionAccessState.GRANTED -> "Granted"
            camPerm == PermissionAccessState.PERMANENTLY_DENIED -> "Permanently Denied"
            else -> "Not Granted"
          }

        Card(
          modifier = Modifier.fillMaxWidth().testTag("settings_camera_card"),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Camera Access", style = MaterialTheme.typography.titleLarge)
              }
              StatusPill(
                label = camLabel,
                containerColor =
                  if (camAvailable && camPerm == PermissionAccessState.GRANTED) palette.verifiedContainer
                  else palette.warningContainer,
                contentColor =
                  if (camAvailable && camPerm == PermissionAccessState.GRANTED) palette.verified
                  else palette.warning,
              )
            }

            Text(
              text =
                if (!camAvailable) "No physical camera detected. Gallery Photo Picker remains available."
                else "Used strictly when you initiate profile photo or document capture.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (camAvailable && camPerm == PermissionAccessState.DENIED_CAN_REQUEST) {
              Button(
                onClick = onRequestCameraPermission,
                modifier = Modifier.testTag("settings_request_camera_button"),
              ) {
                Text("Grant Camera")
              }
            } else if (camAvailable && camPerm == PermissionAccessState.PERMANENTLY_DENIED) {
              Button(
                onClick = onOpenAppSettings,
                modifier = Modifier.testTag("settings_open_app_settings_camera"),
              ) {
                Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.open_app_settings))
              }
            }
          }
        }
      }

      // 3. Biometric Availability & Login Preference Card
      item {
        val bioState = snapshot?.biometricCapability ?: BiometricCapabilityState.Unsupported
        val bioReady = bioState is BiometricCapabilityState.Available
        val bioStatusLabel =
          when (bioState) {
            is BiometricCapabilityState.Available -> "Ready"
            BiometricCapabilityState.NoneEnrolled -> "Not Enrolled"
            BiometricCapabilityState.NoHardware -> "No Hardware"
            BiometricCapabilityState.HardwareUnavailable -> "Unavailable"
            BiometricCapabilityState.SecurityUpdateRequired -> "Update Required"
            BiometricCapabilityState.Unsupported -> "Unsupported"
          }

        Card(
          modifier = Modifier.fillMaxWidth().testTag("settings_biometric_card"),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(Icons.Filled.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Biometric Authentication", style = MaterialTheme.typography.titleLarge)
              }
              StatusPill(
                label = bioStatusLabel,
                containerColor = if (bioReady) palette.verifiedContainer else palette.warningContainer,
                contentColor = if (bioReady) palette.verified else palette.warning,
              )
            }

            Text(
              text =
                when (bioState) {
                  is BiometricCapabilityState.Available ->
                    "${bioState.strengthLabel} • Device PIN/Pattern/Password fallback: ${if (bioState.supportsDeviceCredentialFallback) "Supported" else "Disabled"}"
                  BiometricCapabilityState.NoneEnrolled ->
                    "Biometric hardware is present, but no fingerprint, face, or screen lock credential is enrolled."
                  BiometricCapabilityState.NoHardware ->
                    "No compatible biometric sensor on this device."
                  BiometricCapabilityState.HardwareUnavailable ->
                    "Biometric hardware is currently busy or unavailable."
                  BiometricCapabilityState.SecurityUpdateRequired ->
                    "System security patch required before biometric authentication can run."
                  BiometricCapabilityState.Unsupported ->
                    "Biometric authentication unsupported on this OS version."
                },
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("Optional Biometric Unlock", style = MaterialTheme.typography.titleMedium)
                Text(
                  "Require local BiometricPrompt before resuming saved session or checking in.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Switch(
                checked = uiState.biometricLoginEnabled,
                onCheckedChange = onToggleBiometricLogin,
                enabled = bioReady || uiState.biometricLoginEnabled,
                modifier = Modifier.testTag("biometric_login_switch"),
              )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              if (bioReady) {
                Button(
                  onClick = onTestBiometricPrompt,
                  modifier = Modifier.testTag("test_biometric_prompt_button"),
                ) {
                  Text("Test Biometric Prompt")
                }
              }
              if (bioState == BiometricCapabilityState.NoneEnrolled) {
                OutlinedButton(
                  onClick = onOpenBiometricEnroll,
                  modifier = Modifier.testTag("enroll_biometric_button"),
                ) {
                  Text(stringResource(R.string.open_biometric_settings))
                }
              }
            }

            if (!uiState.settingsBiometricTestMessage.isNullOrBlank()) {
              Text(
                text = uiState.settingsBiometricTestMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("settings_biometric_feedback"),
              )
            }
          }
        }
      }

      // 4. Device Compatibility Information Card
      item {
        val compat = snapshot?.compatibility
        Card(
          modifier = Modifier.fillMaxWidth().testTag("device_compatibility_card"),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Text("Device Compatibility Information", style = MaterialTheme.typography.titleLarge)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            CompatibilityRow("Device Model", compat?.deviceModel ?: "Inspecting...")
            CompatibilityRow("Android OS", compat?.androidVersionLabel ?: "Inspecting...")
            CompatibilityRow("GPS Hardware", if (compat?.hasGpsHardware == true) "Present" else "Not Reported")
            CompatibilityRow(
              "Camera Sensors",
              if (compat?.hasCameraHardware == true) "${compat.cameraCount} available" else "None",
            )
            CompatibilityRow(
              "Fingerprint Sensor",
              if (compat?.hasFingerprintSensor == true) "Present" else "Not Reported",
            )
            CompatibilityRow(
              "Face / Iris Hardware",
              when {
                compat?.hasFaceHardware == true && compat.hasIrisHardware -> "Face & Iris"
                compat?.hasFaceHardware == true -> "Face Sensor"
                compat?.hasIrisHardware == true -> "Iris Sensor"
                else -> "Not Reported"
              },
            )
          }
        }
      }

      // 5. Explicit Privacy & Biometric Security Boundary Card
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            ),
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
          ) {
            Icon(
              imageVector = Icons.Filled.Security,
              contentDescription = "Security guarantee",
              tint = MaterialTheme.colorScheme.primary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("Security & Privacy Boundary", style = MaterialTheme.typography.titleMedium)
              Text(
                text = stringResource(R.string.biometric_security_notice),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CompatibilityRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}
