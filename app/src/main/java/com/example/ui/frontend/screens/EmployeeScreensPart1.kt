package com.example.ui.frontend.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.capabilities.BiometricCapabilityState
import com.example.core.capabilities.LocationPermissionState
import com.example.data.model.GeofenceStepStatus
import com.example.ui.components.PermissionBlockedBanner
import com.example.ui.components.ThreeStagePipelineCard
import com.example.ui.frontend.components.CivicAttendanceBarChart
import com.example.ui.frontend.components.CivicAttendanceStatusBadge
import com.example.ui.frontend.components.CivicDetailFieldRow
import com.example.ui.frontend.components.CivicEmptyState
import com.example.ui.frontend.components.CivicFilterChipRow
import com.example.ui.frontend.components.CivicGeofenceBoundaryCard
import com.example.ui.frontend.components.CivicNoticeBanner
import com.example.ui.frontend.components.CivicPill
import com.example.ui.frontend.components.CivicPrimaryButton
import com.example.ui.frontend.components.CivicProfileHeader
import com.example.ui.frontend.components.CivicQuickNavRow
import com.example.ui.frontend.components.CivicResponsiveScreenContainer
import com.example.ui.frontend.components.CivicSectionHeader
import com.example.ui.frontend.components.CivicSecondaryButton
import com.example.ui.frontend.components.CivicSummaryCard
import com.example.ui.frontend.data.CivicAttendanceStatus
import com.example.ui.frontend.data.CivicLeaveStatus
import com.example.ui.frontend.navigation.FrontendRoutes
import com.example.ui.frontend.state.FrontendUiState
import com.example.ui.theme.LocalCivicStatusPalette

// 1. Employee Dashboard
@Composable
fun EmployeeDashboardFrontendScreen(
  state: FrontendUiState,
  onNavigate: (String) -> Unit,
  onRefresh: () -> Unit,
  onDismissBanner: () -> Unit,
) {
  val palette = LocalCivicStatusPalette.current
  val emp = state.activeEmployee
  val empRecords = state.attendanceRecords.filter { emp == null || it.employeeId == emp.id }
  val empLeaves = state.leaveRequests.filter { emp == null || it.employeeId == emp.id }
  val caps = state.capabilities
  val locReadyLabel =
    when {
      caps == null -> "Inspecting location readiness..."
      caps.isLocationReady -> "GPS & Location permission ready"
      !caps.locationServicesEnabled -> "Location Services disabled"
      else -> "Location permission required at check-in"
    }
  val bioReadyLabel =
    when (val b = caps?.biometricCapability) {
      is BiometricCapabilityState.Available -> "Ready (${b.strengthLabel})"
      BiometricCapabilityState.NoneEnrolled -> "No biometric or screen lock enrolled"
      BiometricCapabilityState.NoHardware -> "No biometric sensor present"
      null -> "Inspecting BiometricManager..."
      else -> "Biometric unavailable on device"
    }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("employee_dashboard_screen"),
  ) {
    item {
      CivicProfileHeader(
        fullName = emp?.fullName ?: state.currentAccountName.ifBlank { state.currentAccountEmail },
        roleLabel = emp?.jobTitle ?: "Workforce Member",
        subtitle =
          "${emp?.employeeCode?.ifBlank { "Unassigned Code" } ?: "Employee"} • ${emp?.workplaceName ?: "Unassigned Workplace"}",
        badgeText = "EMPLOYEE PORTAL",
        avatarUrl = emp?.avatarUrl ?: state.currentAccountAvatarUrl,
      )
    }

    item {
      OutlinedButton(
        onClick = onRefresh,
        modifier = Modifier.fillMaxWidth().testTag("employee_refresh_button"),
      ) {
        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Sync Profile & Attendance from Supabase")
      }
    }

    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Today's Shift Status", style = MaterialTheme.typography.titleMedium)
            CivicPill(
              label = if (state.connectionReport.isConnected) "ONLINE" else "STATUS",
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
          Text(
            text = state.employeeShiftStatusLabel,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text =
              "Assigned Site: ${emp?.workplaceName ?: "Unassigned"} • Window: ${state.orgSettings.workStartTime} – ${state.orgSettings.workEndTime}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
              CivicPrimaryButton(
                text = "Check In",
                icon = Icons.AutoMirrored.Filled.Login,
                testTag = "emp_dash_checkin_button",
                onClick = { onNavigate(FrontendRoutes.EMPLOYEE_CHECK_IN) },
              )
            }
            Box(Modifier.weight(1f)) {
              CivicSecondaryButton(
                text = "Check Out",
                icon = Icons.AutoMirrored.Filled.Logout,
                testTag = "emp_dash_checkout_button",
                onClick = { onNavigate(FrontendRoutes.EMPLOYEE_CHECK_OUT) },
              )
            }
          }
        }
      }
    }

    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CivicSummaryCard(
          title = "Punctuality",
          value = if (empRecords.isEmpty()) "--" else "${emp?.attendanceRatePercent ?: 100}%",
          subtitle = "${empRecords.size} Supabase log(s)",
          icon = Icons.Filled.AccessTime,
          accentColor = palette.verified,
          onClick = { onNavigate(FrontendRoutes.EMPLOYEE_ATTENDANCE_HISTORY) },
          modifier = Modifier.weight(1f),
        )
        CivicSummaryCard(
          title = "Leave Requests",
          value = empLeaves.count { it.status == CivicLeaveStatus.PENDING }.toString(),
          subtitle = "Pending request(s)",
          icon = Icons.Filled.EventBusy,
          accentColor = palette.warning,
          onClick = { onNavigate(FrontendRoutes.EMPLOYEE_LEAVE_HISTORY) },
          modifier = Modifier.weight(1f),
        )
      }
    }

    item {
      CivicAttendanceBarChart(
        title = "Personal Weekly Punctuality",
        subtitle = "Verified check-ins vs late/exception entries",
        bars = state.personalWeeklyChart,
        primaryLabel = "On Time",
        secondaryLabel = "Late / Exception",
      )
    }

    item { CivicSectionHeader(title = "Self-Service Modules") }

    item {
      CivicQuickNavRow(
        title = "Location & Geofence Check",
        subtitle = locReadyLabel,
        icon = Icons.Filled.LocationOn,
        testTag = "emp_nav_location",
        onClick = { onNavigate(FrontendRoutes.EMPLOYEE_LOCATION_PRESENTATION) },
      )
    }
    item {
      CivicQuickNavRow(
        title = "Biometric Security & Test",
        subtitle = bioReadyLabel,
        icon = Icons.Filled.Fingerprint,
        testTag = "emp_nav_biometric",
        onClick = { onNavigate(FrontendRoutes.EMPLOYEE_BIOMETRIC_PRESENTATION) },
      )
    }
    item {
      CivicQuickNavRow(
        title = "Attendance History",
        subtitle = "${empRecords.size} attendance log(s)",
        icon = Icons.Filled.History,
        testTag = "emp_nav_history",
        onClick = { onNavigate(FrontendRoutes.EMPLOYEE_ATTENDANCE_HISTORY) },
      )
    }
    item {
      CivicQuickNavRow(
        title = "Work Schedule",
        subtitle = "Shift timetable & late grace window",
        icon = Icons.Filled.Schedule,
        testTag = "emp_nav_schedule",
        onClick = { onNavigate(FrontendRoutes.EMPLOYEE_WORK_SCHEDULE) },
      )
    }
    item {
      CivicQuickNavRow(
        title = "Apply for Leave",
        subtitle = "Submit a leave application to Supabase",
        icon = Icons.Filled.CalendarMonth,
        testTag = "emp_nav_leave_apply",
        onClick = { onNavigate(FrontendRoutes.EMPLOYEE_LEAVE_APPLICATION) },
      )
    }
    item {
      CivicQuickNavRow(
        title = "Leave History",
        subtitle = "${empLeaves.size} submitted request(s)",
        icon = Icons.Filled.EventBusy,
        testTag = "emp_nav_leave_history",
        onClick = { onNavigate(FrontendRoutes.EMPLOYEE_LEAVE_HISTORY) },
      )
    }
    item {
      CivicQuickNavRow(
        title = "Notifications",
        subtitle = "${state.unreadNotificationsCountForActiveRole} unread alert(s)",
        icon = Icons.Filled.Notifications,
        testTag = "emp_nav_notifications",
        onClick = { onNavigate(FrontendRoutes.EMPLOYEE_NOTIFICATIONS) },
      )
    }
    item {
      CivicQuickNavRow(
        title = "My Documents & Photo",
        subtitle = "${state.employeeDocuments.size} uploaded record(s)",
        icon = Icons.Filled.Description,
        testTag = "emp_nav_documents",
        onClick = { onNavigate(FrontendRoutes.EMPLOYEE_DOCUMENTS) },
      )
    }
    item {
      CivicQuickNavRow(
        title = "Employee Profile",
        subtitle = emp?.email ?: state.currentAccountEmail,
        icon = Icons.Filled.Person,
        testTag = "emp_nav_profile",
        onClick = { onNavigate(FrontendRoutes.EMPLOYEE_PROFILE) },
      )
    }
    item {
      CivicQuickNavRow(
        title = "App Settings & Connection",
        subtitle = "Theme mode, biometrics & Supabase status",
        icon = Icons.Filled.Settings,
        testTag = "emp_nav_settings",
        onClick = { onNavigate(FrontendRoutes.EMPLOYEE_SETTINGS) },
      )
    }
  }
}

// 2. Live Employee Check-In Screen
@Composable
fun EmployeeCheckInScreen(
  state: FrontendUiState,
  onStartCheckIn: () -> Unit,
  onRetrySync: () -> Unit,
  onOpenBlockedSettings: () -> Unit,
  onDismissBlockedBanner: () -> Unit,
  onOpenLocationDiagnostics: () -> Unit,
  onOpenBiometricSettings: () -> Unit,
  onDismissBanner: () -> Unit,
) {
  val emp = state.activeEmployee
  val wp =
    state.workplaces.firstOrNull { emp?.workplaceId?.isNotBlank() == true && it.id == emp.workplaceId }
      ?: state.workplaces.firstOrNull()

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("employee_check_in_screen"),
  ) {
    item { Text("Employee Check-In", style = MaterialTheme.typography.headlineMedium) }

    state.permissionBlockedMessage?.let { blockedMsg ->
      item {
        PermissionBlockedBanner(
          message = blockedMsg,
          requiresLocationSettings = state.blockedPermissionRequiresLocationSettings,
          onOpenSettings = onOpenBlockedSettings,
          onDismiss = onDismissBlockedBanner,
        )
      }
    }

    if (wp == null) {
      item {
        CivicNoticeBanner(
          title = "No Assigned Workplace Geofence",
          message =
            "Your employee profile does not yet have an assigned workplace with coordinates. Ask your Organisation Owner to assign a workplace before checking in.",
          isWarning = true,
        )
      }
    }

    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text("Assigned Shift Context", style = MaterialTheme.typography.titleMedium)
          CivicDetailFieldRow(
            "Employee",
            "${emp?.fullName ?: state.currentAccountName} (${emp?.employeeCode ?: "--"})",
          )
          CivicDetailFieldRow("Workplace", wp?.name ?: "Unassigned")
          CivicDetailFieldRow(
            "Geofence Target",
            wp?.let { "${it.geofenceRadiusMeters}m radius (±${it.maxAccuracyMeters}m accuracy)" }
              ?: "Not configured",
          )
          CivicDetailFieldRow(
            "Biometric Policy",
            if (state.orgSettings.requireBiometric) "Required before check-in" else "Optional",
            showDivider = false,
          )
        }
      }
    }

    item {
      ThreeStagePipelineCard(
        biometricStep = state.biometricStep,
        geofenceStep = state.geofenceStep,
        submissionStep = state.submissionStep,
        onRetrySync = onRetrySync,
      )
    }

    item {
      CivicPrimaryButton(
        text = "Verify & Check In",
        icon = Icons.AutoMirrored.Filled.Login,
        enabled = wp != null && !state.isSubmitting,
        testTag = "checkin_verify_button",
        onClick = onStartCheckIn,
      )
    }

    item {
      CivicQuickNavRow(
        title = "Inspect GPS & Geofence Readiness",
        subtitle = "View Android location permission & test live coordinates",
        icon = Icons.Filled.LocationOn,
        testTag = "checkin_open_location_stage",
        onClick = onOpenLocationDiagnostics,
      )
    }
    item {
      CivicQuickNavRow(
        title = "Biometric Hardware Status",
        subtitle = "Inspect BiometricManager availability & test prompt",
        icon = Icons.Filled.Fingerprint,
        testTag = "checkin_open_biometric_stage",
        onClick = onOpenBiometricSettings,
      )
    }
  }
}

// 3. Live Employee Check-Out Screen
@Composable
fun EmployeeCheckOutScreen(
  state: FrontendUiState,
  onStartCheckOut: () -> Unit,
  onRetrySync: () -> Unit,
  onOpenBlockedSettings: () -> Unit,
  onDismissBlockedBanner: () -> Unit,
  onOpenLocationDiagnostics: () -> Unit,
  onOpenBiometricSettings: () -> Unit,
  onDismissBanner: () -> Unit,
) {
  val emp = state.activeEmployee
  val wp =
    state.workplaces.firstOrNull { emp?.workplaceId?.isNotBlank() == true && it.id == emp.workplaceId }
      ?: state.workplaces.firstOrNull()
  val lastCheckIn = state.attendanceRecords.firstOrNull { it.checkInTime != null }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("employee_check_out_screen"),
  ) {
    item { Text("Employee Check-Out", style = MaterialTheme.typography.headlineMedium) }

    state.permissionBlockedMessage?.let { blockedMsg ->
      item {
        PermissionBlockedBanner(
          message = blockedMsg,
          requiresLocationSettings = state.blockedPermissionRequiresLocationSettings,
          onOpenSettings = onOpenBlockedSettings,
          onDismiss = onDismissBlockedBanner,
        )
      }
    }

    if (wp == null) {
      item {
        CivicNoticeBanner(
          title = "No Assigned Workplace Geofence",
          message =
            "Your employee profile does not yet have an assigned workplace. Ask your administrator to assign a site.",
          isWarning = true,
        )
      }
    }

    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text("Active Shift Summary", style = MaterialTheme.typography.titleMedium)
          CivicDetailFieldRow("Current Status", state.employeeShiftStatusLabel)
          CivicDetailFieldRow("Assigned Workplace", wp?.name ?: "Unassigned")
          CivicDetailFieldRow(
            "Latest Check-In",
            lastCheckIn?.let { "${it.dateLabel} at ${it.checkInTime}" } ?: "No recent check-in log",
            showDivider = false,
          )
        }
      }
    }

    item {
      ThreeStagePipelineCard(
        biometricStep = state.biometricStep,
        geofenceStep = state.geofenceStep,
        submissionStep = state.submissionStep,
        onRetrySync = onRetrySync,
      )
    }

    item {
      CivicPrimaryButton(
        text = "Verify & Check Out",
        icon = Icons.AutoMirrored.Filled.Logout,
        enabled = wp != null && !state.isSubmitting,
        testTag = "checkout_verify_button",
        onClick = onStartCheckOut,
      )
    }

    item {
      CivicQuickNavRow(
        title = "Inspect GPS & Geofence Readiness",
        subtitle = "View Android location permission & live fix status",
        icon = Icons.Filled.LocationOn,
        onClick = onOpenLocationDiagnostics,
      )
    }
    item {
      CivicQuickNavRow(
        title = "Biometric Hardware Status",
        subtitle = "Inspect BiometricManager availability",
        icon = Icons.Filled.Fingerprint,
        onClick = onOpenBiometricSettings,
      )
    }
  }
}

// 4. Live Location Verification Diagnostics Screen
@Composable
fun EmployeeLocationVerificationScreen(
  state: FrontendUiState,
  onRunLiveLocationCheck: () -> Unit,
  onOpenLocationSettings: () -> Unit,
  onOpenAppSettings: () -> Unit,
  onDismissBlockedBanner: () -> Unit,
  onDismissBanner: () -> Unit,
) {
  val emp = state.activeEmployee
  val wp =
    state.workplaces.firstOrNull { emp?.workplaceId?.isNotBlank() == true && it.id == emp.workplaceId }
      ?: state.workplaces.firstOrNull()
  val caps = state.capabilities
  val step = state.geofenceStep

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("employee_location_presentation_screen"),
  ) {
    item { Text("Location & Geofence Verification", style = MaterialTheme.typography.headlineMedium) }

    state.permissionBlockedMessage?.let { blockedMsg ->
      item {
        PermissionBlockedBanner(
          message = blockedMsg,
          requiresLocationSettings = state.blockedPermissionRequiresLocationSettings,
          onOpenSettings =
            if (state.blockedPermissionRequiresLocationSettings) onOpenLocationSettings
            else onOpenAppSettings,
          onDismiss = onDismissBlockedBanner,
        )
      }
    }

    if (wp != null) {
      item {
        CivicGeofenceBoundaryCard(
          workplaceName = wp.name,
          radiusMeters = wp.geofenceRadiusMeters,
          latitude = wp.latitude,
          longitude = wp.longitude,
          isAttentionFlagged =
            step is GeofenceStepStatus.OutsideGeofence ||
              step is GeofenceStepStatus.InsufficientAccuracy ||
              step is GeofenceStepStatus.ServicesDisabled ||
              step is GeofenceStepStatus.PermissionDenied,
        )
      }
    } else {
      item {
        CivicNoticeBanner(
          title = "No Workplace Coordinates Assigned",
          message = "Once assigned to a workplace, live GPS fixes are compared against its geofence radius.",
          isWarning = true,
        )
      }
    }

    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Live Android Location Diagnostics", style = MaterialTheme.typography.titleMedium)
          val permText =
            when (caps?.locationPermission) {
              LocationPermissionState.FINE_GRANTED -> "Granted (ACCESS_FINE_LOCATION)"
              LocationPermissionState.COARSE_ONLY_GRANTED -> "Approximate Only (ACCESS_COARSE_LOCATION)"
              LocationPermissionState.DENIED_CAN_REQUEST -> "Not Yet Granted"
              LocationPermissionState.PERMANENTLY_DENIED -> "Permanently Denied"
              null -> "Checking..."
            }
          CivicDetailFieldRow("Runtime Permission", permText)
          CivicDetailFieldRow(
            "Location Services (GPS)",
            if (caps?.locationServicesEnabled == true) "Enabled" else "Disabled",
          )
          val stepSummary =
            when (step) {
              GeofenceStepStatus.Idle -> "Tap below to acquire a real FusedLocationProvider fix"
              GeofenceStepStatus.AcquiringLocation -> "Acquiring live GPS fix..."
              is GeofenceStepStatus.Verified ->
                "Verified inside radius: %.1fm from site (±%.1fm accuracy, %s)".format(
                  step.distanceMeters,
                  step.accuracyMeters,
                  step.timestampIso,
                )
              is GeofenceStepStatus.OutsideGeofence ->
                "Outside radius: %.1fm from site (limit %.0fm, ±%.1fm accuracy)".format(
                  step.distanceMeters,
                  step.permittedRadiusMeters,
                  step.accuracyMeters,
                )
              is GeofenceStepStatus.InsufficientAccuracy ->
                "Insufficient GPS accuracy: ±%.1fm (max allowed ±%.0fm)".format(
                  step.accuracyMeters,
                  step.maxAllowedMeters,
                )
              is GeofenceStepStatus.LocationUnavailable -> step.reason
              is GeofenceStepStatus.PermissionDenied ->
                if (step.isPermanent) "Permanently denied in Android Settings" else "Permission denied"
              GeofenceStepStatus.ServicesDisabled -> "Device Location Services turned off"
            }
          CivicDetailFieldRow("Latest Live Fix", stepSummary, showDivider = false)
        }
      }
    }

    item {
      CivicPrimaryButton(
        text =
          if (step is GeofenceStepStatus.AcquiringLocation) "Acquiring Live GPS..."
          else "Test Live Location Check",
        icon = Icons.Filled.LocationOn,
        enabled = step !is GeofenceStepStatus.AcquiringLocation,
        testTag = "emp_run_live_location_button",
        onClick = onRunLiveLocationCheck,
      )
    }

    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f)) {
          CivicSecondaryButton(text = "Location Settings", onClick = onOpenLocationSettings)
        }
        Box(Modifier.weight(1f)) {
          CivicSecondaryButton(text = "App Settings", onClick = onOpenAppSettings)
        }
      }
    }
  }
}

// 5. Live Biometric Confirmation & Device Security Screen
@Composable
fun EmployeeBiometricConfirmationScreen(
  state: FrontendUiState,
  onToggleBiometricLogin: (Boolean) -> Unit,
  onTestBiometricPrompt: () -> Unit,
  onOpenBiometricEnroll: () -> Unit,
  onDismissBanner: () -> Unit,
) {
  val bioCap = state.capabilities?.biometricCapability

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("employee_biometric_presentation_screen"),
  ) {
    item { Text("Biometric Security & BiometricPrompt", style = MaterialTheme.typography.headlineMedium) }

    item {
      CivicNoticeBanner(
        title = "Android Keystore Hardware Isolation",
        message =
          "Biometric verification uses AndroidX BiometricPrompt and remains strictly local to your device hardware. Biometric templates never leave Android Keystore.",
      )
    }

    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("BiometricManager Hardware Status", style = MaterialTheme.typography.titleMedium)
          val statusLabel =
            when (bioCap) {
              is BiometricCapabilityState.Available -> "Ready (${bioCap.strengthLabel})"
              BiometricCapabilityState.NoneEnrolled -> "Hardware present, no credentials enrolled"
              BiometricCapabilityState.NoHardware -> "No biometric hardware detected"
              BiometricCapabilityState.HardwareUnavailable -> "Sensor temporarily busy or unavailable"
              BiometricCapabilityState.SecurityUpdateRequired -> "System security update required"
              BiometricCapabilityState.Unsupported -> "Unsupported on this device configuration"
              null -> "Checking BiometricManager..."
            }
          CivicDetailFieldRow("Hardware Readiness", statusLabel)
          CivicDetailFieldRow(
            "Attendance Requirement",
            if (state.orgSettings.requireBiometric) "Required by Organisation Settings" else "Optional by Policy",
          )
          state.settingsBiometricTestMessage?.let { msg ->
            CivicDetailFieldRow("Latest Prompt Result", msg, showDivider = false)
          } ?: CivicDetailFieldRow("Latest Prompt Result", "Not tested in this session", showDivider = false)
        }
      }
    }

    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          SettingToggleRow(
            title = "Biometric Quick-Unlock",
            subtitle = "Use OS BiometricPrompt on Welcome screen to unlock your saved Supabase session",
            checked = state.biometricLoginEnabled,
            onCheckedChange = onToggleBiometricLogin,
          )
          CivicPrimaryButton(
            text = "Test OS BiometricPrompt",
            icon = Icons.Filled.CheckCircle,
            testTag = "emp_test_biometric_button",
            onClick = onTestBiometricPrompt,
          )
          CivicSecondaryButton(
            text = "Open Device Security Settings",
            onClick = onOpenBiometricEnroll,
          )
        }
      }
    }
  }
}

// 6. Attendance History (Employee)
@Composable
fun EmployeeAttendanceHistoryScreen(
  state: FrontendUiState,
  onOpenRecordDetails: (String) -> Unit,
  onDismissBanner: () -> Unit,
) {
  val empId = state.activeEmployee?.id
  var selectedStatus by rememberSaveable { mutableStateOf("All") }
  val statusOptions = listOf("All") + CivicAttendanceStatus.entries.map { it.label }

  val filtered =
    state.attendanceRecords.filter { rec ->
      (empId == null || rec.employeeId == empId) &&
        (selectedStatus == "All" || rec.status.label == selectedStatus)
    }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("employee_attendance_history_screen"),
  ) {
    item { Text("My Attendance History", style = MaterialTheme.typography.headlineMedium) }
    item {
      CivicFilterChipRow(
        options = statusOptions,
        selectedOption = selectedStatus,
        onOptionSelected = { selectedStatus = it },
      )
    }

    if (filtered.isEmpty()) {
      item {
        CivicEmptyState(
          icon = Icons.Filled.History,
          title = "No matching attendance logs",
          description = "Check in or select 'All' to view your recorded attendance entries.",
          actionLabel = if (selectedStatus != "All") "Show All" else null,
          onAction = if (selectedStatus != "All") ({ selectedStatus = "All" }) else null,
        )
      }
    } else {
      items(filtered, key = { it.id }) { rec ->
        Card(
          onClick = { onOpenRecordDetails(rec.id) },
          modifier = Modifier.fillMaxWidth().testTag("emp_attendance_row_${rec.id}"),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(rec.dateLabel, style = MaterialTheme.typography.titleMedium)
              Text(
                text = "${rec.workplaceName} • In: ${rec.checkInTime ?: "--"} • Out: ${rec.checkOutTime ?: "--"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            CivicAttendanceStatusBadge(status = rec.status)
          }
        }
      }
    }
  }
}

// 7. Attendance Record Details (Employee)
@Composable
fun EmployeeAttendanceDetailScreen(
  state: FrontendUiState,
  recordId: String,
  onDismissBanner: () -> Unit,
) {
  val empId = state.activeEmployee?.id
  val record =
    state.attendanceRecords.firstOrNull { it.id == recordId }
      ?: state.attendanceRecords.firstOrNull { empId == null || it.employeeId == empId }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("employee_attendance_detail_screen"),
  ) {
    if (record == null) {
      item {
        CivicEmptyState(
          icon = Icons.Filled.Badge,
          title = "Attendance record not found",
          description = "Select an entry from Attendance History.",
        )
      }
    } else {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(record.dateLabel, style = MaterialTheme.typography.headlineMedium)
              CivicAttendanceStatusBadge(status = record.status)
            }
            CivicDetailFieldRow("Workplace", record.workplaceName)
            CivicDetailFieldRow("Check Type", record.checkType)
            CivicDetailFieldRow("Check-In Time", record.checkInTime ?: "Not logged")
            CivicDetailFieldRow("Check-Out Time", record.checkOutTime ?: "Not logged")
            CivicDetailFieldRow("Location Verification", record.locationNote)
            record.distanceMeters?.let { dist ->
              CivicDetailFieldRow("Measured Distance", "%.1f metres".format(dist))
            }
            record.accuracyMeters?.let { acc ->
              CivicDetailFieldRow("GPS Accuracy", "±%.1f metres".format(acc))
            }
            CivicDetailFieldRow("Device Biometric", record.biometricNote)
            CivicDetailFieldRow(
              "Supervisor Review",
              record.reviewedBy ?: "No exception review recorded",
              showDivider = false,
            )
          }
        }
      }
    }
  }
}
