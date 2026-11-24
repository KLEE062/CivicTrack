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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Schedule
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
import com.example.ui.frontend.components.CivicAttendanceBarChart
import com.example.ui.frontend.components.CivicAttendanceStatusBadge
import com.example.ui.frontend.components.CivicDetailFieldRow
import com.example.ui.frontend.components.CivicEmployeeCard
import com.example.ui.frontend.components.CivicEmptyState
import com.example.ui.frontend.components.CivicFilterChipRow
import com.example.ui.frontend.components.CivicFormField
import com.example.ui.frontend.components.CivicLeaveStatusBadge
import com.example.ui.frontend.components.CivicNoticeBanner
import com.example.ui.frontend.components.CivicPrimaryButton
import com.example.ui.frontend.components.CivicProfileHeader
import com.example.ui.frontend.components.CivicQuickNavRow
import com.example.ui.frontend.components.CivicResponsiveScreenContainer
import com.example.ui.frontend.components.CivicSearchBar
import com.example.ui.frontend.components.CivicSectionHeader
import com.example.ui.frontend.components.CivicSecondaryButton
import com.example.ui.frontend.components.CivicSummaryCard
import com.example.ui.frontend.data.CivicAttendanceStatus
import com.example.ui.frontend.data.CivicLeaveStatus
import com.example.ui.frontend.navigation.FrontendRoutes
import com.example.ui.frontend.state.FrontendUiState
import com.example.ui.theme.LocalCivicStatusPalette

// 1. Supervisor Dashboard
@Composable
fun SupervisorDashboardFrontendScreen(
  state: FrontendUiState,
  onNavigate: (String) -> Unit,
  onRefresh: () -> Unit,
  onDismissBanner: () -> Unit,
) {
  val palette = LocalCivicStatusPalette.current
  val supervisor = state.activeSupervisor
  val deptLabel = supervisor?.departmentName ?: "Assigned Department"
  val workplaceLabel = supervisor?.workplaceName ?: "Assigned Workplace"
  val team = state.scopedSupervisorEmployees
  val scopedAttendance = state.scopedSupervisorAttendance
  val exceptions =
    scopedAttendance.filter {
      it.status == CivicAttendanceStatus.LATE ||
        it.status == CivicAttendanceStatus.INCOMPLETE ||
        it.status == CivicAttendanceStatus.EXCEPTION
    }
  val pendingLeaves = state.scopedSupervisorLeaves.count { it.status == CivicLeaveStatus.PENDING }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("supervisor_dashboard_screen"),
  ) {
    item {
      CivicProfileHeader(
        fullName = supervisor?.fullName ?: state.currentAccountName.ifBlank { state.currentAccountEmail },
        roleLabel = "$deptLabel Supervisor",
        subtitle = workplaceLabel,
        badgeText = "SCOPED UNIT WORKSPACE",
        avatarUrl = supervisor?.avatarUrl ?: state.currentAccountAvatarUrl,
      )
    }

    item {
      OutlinedButton(
        onClick = onRefresh,
        modifier = Modifier.fillMaxWidth().testTag("supervisor_refresh_button"),
      ) {
        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Refresh Scoped Unit Data")
      }
    }

    item {
      CivicNoticeBanner(
        title = "Department & Workplace Authority Scope",
        message =
          "Visibility is scoped to $deptLabel at $workplaceLabel. Organisation-wide administration remains Owner-only.",
      )
    }

    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          CivicSummaryCard(
            title = "Assigned Team",
            value = team.size.toString(),
            subtitle = "${team.count { it.status == "Active" }} active staff",
            icon = Icons.Filled.People,
            onClick = { onNavigate(FrontendRoutes.SUPERVISOR_WORKFORCE) },
            modifier = Modifier.weight(1f),
          )
          CivicSummaryCard(
            title = "Exceptions",
            value = exceptions.size.toString(),
            subtitle = "Late / incomplete / off-site",
            icon = Icons.Filled.ReportProblem,
            accentColor = palette.warning,
            onClick = { onNavigate(FrontendRoutes.SUPERVISOR_EXCEPTIONS) },
            modifier = Modifier.weight(1f),
          )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          CivicSummaryCard(
            title = "Team Logs",
            value = scopedAttendance.size.toString(),
            subtitle = "Scoped attendance entries",
            icon = Icons.Filled.CalendarMonth,
            accentColor = palette.verified,
            onClick = { onNavigate(FrontendRoutes.SUPERVISOR_ATTENDANCE_OVERVIEW) },
            modifier = Modifier.weight(1f),
          )
          CivicSummaryCard(
            title = "Pending Leave",
            value = pendingLeaves.toString(),
            subtitle = "Unit requests awaiting decision",
            icon = Icons.Filled.EventBusy,
            accentColor = MaterialTheme.colorScheme.tertiary,
            onClick = { onNavigate(FrontendRoutes.SUPERVISOR_LEAVE_REQUESTS) },
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    item {
      CivicAttendanceBarChart(
        title = "Unit Punctuality Trend",
        subtitle = "Scoped attendance compliance for $deptLabel",
        bars = state.unitWeeklyChart,
      )
    }

    item { CivicSectionHeader(title = "Supervisor Operations") }

    item {
      CivicQuickNavRow(
        title = "Assigned Workforce",
        subtitle = "${team.size} personnel in assigned unit",
        icon = Icons.Filled.People,
        trailingBadge = team.size.toString(),
        testTag = "sup_nav_workforce",
        onClick = { onNavigate(FrontendRoutes.SUPERVISOR_WORKFORCE) },
      )
    }
    item {
      CivicQuickNavRow(
        title = "Team Attendance Overview",
        subtitle = "Inspect daily arrivals & departures",
        icon = Icons.Filled.CalendarMonth,
        testTag = "sup_nav_attendance",
        onClick = { onNavigate(FrontendRoutes.SUPERVISOR_ATTENDANCE_OVERVIEW) },
      )
    }
    item {
      CivicQuickNavRow(
        title = "Late & Exception Review",
        subtitle = "${exceptions.size} flagged entries needing acknowledgement",
        icon = Icons.Filled.ReportProblem,
        trailingBadge = exceptions.size.toString(),
        testTag = "sup_nav_exceptions",
        onClick = { onNavigate(FrontendRoutes.SUPERVISOR_EXCEPTIONS) },
      )
    }
    item {
      CivicQuickNavRow(
        title = "Team Leave Requests",
        subtitle = "$pendingLeaves pending approval",
        icon = Icons.Filled.EventBusy,
        trailingBadge = pendingLeaves.toString(),
        testTag = "sup_nav_leave",
        onClick = { onNavigate(FrontendRoutes.SUPERVISOR_LEAVE_REQUESTS) },
      )
    }
    item {
      CivicQuickNavRow(
        title = "Team Schedules",
        subtitle = "Shift timetables for $deptLabel",
        icon = Icons.Filled.Schedule,
        testTag = "sup_nav_schedules",
        onClick = { onNavigate(FrontendRoutes.SUPERVISOR_TEAM_SCHEDULES) },
      )
    }
    item {
      CivicQuickNavRow(
        title = "Assigned Workplace Info",
        subtitle = workplaceLabel,
        icon = Icons.Filled.LocationOn,
        testTag = "sup_nav_workplace",
        onClick = { onNavigate(FrontendRoutes.SUPERVISOR_WORKPLACE_INFO) },
      )
    }
    item {
      CivicQuickNavRow(
        title = "Supervisor Reports",
        subtitle = "Unit punctuality summary & table",
        icon = Icons.Filled.Assessment,
        testTag = "sup_nav_reports",
        onClick = { onNavigate(FrontendRoutes.SUPERVISOR_REPORTS) },
      )
    }
    item {
      CivicQuickNavRow(
        title = "Supervisor Notifications",
        subtitle = "${state.unreadNotificationsCountForActiveRole} unread unit alerts",
        icon = Icons.Filled.Notifications,
        testTag = "sup_nav_notifications",
        onClick = { onNavigate(FrontendRoutes.SUPERVISOR_NOTIFICATIONS) },
      )
    }
    item {
      CivicQuickNavRow(
        title = "Profile & Preferences",
        subtitle = state.currentAccountEmail,
        icon = Icons.Filled.Person,
        testTag = "sup_nav_profile",
        onClick = { onNavigate(FrontendRoutes.SUPERVISOR_PROFILE_SETTINGS) },
      )
    }

    if (exceptions.isNotEmpty()) {
      item {
        CivicSectionHeader(
          title = "Unit Exceptions Requiring Attention",
          actionLabel = "Review All",
          onAction = { onNavigate(FrontendRoutes.SUPERVISOR_EXCEPTIONS) },
        )
      }
      items(exceptions.take(3), key = { it.id }) { rec ->
        Card(
          onClick = { onNavigate(FrontendRoutes.SUPERVISOR_EXCEPTIONS) },
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(rec.employeeName, style = MaterialTheme.typography.titleMedium)
              Text(
                text = rec.exceptionReason ?: rec.locationNote,
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

// 2. Assigned Workforce List
@Composable
fun SupervisorWorkforceScreen(
  state: FrontendUiState,
  onOpenEmployeeDetails: (String) -> Unit,
  onDismissBanner: () -> Unit,
) {
  var searchQuery by rememberSaveable { mutableStateOf("") }
  var selectedStatus by rememberSaveable { mutableStateOf("All") }
  val supervisor = state.activeSupervisor
  val team = state.scopedSupervisorEmployees

  val filtered =
    team.filter { emp ->
      val matchesStatus = selectedStatus == "All" || emp.status.equals(selectedStatus, ignoreCase = true)
      val matchesQuery =
        searchQuery.isBlank() ||
          emp.fullName.contains(searchQuery, ignoreCase = true) ||
          emp.employeeCode.contains(searchQuery, ignoreCase = true) ||
          emp.jobTitle.contains(searchQuery, ignoreCase = true)
      matchesStatus && matchesQuery
    }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("supervisor_workforce_screen"),
  ) {
    item { Text("Assigned Workforce", style = MaterialTheme.typography.headlineMedium) }
    item {
      CivicNoticeBanner(
        title = "Scoped Unit Directory",
        message =
          "Showing ${team.size} personnel assigned to ${supervisor?.departmentName ?: "Assigned Department"} (${supervisor?.workplaceName ?: "Assigned Workplace"}).",
      )
    }
    item {
      CivicSearchBar(
        query = searchQuery,
        onQueryChange = { searchQuery = it },
        placeholder = "Search team member, code or role...",
        testTag = "supervisor_workforce_search",
      )
    }
    item {
      CivicFilterChipRow(
        options = listOf("All", "Active", "Inactive"),
        selectedOption = selectedStatus,
        onOptionSelected = { selectedStatus = it },
      )
    }

    if (filtered.isEmpty()) {
      item {
        CivicEmptyState(
          icon = Icons.Filled.People,
          title = "No assigned staff match",
          description = "Adjust your search or status filter.",
          actionLabel = "Clear Filters",
          onAction = {
            searchQuery = ""
            selectedStatus = "All"
          },
        )
      }
    } else {
      items(filtered, key = { it.id }) { emp ->
        CivicEmployeeCard(
          employee = emp,
          onClick = { onOpenEmployeeDetails(emp.id) },
          modifier = Modifier.testTag("sup_employee_card_${emp.id}"),
        )
      }
    }
  }
}

// 3. Employee Details (Supervisor View)
@Composable
fun SupervisorEmployeeDetailsScreen(
  state: FrontendUiState,
  employeeId: String,
  onOpenExceptions: () -> Unit,
  onDismissBanner: () -> Unit,
) {
  val employee =
    state.scopedSupervisorEmployees.firstOrNull { it.id == employeeId }
      ?: state.scopedSupervisorEmployees.firstOrNull()
  val records = state.scopedSupervisorAttendance.filter { it.employeeId == employee?.id }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("supervisor_employee_details_screen"),
  ) {
    if (employee == null) {
      item {
        CivicEmptyState(
          icon = Icons.Filled.People,
          title = "Staff record outside supervisor scope",
          description = "Select an employee from your assigned workforce list.",
        )
      }
    } else {
      item {
        CivicProfileHeader(
          fullName = employee.fullName,
          roleLabel = employee.jobTitle,
          subtitle = "${employee.employeeCode} • ${employee.departmentName}",
          badgeText = employee.status.uppercase(),
          avatarUrl = employee.avatarUrl,
        )
      }
      item {
        CivicNoticeBanner(
          title = "Read-Only Employment Metadata",
          message =
            "Role assignments, transfers, and termination actions are restricted to Organisation Owners.",
        )
      }
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text("Assigned Unit Profile", style = MaterialTheme.typography.titleMedium)
            CivicDetailFieldRow("Department", employee.departmentName)
            CivicDetailFieldRow("Workplace", employee.workplaceName)
            CivicDetailFieldRow("Work Email", employee.email)
            CivicDetailFieldRow("Phone", employee.phone.ifBlank { "Not provided" })
            CivicDetailFieldRow(
              "Punctuality Rate",
              "${employee.attendanceRatePercent}%",
              showDivider = false,
            )
          }
        }
      }

      item {
        CivicSectionHeader(
          title = "Recent Unit Attendance",
          actionLabel = "Exceptions",
          onAction = onOpenExceptions,
        )
      }

      if (records.isEmpty()) {
        item {
          CivicEmptyState(
            icon = Icons.Filled.Schedule,
            title = "No attendance records",
            description = "Scoped attendance logs for this employee will appear here.",
          )
        }
      } else {
        items(records, key = { it.id }) { rec ->
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(rec.dateLabel, style = MaterialTheme.typography.titleMedium)
                Text(
                  text = "In: ${rec.checkInTime ?: "--"} • Out: ${rec.checkOutTime ?: "--"}",
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
}

// 4. Team Attendance Overview
@Composable
fun SupervisorAttendanceOverviewScreen(
  state: FrontendUiState,
  onOpenExceptions: () -> Unit,
  onOpenEmployeeDetails: (String) -> Unit,
  onDismissBanner: () -> Unit,
) {
  var query by rememberSaveable { mutableStateOf("") }
  var selectedStatus by rememberSaveable { mutableStateOf("All") }
  val statusOptions = listOf("All") + CivicAttendanceStatus.entries.map { it.label }

  val filtered =
    state.scopedSupervisorAttendance.filter { rec ->
      val matchesStatus = selectedStatus == "All" || rec.status.label == selectedStatus
      val matchesQuery =
        query.isBlank() ||
          rec.employeeName.contains(query, ignoreCase = true) ||
          rec.employeeCode.contains(query, ignoreCase = true)
      matchesStatus && matchesQuery
    }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("supervisor_attendance_screen"),
  ) {
    item {
      CivicSectionHeader(
        title = "Team Attendance Overview",
        actionLabel = "Exceptions Queue",
        onAction = onOpenExceptions,
      )
    }
    item {
      CivicSearchBar(
        query = query,
        onQueryChange = { query = it },
        placeholder = "Filter team attendance by name or code...",
      )
    }
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
          icon = Icons.Filled.CalendarMonth,
          title = "No team records match",
          description = "Clear filters to display all scoped team attendance logs.",
          actionLabel = "Reset",
          onAction = {
            query = ""
            selectedStatus = "All"
          },
        )
      }
    } else {
      items(filtered, key = { it.id }) { rec ->
        Card(
          onClick = { onOpenEmployeeDetails(rec.employeeId) },
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text("${rec.employeeName} (${rec.employeeCode})", style = MaterialTheme.typography.titleMedium)
              Text(
                text = "${rec.dateLabel} • In: ${rec.checkInTime ?: "--"} • Out: ${rec.checkOutTime ?: "--"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = rec.locationNote,
                style = MaterialTheme.typography.labelSmall,
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

// 5. Late & Exception Review
@Composable
fun SupervisorExceptionsScreen(
  state: FrontendUiState,
  onReviewRecord: (recordId: String, geofenceStatus: String?, reviewStatus: String, notes: String) -> Unit,
  onDismissBanner: () -> Unit,
) {
  val exceptions =
    state.scopedSupervisorAttendance.filter {
      it.status == CivicAttendanceStatus.LATE ||
        it.status == CivicAttendanceStatus.INCOMPLETE ||
        it.status == CivicAttendanceStatus.EXCEPTION
    }
  var notesByRecord by rememberSaveable { mutableStateOf(mapOf<String, String>()) }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("supervisor_exceptions_screen"),
  ) {
    item { Text("Late & Exception Review", style = MaterialTheme.typography.headlineMedium) }
    item {
      CivicNoticeBanner(
        title = "Scoped Exception Queue",
        message =
          "Review late arrivals, missing check-outs, and off-site exceptions for your assigned unit.",
      )
    }

    if (exceptions.isEmpty()) {
      item {
        CivicEmptyState(
          icon = Icons.Filled.CheckCircle,
          title = "No open unit exceptions",
          description = "All scoped team attendance records have been resolved.",
        )
      }
    } else {
      exceptions.forEach { rec ->
        item(key = "exc_card_${rec.id}") {
          val note = notesByRecord[rec.id] ?: rec.exceptionReason.orEmpty()
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text("${rec.employeeName} (${rec.employeeCode})", style = MaterialTheme.typography.titleMedium)
                  Text(
                    text = "${rec.dateLabel} • Check-In: ${rec.checkInTime ?: "--"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                CivicAttendanceStatusBadge(status = rec.status)
              }
              Text(
                text = rec.locationNote,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              CivicFormField(
                value = note,
                onValueChange = { updated -> notesByRecord = notesByRecord + (rec.id to updated) },
                label = "Supervisor Review Note",
                placeholder = "Record field justification or follow-up note...",
                singleLine = false,
                enabled = !state.isSubmitting,
                testTag = "sup_exception_note_${rec.id}",
              )
            }
          }
        }
        item(key = "exc_actions_${rec.id}") {
          val note = notesByRecord[rec.id] ?: rec.exceptionReason.orEmpty()
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
              CivicPrimaryButton(
                text = "Acknowledge & Clear",
                enabled = !state.isSubmitting,
                testTag = "sup_resolve_exception_${rec.id}",
                onClick = {
                  onReviewRecord(
                    rec.id,
                    "verified",
                    "approved",
                    note.ifBlank { "Acknowledged by unit supervisor" },
                  )
                },
              )
            }
            Box(Modifier.weight(1f)) {
              CivicSecondaryButton(
                text = "Keep Flagged",
                isDestructive = true,
                enabled = !state.isSubmitting,
                onClick = {
                  onReviewRecord(
                    rec.id,
                    null,
                    "flagged",
                    note.ifBlank { "Escalated for owner audit" },
                  )
                },
              )
            }
          }
        }
      }
    }
  }
}

// 6. Leave Requests Queue (Supervisor)
@Composable
fun SupervisorLeaveRequestsScreen(
  state: FrontendUiState,
  onOpenLeaveDetail: (String) -> Unit,
  onDismissBanner: () -> Unit,
) {
  var selectedTab by rememberSaveable { mutableStateOf("Pending") }
  val filtered =
    state.scopedSupervisorLeaves.filter {
      when (selectedTab) {
        "Pending" -> it.status == CivicLeaveStatus.PENDING
        "Approved" -> it.status == CivicLeaveStatus.APPROVED
        "Rejected" -> it.status == CivicLeaveStatus.REJECTED
        else -> true
      }
    }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("supervisor_leave_requests_screen"),
  ) {
    item { Text("Team Leave Requests", style = MaterialTheme.typography.headlineMedium) }
    item {
      CivicFilterChipRow(
        options = listOf("Pending", "Approved", "Rejected", "All"),
        selectedOption = selectedTab,
        onOptionSelected = { selectedTab = it },
      )
    }

    if (filtered.isEmpty()) {
      item {
        CivicEmptyState(
          icon = Icons.Filled.EventBusy,
          title = "No $selectedTab leave requests",
          description = "Switch filter chips to inspect other leave submissions in your unit.",
        )
      }
    } else {
      items(filtered, key = { it.id }) { req ->
        Card(
          onClick = { onOpenLeaveDetail(req.id) },
          modifier = Modifier.fillMaxWidth().testTag("sup_leave_card_${req.id}"),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text(req.employeeName, style = MaterialTheme.typography.titleMedium)
                Text(
                  text = "${req.leaveType} • ${req.daysCount} day(s)",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
              CivicLeaveStatusBadge(status = req.status)
            }
            Text("${req.startDate} → ${req.endDate}", style = MaterialTheme.typography.bodyMedium)
            if (req.reason.isNotBlank()) {
              Text(
                text = req.reason,
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
