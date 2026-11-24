package com.example.ui.frontend.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.frontend.components.CivicConfirmationDialog
import com.example.ui.frontend.components.CivicDataTableCard
import com.example.ui.frontend.components.CivicDetailFieldRow
import com.example.ui.frontend.components.CivicEmptyState
import com.example.ui.frontend.components.CivicFilterChipRow
import com.example.ui.frontend.components.CivicFormField
import com.example.ui.frontend.components.CivicGeofenceBoundaryCard
import com.example.ui.frontend.components.CivicLeaveStatusBadge
import com.example.ui.frontend.components.CivicNoticeBanner
import com.example.ui.frontend.components.CivicNotificationCard
import com.example.ui.frontend.components.CivicPrimaryButton
import com.example.ui.frontend.components.CivicProfileHeader
import com.example.ui.frontend.components.CivicResponsiveScreenContainer
import com.example.ui.frontend.components.CivicSecondaryButton
import com.example.ui.frontend.components.CivicSummaryCard
import com.example.ui.frontend.components.SupabaseConnectionCard
import com.example.ui.frontend.data.CivicLeaveStatus
import com.example.ui.frontend.data.CivicRole
import com.example.ui.frontend.data.CivicThemeMode
import com.example.ui.frontend.state.FrontendUiState

// 7. Leave Request Details (Supervisor)
@Composable
fun SupervisorLeaveDetailScreen(
  state: FrontendUiState,
  leaveId: String,
  onDecideLeave: (String, Boolean) -> Unit,
  onDismissBanner: () -> Unit,
) {
  val leave =
    state.scopedSupervisorLeaves.firstOrNull { it.id == leaveId }
      ?: state.scopedSupervisorLeaves.firstOrNull()
  var pendingDecision by rememberSaveable { mutableStateOf<Boolean?>(null) }

  if (leave != null && pendingDecision != null) {
    val approve = pendingDecision == true
    CivicConfirmationDialog(
      title = if (approve) "Approve Team Leave?" else "Reject Team Leave?",
      message =
        if (approve) "Approve ${leave.daysCount} day(s) of ${leave.leaveType} for ${leave.employeeName} in Supabase?"
        else "Reject ${leave.employeeName}'s ${leave.leaveType} request in Supabase?",
      confirmLabel = if (approve) "Approve" else "Reject",
      isDestructive = !approve,
      onConfirm = {
        onDecideLeave(leave.id, approve)
        pendingDecision = null
      },
      onDismiss = { pendingDecision = null },
    )
  }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("supervisor_leave_detail_screen"),
  ) {
    if (!state.leaveFeatureAvailable) {
      item {
        CivicNoticeBanner(
          title = "Leave Module Not Provisioned",
          message = "The leave_requests table was not found on this Supabase project.",
          isWarning = true,
        )
      }
    }
    if (leave == null) {
      item {
        CivicEmptyState(
          icon = Icons.Filled.EventBusy,
          title = "Leave request unavailable",
          description = "Select a request from Team Leave Requests.",
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
              Text(leave.employeeName, style = MaterialTheme.typography.headlineMedium)
              CivicLeaveStatusBadge(status = leave.status)
            }
            CivicDetailFieldRow("Department", leave.departmentName)
            CivicDetailFieldRow("Leave Type", leave.leaveType)
            CivicDetailFieldRow("Period", "${leave.startDate} → ${leave.endDate}")
            CivicDetailFieldRow("Duration", "${leave.daysCount} working day(s)")
            CivicDetailFieldRow("Submitted", leave.submittedDate)
            CivicDetailFieldRow("Reason", leave.reason, showDivider = false)
          }
        }
      }

      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Supervisor Review Decision", style = MaterialTheme.typography.titleMedium)
            if (leave.status == CivicLeaveStatus.PENDING) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                  CivicPrimaryButton(
                    text = if (state.isSubmitting) "Saving..." else "Approve Leave",
                    icon = Icons.Filled.CheckCircle,
                    enabled = !state.isSubmitting,
                    testTag = "sup_approve_leave_button",
                    onClick = { pendingDecision = true },
                  )
                }
                Box(Modifier.weight(1f)) {
                  CivicSecondaryButton(
                    text = "Reject Leave",
                    isDestructive = true,
                    enabled = !state.isSubmitting,
                    testTag = "sup_reject_leave_button",
                    onClick = { pendingDecision = false },
                  )
                }
              }
            } else {
              CivicNoticeBanner(
                title = "Decision Recorded",
                message = "Status in Supabase is currently ${leave.status.label}.",
              )
            }
          }
        }
      }
    }
  }
}

// 8. Team Schedules
@Composable
fun SupervisorTeamSchedulesScreen(
  state: FrontendUiState,
  onDismissBanner: () -> Unit,
) {
  val sup = state.activeSupervisor
  val deptName = sup?.departmentName ?: "Assigned Department"
  val scopedSchedules =
    state.schedules.filter {
      it.departmentName == deptName || it.departmentName == "All Departments"
    }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("supervisor_schedules_screen"),
  ) {
    item { Text("Team Shift Schedules", style = MaterialTheme.typography.headlineMedium) }
    item {
      CivicNoticeBanner(
        title = "Department Shift Timetables",
        message =
          "Showing schedules governing $deptName. Shift policy edits are managed by the Organisation Owner.",
      )
    }

    if (scopedSchedules.isEmpty()) {
      item {
        CivicEmptyState(
          icon = Icons.Filled.Schedule,
          title = "No schedules configured",
          description = "No shift timetables are currently configured for $deptName.",
        )
      }
    } else {
      items(scopedSchedules, key = { it.id }) { sch ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(sch.title, style = MaterialTheme.typography.titleLarge)
            CivicDetailFieldRow("Department", sch.departmentName)
            CivicDetailFieldRow("Shift Hours", "${sch.startTime} – ${sch.endTime}")
            CivicDetailFieldRow("Working Days", sch.workingDays.joinToString(", "))
            CivicDetailFieldRow("Late Grace Window", "${sch.lateGraceMinutes} minutes", showDivider = false)
          }
        }
      }
    }
  }
}

// 9. Assigned Workplace Information
@Composable
fun SupervisorWorkplaceInfoScreen(
  state: FrontendUiState,
  onDismissBanner: () -> Unit,
) {
  val sup = state.activeSupervisor
  val workplace =
    state.workplaces.firstOrNull { sup?.workplaceId?.isNotBlank() == true && it.id == sup.workplaceId }
      ?: state.workplaces.firstOrNull()

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("supervisor_workplace_screen"),
  ) {
    item { Text("Assigned Workplace Information", style = MaterialTheme.typography.headlineMedium) }
    item {
      CivicNoticeBanner(
        title = "Read-Only Site Boundaries",
        message =
          "Geofence coordinates and tolerance limits are configured centrally by the Organisation Owner.",
      )
    }

    if (workplace == null) {
      item {
        CivicEmptyState(
          icon = Icons.Filled.LocationOn,
          title = "No workplace assigned",
          description = "Contact your Organisation Owner for workplace scope assignment.",
        )
      }
    } else {
      item {
        CivicGeofenceBoundaryCard(
          workplaceName = workplace.name,
          radiusMeters = workplace.geofenceRadiusMeters,
          latitude = workplace.latitude,
          longitude = workplace.longitude,
        )
      }
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(workplace.name, style = MaterialTheme.typography.titleLarge)
            CivicDetailFieldRow("Physical Address", workplace.address.ifBlank { "No street address recorded" })
            CivicDetailFieldRow("Active Shift Window", workplace.activeShiftWindow)
            CivicDetailFieldRow("Geofence Radius", "${workplace.geofenceRadiusMeters} metres")
            CivicDetailFieldRow("Max GPS Accuracy", "±${workplace.maxAccuracyMeters} metres")
            CivicDetailFieldRow("Assigned Staff", "${workplace.assignedStaffCount} personnel", showDivider = false)
          }
        }
      }
    }
  }
}

// 10. Supervisor Reports
@Composable
fun SupervisorReportsScreen(
  state: FrontendUiState,
  onDismissBanner: () -> Unit,
) {
  val sup = state.activeSupervisor
  val team = state.scopedSupervisorEmployees

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("supervisor_reports_screen"),
  ) {
    item { Text("Unit Performance Reports", style = MaterialTheme.typography.headlineMedium) }
    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CivicSummaryCard(
          title = "Team Size",
          value = team.size.toString(),
          subtitle = sup?.departmentName ?: "Assigned Unit",
          icon = Icons.Filled.Assessment,
          modifier = Modifier.weight(1f),
        )
        CivicSummaryCard(
          title = "Avg Punctuality",
          value =
            if (team.isEmpty()) "--"
            else "${team.map { it.attendanceRatePercent }.average().toInt()}%",
          subtitle = "${state.scopedSupervisorAttendance.size} attendance log(s)",
          icon = Icons.Filled.CheckCircle,
          modifier = Modifier.weight(1f),
        )
      }
    }
    item {
      CivicAttendanceBarChart(
        title = "Weekly Unit Compliance",
        subtitle = "Scoped attendance reliability across recorded days",
        bars = state.unitWeeklyChart,
      )
    }
    if (team.isNotEmpty()) {
      item {
        CivicDataTableCard(
          title = "Team Attendance Rates",
          headers = listOf("Member", "Code", "Status", "Rate"),
          rows =
            team.map {
              listOf(
                it.fullName,
                it.employeeCode.ifBlank { "--" },
                it.status,
                "${it.attendanceRatePercent}%",
              )
            },
        )
      }
    }
  }
}

// 11. Supervisor Notifications
@Composable
fun SupervisorNotificationsScreen(
  state: FrontendUiState,
  onMarkRead: (String) -> Unit,
  onMarkAllRead: () -> Unit,
  onDismissBanner: () -> Unit,
) {
  var filter by rememberSaveable { mutableStateOf("All") }
  val supNotifications =
    state.notifications.filter {
      it.targetRole == CivicRole.SUPERVISOR && (filter == "All" || !it.isRead)
    }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("supervisor_notifications_screen"),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Unit Notifications", style = MaterialTheme.typography.headlineMedium)
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

    if (supNotifications.isEmpty()) {
      item {
        CivicEmptyState(
          icon = Icons.Filled.Notifications,
          title = "No unread alerts",
          description = "All supervisor unit notifications are up to date.",
        )
      }
    } else {
      items(supNotifications, key = { it.id }) { notif ->
        CivicNotificationCard(notification = notif, onMarkRead = { onMarkRead(notif.id) })
      }
    }
  }
}

// 12. Supervisor Profile & Settings
@Composable
fun SupervisorProfileSettingsScreen(
  state: FrontendUiState,
  onSaveProfile: (String, String) -> Unit,
  onToggleSupervisorPrefs: (Boolean, Boolean) -> Unit,
  onThemeChange: (CivicThemeMode) -> Unit,
  onRecheckConnection: () -> Unit,
  onSignOut: () -> Unit,
  onDismissBanner: () -> Unit,
) {
  val sup = state.activeSupervisor
  var fullName by rememberSaveable(state.currentAccountName) { mutableStateOf(state.currentAccountName) }
  var phone by rememberSaveable(state.currentAccountPhone) { mutableStateOf(state.currentAccountPhone) }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("supervisor_profile_settings_screen"),
  ) {
    item {
      CivicProfileHeader(
        fullName = state.currentAccountName.ifBlank { state.currentAccountEmail },
        roleLabel = "Supervisor • ${sup?.departmentName ?: "Assigned Department"}",
        subtitle = sup?.workplaceName ?: state.organization?.name ?: "Assigned Workplace",
        badgeText = "SUPERVISOR PROFILE",
        avatarUrl = sup?.avatarUrl ?: state.currentAccountAvatarUrl,
      )
    }

    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Account Details", style = MaterialTheme.typography.titleMedium)
          CivicFormField(
            value = fullName,
            onValueChange = { fullName = it },
            label = "Full Name",
            enabled = !state.isSubmitting,
          )
          CivicFormField(
            value = state.currentAccountEmail,
            onValueChange = {},
            label = "Work Email (Locked)",
            readOnly = true,
            enabled = false,
          )
          CivicFormField(
            value = phone,
            onValueChange = { phone = it },
            label = "Phone Number",
            enabled = !state.isSubmitting,
          )
          CivicPrimaryButton(
            text = if (state.isSubmitting) "Saving Profile..." else "Save Profile",
            enabled = !state.isSubmitting,
            onClick = { onSaveProfile(fullName, phone) },
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
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Supervisor Alerts & Theme", style = MaterialTheme.typography.titleMedium)
          SettingToggleRow(
            title = "Real-Time Exception Alerts",
            subtitle = "Flag off-site or late arrivals in assigned unit",
            checked = state.supervisorExceptionAlertsEnabled,
            onCheckedChange = {
              onToggleSupervisorPrefs(it, state.supervisorLeaveDigestEnabled)
            },
          )
          SettingToggleRow(
            title = "Daily Leave Digest",
            subtitle = "Summarise pending unit leave approvals",
            checked = state.supervisorLeaveDigestEnabled,
            onCheckedChange = {
              onToggleSupervisorPrefs(state.supervisorExceptionAlertsEnabled, it)
            },
          )
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

    item {
      SupabaseConnectionCard(
        report = state.connectionReport,
        onRecheck = onRecheckConnection,
        isChecking = state.isCheckingConnection,
      )
    }

    item {
      CivicSecondaryButton(
        text = "Sign Out to Entry Screen",
        icon = Icons.AutoMirrored.Filled.Logout,
        isDestructive = true,
        testTag = "supervisor_sign_out_button",
        onClick = onSignOut,
      )
    }
  }
}
