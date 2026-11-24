package com.example.ui.frontend.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.PermissionBlockedBanner
import com.example.ui.frontend.components.CivicAttendanceStatusBadge
import com.example.ui.frontend.components.CivicConfirmationDialog
import com.example.ui.frontend.components.CivicDetailFieldRow
import com.example.ui.frontend.components.CivicEmptyState
import com.example.ui.frontend.components.CivicFilterChipRow
import com.example.ui.frontend.components.CivicFormField
import com.example.ui.frontend.components.CivicGeofenceBoundaryCard
import com.example.ui.frontend.components.CivicLeaveStatusBadge
import com.example.ui.frontend.components.CivicNoticeBanner
import com.example.ui.frontend.components.CivicPill
import com.example.ui.frontend.components.CivicPrimaryButton
import com.example.ui.frontend.components.CivicResponsiveScreenContainer
import com.example.ui.frontend.components.CivicSearchBar
import com.example.ui.frontend.components.CivicSecondaryButton
import com.example.ui.frontend.data.CivicAttendanceStatus
import com.example.ui.frontend.data.CivicLeaveStatus
import com.example.ui.frontend.state.FrontendUiState

// 7. Invite Supervisor Interface
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OwnerInviteSupervisorScreen(
  state: FrontendUiState,
  onInviteSupervisor: (email: String, departmentId: String, workplaceId: String) -> Unit,
  onDismissBanner: () -> Unit,
) {
  var email by rememberSaveable { mutableStateOf("") }
  var selectedDeptId by rememberSaveable { mutableStateOf(state.departments.firstOrNull()?.id.orEmpty()) }
  var selectedWpId by rememberSaveable { mutableStateOf(state.workplaces.firstOrNull()?.id.orEmpty()) }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("owner_invite_supervisor_screen"),
  ) {
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Invite Supervisor", style = MaterialTheme.typography.headlineMedium)
          Text(
            text = "Assign an authorised department and workplace scope, then generate a hashed one-time invitation token in Supabase.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          CivicFormField(
            value = email,
            onValueChange = { email = it },
            label = "Supervisor Email",
            enabled = !state.isSubmitting,
            testTag = "invite_sup_email_input",
          )

          if (state.departments.isNotEmpty()) {
            Text("Department Scope", style = MaterialTheme.typography.titleMedium)
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
            Text("Workplace Scope", style = MaterialTheme.typography.titleMedium)
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

          CivicPrimaryButton(
            text = if (state.isSubmitting) "Generating Invitation..." else "Generate Supervisor Invitation",
            icon = Icons.Filled.GroupAdd,
            enabled = !state.isSubmitting,
            testTag = "invite_sup_submit_button",
            onClick = { onInviteSupervisor(email, selectedDeptId, selectedWpId) },
          )

          state.lastGeneratedSupervisorInviteLink?.let { link ->
            CivicNoticeBanner(
              title = "One-Time Supervisor Invite Link",
              message = link,
            )
          }
        }
      }
    }
  }
}

// 8. Department Management
@Composable
fun OwnerDepartmentManagementScreen(
  state: FrontendUiState,
  onAddDepartment: (name: String, code: String, description: String) -> Unit,
  onDismissBanner: () -> Unit,
) {
  var searchQuery by rememberSaveable { mutableStateOf("") }
  var showAddForm by rememberSaveable { mutableStateOf(false) }
  var name by rememberSaveable { mutableStateOf("") }
  var code by rememberSaveable { mutableStateOf("") }
  var description by rememberSaveable { mutableStateOf("") }

  val filtered =
    state.departments.filter {
      searchQuery.isBlank() ||
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.code.contains(searchQuery, ignoreCase = true)
    }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("owner_departments_screen"),
  ) {
    item { Text("Department Management", style = MaterialTheme.typography.headlineMedium) }
    item {
      CivicSecondaryButton(
        text = if (showAddForm) "Hide Add Department Form" else "Add New Department",
        icon = Icons.Filled.AddBusiness,
        testTag = "toggle_add_dept_button",
        onClick = { showAddForm = !showAddForm },
      )
    }

    if (showAddForm) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CivicFormField(value = name, onValueChange = { name = it }, label = "Department Name", enabled = !state.isSubmitting, testTag = "dept_name_input")
            CivicFormField(value = code, onValueChange = { code = it }, label = "Department Code (Optional)", enabled = !state.isSubmitting)
            CivicFormField(value = description, onValueChange = { description = it }, label = "Description", singleLine = false, enabled = !state.isSubmitting)
            CivicPrimaryButton(
              text = if (state.isSubmitting) "Saving..." else "Save Department",
              enabled = !state.isSubmitting,
              testTag = "dept_save_button",
              onClick = {
                onAddDepartment(name, code, description)
                name = ""
                code = ""
                description = ""
              },
            )
          }
        }
      }
    }

    item {
      CivicSearchBar(
        query = searchQuery,
        onQueryChange = { searchQuery = it },
        placeholder = "Filter departments by name or code...",
      )
    }

    if (filtered.isEmpty()) {
      item {
        CivicEmptyState(
          icon = Icons.Filled.AddBusiness,
          title = "No departments registered",
          description = "Create a department above to structure workforce teams.",
        )
      }
    } else {
      items(filtered, key = { it.id }) { dept ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(dept.name, style = MaterialTheme.typography.titleMedium)
              CivicPill(
                label = dept.code,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
            if (dept.description.isNotBlank()) {
              Text(dept.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
              text = "Lead: ${dept.headSupervisorName} • ${dept.staffCount} assigned staff",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }
    }
  }
}

// 9. Workplace Management
@Composable
fun OwnerWorkplaceManagementScreen(
  state: FrontendUiState,
  onCaptureCurrentGps: () -> Unit,
  onAddWorkplace: (name: String, address: String, latText: String, lngText: String, radiusText: String, maxAccuracyText: String) -> Unit,
  onOpenBlockedSettings: () -> Unit,
  onDismissBlockedBanner: () -> Unit,
  onDismissBanner: () -> Unit,
) {
  var showAddForm by rememberSaveable { mutableStateOf(false) }
  var name by rememberSaveable { mutableStateOf("") }
  var address by rememberSaveable { mutableStateOf("") }
  var lat by rememberSaveable { mutableStateOf("") }
  var lng by rememberSaveable { mutableStateOf("") }
  var radius by rememberSaveable { mutableStateOf("150") }
  var maxAccuracy by rememberSaveable { mutableStateOf("60") }

  LaunchedEffect(state.capturedWorkplaceLat, state.capturedWorkplaceLng) {
    state.capturedWorkplaceLat?.let { lat = "%.6f".format(it) }
    state.capturedWorkplaceLng?.let { lng = "%.6f".format(it) }
  }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("owner_workplaces_screen"),
  ) {
    if (state.permissionBlockedMessage != null) {
      item {
        PermissionBlockedBanner(
          message = state.permissionBlockedMessage,
          requiresLocationSettings = state.blockedPermissionRequiresLocationSettings,
          onOpenSettings = onOpenBlockedSettings,
          onDismiss = onDismissBlockedBanner,
        )
      }
    }

    item { Text("Workplace & Geofence Sites", style = MaterialTheme.typography.headlineMedium) }
    item {
      CivicSecondaryButton(
        text = if (showAddForm) "Cancel New Workplace" else "Register Workplace Site",
        icon = Icons.Filled.AddLocationAlt,
        testTag = "toggle_add_workplace_button",
        onClick = { showAddForm = !showAddForm },
      )
    }

    if (showAddForm) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CivicFormField(value = name, onValueChange = { name = it }, label = "Workplace Name", enabled = !state.isSubmitting, testTag = "wp_name_input")
            CivicFormField(value = address, onValueChange = { address = it }, label = "Physical Address", enabled = !state.isSubmitting)
            CivicSecondaryButton(
              text = if (state.isCapturingWorkplaceGps) "Acquiring Device GPS..." else "Capture Current Device GPS",
              icon = Icons.Filled.GpsFixed,
              enabled = !state.isCapturingWorkplaceGps && !state.isSubmitting,
              testTag = "wp_capture_gps_button",
              onClick = onCaptureCurrentGps,
            )
            state.capturedWorkplaceAccuracy?.let { acc ->
              Text(
                text = "GPS fix accuracy: ±%.1fm".format(acc),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Box(Modifier.weight(1f)) {
                CivicFormField(value = lat, onValueChange = { lat = it }, label = "Latitude", placeholder = "e.g. 0.313600", enabled = !state.isSubmitting, testTag = "wp_lat_input")
              }
              Box(Modifier.weight(1f)) {
                CivicFormField(value = lng, onValueChange = { lng = it }, label = "Longitude", placeholder = "e.g. 32.581100", enabled = !state.isSubmitting, testTag = "wp_lng_input")
              }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Box(Modifier.weight(1f)) {
                CivicFormField(value = radius, onValueChange = { radius = it.filter(Char::isDigit) }, label = "Geofence (m)", enabled = !state.isSubmitting)
              }
              Box(Modifier.weight(1f)) {
                CivicFormField(value = maxAccuracy, onValueChange = { maxAccuracy = it.filter(Char::isDigit) }, label = "Max GPS Error (m)", enabled = !state.isSubmitting)
              }
            }
            CivicPrimaryButton(
              text = if (state.isSubmitting) "Saving Workplace..." else "Save Workplace",
              enabled = !state.isSubmitting,
              testTag = "wp_save_button",
              onClick = {
                onAddWorkplace(name, address, lat, lng, radius, maxAccuracy)
                name = ""
                address = ""
              },
            )
          }
        }
      }
    }

    if (state.workplaces.isEmpty()) {
      item {
        CivicEmptyState(
          icon = Icons.Filled.AddLocationAlt,
          title = "No workplaces registered yet",
          description = "Register at least one geofenced workplace so employees can verify check-in coordinates.",
        )
      }
    } else {
      items(state.workplaces, key = { it.id }) { wp ->
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          CivicGeofenceBoundaryCard(
            workplaceName = wp.name,
            radiusMeters = wp.geofenceRadiusMeters,
            latitude = wp.latitude,
            longitude = wp.longitude,
          )
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              if (wp.address.isNotBlank()) {
                Text(wp.address, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              CivicDetailFieldRow("Active Window", wp.activeShiftWindow)
              CivicDetailFieldRow("Assigned Staff", "${wp.assignedStaffCount} employees")
              CivicDetailFieldRow("Max GPS Accuracy", "±${wp.maxAccuracyMeters}m", showDivider = false)
            }
          }
        }
      }
    }
  }
}

// 10. Work Schedule Management
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OwnerWorkScheduleScreen(
  state: FrontendUiState,
  onSaveScheduleAndPolicies: (
    startTime: String,
    endTime: String,
    graceMinutes: Int,
    workingDays: List<String>,
    requireLocation: Boolean,
    requireBiometric: Boolean,
    allowOutsideGeofence: Boolean,
  ) -> Unit,
  onDismissBanner: () -> Unit,
) {
  val settings = state.orgSettings
  var startTime by rememberSaveable(settings.settingsId, settings.workStartTime) { mutableStateOf(settings.workStartTime) }
  var endTime by rememberSaveable(settings.settingsId, settings.workEndTime) { mutableStateOf(settings.workEndTime) }
  var graceText by rememberSaveable(settings.settingsId, settings.lateGraceMinutes) { mutableStateOf(settings.lateGraceMinutes.toString()) }
  var selectedDays by rememberSaveable(settings.settingsId, settings.workingDays) { mutableStateOf(settings.workingDays) }
  val allDays = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("owner_work_schedule_screen"),
  ) {
    item { Text("Work Schedule Management", style = MaterialTheme.typography.headlineMedium) }

    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Organisation Default Shift Hours", style = MaterialTheme.typography.titleMedium)
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
              CivicFormField(value = startTime, onValueChange = { startTime = it }, label = "Start Time", enabled = !state.isSubmitting)
            }
            Box(Modifier.weight(1f)) {
              CivicFormField(value = endTime, onValueChange = { endTime = it }, label = "End Time", enabled = !state.isSubmitting)
            }
            Box(Modifier.weight(1f)) {
              CivicFormField(value = graceText, onValueChange = { graceText = it.filter(Char::isDigit) }, label = "Grace (min)", enabled = !state.isSubmitting)
            }
          }
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            allDays.forEach { day ->
              val active = day in selectedDays
              FilterChip(
                selected = active,
                onClick = {
                  selectedDays = if (active) selectedDays - day else selectedDays + day
                },
                label = { Text(day) },
              )
            }
          }
          CivicPrimaryButton(
            text = if (state.isSubmitting) "Saving Schedule..." else "Save Organisation Schedule",
            icon = Icons.Filled.Schedule,
            enabled = !state.isSubmitting,
            testTag = "schedule_save_button",
            onClick = {
              onSaveScheduleAndPolicies(
                startTime,
                endTime,
                graceText.toIntOrNull() ?: 15,
                selectedDays,
                settings.requireLocation,
                settings.requireBiometric,
                settings.allowOutsideGeofenceWithNote,
              )
            },
          )
        }
      }
    }

    items(state.schedules, key = { it.id }) { sch ->
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(sch.title, style = MaterialTheme.typography.titleMedium)
          Text(
            text = "${sch.departmentName} • ${sch.startTime} – ${sch.endTime} (Grace: ${sch.lateGraceMinutes}m)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "Days: ${sch.workingDays.joinToString(", ")}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }
  }
}

// 11. Attendance Overview (Owner)
@Composable
fun OwnerAttendanceOverviewScreen(
  state: FrontendUiState,
  onOpenRecordDetails: (String) -> Unit,
  onDismissBanner: () -> Unit,
) {
  var query by rememberSaveable { mutableStateOf("") }
  var selectedStatus by rememberSaveable { mutableStateOf("All") }
  val statusOptions = listOf("All") + CivicAttendanceStatus.entries.map { it.label }

  val filtered =
    state.attendanceRecords.filter { rec ->
      val matchesStatus = selectedStatus == "All" || rec.status.label == selectedStatus
      val matchesQuery =
        query.isBlank() ||
          rec.employeeName.contains(query, ignoreCase = true) ||
          rec.workplaceName.contains(query, ignoreCase = true)
      matchesStatus && matchesQuery
    }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("owner_attendance_overview_screen"),
  ) {
    item { Text("Organisational Attendance Overview", style = MaterialTheme.typography.headlineMedium) }
    item {
      CivicSearchBar(
        query = query,
        onQueryChange = { query = it },
        placeholder = "Filter attendance by staff name or workplace...",
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
          title = "No attendance records match",
          description = "Clear filters or wait for staff check-ins to sync to Supabase.",
          actionLabel = "Clear Filters",
          onAction = {
            query = ""
            selectedStatus = "All"
          },
        )
      }
    } else {
      items(filtered, key = { it.id }) { rec ->
        Card(
          onClick = { onOpenRecordDetails(rec.id) },
          modifier = Modifier.fillMaxWidth().testTag("owner_attendance_row_${rec.id}"),
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
                text = "${rec.workplaceName} • ${rec.dateLabel}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = "In: ${rec.checkInTime ?: "--"} • Out: ${rec.checkOutTime ?: "--"}",
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

// 12. Attendance Record Details (Owner)
@Composable
fun OwnerAttendanceDetailsScreen(
  state: FrontendUiState,
  recordId: String,
  onReviewRecord: (recordId: String, geofenceStatus: String?, reviewStatus: String, notes: String) -> Unit,
  onDismissBanner: () -> Unit,
) {
  val record = state.attendanceRecords.firstOrNull { it.id == recordId } ?: state.attendanceRecords.firstOrNull()
  var reviewNote by rememberSaveable(recordId) { mutableStateOf(record?.exceptionReason.orEmpty()) }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("owner_attendance_details_screen"),
  ) {
    if (record == null) {
      item {
        CivicEmptyState(
          icon = Icons.Filled.Schedule,
          title = "Attendance record unavailable",
          description = "Select a record from Attendance Overview.",
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
              Text(record.employeeName, style = MaterialTheme.typography.headlineMedium)
              CivicAttendanceStatusBadge(status = record.status)
            }
            CivicDetailFieldRow("Employee Code", record.employeeCode.ifBlank { "N/A" })
            CivicDetailFieldRow("Department", record.departmentName.ifBlank { "General" })
            CivicDetailFieldRow("Workplace", record.workplaceName)
            CivicDetailFieldRow("Date", record.dateLabel)
            CivicDetailFieldRow("Check-In", record.checkInTime ?: "Not logged")
            CivicDetailFieldRow("Check-Out", record.checkOutTime ?: "Not logged")
            CivicDetailFieldRow("Location Verification", record.locationNote)
            CivicDetailFieldRow("Biometric Check", record.biometricNote)
            CivicDetailFieldRow("Review Status", record.reviewedBy ?: record.reviewStatusRaw, showDivider = false)
          }
        }
      }

      val wp = state.workplaces.firstOrNull { it.name == record.workplaceName } ?: state.workplaces.firstOrNull()
      if (wp != null) {
        item {
          CivicGeofenceBoundaryCard(
            workplaceName = wp.name,
            radiusMeters = wp.geofenceRadiusMeters,
            latitude = wp.latitude,
            longitude = wp.longitude,
            isAttentionFlagged = record.status == CivicAttendanceStatus.EXCEPTION,
          )
        }
      }

      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Administrative Review Action", style = MaterialTheme.typography.titleMedium)
            CivicFormField(
              value = reviewNote,
              onValueChange = { reviewNote = it },
              label = "Audit / Exception Note",
              singleLine = false,
              enabled = !state.isSubmitting,
            )
            CivicPrimaryButton(
              text = "Approve Exception / Mark Compliant",
              icon = Icons.Filled.CheckCircle,
              enabled = !state.isSubmitting,
              testTag = "owner_approve_attendance_button",
              onClick = {
                onReviewRecord(record.id, "verified", "approved", reviewNote)
              },
            )
            CivicSecondaryButton(
              text = "Keep Flagged as Exception",
              isDestructive = true,
              enabled = !state.isSubmitting,
              onClick = {
                onReviewRecord(record.id, null, "flagged", reviewNote)
              },
            )
          }
        }
      }
    }
  }
}

// 13. Leave Management (Owner)
@Composable
fun OwnerLeaveManagementScreen(
  state: FrontendUiState,
  onDecideLeave: (String, Boolean) -> Unit,
  onDismissBanner: () -> Unit,
) {
  var selectedTab by rememberSaveable { mutableStateOf("Pending") }
  var pendingDialogLeaveId by rememberSaveable { mutableStateOf<String?>(null) }
  var pendingDialogApprove by rememberSaveable { mutableStateOf(true) }

  val filtered =
    state.leaveRequests.filter {
      when (selectedTab) {
        "Pending" -> it.status == CivicLeaveStatus.PENDING
        "Approved" -> it.status == CivicLeaveStatus.APPROVED
        "Rejected" -> it.status == CivicLeaveStatus.REJECTED
        else -> true
      }
    }

  if (pendingDialogLeaveId != null) {
    CivicConfirmationDialog(
      title = if (pendingDialogApprove) "Approve Leave Request?" else "Reject Leave Request?",
      message =
        if (pendingDialogApprove) "Confirm approval of this leave request in Supabase?"
        else "Confirm rejection of this leave request in Supabase?",
      confirmLabel = if (pendingDialogApprove) "Approve" else "Reject",
      isDestructive = !pendingDialogApprove,
      onConfirm = {
        onDecideLeave(pendingDialogLeaveId!!, pendingDialogApprove)
        pendingDialogLeaveId = null
      },
      onDismiss = { pendingDialogLeaveId = null },
    )
  }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("owner_leave_management_screen"),
  ) {
    item { Text("Organisational Leave Management", style = MaterialTheme.typography.headlineMedium) }

    if (!state.leaveFeatureAvailable) {
      item {
        CivicNoticeBanner(
          title = "Leave Table Schema Missing",
          message = "The 'leave_requests' table was not found in your Supabase project. Run the leave schema migration to enable persistent leave approvals.",
          isWarning = true,
        )
      }
    }

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
          description = "Switch filter tabs to inspect other leave records.",
        )
      }
    } else {
      items(filtered, key = { it.id }) { req ->
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
              Column {
                Text(req.employeeName, style = MaterialTheme.typography.titleMedium)
                Text("${req.departmentName} • ${req.leaveType}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
              }
              CivicLeaveStatusBadge(status = req.status)
            }
            Text("${req.startDate} → ${req.endDate} (${req.daysCount} day(s))", style = MaterialTheme.typography.bodyMedium)
            if (req.reason.isNotBlank()) {
              Text(req.reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (req.status == CivicLeaveStatus.PENDING) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                  CivicPrimaryButton(
                    text = "Approve",
                    enabled = !state.isSubmitting,
                    testTag = "owner_approve_leave_${req.id}",
                    onClick = {
                      pendingDialogLeaveId = req.id
                      pendingDialogApprove = true
                    },
                  )
                }
                Box(Modifier.weight(1f)) {
                  CivicSecondaryButton(
                    text = "Reject",
                    isDestructive = true,
                    enabled = !state.isSubmitting,
                    testTag = "owner_reject_leave_${req.id}",
                    onClick = {
                      pendingDialogLeaveId = req.id
                      pendingDialogApprove = false
                    },
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
