package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.FilterChip
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AttendanceCheckType
import com.example.data.model.AttendanceRecordDto
import com.example.data.model.LocationVerificationOutcome
import com.example.ui.CivicTrackUiState
import com.example.ui.components.PermissionBlockedBanner
import com.example.ui.components.StatusPill
import com.example.ui.components.SupabaseConfigBanner
import com.example.ui.components.ThreeStagePipelineCard
import com.example.ui.theme.LocalCivicStatusPalette

@Composable
fun AttendanceScreen(
  uiState: CivicTrackUiState,
  onSelectAuthMode: (Boolean) -> Unit,
  onRegister: (String, String, String, String, String, String, String, String?) -> Unit,
  onSignIn: (String, String) -> Unit,
  onBiometricUnlock: () -> Unit,
  onSignOut: () -> Unit,
  onRefreshProfile: () -> Unit,
  onInitiateAttendance: (AttendanceCheckType) -> Unit,
  onRetrySync: () -> Unit,
  onOpenBlockedSettings: () -> Unit,
  onDismissBlockedBanner: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalCivicStatusPalette.current

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      if (!uiState.isBackendConfigured) {
        item { SupabaseConfigBanner() }
      }

      if (uiState.permissionBlockedMessage != null) {
        item {
          PermissionBlockedBanner(
            message = uiState.permissionBlockedMessage,
            requiresLocationSettings = uiState.blockedPermissionRequiresLocationSettings,
            onOpenSettings = onOpenBlockedSettings,
            onDismiss = onDismissBlockedBanner,
          )
        }
      }

      if (uiState.session == null) {
        item {
          WorkplaceHeroHeader(
            title = stringResource(R.string.app_name),
            subtitle = stringResource(R.string.tagline),
            badgeText = "Geofence & Biometric Ready",
          )
        }
        item {
          EmployeeAuthCard(
            isRegisterMode = uiState.isRegisterMode,
            isLoading = uiState.isAuthLoading,
            errorMessage = uiState.authError,
            successMessage = uiState.authSuccessMessage,
            availableWorkplaces = uiState.availableWorkplaces,
            hasStoredSessionForBiometricUnlock = uiState.hasStoredSessionForBiometricUnlock,
            onSelectAuthMode = onSelectAuthMode,
            onRegister = onRegister,
            onSignIn = onSignIn,
            onBiometricUnlock = onBiometricUnlock,
          )
        }
      } else {
        item {
          val workplace = uiState.employee?.workplaces
          WorkplaceHeroHeader(
            title = workplace?.name ?: "Assigned Workplace",
            subtitle =
              workplace?.address ?: uiState.employee?.department ?: "Loading workplace details...",
            badgeText =
              if (workplace != null) {
                "Radius %.0fm • Max ±%.0fm".format(
                  workplace.geofenceRadiusMeters,
                  workplace.maxLocationAccuracyMeters,
                )
              } else {
                "Awaiting Workplace Assignment"
              },
          )
        }

        item {
          EmployeeProfileSummaryCard(
            uiState = uiState,
            onRefresh = onRefreshProfile,
            onSignOut = onSignOut,
          )
        }

        if (uiState.employee?.workplaces != null) {
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Button(
                onClick = { onInitiateAttendance(AttendanceCheckType.CHECK_IN) },
                modifier = Modifier.weight(1f).height(54.dp).testTag("check_in_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.LocationOn,
                  contentDescription = null,
                  modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.check_in_button))
              }
              OutlinedButton(
                onClick = { onInitiateAttendance(AttendanceCheckType.CHECK_OUT) },
                modifier = Modifier.weight(1f).height(54.dp).testTag("check_out_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.VerifiedUser,
                  contentDescription = null,
                  modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.check_out_button))
              }
            }
          }
        }

        item {
          ThreeStagePipelineCard(
            biometricStep = uiState.biometricStep,
            geofenceStep = uiState.geofenceStep,
            submissionStep = uiState.submissionStep,
            onRetrySync = onRetrySync,
          )
        }

        item {
          Text(
            text = "Recent Attendance & Location Verifications",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 4.dp),
          )
        }

        if (uiState.recentAttendance.isEmpty()) {
          item {
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors =
                CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.LocationOn,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = "No attendance logs recorded yet",
                  style = MaterialTheme.typography.titleMedium,
                )
                Text(
                  text =
                    "Tap Check In when at your assigned workplace to record an attendance event and location verification.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        } else {
          items(uiState.recentAttendance, key = { it.id }) { record ->
            AttendanceHistoryCard(record = record, paletteVerified = palette.verified)
          }
        }
      }
    }
  }
}

@Composable
private fun WorkplaceHeroHeader(
  title: String,
  subtitle: String,
  badgeText: String,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
  ) {
    Box(modifier = Modifier.fillMaxWidth().height(172.dp)) {
      Image(
        painter = painterResource(id = R.drawable.img_workplace_hero_1795004542758),
        contentDescription = "Civic workplace facility",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
      )
      Box(
        modifier =
          Modifier.fillMaxSize()
            .background(
              Brush.verticalGradient(
                colors =
                  listOf(
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f),
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.90f),
                  )
              )
            )
      )
      Column(
        modifier = Modifier.align(Alignment.BottomStart).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        StatusPill(
          label = badgeText,
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.headlineMedium,
          color = Color.White,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = Color.White.copy(alpha = 0.88f),
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmployeeAuthCard(
  isRegisterMode: Boolean,
  isLoading: Boolean,
  errorMessage: String?,
  successMessage: String?,
  availableWorkplaces: List<com.example.data.model.WorkplaceDto>,
  hasStoredSessionForBiometricUnlock: Boolean,
  onSelectAuthMode: (Boolean) -> Unit,
  onRegister: (String, String, String, String, String, String, String, String?) -> Unit,
  onSignIn: (String, String) -> Unit,
  onBiometricUnlock: () -> Unit,
) {
  val palette = LocalCivicStatusPalette.current
  var email by rememberSaveable { mutableStateOf("") }
  var password by rememberSaveable { mutableStateOf("") }

  Card(
    modifier = Modifier.fillMaxWidth().testTag("employee_auth_card"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = stringResource(R.string.login_title),
        style = MaterialTheme.typography.headlineMedium,
      )
      Text(
        text = stringResource(R.string.login_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      if (hasStoredSessionForBiometricUnlock) {
        Button(
          onClick = onBiometricUnlock,
          enabled = !isLoading,
          modifier = Modifier.fillMaxWidth().height(50.dp).testTag("biometric_unlock_button"),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
        ) {
          Icon(Icons.Filled.Fingerprint, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Unlock Saved Session with Device Biometric")
        }
      }

      OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text(stringResource(R.string.email_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("email_input"),
      )

      OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text(stringResource(R.string.password_label)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth().testTag("password_input"),
      )

      if (!successMessage.isNullOrBlank()) {
        Text(
          text = successMessage,
          style = MaterialTheme.typography.bodyMedium,
          color = palette.verified,
          modifier = Modifier.testTag("auth_success_text"),
        )
      }

      if (!errorMessage.isNullOrBlank()) {
        Text(
          text = errorMessage,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.testTag("auth_error_text"),
        )
      }

      Button(
        onClick = { onSignIn(email, password) },
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sign_in_button"),
      ) {
        if (isLoading) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary,
          )
        } else {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Login,
            contentDescription = null,
          )
          Spacer(Modifier.width(8.dp))
          Text(stringResource(R.string.sign_in_button))
        }
      }
    }
  }
}

@Composable
private fun EmployeeProfileSummaryCard(
  uiState: CivicTrackUiState,
  onRefresh: () -> Unit,
  onSignOut: () -> Unit,
) {
  val emp = uiState.employee
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (!emp?.profilePhotoUrl.isNullOrBlank()) {
        AsyncImage(
          model = emp?.profilePhotoUrl,
          contentDescription = "Employee profile photo",
          modifier = Modifier.size(52.dp).clip(CircleShape),
          contentScale = ContentScale.Crop,
        )
      } else {
        Box(
          modifier =
            Modifier.size(52.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.Badge,
            contentDescription = "Employee badge",
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
          )
        }
      }

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = emp?.fullName ?: uiState.session?.email.orEmpty(),
          style = MaterialTheme.typography.titleMedium,
        )
        Text(
          text =
            if (emp != null) "${emp.employeeCode} • ${emp.role ?: "Civic Staff"}"
            else uiState.profileError ?: "Syncing employee profile...",
          style = MaterialTheme.typography.bodyMedium,
          color =
            if (uiState.profileError != null) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      IconButton(onClick = onRefresh, modifier = Modifier.testTag("refresh_profile_button")) {
        if (uiState.isProfileLoading) {
          CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
          Icon(Icons.Filled.Refresh, contentDescription = "Refresh workplace data")
        }
      }
      IconButton(onClick = onSignOut, modifier = Modifier.testTag("sign_out_button")) {
        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = stringResource(R.string.sign_out_button))
      }
    }
  }
}

@Composable
private fun AttendanceHistoryCard(
  record: AttendanceRecordDto,
  paletteVerified: Color,
) {
  val palette = LocalCivicStatusPalette.current
  val verification = record.locationVerifications.firstOrNull()
  val isVerified = record.geofenceStatus == LocationVerificationOutcome.VERIFIED.apiValue

  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = record.checkType.replace('_', ' '),
          style = MaterialTheme.typography.titleMedium,
        )
        StatusPill(
          label = record.geofenceStatus,
          containerColor =
            if (isVerified) palette.verifiedContainer else palette.errorContainer,
          contentColor = if (isVerified) paletteVerified else palette.error,
        )
      }
      Text(
        text = "Recorded: ${record.recordedAtIso}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      if (verification != null && verification.distanceMeters != null) {
        Text(
          text =
            "Distance %.1fm (limit %.0fm) • Accuracy ±%.1fm".format(
              verification.distanceMeters,
              verification.permittedRadiusMeters ?: 0.0,
              verification.accuracyMeters ?: 0.0,
            ),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (record.biometricVerifiedLocally) {
        Text(
          text = "Device Biometric Unlocked Locally",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}
