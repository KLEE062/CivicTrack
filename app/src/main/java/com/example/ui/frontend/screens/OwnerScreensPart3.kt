package com.example.ui.frontend.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.frontend.components.CivicAttendanceBarChart
import com.example.ui.frontend.components.CivicDataTableCard
import com.example.ui.frontend.components.CivicDetailFieldRow
import com.example.ui.frontend.components.CivicEmptyState
import com.example.ui.frontend.components.CivicFilterChipRow
import com.example.ui.frontend.components.CivicFormField
import com.example.ui.frontend.components.CivicNoticeBanner
import com.example.ui.frontend.components.CivicNotificationCard
import com.example.ui.frontend.components.CivicPill
import com.example.ui.frontend.components.CivicPrimaryButton
import com.example.ui.frontend.components.CivicProfileHeader
import com.example.ui.frontend.components.CivicResponsiveScreenContainer
import com.example.ui.frontend.components.CivicSearchBar
import com.example.ui.frontend.components.CivicSecondaryButton
import com.example.ui.frontend.components.CivicSummaryCard
import com.example.ui.frontend.components.SupabaseConnectionCard
import com.example.ui.frontend.data.CivicAttendanceStatus
import com.example.ui.frontend.data.CivicRole
import com.example.ui.frontend.data.CivicThemeMode
import com.example.ui.frontend.state.FrontendUiState

// 14. Reports and Analytics
@Composable
fun OwnerReportsAnalyticsScreen(
  state: FrontendUiState,
  onDismissBanner: () -> Unit,
) {
  val totalRecs = state.attendanceRecords.size
  val onTimeRecs = state.attendanceRecords.count { it.status == CivicAttendanceStatus.ON_TIME }
  val avgPunctualityLabel =
    if (totalRecs == 0) "--" else "${((onTimeRecs.toDouble() / totalRecs) * 100).toInt()}%"
  val exceptionCount =
    state.attendanceRecords.count { it.status == CivicAttendanceStatus.EXCEPTION }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("owner_reports_screen"),
  ) {
    item { Text("Reports & Analytics", style = MaterialTheme.typography.headlineMedium) }

    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CivicSummaryCard(
          title = "Punctual Rate",
          value = avgPunctualityLabel,
          subtitle = "From $totalRecs Supabase logs",
          icon = Icons.Filled.Assessment,
          modifier = Modifier.weight(1f),
        )
        CivicSummaryCard(
          title = "Exceptions",
          value = exceptionCount.toString(),
          subtitle = "Geofence/incomplete flags",
          icon = Icons.Filled.Assessment,
          modifier = Modifier.weight(1f),
        )
      }
    }

    item {
      CivicAttendanceBarChart(
        title = "Department Compliance Breakdown",
        subtitle = "Verified punctual attendance rate per department",
        bars = state.departmentComplianceChart,
        primaryLabel = "Punctual %",
        secondaryLabel = null,
      )
    }

    if (state.workplaces.isEmpty()) {
      item {
        CivicEmptyState(
          icon = Icons.Filled.Assessment,
          title = "No workplaces configured",
          description = "Register workplaces to display per-site staffing and geofence tolerances.",
        )
      }
    } else {
      item {
        CivicDataTableCard(
          title = "Site Summary Matrix",
          headers = listOf("Workplace", "Staff", "Radius"),
          rows = state.workplaces.map { listOf(it.name, "${it.assignedStaffCount}", "${it.geofenceRadiusMeters}m") },
        )
      }
    }
  }
}

// 15. Notification Centre (Owner)
@Composable
fun OwnerNotificationCentreScreen(
  state: FrontendUiState,
  onMarkRead: (String) -> Unit,
  onMarkAllRead: () -> Unit,
  onDismissBanner: () -> Unit,
) {
  var filter by rememberSaveable { mutableStateOf("All") }
  val ownerNotifications =
    state.notifications.filter {
      it.targetRole == CivicRole.OWNER && (filter == "All" || !it.isRead)
    }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("owner_notifications_screen"),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Notification Centre", style = MaterialTheme.typography.headlineMedium)
        TextButton(onClick = onMarkAllRead) { Text("Mark All Read") }
      }
    }
    item {
      CivicFilterChipRow(
        options = listOf("All", "Unread"),
        selectedOption = filter,
        onOptionSelected = { filter = it },
      )
    }

    if (ownerNotifications.isEmpty()) {
      item {
        CivicEmptyState(
          icon = Icons.Filled.Notifications,
          title = "No notifications",
          description = "Live alerts from leave requests, attendance exceptions, and audit events appear here.",
        )
      }
    } else {
      items(ownerNotifications, key = { it.id }) { notif ->
        CivicNotificationCard(notification = notif, onMarkRead = { onMarkRead(notif.id) })
      }
    }
  }
}

// 16. Audit Activity (Owner)
@Composable
fun OwnerAuditActivityScreen(
  state: FrontendUiState,
  onDismissBanner: () -> Unit,
) {
  var query by rememberSaveable { mutableStateOf("") }
  val filtered =
    state.auditEvents.filter {
      query.isBlank() ||
        it.action.contains(query, ignoreCase = true) ||
        it.summary.contains(query, ignoreCase = true) ||
        it.actorName.contains(query, ignoreCase = true)
    }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("owner_audit_screen"),
  ) {
    item { Text("Audit Activity Trail", style = MaterialTheme.typography.headlineMedium) }
    item {
      CivicSearchBar(
        query = query,
        onQueryChange = { query = it },
        placeholder = "Filter audit actions, actors or summaries...",
      )
    }

    if (filtered.isEmpty()) {
      item {
        CivicEmptyState(
          icon = Icons.Filled.History,
          title = "No matching audit entries",
          description = "Recorded organisation governance events appear here.",
        )
      }
    } else {
      items(filtered, key = { it.id }) { event ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text(event.action, style = MaterialTheme.typography.titleMedium)
              CivicPill(
                label = event.category,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
              )
            }
            Text(event.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${event.actorName} • ${event.timestampLabel}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }
    }
  }
}

// 17. Organisation Settings
@Composable
fun OwnerOrgSettingsScreen(
  state: FrontendUiState,
  onSaveOrgSettings: (
    startTime: String,
    endTime: String,
    graceMinutes: Int,
    workingDays: List<String>,
    requireLocation: Boolean,
    requireBiometric: Boolean,
    allowOutsideGeofence: Boolean,
  ) -> Unit,
  onThemeChange: (CivicThemeMode) -> Unit,
  onDismissBanner: () -> Unit,
) {
  val settings = state.orgSettings

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("owner_settings_screen"),
  ) {
    item { Text("Organisation Settings", style = MaterialTheme.typography.headlineMedium) }
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Attendance & Accountability Policies", style = MaterialTheme.typography.titleMedium)
          SettingToggleRow(
            title = "Require Location Verification",
            subtitle = "Enforce workplace geofence checks on employee check-in",
            checked = settings.requireLocation,
            onCheckedChange = { v ->
              onSaveOrgSettings(
                settings.workStartTime,
                settings.workEndTime,
                settings.lateGraceMinutes,
                settings.workingDays,
                v,
                settings.requireBiometric,
                settings.allowOutsideGeofenceWithNote,
              )
            },
          )
          SettingToggleRow(
            title = "Require Biometric Confirmation",
            subtitle = "Require local OS biometric unlock before attendance capture",
            checked = settings.requireBiometric,
            onCheckedChange = { v ->
              onSaveOrgSettings(
                settings.workStartTime,
                settings.workEndTime,
                settings.lateGraceMinutes,
                settings.workingDays,
                settings.requireLocation,
                v,
                settings.allowOutsideGeofenceWithNote,
              )
            },
          )
          SettingToggleRow(
            title = "Allow Off-Site Check-In with Exception",
            subtitle = "Route outside-geofence submissions to supervisor exception queue",
            checked = settings.allowOutsideGeofenceWithNote,
            onCheckedChange = { v ->
              onSaveOrgSettings(
                settings.workStartTime,
                settings.workEndTime,
                settings.lateGraceMinutes,
                settings.workingDays,
                settings.requireLocation,
                settings.requireBiometric,
                v,
              )
            },
          )
        }
      }
    }

    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Application Theme", style = MaterialTheme.typography.titleMedium)
          CivicFilterChipRow(
            options = CivicThemeMode.entries.map { it.label },
            selectedOption = state.themeMode.label,
            onOptionSelected = { label ->
              CivicThemeMode.entries.firstOrNull { it.label == label }?.let(onThemeChange)
            },
          )
        }
      }
    }
  }
}

@Composable
fun SettingToggleRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(title, style = MaterialTheme.typography.titleMedium)
      Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}

// 18. Workspace Capacity & Backend Status
@Composable
fun OwnerBillingScreen(
  state: FrontendUiState,
  onRecheckConnection: () -> Unit,
  onDismissBanner: () -> Unit,
) {
  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("owner_billing_screen"),
  ) {
    item { Text("Workspace Capacity & Backend Status", style = MaterialTheme.typography.headlineMedium) }
    item {
      SupabaseConnectionCard(
        report = state.connectionReport,
        onRecheck = onRecheckConnection,
        isChecking = state.isCheckingConnection,
      )
    }
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            CivicPill(
              label = "SUPABASE WORKSPACE",
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
          CivicDetailFieldRow("Organisation", state.organization?.name ?: "Not Loaded")
          CivicDetailFieldRow("Registered Employees", state.employees.size.toString())
          CivicDetailFieldRow("Supervisors & Pending Invites", state.supervisors.size.toString())
          CivicDetailFieldRow("Departments", state.departments.size.toString())
          CivicDetailFieldRow("Geofenced Workplaces", state.workplaces.size.toString())
          CivicDetailFieldRow("Attendance Records Loaded", state.attendanceRecords.size.toString(), showDivider = false)
        }
      }
    }
    item {
      CivicNoticeBanner(
        title = "External Billing Not Configured",
        message = "CivicTrack operates directly against your connected Supabase project without an external payment gateway integration.",
      )
    }
  }
}

// 19. Owner Profile
@Composable
fun OwnerProfileScreen(
  state: FrontendUiState,
  onSaveProfile: (String, String) -> Unit,
  onSignOut: () -> Unit,
  onDismissBanner: () -> Unit,
) {
  var fullName by rememberSaveable(state.currentAccountName) { mutableStateOf(state.currentAccountName) }
  var phone by rememberSaveable(state.currentAccountPhone) { mutableStateOf(state.currentAccountPhone) }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("owner_profile_screen"),
  ) {
    item {
      CivicProfileHeader(
        fullName = state.currentAccountName.ifBlank { state.currentAccountEmail },
        roleLabel = CivicRole.OWNER.label,
        subtitle = state.currentAccountEmail,
        badgeText = "ADMINISTRATOR",
        avatarUrl = state.currentAccountAvatarUrl,
      )
    }
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Account Information", style = MaterialTheme.typography.titleMedium)
          CivicFormField(value = fullName, onValueChange = { fullName = it }, label = "Full Name", enabled = !state.isSubmitting)
          CivicFormField(value = state.currentAccountEmail, onValueChange = {}, label = "Email", readOnly = true, enabled = false)
          CivicFormField(value = phone, onValueChange = { phone = it }, label = "Phone Number", enabled = !state.isSubmitting)
          CivicPrimaryButton(
            text = if (state.isSubmitting) "Saving Profile..." else "Update Profile",
            enabled = !state.isSubmitting,
            onClick = { onSaveProfile(fullName, phone) },
          )
        }
      }
    }
    item {
      CivicSecondaryButton(
        text = "Sign Out to Entry Screen",
        icon = Icons.AutoMirrored.Filled.Logout,
        isDestructive = true,
        testTag = "owner_profile_sign_out_button",
        onClick = onSignOut,
      )
    }
  }
}
