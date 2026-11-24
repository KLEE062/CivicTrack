package com.example.ui.frontend.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import com.example.ui.components.PermissionBlockedBanner
import com.example.ui.frontend.components.CivicConfirmationDialog
import com.example.ui.frontend.components.CivicDetailFieldRow
import com.example.ui.frontend.components.CivicEmptyState
import com.example.ui.frontend.components.CivicFilterChipRow
import com.example.ui.frontend.components.CivicFormField
import com.example.ui.frontend.components.CivicLeaveStatusBadge
import com.example.ui.frontend.components.CivicNoticeBanner
import com.example.ui.frontend.components.CivicNotificationCard
import com.example.ui.frontend.components.CivicPill
import com.example.ui.frontend.components.CivicPrimaryButton
import com.example.ui.frontend.components.CivicProfileHeader
import com.example.ui.frontend.components.CivicResponsiveScreenContainer
import com.example.ui.frontend.components.CivicSecondaryButton
import com.example.ui.frontend.components.SupabaseConnectionCard
import com.example.ui.frontend.data.CivicLeaveStatus
import com.example.ui.frontend.data.CivicRole
import com.example.ui.frontend.data.CivicThemeMode
import com.example.ui.frontend.state.FrontendUiState
import java.time.LocalDate
import java.time.ZoneOffset

// 8. Work Schedule Screen (Employee)
@Composable
fun EmployeeWorkScheduleScreen(
  state: FrontendUiState,
  onDismissBanner: () -> Unit,
) {
  val emp = state.activeEmployee
  val dept = emp?.departmentName ?: "Assigned Department"
  val mySchedules =
    state.schedules.filter {
      it.departmentName == dept || it.departmentName == "All Departments"
    }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("employee_work_schedule_screen"),
  ) {
    item { Text("My Work Schedule", style = MaterialTheme.typography.headlineMedium) }
    item {
      CivicNoticeBanner(
        title = "Assigned Shift Timetable",
        message =
          "Showing shift policy for ${emp?.fullName ?: state.currentAccountName} ($dept at ${emp?.workplaceName ?: "Assigned Workplace"}).",
      )
    }

    if (mySchedules.isEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Organisation Default Shift", style = MaterialTheme.typography.titleLarge)
            CivicDetailFieldRow(
              "Shift Hours",
              "${state.orgSettings.workStartTime} – ${state.orgSettings.workEndTime}",
            )
            CivicDetailFieldRow(
              "Working Days",
              state.orgSettings.workingDays.joinToString(", "),
            )
            CivicDetailFieldRow(
              "Late Grace Policy",
              "${state.orgSettings.lateGraceMinutes} minutes",
              showDivider = false,
            )
          }
        }
      }
    } else {
      items(mySchedules, key = { it.id }) { sch ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(sch.title, style = MaterialTheme.typography.titleLarge)
            CivicDetailFieldRow("Department", sch.departmentName)
            CivicDetailFieldRow("Workplace", emp?.workplaceName ?: "Assigned Workplace")
            CivicDetailFieldRow("Shift Hours", "${sch.startTime} – ${sch.endTime}")
            CivicDetailFieldRow("Working Days", sch.workingDays.joinToString(", "))
            CivicDetailFieldRow("Late Grace Policy", "${sch.lateGraceMinutes} minutes", showDivider = false)
          }
        }
      }
    }
  }
}

// 9. Leave Application Screen (Employee)
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmployeeLeaveApplicationScreen(
  state: FrontendUiState,
  onSubmitLeave: (String, String, String, String, String, () -> Unit) -> Unit,
  onSubmittedNavigateToHistory: () -> Unit,
  onDismissBanner: () -> Unit,
) {
  val leaveTypes = listOf("Annual Leave", "Medical Appointment", "Compassionate Leave", "Study Leave")
  val defaultTomorrow = LocalDate.now(ZoneOffset.UTC).plusDays(1).toString()
  var selectedType by rememberSaveable { mutableStateOf(leaveTypes.first()) }
  var startDate by rememberSaveable { mutableStateOf(defaultTomorrow) }
  var endDate by rememberSaveable { mutableStateOf(defaultTomorrow) }
  var daysText by rememberSaveable { mutableStateOf("1") }
  var reason by rememberSaveable { mutableStateOf("") }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("employee_leave_application_screen"),
  ) {
    item { Text("Apply for Leave", style = MaterialTheme.typography.headlineMedium) }

    if (!state.leaveFeatureAvailable) {
      item {
        CivicNoticeBanner(
          title = "Leave Module Not Provisioned",
          message = "The leave_requests table is not currently enabled on this Supabase project.",
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
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Leave Category", style = MaterialTheme.typography.titleMedium)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            leaveTypes.forEach { type ->
              FilterChip(
                selected = selectedType == type,
                onClick = { selectedType = type },
                label = { Text(type) },
              )
            }
          }
          CivicFormField(
            value = startDate,
            onValueChange = { startDate = it },
            label = "Start Date (YYYY-MM-DD)",
            enabled = !state.isSubmitting,
            testTag = "emp_leave_start_input",
          )
          CivicFormField(
            value = endDate,
            onValueChange = { endDate = it },
            label = "End Date (YYYY-MM-DD)",
            enabled = !state.isSubmitting,
            testTag = "emp_leave_end_input",
          )
          CivicFormField(
            value = daysText,
            onValueChange = { daysText = it.filter(Char::isDigit) },
            label = "Number of Working Days",
            enabled = !state.isSubmitting,
            testTag = "emp_leave_days_input",
          )
          CivicFormField(
            value = reason,
            onValueChange = { reason = it },
            label = "Reason & Handover Notes",
            placeholder = "Explain the reason for leave and coverage arrangements...",
            singleLine = false,
            enabled = !state.isSubmitting,
            testTag = "emp_leave_reason_input",
          )
        }
      }
    }

    item {
      CivicPrimaryButton(
        text = if (state.isSubmitting) "Submitting..." else "Submit Leave Application",
        icon = Icons.Filled.CalendarMonth,
        enabled = !state.isSubmitting && state.leaveFeatureAvailable,
        testTag = "emp_leave_submit_button",
        onClick = {
          onSubmitLeave(selectedType, startDate, endDate, daysText, reason) {
            reason = ""
            onSubmittedNavigateToHistory()
          }
        },
      )
    }
  }
}

// 10. Leave History Screen (Employee)
@Composable
fun EmployeeLeaveHistoryScreen(
  state: FrontendUiState,
  onOpenApplyLeave: () -> Unit,
  onCancelLeave: (String) -> Unit,
  onDismissBanner: () -> Unit,
) {
  val empId = state.activeEmployee?.id
  var selectedTab by rememberSaveable { mutableStateOf("All") }
  var pendingCancelId by rememberSaveable { mutableStateOf<String?>(null) }

  val myLeaves =
    state.leaveRequests.filter { req ->
      (empId == null || req.employeeId == empId) &&
        when (selectedTab) {
          "Pending" -> req.status == CivicLeaveStatus.PENDING
          "Approved" -> req.status == CivicLeaveStatus.APPROVED
          "Rejected" -> req.status == CivicLeaveStatus.REJECTED
          else -> true
        }
    }

  if (pendingCancelId != null) {
    CivicConfirmationDialog(
      title = "Withdraw Leave Request?",
      message = "Delete this pending leave application from Supabase?",
      confirmLabel = "Withdraw",
      isDestructive = true,
      onConfirm = {
        onCancelLeave(pendingCancelId!!)
        pendingCancelId = null
      },
      onDismiss = { pendingCancelId = null },
    )
  }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("employee_leave_history_screen"),
  ) {
    item { Text("My Leave History", style = MaterialTheme.typography.headlineMedium) }
    item {
      CivicPrimaryButton(
        text = "New Leave Application",
        icon = Icons.Filled.Add,
        enabled = state.leaveFeatureAvailable,
        testTag = "emp_leave_history_apply_button",
        onClick = onOpenApplyLeave,
      )
    }
    item {
      CivicFilterChipRow(
        options = listOf("All", "Pending", "Approved", "Rejected"),
        selectedOption = selectedTab,
        onOptionSelected = { selectedTab = it },
      )
    }

    if (myLeaves.isEmpty()) {
      item {
        CivicEmptyState(
          icon = Icons.Filled.EventBusy,
          title = "No $selectedTab leave records",
          description = "Submit a new leave request or select another status filter.",
        )
      }
    } else {
      items(myLeaves, key = { it.id }) { req ->
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
              Text(req.leaveType, style = MaterialTheme.typography.titleMedium)
              CivicLeaveStatusBadge(status = req.status)
            }
            Text("${req.startDate} → ${req.endDate} (${req.daysCount} day(s))", style = MaterialTheme.typography.bodyMedium)
            Text(req.reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            req.reviewerNote?.let { note ->
              Text("Reviewer Note: $note", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            if (req.status == CivicLeaveStatus.PENDING) {
              CivicSecondaryButton(
                text = "Withdraw Pending Request",
                isDestructive = true,
                enabled = !state.isSubmitting,
                testTag = "emp_cancel_leave_${req.id}",
                onClick = { pendingCancelId = req.id },
              )
            }
          }
        }
      }
    }
  }
}

// 11. Notifications Screen (Employee)
@Composable
fun EmployeeNotificationsScreen(
  state: FrontendUiState,
  onMarkRead: (String) -> Unit,
  onMarkAllRead: () -> Unit,
  onDismissBanner: () -> Unit,
) {
  var filter by rememberSaveable { mutableStateOf("All") }
  val empNotifications =
    state.notifications.filter {
      it.targetRole == CivicRole.EMPLOYEE && (filter == "All" || !it.isRead)
    }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("employee_notifications_screen"),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("My Notifications", style = MaterialTheme.typography.headlineMedium)
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

    if (empNotifications.isEmpty()) {
      item {
        CivicEmptyState(
          icon = Icons.Filled.Notifications,
          title = "No unread notifications",
          description = "All employee alerts have been marked as read.",
        )
      }
    } else {
      items(empNotifications, key = { it.id }) { notif ->
        CivicNotificationCard(notification = notif, onMarkRead = { onMarkRead(notif.id) })
      }
    }
  }
}

// 12. Employee Documents & Photo Capture Screen
@Composable
fun EmployeeDocumentsScreen(
  state: FrontendUiState,
  onStartProfilePhotoCapture: () -> Unit,
  onStartDocumentCapture: () -> Unit,
  onPickDocumentFromGallery: () -> Unit,
  onOpenAppSettings: () -> Unit,
  onDismissBlockedBanner: () -> Unit,
  onDismissBanner: () -> Unit,
) {
  val categories = listOf("All", "ID_BADGE", "SITE_PERMIT", "MEDICAL_CLEARANCE")
  var selectedCategory by rememberSaveable { mutableStateOf("All") }
  val cameraAvailable = state.capabilities?.cameraAvailable != false

  val filtered =
    state.employeeDocuments.filter { selectedCategory == "All" || it.documentType == selectedCategory }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("employee_documents_screen"),
  ) {
    item { Text("My Personnel Documents", style = MaterialTheme.typography.headlineMedium) }

    state.permissionBlockedMessage?.let { blockedMsg ->
      item {
        PermissionBlockedBanner(
          message = blockedMsg,
          requiresLocationSettings = false,
          onOpenSettings = onOpenAppSettings,
          onDismiss = onDismissBlockedBanner,
        )
      }
    }

    if (!cameraAvailable) {
      item {
        CivicNoticeBanner(
          title = "Camera Hardware Unavailable",
          message = "No active camera sensor was detected on this device. You can still upload documents via the Android Photo Picker.",
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
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Capture & Upload Media", style = MaterialTheme.typography.titleMedium)
          CivicPrimaryButton(
            text = "Capture Profile Photo",
            icon = Icons.Filled.CameraAlt,
            enabled = cameraAvailable && !state.isUploadingMedia,
            testTag = "emp_capture_profile_photo_button",
            onClick = onStartProfilePhotoCapture,
          )
          CivicSecondaryButton(
            text = "Scan Document with Camera",
            icon = Icons.Filled.CameraAlt,
            enabled = cameraAvailable && !state.isUploadingMedia,
            testTag = "emp_capture_document_button",
            onClick = onStartDocumentCapture,
          )
          CivicSecondaryButton(
            text = "Choose Document from Gallery",
            icon = Icons.Filled.PhotoLibrary,
            enabled = !state.isUploadingMedia,
            testTag = "emp_pick_document_button",
            onClick = onPickDocumentFromGallery,
          )
        }
      }
    }

    item {
      CivicFilterChipRow(
        options = categories,
        selectedOption = selectedCategory,
        onOptionSelected = { selectedCategory = it },
      )
    }

    if (filtered.isEmpty()) {
      item {
        CivicEmptyState(
          icon = Icons.Filled.Description,
          title = "No uploaded documents",
          description = "Capture a document with CameraX or pick an image from your gallery to upload to Supabase Storage.",
        )
      }
    } else {
      items(filtered, key = { it.id }) { doc ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(doc.title, style = MaterialTheme.typography.titleMedium)
              CivicPill(
                label = doc.documentType,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
              )
            }
            Text(
              text = "${doc.mimeType} • ${doc.fileSizeBytes / 1024} KB • ${doc.uploadedAtIso.take(10)}",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

// 13. Employee Profile Screen
@Composable
fun EmployeeProfileScreen(
  state: FrontendUiState,
  onSaveProfile: (String, String) -> Unit,
  onStartProfilePhotoCapture: () -> Unit,
  onOpenSettings: () -> Unit,
  onDismissBanner: () -> Unit,
) {
  val emp = state.activeEmployee
  var fullName by rememberSaveable(state.currentAccountName) { mutableStateOf(state.currentAccountName) }
  var phone by rememberSaveable(state.currentAccountPhone) { mutableStateOf(state.currentAccountPhone) }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("employee_profile_screen"),
  ) {
    item {
      CivicProfileHeader(
        fullName = state.currentAccountName.ifBlank { state.currentAccountEmail },
        roleLabel = emp?.jobTitle ?: "Workforce Member",
        subtitle = emp?.employeeCode?.ifBlank { state.currentAccountEmail } ?: state.currentAccountEmail,
        badgeText = "EMPLOYEE RECORD",
        avatarUrl = emp?.avatarUrl ?: state.currentAccountAvatarUrl,
      )
    }
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Assigned Role & Workplace", style = MaterialTheme.typography.titleMedium)
          CivicDetailFieldRow("Employee Code", emp?.employeeCode?.ifBlank { "Unassigned" } ?: "Unassigned")
          CivicDetailFieldRow("Department", emp?.departmentName ?: "Unassigned")
          CivicDetailFieldRow("Workplace", emp?.workplaceName ?: "Unassigned")
          CivicDetailFieldRow("Work Email", state.currentAccountEmail, showDivider = false)
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
          Text("Editable Contact Information", style = MaterialTheme.typography.titleMedium)
          CivicFormField(
            value = fullName,
            onValueChange = { fullName = it },
            label = "Full Name",
            enabled = !state.isSubmitting,
          )
          CivicFormField(
            value = phone,
            onValueChange = { phone = it },
            label = "Phone Number",
            enabled = !state.isSubmitting,
          )
          CivicPrimaryButton(
            text = if (state.isSubmitting) "Saving Contact Info..." else "Update Contact Info",
            enabled = !state.isSubmitting,
            onClick = { onSaveProfile(fullName, phone) },
          )
          CivicSecondaryButton(
            text = "Update Profile Photo",
            icon = Icons.Filled.CameraAlt,
            enabled = state.capabilities?.cameraAvailable != false,
            onClick = onStartProfilePhotoCapture,
          )
          CivicSecondaryButton(text = "Open App Settings", onClick = onOpenSettings)
        }
      }
    }
  }
}

// 14. Employee Settings Screen
@Composable
fun EmployeeSettingsScreen(
  state: FrontendUiState,
  onToggleEmployeeSettings: (Boolean, Boolean) -> Unit,
  onToggleBiometricLogin: (Boolean) -> Unit,
  onThemeChange: (CivicThemeMode) -> Unit,
  onRecheckConnection: () -> Unit,
  onSignOut: () -> Unit,
  onDismissBanner: () -> Unit,
) {
  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("employee_settings_screen"),
  ) {
    item { Text("Employee App Settings", style = MaterialTheme.typography.headlineMedium) }
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Shift & Security Preferences", style = MaterialTheme.typography.titleMedium)
          SettingToggleRow(
            title = "Attendance & Leave Alerts",
            subtitle = "Receive alerts when leave decisions or exceptions update",
            checked = state.employeeNotificationAlertsEnabled,
            onCheckedChange = {
              onToggleEmployeeSettings(it, state.employeeScheduleRemindersEnabled)
            },
          )
          SettingToggleRow(
            title = "Shift Start Reminders",
            subtitle = "Display pre-shift reminder notices before check-in window",
            checked = state.employeeScheduleRemindersEnabled,
            onCheckedChange = {
              onToggleEmployeeSettings(state.employeeNotificationAlertsEnabled, it)
            },
          )
          SettingToggleRow(
            title = "Biometric Quick-Unlock",
            subtitle = "Allow unlocking saved Supabase session with OS biometrics",
            checked = state.biometricLoginEnabled,
            onCheckedChange = onToggleBiometricLogin,
          )
          Text("Theme Appearance", style = MaterialTheme.typography.titleMedium)
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
      CivicNoticeBanner(
        title = "Location & Biometric Privacy Notice",
        message =
          "Location coordinates are requested strictly during authorized attendance check-in/out operations, and biometric credentials remain inside Android Keystore.",
      )
    }
    item {
      CivicSecondaryButton(
        text = "Sign Out to Entry Screen",
        icon = Icons.AutoMirrored.Filled.Logout,
        isDestructive = true,
        testTag = "employee_sign_out_button",
        onClick = onSignOut,
      )
    }
  }
}
