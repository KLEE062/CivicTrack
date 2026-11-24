package com.example.ui.frontend.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.frontend.components.CivicAttendanceBarChart
import com.example.ui.frontend.components.CivicAttendanceStatusBadge
import com.example.ui.frontend.components.CivicDetailFieldRow
import com.example.ui.frontend.components.CivicEmployeeCard
import com.example.ui.frontend.components.CivicEmptyState
import com.example.ui.frontend.components.CivicFilterChipRow
import com.example.ui.frontend.components.CivicFormField
import com.example.ui.frontend.components.CivicNoticeBanner
import com.example.ui.frontend.components.CivicPill
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
import java.time.LocalDate
import java.time.ZoneOffset

// 1. Owner Dashboard
@Composable
fun OwnerDashboardFrontendScreen(
  state: FrontendUiState,
  onNavigate: (String) -> Unit,
  onRefresh: () -> Unit,
  onDismissBanner: () -> Unit,
) {
  val palette = LocalCivicStatusPalette.current
  val org = state.organization
  val todayIso = LocalDate.now(ZoneOffset.UTC).toString()
  val presentTodayCount =
    state.attendanceRecords.count {
      it.dateLabel.startsWith(todayIso) &&
        (it.status == CivicAttendanceStatus.ON_TIME || it.status == CivicAttendanceStatus.LATE)
    }
  val exceptionCount =
    state.attendanceRecords.count {
      it.status == CivicAttendanceStatus.EXCEPTION || it.status == CivicAttendanceStatus.INCOMPLETE
    }
  val pendingLeaves = state.leaveRequests.count { it.status == CivicLeaveStatus.PENDING }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("owner_dashboard_screen"),
  ) {
    item {
      CivicProfileHeader(
        fullName = org?.name ?: "Organisation Workspace",
        roleLabel = state.currentAccountName.ifBlank { state.currentAccountEmail },
        subtitle = "${org?.countryCode ?: "UG"} (${org?.currencyCode ?: "UGX"}) • Owner",
        badgeText = if (state.connectionReport.isConnected) "ORGANISATION OWNER • LIVE" else "ORGANISATION OWNER",
        avatarUrl = org?.logoUrl,
      )
    }

    item {
      OutlinedButton(
        onClick = onRefresh,
        modifier = Modifier.fillMaxWidth().testTag("owner_refresh_workspace_button"),
      ) {
        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Sync Latest Supabase Records")
      }
    }

    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          CivicSummaryCard(
            title = "Employees",
            value = state.employees.size.toString(),
            subtitle = "${state.departments.size} departments • ${state.workplaces.size} sites",
            icon = Icons.Filled.People,
            onClick = { onNavigate(FrontendRoutes.OWNER_EMPLOYEE_DIRECTORY) },
            modifier = Modifier.weight(1f),
          )
          CivicSummaryCard(
            title = "Present Today",
            value = presentTodayCount.toString(),
            subtitle = "$exceptionCount exceptions flagged",
            icon = Icons.Filled.VerifiedUser,
            accentColor = palette.verified,
            onClick = { onNavigate(FrontendRoutes.OWNER_ATTENDANCE_OVERVIEW) },
            modifier = Modifier.weight(1f),
          )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          CivicSummaryCard(
            title = "Pending Leave",
            value = pendingLeaves.toString(),
            subtitle = "Awaiting review",
            icon = Icons.Filled.EventBusy,
            accentColor = palette.warning,
            onClick = { onNavigate(FrontendRoutes.OWNER_LEAVE_MANAGEMENT) },
            modifier = Modifier.weight(1f),
          )
          CivicSummaryCard(
            title = "Supervisors",
            value = state.supervisors.size.toString(),
            subtitle = "Department-scoped leads",
            icon = Icons.Filled.SupervisorAccount,
            accentColor = MaterialTheme.colorScheme.tertiary,
            onClick = { onNavigate(FrontendRoutes.OWNER_SUPERVISOR_DIRECTORY) },
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    item {
      CivicAttendanceBarChart(
        title = "Weekly Organisational Attendance",
        subtitle = "Punctual check-in share vs. exceptions across recorded dates",
        bars = state.weeklyAttendanceChart,
      )
    }

    item {
      CivicSectionHeader(
        title = "Workforce & Operations",
        actionLabel = "Reports",
        onAction = { onNavigate(FrontendRoutes.OWNER_REPORTS) },
      )
    }

    item {
      CivicQuickNavRow(
        title = "Organisation Profile",
        subtitle = "Edit identity, registration, logo & contacts",
        icon = Icons.Filled.Apartment,
        testTag = "owner_nav_org_profile",
        onClick = { onNavigate(FrontendRoutes.OWNER_ORG_PROFILE) },
      )
    }
    item {
      CivicQuickNavRow(
        title = "Employee Directory & Invitations",
        subtitle = "${state.employees.size} staff records • Invite employee",
        icon = Icons.Filled.People,
        trailingBadge = state.employees.size.toString(),
        testTag = "owner_nav_employees",
        onClick = { onNavigate(FrontendRoutes.OWNER_EMPLOYEE_DIRECTORY) },
      )
    }
    item {
      CivicQuickNavRow(
        title = "Supervisors & Invitations",
        subtitle = "${state.supervisors.size} active/invited unit supervisors",
        icon = Icons.Filled.SupervisorAccount,
        trailingBadge = state.supervisors.size.toString(),
        testTag = "owner_nav_supervisors",
        onClick = { onNavigate(FrontendRoutes.OWNER_SUPERVISOR_DIRECTORY) },
      )
    }
    item {
      CivicQuickNavRow(
        title = "Departments & Workplaces",
        subtitle = "${state.departments.size} depts • ${state.workplaces.size} geofenced sites",
        icon = Icons.Filled.Business,
        testTag = "owner_nav_departments",
        onClick = { onNavigate(FrontendRoutes.OWNER_DEPARTMENTS) },
      )
    }
    item {
      CivicQuickNavRow(
        title = "Workplaces & Geofences",
        subtitle = "Manage sites, GPS coordinates & tolerances",
        icon = Icons.Filled.LocationCity,
        testTag = "owner_nav_workplaces",
        onClick = { onNavigate(FrontendRoutes.OWNER_WORKPLACES) },
      )
    }
    item {
      CivicQuickNavRow(
        title = "Work Schedules",
        subtitle = "Working days, hours & late grace window",
        icon = Icons.Filled.Schedule,
        testTag = "owner_nav_schedules",
        onClick = { onNavigate(FrontendRoutes.OWNER_WORK_SCHEDULES) },
      )
    }
    item {
      CivicQuickNavRow(
        title = "Attendance & Leave Management",
        subtitle = "$pendingLeaves pending leave • ${state.attendanceRecords.size} attendance logs",
        icon = Icons.Filled.CalendarMonth,
        testTag = "owner_nav_leave",
        onClick = { onNavigate(FrontendRoutes.OWNER_LEAVE_MANAGEMENT) },
      )
    }
    item {
      CivicQuickNavRow(
        title = "Reports & Analytics",
        subtitle = "Live compliance summaries across departments & sites",
        icon = Icons.Filled.Assessment,
        testTag = "owner_nav_reports",
        onClick = { onNavigate(FrontendRoutes.OWNER_REPORTS) },
      )
    }
    item {
      CivicQuickNavRow(
        title = "Notifications",
        subtitle = "${state.unreadNotificationsCountForActiveRole} unread alert(s)",
        icon = Icons.Filled.Notifications,
        testTag = "owner_nav_notifications",
        onClick = { onNavigate(FrontendRoutes.OWNER_NOTIFICATIONS) },
      )
    }
    item {
      CivicQuickNavRow(
        title = "Audit Trail",
        subtitle = "${state.auditEvents.size} recorded institutional audit entries",
        icon = Icons.Filled.History,
        testTag = "owner_nav_audit",
        onClick = { onNavigate(FrontendRoutes.OWNER_AUDIT_ACTIVITY) },
      )
    }
    item {
      CivicQuickNavRow(
        title = "Organisation Settings",
        subtitle = "Verification rules & application theme",
        icon = Icons.Filled.Settings,
        testTag = "owner_nav_settings",
        onClick = { onNavigate(FrontendRoutes.OWNER_SETTINGS) },
      )
    }
    item {
      CivicQuickNavRow(
        title = "Workspace Capacity & Backend Status",
        subtitle = if (state.connectionReport.isConnected) "Supabase Connected" else state.connectionReport.statusSummary,
        icon = Icons.Filled.Storage,
        testTag = "owner_nav_billing",
        onClick = { onNavigate(FrontendRoutes.OWNER_BILLING) },
      )
    }
    item {
      CivicQuickNavRow(
        title = "Owner Profile",
        subtitle = state.currentAccountEmail,
        icon = Icons.Filled.Person,
        testTag = "owner_nav_profile",
        onClick = { onNavigate(FrontendRoutes.OWNER_PROFILE) },
      )
    }

    item {
      CivicSectionHeader(
        title = "Recent Audit Activity",
        actionLabel = "Full Log",
        onAction = { onNavigate(FrontendRoutes.OWNER_AUDIT_ACTIVITY) },
      )
    }

    if (state.auditEvents.isEmpty()) {
      item {
        CivicEmptyState(
          icon = Icons.Filled.History,
          title = "No audit events recorded yet",
          description = "Organisation changes, invitations, and attendance reviews will log here.",
        )
      }
    } else {
      items(state.auditEvents.take(3), key = { it.id }) { event ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(event.action, style = MaterialTheme.typography.titleMedium)
              CivicPill(
                label = event.category,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
              )
            }
            Text(
              text = event.summary,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = "${event.actorName} • ${event.timestampLabel}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

// 2. Organisation Profile
@Composable
fun OwnerOrgProfileFrontendScreen(
  state: FrontendUiState,
  onSaveProfile: (String, String, String, String, String, String, String) -> Unit,
  onPickLogo: () -> Unit,
  onDismissBanner: () -> Unit,
) {
  val org = state.organization
  var name by rememberSaveable(org?.id, org?.name) { mutableStateOf(org?.name.orEmpty()) }
  var regNo by rememberSaveable(org?.id, org?.registrationNumber) { mutableStateOf(org?.registrationNumber.orEmpty()) }
  var industry by rememberSaveable(org?.id, org?.industry) { mutableStateOf(org?.industry.orEmpty()) }
  var description by rememberSaveable(org?.id, org?.description) { mutableStateOf(org?.description.orEmpty()) }
  var contactEmail by rememberSaveable(org?.id, org?.contactEmail) { mutableStateOf(org?.contactEmail.orEmpty()) }
  var contactPhone by rememberSaveable(org?.id, org?.contactPhone) { mutableStateOf(org?.contactPhone.orEmpty()) }
  var address by rememberSaveable(org?.id, org?.headquartersAddress) { mutableStateOf(org?.headquartersAddress.orEmpty()) }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("owner_org_profile_screen"),
  ) {
    if (org == null) {
      item {
        CivicEmptyState(
          icon = Icons.Filled.Apartment,
          title = "Organisation profile not loaded",
          description = "Sign in as Organisation Owner to load institutional profile records.",
        )
      }
    } else {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Organisation Profile", style = MaterialTheme.typography.headlineMedium)
            Text(
              text = "Country: ${org.countryCode} • Currency: ${org.currencyCode}",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
            if (!org.logoUrl.isNullOrBlank()) {
              AsyncImage(
                model = org.logoUrl,
                contentDescription = "Organisation Logo",
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop,
              )
            }
            CivicSecondaryButton(
              text = "Upload Organisation Logo",
              icon = Icons.Filled.PhotoLibrary,
              enabled = !state.isSubmitting,
              onClick = onPickLogo,
              testTag = "org_profile_logo_button",
            )
            CivicFormField(value = name, onValueChange = { name = it }, label = "Organisation Name", enabled = !state.isSubmitting, testTag = "org_profile_name_input")
            CivicFormField(value = regNo, onValueChange = { regNo = it }, label = "Registration Number", enabled = !state.isSubmitting)
            CivicFormField(value = industry, onValueChange = { industry = it }, label = "Sector / Industry", enabled = !state.isSubmitting)
            CivicFormField(value = description, onValueChange = { description = it }, label = "Mission / Description", singleLine = false, enabled = !state.isSubmitting)
            CivicFormField(value = contactEmail, onValueChange = { contactEmail = it }, label = "Official Contact Email", enabled = !state.isSubmitting)
            CivicFormField(value = contactPhone, onValueChange = { contactPhone = it }, label = "Official Phone", enabled = !state.isSubmitting)
            CivicFormField(value = address, onValueChange = { address = it }, label = "Headquarters Address", enabled = !state.isSubmitting)
            CivicPrimaryButton(
              text = if (state.isSubmitting) "Saving..." else "Save Organisation Profile",
              enabled = !state.isSubmitting,
              testTag = "org_profile_save_button",
              onClick = { onSaveProfile(name, regNo, industry, description, contactEmail, contactPhone, address) },
            )
          }
        }
      }
    }
  }
}

// 3. Employee Directory
@Composable
fun OwnerEmployeeDirectoryScreen(
  state: FrontendUiState,
  onOpenEmployeeDetails: (String) -> Unit,
  onOpenAddEmployee: () -> Unit,
  onDismissBanner: () -> Unit,
) {
  var searchQuery by rememberSaveable { mutableStateOf("") }
  var selectedDept by rememberSaveable { mutableStateOf("All") }
  val deptOptions = listOf("All") + state.departments.map { it.name }

  val filteredEmployees =
    state.employees.filter { emp ->
      val matchesDept = selectedDept == "All" || emp.departmentName == selectedDept
      val matchesSearch =
        searchQuery.isBlank() ||
          emp.fullName.contains(searchQuery, ignoreCase = true) ||
          emp.employeeCode.contains(searchQuery, ignoreCase = true) ||
          emp.jobTitle.contains(searchQuery, ignoreCase = true)
      matchesDept && matchesSearch
    }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("owner_employee_directory_screen"),
  ) {
    item {
      Column {
        Text("Employee Directory", style = MaterialTheme.typography.headlineMedium)
        Text(
          text = "${filteredEmployees.size} of ${state.employees.size} registered personnel",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    item {
      CivicPrimaryButton(
        text = "Invite Employee",
        icon = Icons.Filled.GroupAdd,
        testTag = "directory_add_employee_button",
        onClick = onOpenAddEmployee,
      )
    }

    item {
      CivicSearchBar(
        query = searchQuery,
        onQueryChange = { searchQuery = it },
        placeholder = "Search name, employee code or job title...",
        testTag = "employee_directory_search",
      )
    }

    item {
      CivicFilterChipRow(
        options = deptOptions,
        selectedOption = selectedDept,
        onOptionSelected = { selectedDept = it },
      )
    }

    if (filteredEmployees.isEmpty()) {
      item {
        CivicEmptyState(
          icon = Icons.Filled.People,
          title = "No matching employees",
          description = "Adjust your filters or invite a new employee to join your organisation.",
          actionLabel = "Reset Filters",
          onAction = {
            searchQuery = ""
            selectedDept = "All"
          },
        )
      }
    } else {
      items(filteredEmployees, key = { it.id }) { emp ->
        CivicEmployeeCard(
          employee = emp,
          onClick = { onOpenEmployeeDetails(emp.id) },
          modifier = Modifier.testTag("employee_card_${emp.id}"),
        )
      }
    }
  }
}

// 4. Employee Details (Owner view)
@Composable
fun OwnerEmployeeDetailsScreen(
  state: FrontendUiState,
  employeeId: String,
  onOpenAttendanceDetails: (String) -> Unit,
  onDismissBanner: () -> Unit,
) {
  val employee = state.employees.firstOrNull { it.id == employeeId } ?: state.employees.firstOrNull()
  val empRecords = state.attendanceRecords.filter { it.employeeId == employee?.id }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("owner_employee_details_screen"),
  ) {
    if (employee == null) {
      item {
        CivicEmptyState(
          icon = Icons.Filled.Badge,
          title = "Employee not found",
          description = "Select an employee from the directory.",
        )
      }
    } else {
      item {
        CivicProfileHeader(
          fullName = employee.fullName,
          roleLabel = employee.jobTitle,
          subtitle = employee.employeeCode,
          badgeText = employee.status.uppercase(),
          avatarUrl = employee.avatarUrl,
        )
      }

      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text("Organisational Assignment", style = MaterialTheme.typography.titleMedium)
            CivicDetailFieldRow("Department", employee.departmentName)
            CivicDetailFieldRow("Workplace", employee.workplaceName)
            CivicDetailFieldRow("Work Email", employee.email)
            CivicDetailFieldRow("Phone", employee.phone.ifBlank { "Not provided" })
            CivicDetailFieldRow("Attendance Punctuality", "${employee.attendanceRatePercent}%", showDivider = false)
          }
        }
      }

      item { CivicSectionHeader(title = "Recent Attendance Logs") }

      if (empRecords.isEmpty()) {
        item {
          CivicEmptyState(
            icon = Icons.Filled.Schedule,
            title = "No attendance records",
            description = "Check-in and check-out logs for this employee will appear here.",
          )
        }
      } else {
        items(empRecords, key = { it.id }) { rec ->
          Card(
            onClick = { onOpenAttendanceDetails(rec.id) },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
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

// 5. Invite Employee Interface (replaces fake local add)
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OwnerAddEmployeeScreen(
  state: FrontendUiState,
  onInviteEmployee: (email: String, departmentId: String, workplaceId: String) -> Unit,
  onDismissBanner: () -> Unit,
) {
  var email by rememberSaveable { mutableStateOf("") }
  var selectedDeptId by rememberSaveable {
    mutableStateOf(state.departments.firstOrNull()?.id.orEmpty())
  }
  var selectedWpId by rememberSaveable {
    mutableStateOf(state.workplaces.firstOrNull()?.id.orEmpty())
  }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("owner_add_employee_screen"),
  ) {
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Invite Employee", style = MaterialTheme.typography.headlineMedium)
          Text(
            text = "Create an authorised employee invitation token hashed into Supabase. Share the generated invite link with the employee so they can register and link to this organisation.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          CivicFormField(
            value = email,
            onValueChange = { email = it },
            label = "Employee Work Email",
            enabled = !state.isSubmitting,
            testTag = "add_emp_email_input",
          )

          if (state.departments.isNotEmpty()) {
            Text("Assign Department", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              state.departments.forEach { dept ->
                FilterChip(
                  selected = selectedDeptId == dept.id,
                  onClick = { selectedDeptId = dept.id },
                  label = { Text(dept.name) },
                )
              }
            }
          }

          if (state.workplaces.isNotEmpty()) {
            Text("Assign Workplace", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              state.workplaces.forEach { wp ->
                FilterChip(
                  selected = selectedWpId == wp.id,
                  onClick = { selectedWpId = wp.id },
                  label = { Text(wp.name) },
                )
              }
            }
          }
        }
      }
    }

    item {
      CivicPrimaryButton(
        text = if (state.isSubmitting) "Generating Invitation..." else "Create Employee Invitation",
        icon = Icons.Filled.GroupAdd,
        enabled = !state.isSubmitting,
        testTag = "add_emp_submit_button",
        onClick = { onInviteEmployee(email, selectedDeptId, selectedWpId) },
      )
    }

    state.lastGeneratedEmployeeInviteLink?.let { link ->
      item {
        CivicNoticeBanner(
          title = "One-Time Employee Invite Link",
          message = link,
        )
      }
    }
  }
}

// 6. Supervisor Directory
@Composable
fun OwnerSupervisorDirectoryScreen(
  state: FrontendUiState,
  onOpenInviteSupervisor: () -> Unit,
  onDismissBanner: () -> Unit,
) {
  var searchQuery by rememberSaveable { mutableStateOf("") }
  val filtered =
    state.supervisors.filter { sup ->
      searchQuery.isBlank() ||
        sup.fullName.contains(searchQuery, ignoreCase = true) ||
        sup.departmentName.contains(searchQuery, ignoreCase = true) ||
        sup.workplaceName.contains(searchQuery, ignoreCase = true)
    }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("owner_supervisor_directory_screen"),
  ) {
    item {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Supervisor Directory", style = MaterialTheme.typography.headlineMedium)
        Text(
          text = "Supervisors have authority restricted to their assigned department and workplace.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    item {
      CivicPrimaryButton(
        text = "Invite New Supervisor",
        icon = Icons.Filled.SupervisorAccount,
        testTag = "open_invite_supervisor_button",
        onClick = onOpenInviteSupervisor,
      )
    }

    item {
      CivicSearchBar(
        query = searchQuery,
        onQueryChange = { searchQuery = it },
        placeholder = "Filter supervisors by name, department or site...",
      )
    }

    if (filtered.isEmpty()) {
      item {
        CivicEmptyState(
          icon = Icons.Filled.SupervisorAccount,
          title = "No supervisors match filter",
          description = "Clear your filter or invite a supervisor to oversee an operational unit.",
        )
      }
    } else {
      items(filtered, key = { it.id }) { sup ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(sup.fullName, style = MaterialTheme.typography.titleMedium)
              CivicPill(
                label = sup.status,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
              )
            }
            if (sup.email.isNotBlank()) {
              Text(sup.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Filled.Business, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
              Spacer(Modifier.width(6.dp))
              Text(
                text = "${sup.departmentName} • ${sup.workplaceName} • ${sup.assignedStaffCount} staff",
                style = MaterialTheme.typography.labelMedium,
              )
            }
          }
        }
      }
    }
  }
}
