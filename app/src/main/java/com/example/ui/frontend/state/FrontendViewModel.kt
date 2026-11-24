package com.example.ui.frontend.state

import android.Manifest
import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.biometric.AndroidBiometricAuthenticator
import com.example.core.biometric.BiometricAuthOutcome
import com.example.core.biometric.BiometricAuthenticator
import com.example.core.camera.ImageProcessingResult
import com.example.core.camera.ImageProcessor
import com.example.core.camera.ImageUploadPreset
import com.example.core.camera.ProcessedImageAsset
import com.example.core.capabilities.AndroidDeviceCapabilityManager
import com.example.core.capabilities.BiometricCapabilityState
import com.example.core.capabilities.DeviceCapabilityChecker
import com.example.core.capabilities.DeviceCapabilitySnapshot
import com.example.core.capabilities.LocationPermissionState
import com.example.core.capabilities.PermissionAccessState
import com.example.core.location.AttendanceLocationProvider
import com.example.core.location.FusedAttendanceLocationProvider
import com.example.core.location.GeofenceEvaluationResult
import com.example.core.location.GeofenceEvaluator
import com.example.core.location.LocationFetchResult
import com.example.core.security.InvitationTokenUtils
import com.example.data.local.DataStorePreferencesRepository
import com.example.data.local.PreferencesRepository
import com.example.data.model.AttendanceCheckType
import com.example.data.model.AttendanceRecordDto
import com.example.data.model.AttendanceSubmissionStatus
import com.example.data.model.AuditLogDto
import com.example.data.model.AuthSession
import com.example.data.model.BiometricStepStatus
import com.example.data.model.CreateDepartmentRequest
import com.example.data.model.CreateLeaveRequestPayload
import com.example.data.model.CreateWorkplaceRequest
import com.example.data.model.DepartmentDto
import com.example.data.model.DocumentCaptureCategory
import com.example.data.model.EmployeeDocumentDto
import com.example.data.model.EmployeeDto
import com.example.data.model.EmployeeProfileRecordDto
import com.example.data.model.GeofenceStepStatus
import com.example.data.model.LeaveRequestDto
import com.example.data.model.LocationVerificationOutcome
import com.example.data.model.OrgMemberRole
import com.example.data.model.OrganizationDto
import com.example.data.model.OrganizationMemberDto
import com.example.data.model.OrganizationProfilePatch
import com.example.data.model.OrganizationSettingsDto
import com.example.data.model.OwnerWorkspaceSnapshot
import com.example.data.model.RegistrationOutcome
import com.example.data.model.SupabaseConnectionReport
import com.example.data.model.SupervisorWorkspaceSnapshot
import com.example.data.model.WorkplaceDto
import com.example.data.remote.SupabaseClientFactory
import com.example.data.repository.AccountSignUpOutcome
import com.example.data.repository.AttendanceSubmissionPayload
import com.example.data.repository.BackendResult
import com.example.data.repository.CivicTrackRepository
import com.example.data.repository.InvitationValidationOutcome
import com.example.data.repository.OnboardingRepository
import com.example.data.repository.ResolvedUserContext
import com.example.data.repository.SupabaseCivicTrackRepository
import com.example.data.repository.SupabaseOnboardingRepository
import com.example.ui.CaptureTarget
import com.example.ui.RationaleType
import com.example.ui.frontend.data.CivicAttendanceRecord
import com.example.ui.frontend.data.CivicAttendanceStatus
import com.example.ui.frontend.data.CivicAuditEvent
import com.example.ui.frontend.data.CivicChartBar
import com.example.ui.frontend.data.CivicDepartment
import com.example.ui.frontend.data.CivicEmployee
import com.example.ui.frontend.data.CivicInvitationDetails
import com.example.ui.frontend.data.CivicLeaveRequest
import com.example.ui.frontend.data.CivicLeaveStatus
import com.example.ui.frontend.data.CivicNotification
import com.example.ui.frontend.data.CivicOrgSettings
import com.example.ui.frontend.data.CivicOrganization
import com.example.ui.frontend.data.CivicRole
import com.example.ui.frontend.data.CivicSupervisor
import com.example.ui.frontend.data.CivicThemeMode
import com.example.ui.frontend.data.CivicWorkSchedule
import com.example.ui.frontend.data.CivicWorkplace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

enum class LocationActionKind {
  CHECK_IN,
  CHECK_OUT,
  LIVE_LOCATION_CHECK,
  WORKPLACE_GPS_CAPTURE,
}

data class PendingOwnerDraft(
  val fullName: String,
  val email: String,
  val phone: String,
  val password: String,
  val orgName: String,
  val regNumber: String,
  val countryCode: String,
  val currencyCode: String,
)

data class FrontendUiState(
  val themeMode: CivicThemeMode = CivicThemeMode.SYSTEM,
  val connectionReport: SupabaseConnectionReport =
    SupabaseConnectionReport(
      isConfigured = false,
      isConnected = false,
      projectHost = null,
      authEndpointReachable = false,
      restEndpointReachable = false,
      latencyMs = null,
      statusSummary = "Checking Supabase Configuration...",
      guidance = "Verifying SUPABASE_URL and SUPABASE_ANON_KEY from BuildConfig.",
    ),
  val isCheckingConnection: Boolean = false,
  val isSubmitting: Boolean = false,
  val isRestoringSession: Boolean = false,
  val activeSession: AuthSession? = null,
  val activeRole: CivicRole? = null,
  val hasStoredSessionForBiometricUnlock: Boolean = false,
  val biometricLoginEnabled: Boolean = false,
  val currentUserId: String = "",
  val currentAccountName: String = "",
  val currentAccountEmail: String = "",
  val currentAccountPhone: String = "",
  val currentAccountAvatarUrl: String? = null,
  val pendingEmailVerificationAddress: String? = null,
  val pendingOwnerDraft: PendingOwnerDraft? = null,
  val passwordResetEmail: String = "",
  val incomingInviteToken: String? = null,
  val validatedInvitation: CivicInvitationDetails? = null,
  val validatedTokenHash: String? = null,
  val lastGeneratedEmployeeInviteLink: String? = null,
  val lastGeneratedSupervisorInviteLink: String? = null,
  val organization: CivicOrganization? = null,
  val orgSettings: CivicOrgSettings = CivicOrgSettings(),
  val departments: List<CivicDepartment> = emptyList(),
  val workplaces: List<CivicWorkplace> = emptyList(),
  val schedules: List<CivicWorkSchedule> = emptyList(),
  val supervisors: List<CivicSupervisor> = emptyList(),
  val employees: List<CivicEmployee> = emptyList(),
  val attendanceRecords: List<CivicAttendanceRecord> = emptyList(),
  val leaveRequests: List<CivicLeaveRequest> = emptyList(),
  val leaveFeatureAvailable: Boolean = true,
  val auditEvents: List<CivicAuditEvent> = emptyList(),
  val employeeDocuments: List<EmployeeDocumentDto> = emptyList(),
  val readNotificationIds: Set<String> = emptySet(),
  val currentSupervisorId: String? = null,
  val currentEmployeeId: String? = null,
  // Real device hardware & pipeline states
  val capabilities: DeviceCapabilitySnapshot? = null,
  val activeCheckType: AttendanceCheckType? = null,
  val biometricStep: BiometricStepStatus = BiometricStepStatus.Idle,
  val geofenceStep: GeofenceStepStatus = GeofenceStepStatus.Idle,
  val submissionStep: AttendanceSubmissionStatus = AttendanceSubmissionStatus.Idle,
  val pendingRetryPayload: AttendanceSubmissionPayload? = null,
  val activeRationale: RationaleType? = null,
  val pendingLocationAction: LocationActionKind? = null,
  val pendingCameraTarget: CaptureTarget? = null,
  val permissionBlockedMessage: String? = null,
  val blockedPermissionRequiresLocationSettings: Boolean = false,
  // Real CameraX & Storage Upload states
  val activeCameraTarget: CaptureTarget? = null,
  val selectedDocumentCategory: DocumentCaptureCategory = DocumentCaptureCategory.ID_BADGE,
  val documentTitleInput: String = "",
  val pendingUploadAsset: ProcessedImageAsset? = null,
  val isUploadingMedia: Boolean = false,
  // Workplace GPS coordinate capture states
  val capturedWorkplaceLat: Double? = null,
  val capturedWorkplaceLng: Double? = null,
  val capturedWorkplaceAccuracy: Double? = null,
  val isCapturingWorkplaceGps: Boolean = false,
  // User preferences
  val employeeNotificationAlertsEnabled: Boolean = true,
  val employeeScheduleRemindersEnabled: Boolean = true,
  val supervisorExceptionAlertsEnabled: Boolean = true,
  val supervisorLeaveDigestEnabled: Boolean = true,
  val settingsBiometricTestMessage: String? = null,
  val feedbackBanner: String? = null,
  val errorBanner: String? = null,
) {
  val activeSupervisor: CivicSupervisor?
    get() =
      supervisors.firstOrNull { it.id == currentSupervisorId || it.userId == currentUserId }
        ?: supervisors.firstOrNull()

  val activeEmployee: CivicEmployee?
    get() =
      employees.firstOrNull { it.id == currentEmployeeId || it.userId == currentUserId }
        ?: employees.firstOrNull()

  val scopedSupervisorEmployees: List<CivicEmployee>
    get() {
      val sup = activeSupervisor ?: return emptyList()
      return employees.filter { emp ->
        val deptOk = sup.departmentId.isBlank() || emp.departmentId == sup.departmentId
        val wpOk = sup.workplaceId.isBlank() || emp.workplaceId == sup.workplaceId
        deptOk && wpOk
      }
    }

  val scopedSupervisorAttendance: List<CivicAttendanceRecord>
    get() {
      val scopedIds = scopedSupervisorEmployees.map { it.id }.toSet()
      if (scopedIds.isEmpty()) return emptyList()
      return attendanceRecords.filter { it.employeeId in scopedIds }
    }

  val scopedSupervisorLeaves: List<CivicLeaveRequest>
    get() {
      val scopedIds = scopedSupervisorEmployees.map { it.id }.toSet()
      if (scopedIds.isEmpty()) return emptyList()
      return leaveRequests.filter { it.employeeId in scopedIds }
    }

  val employeeShiftStatusLabel: String
    get() {
      val today = LocalDate.now(ZoneOffset.UTC).toString()
      val empId = activeEmployee?.id ?: return "Not Checked In"
      val todayLogs = attendanceRecords.filter { it.employeeId == empId && it.dateLabel.startsWith(today) }
      val hasOut = todayLogs.any { it.checkType == "CHECK_OUT" || !it.checkOutTime.isNullOrBlank() }
      val checkInLog = todayLogs.firstOrNull { it.checkType == "CHECK_IN" || !it.checkInTime.isNullOrBlank() }
      return when {
        hasOut -> "Checked Out Today"
        checkInLog != null -> "Checked In (${checkInLog.checkInTime ?: today})"
        else -> "Not Checked In Today"
      }
    }

  val notifications: List<CivicNotification>
    get() {
      val list = mutableListOf<CivicNotification>()
      val role = activeRole ?: return emptyList()
      when (role) {
        CivicRole.OWNER -> {
          leaveRequests.filter { it.status == CivicLeaveStatus.PENDING }.forEach { lv ->
            val id = "leave_${lv.id}"
            list +=
              CivicNotification(
                id = id,
                targetRole = CivicRole.OWNER,
                title = "Pending Leave: ${lv.employeeName}",
                message = "${lv.leaveType} (${lv.startDate} to ${lv.endDate})",
                timestampLabel = lv.submittedDate,
                category = "Leave Request",
                isRead = id in readNotificationIds,
              )
          }
          attendanceRecords
            .filter {
              it.status == CivicAttendanceStatus.EXCEPTION || it.status == CivicAttendanceStatus.LATE
            }
            .take(10)
            .forEach { rec ->
              val id = "att_${rec.id}"
              list +=
                CivicNotification(
                  id = id,
                  targetRole = CivicRole.OWNER,
                  title = "${rec.status.label}: ${rec.employeeName}",
                  message = "${rec.workplaceName} • ${rec.locationNote}",
                  timestampLabel = rec.dateLabel,
                  category = "Attendance",
                  isRead = id in readNotificationIds,
                )
            }
          auditEvents.take(10).forEach { ev ->
            val id = "aud_${ev.id}"
            list +=
              CivicNotification(
                id = id,
                targetRole = CivicRole.OWNER,
                title = ev.action,
                message = ev.summary,
                timestampLabel = ev.timestampLabel,
                category = ev.category,
                isRead = id in readNotificationIds,
              )
          }
        }
        CivicRole.SUPERVISOR -> {
          scopedSupervisorLeaves.filter { it.status == CivicLeaveStatus.PENDING }.forEach { lv ->
            val id = "sup_leave_${lv.id}"
            list +=
              CivicNotification(
                id = id,
                targetRole = CivicRole.SUPERVISOR,
                title = "Team Leave Request: ${lv.employeeName}",
                message = "${lv.leaveType} (${lv.startDate} to ${lv.endDate})",
                timestampLabel = lv.submittedDate,
                category = "Team Leave",
                isRead = id in readNotificationIds,
              )
          }
          scopedSupervisorAttendance
            .filter {
              it.status == CivicAttendanceStatus.EXCEPTION || it.status == CivicAttendanceStatus.LATE
            }
            .take(10)
            .forEach { rec ->
              val id = "sup_att_${rec.id}"
              list +=
                CivicNotification(
                  id = id,
                  targetRole = CivicRole.SUPERVISOR,
                  title = "Unit Flag: ${rec.employeeName}",
                  message = "${rec.status.label} • ${rec.locationNote}",
                  timestampLabel = rec.dateLabel,
                  category = "Unit Attendance",
                  isRead = id in readNotificationIds,
                )
            }
        }
        CivicRole.EMPLOYEE -> {
          val empId = activeEmployee?.id
          leaveRequests
            .filter { empId == null || it.employeeId == empId }
            .forEach { lv ->
              val id = "emp_leave_${lv.id}_${lv.status.name}"
              list +=
                CivicNotification(
                  id = id,
                  targetRole = CivicRole.EMPLOYEE,
                  title = "Leave ${lv.status.label}: ${lv.leaveType}",
                  message = "${lv.startDate} to ${lv.endDate}",
                  timestampLabel = lv.submittedDate,
                  category = "Leave Update",
                  isRead = id in readNotificationIds,
                )
            }
          attendanceRecords
            .filter { (empId == null || it.employeeId == empId) && it.status == CivicAttendanceStatus.EXCEPTION }
            .take(5)
            .forEach { rec ->
              val id = "emp_att_${rec.id}"
              list +=
                CivicNotification(
                  id = id,
                  targetRole = CivicRole.EMPLOYEE,
                  title = "Attendance Exception Flagged",
                  message = rec.locationNote,
                  timestampLabel = rec.dateLabel,
                  category = "Verification",
                  isRead = id in readNotificationIds,
                )
            }
        }
      }
      return list
    }

  val unreadNotificationsCountForActiveRole: Int
    get() = notifications.count { !it.isRead }

  val weeklyAttendanceChart: List<CivicChartBar>
    get() = computeWeeklyChartBars(attendanceRecords)

  val unitWeeklyChart: List<CivicChartBar>
    get() = computeWeeklyChartBars(scopedSupervisorAttendance)

  val personalWeeklyChart: List<CivicChartBar>
    get() {
      val empId = activeEmployee?.id ?: return emptyList()
      return computeWeeklyChartBars(attendanceRecords.filter { it.employeeId == empId })
    }

  val departmentComplianceChart: List<CivicChartBar>
    get() {
      if (departments.isEmpty() || attendanceRecords.isEmpty()) return emptyList()
      return departments.mapNotNull { dept ->
        val deptRecords = attendanceRecords.filter { it.departmentName == dept.name }
        if (deptRecords.isEmpty()) null
        else {
          val compliant = deptRecords.count { it.status == CivicAttendanceStatus.ON_TIME }.toFloat() / deptRecords.size
          CivicChartBar(label = dept.name.take(8), primaryValue = compliant)
        }
      }
    }
}

private fun computeWeeklyChartBars(records: List<CivicAttendanceRecord>): List<CivicChartBar> {
  if (records.isEmpty()) return emptyList()
  val grouped = records.groupBy { it.dateLabel.take(10) }
  return grouped.entries.sortedBy { it.key }.takeLast(5).map { (dateStr, recs) ->
    val dayLabel =
      runCatching {
        LocalDate.parse(dateStr).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US)
      }.getOrElse { dateStr.takeLast(5) }
    val total = recs.size.toFloat().coerceAtLeast(1f)
    val onTime = recs.count { it.status == CivicAttendanceStatus.ON_TIME } / total
    val flagged = recs.count { it.status != CivicAttendanceStatus.ON_TIME } / total
    CivicChartBar(label = dayLabel, primaryValue = onTime, secondaryValue = flagged)
  }
}

data class ParsedDeepLink(
  val rawInput: String,
  val tokenHash: String? = null,
  val type: String = "signup",
  val accessToken: String? = null,
  val refreshToken: String? = null,
  val code: String? = null,
  val inviteToken: String? = null,
)

class FrontendViewModel(
  private val onboardingRepository: OnboardingRepository,
  private val civicRepository: CivicTrackRepository,
  private val preferences: PreferencesRepository,
  private val capabilityChecker: DeviceCapabilityChecker,
  private val locationProvider: AttendanceLocationProvider,
  private val biometricAuthenticator: BiometricAuthenticator,
) : ViewModel() {

  private val _uiState = MutableStateFlow(FrontendUiState())
  val uiState: StateFlow<FrontendUiState> = _uiState.asStateFlow()

  private var activeOrgMember: OrganizationMemberDto? = null
  private var activeOrgDto: OrganizationDto? = null

  init {
    viewModelScope.launch {
      preferences.themeModePreference.collectLatest { stored ->
        val mode = CivicThemeMode.fromStorage(stored)
        _uiState.update { it.copy(themeMode = mode) }
      }
    }
    viewModelScope.launch {
      combine(preferences.biometricLoginEnabled, preferences.authSession) { enabled, saved ->
        enabled to (saved != null && enabled)
      }.collectLatest { (enabled, canUnlock) ->
        _uiState.update {
          it.copy(
            biometricLoginEnabled = enabled,
            hasStoredSessionForBiometricUnlock = canUnlock,
          )
        }
      }
    }
    checkSupabaseConnection()
  }

  fun clearBanners() {
    _uiState.update { it.copy(feedbackBanner = null, errorBanner = null) }
  }

  fun setThemeMode(mode: CivicThemeMode) {
    _uiState.update {
      it.copy(
        themeMode = mode,
        feedbackBanner = "Appearance preference saved (${mode.label}).",
      )
    }
    viewModelScope.launch { preferences.setThemeModePreference(mode.storageKey) }
  }

  fun setBiometricLoginEnabled(enabled: Boolean) {
    _uiState.update {
      it.copy(
        biometricLoginEnabled = enabled,
        feedbackBanner =
          if (enabled) "Biometric quick-unlock enabled for saved sessions."
          else "Biometric quick-unlock disabled.",
      )
    }
    viewModelScope.launch { preferences.setBiometricLoginEnabled(enabled) }
  }

  fun checkSupabaseConnection() {
    viewModelScope.launch {
      _uiState.update { it.copy(isCheckingConnection = true) }
      val session = _uiState.value.activeSession ?: preferences.authSession.first()
      val report = onboardingRepository.checkSupabaseConnection(session)
      _uiState.update { it.copy(connectionReport = report, isCheckingConnection = false) }
    }
  }

  fun refreshCapabilities(activity: Activity?) {
    viewModelScope.launch {
      val locAsked = preferences.wasPermissionRequested(Manifest.permission.ACCESS_FINE_LOCATION)
      val camAsked = preferences.wasPermissionRequested(Manifest.permission.CAMERA)
      val snapshot = capabilityChecker.inspectCapabilities(activity, locAsked, camAsked)
      _uiState.update { it.copy(capabilities = snapshot) }
    }
  }

  private val processedDeepLinkHashes = mutableSetOf<String>()

  fun setIncomingInviteToken(token: String?) {
    if (!token.isNullOrBlank()) {
      _uiState.update { it.copy(incomingInviteToken = token) }
    }
  }

  fun handleDeepLink(rawUri: String?, onNavigateToWorkspace: (CivicRole) -> Unit) {
    if (rawUri.isNullOrBlank()) return
    val parsed = parseDeepLinkUri(rawUri)
    val linkKey = parsed.rawInput
    if (processedDeepLinkHashes.contains(linkKey)) return
    processedDeepLinkHashes.add(linkKey)

    viewModelScope.launch {
      _uiState.update {
        it.copy(
          isSubmitting = true,
          errorBanner = null,
          feedbackBanner = "Verifying your email and preparing your CivicTrack account...",
        )
      }

      var establishedSession: AuthSession? = null

      if (!parsed.accessToken.isNullOrBlank()) {
        when (val hydrated = onboardingRepository.hydrateSession(parsed.accessToken, parsed.refreshToken)) {
          is BackendResult.Success -> establishedSession = hydrated.data
          is BackendResult.ApiFailure -> {
            _uiState.update { it.copy(isSubmitting = false, errorBanner = "Could not verify your sign-in link (${hydrated.message}).") }
            return@launch
          }
          is BackendResult.NetworkFailure -> {
            _uiState.update { it.copy(isSubmitting = false, errorBanner = hydrated.message) }
            return@launch
          }
          is BackendResult.NotConfigured -> {
            _uiState.update { it.copy(isSubmitting = false, errorBanner = "Supabase secrets not configured.") }
            return@launch
          }
        }
      } else if (!parsed.tokenHash.isNullOrBlank()) {
        when (val verifyResult = onboardingRepository.verifyEmailTokenHash(parsed.tokenHash, parsed.type)) {
          is BackendResult.Success -> {
            establishedSession = verifyResult.data
          }
          is BackendResult.ApiFailure -> {
            _uiState.update {
              it.copy(
                isSubmitting = false,
                errorBanner = "Email verification link invalid or expired (${verifyResult.message}).",
              )
            }
            return@launch
          }
          is BackendResult.NetworkFailure -> {
            _uiState.update { it.copy(isSubmitting = false, errorBanner = verifyResult.message) }
            return@launch
          }
          is BackendResult.NotConfigured -> {
            _uiState.update {
              it.copy(isSubmitting = false, errorBanner = "Supabase secrets not configured.")
            }
            return@launch
          }
        }
      }

      if (establishedSession != null) {
        preferences.saveAuthSession(establishedSession)

        val draft = _uiState.value.pendingOwnerDraft
        if (draft != null) {
          completeOwnerOrgCreation(establishedSession, draft) {
            _uiState.update {
              it.copy(
                isSubmitting = false,
                activeRole = CivicRole.OWNER,
                feedbackBanner = "Your organisation account is ready. Welcome to CivicTrack.",
              )
            }
            onNavigateToWorkspace(CivicRole.OWNER)
          }
          return@launch
        }

        routeSessionToWorkspace(establishedSession) { role ->
          val welcomeMsg =
            when (role) {
              CivicRole.OWNER -> "Your organisation account is ready. Welcome to CivicTrack."
              CivicRole.SUPERVISOR -> "Your supervisor account is ready. Welcome to CivicTrack."
              CivicRole.EMPLOYEE -> "Your employee account is ready. Welcome to CivicTrack."
            }
          _uiState.update {
            it.copy(
              isSubmitting = false,
              feedbackBanner = welcomeMsg,
            )
          }
          onNavigateToWorkspace(role)
        }
      } else if (!parsed.inviteToken.isNullOrBlank()) {
        setIncomingInviteToken(parsed.inviteToken)
        validateInvitationToken(parsed.inviteToken)
        _uiState.update { it.copy(isSubmitting = false, feedbackBanner = null) }
      } else {
        _uiState.update {
          it.copy(
            isSubmitting = false,
            errorBanner = "Verification link format unrecognized or expired.",
          )
        }
      }
    }
  }

  private fun parseDeepLinkUri(input: String): ParsedDeepLink {
    val trimmed = input.trim()
    val uri = runCatching { Uri.parse(trimmed) }.getOrNull()

    var tokenHash = uri?.getQueryParameter("token_hash")
    var type = uri?.getQueryParameter("type") ?: "signup"
    var code = uri?.getQueryParameter("code")
    var inviteToken = uri?.getQueryParameter("token")

    var accessToken: String? = null
    var refreshToken: String? = null

    val fragment = uri?.fragment
    if (!fragment.isNullOrBlank()) {
      val fragmentParams =
        fragment.split("&").associate { param ->
          val parts = param.split("=")
          if (parts.size == 2) parts[0] to Uri.decode(parts[1]) else "" to ""
        }
      accessToken = fragmentParams["access_token"]
      refreshToken = fragmentParams["refresh_token"]
      if (type == "signup" && fragmentParams["type"] != null) {
        type = fragmentParams["type"]!!
      }
    }

    if (tokenHash == null && trimmed.contains("token_hash=")) {
      tokenHash = trimmed.substringAfter("token_hash=").substringBefore("&").substringBefore("#")
    }
    if (accessToken == null && trimmed.contains("access_token=")) {
      accessToken = trimmed.substringAfter("access_token=").substringBefore("&")
    }
    if (refreshToken == null && trimmed.contains("refresh_token=")) {
      refreshToken = trimmed.substringAfter("refresh_token=").substringBefore("&")
    }
    if (inviteToken == null && trimmed.contains("token=") && !trimmed.contains("token_hash=")) {
      inviteToken = trimmed.substringAfter("token=").substringBefore("&")
    }

    return ParsedDeepLink(
      rawInput = trimmed,
      tokenHash = tokenHash?.trim()?.takeIf { it.isNotEmpty() },
      type = type.trim().ifEmpty { "signup" },
      accessToken = accessToken?.trim()?.takeIf { it.isNotEmpty() },
      refreshToken = refreshToken?.trim()?.takeIf { it.isNotEmpty() },
      code = code?.trim()?.takeIf { it.isNotEmpty() },
      inviteToken = inviteToken?.trim()?.takeIf { it.isNotEmpty() },
    )
  }

  fun unlockWithBiometrics(activity: FragmentActivity, onAuthenticated: (CivicRole) -> Unit) {
    viewModelScope.launch {
      val outcome =
        biometricAuthenticator.authenticate(
          activity = activity,
          title = "Unlock CivicTrack Session",
          subtitle = "Confirm device identity to restore your authenticated session",
          description = "OS biometric or screen-lock confirmation unlocks your saved local session.",
          allowDeviceCredentialFallback = true,
        )
      when (outcome) {
        is BiometricAuthOutcome.Success -> {
          val saved = preferences.authSession.first()
          if (saved == null) {
            _uiState.update { it.copy(errorBanner = "No saved Supabase session found.") }
          } else {
            routeSessionToWorkspace(saved, onAuthenticated)
          }
        }
        is BiometricAuthOutcome.Cancelled ->
          _uiState.update { it.copy(errorBanner = outcome.reason) }
        is BiometricAuthOutcome.LockedOut ->
          _uiState.update { it.copy(errorBanner = outcome.message) }
        BiometricAuthOutcome.NotEnrolled ->
          _uiState.update {
            it.copy(errorBanner = "No biometric or device lock credential enrolled on this device.")
          }
        is BiometricAuthOutcome.HardwareUnavailable ->
          _uiState.update { it.copy(errorBanner = outcome.message) }
        is BiometricAuthOutcome.Error ->
          _uiState.update { it.copy(errorBanner = outcome.message) }
      }
    }
  }

  fun testBiometricPrompt(activity: FragmentActivity) {
    val cap = capabilityChecker.checkBiometricAvailability(allowDeviceCredentialFallback = true)
    if (cap !is BiometricCapabilityState.Available) {
      val msg = capabilityChecker.userMessageForBiometricState(cap)
      _uiState.update {
        it.copy(
          settingsBiometricTestMessage = msg,
          biometricStep = BiometricStepStatus.Unavailable(msg),
        )
      }
      return
    }
    _uiState.update { it.copy(biometricStep = BiometricStepStatus.Prompting) }
    viewModelScope.launch {
      when (
        val outcome =
          biometricAuthenticator.authenticate(
            activity = activity,
            title = "Verify Device Biometrics",
            subtitle = "Testing Android Keystore / BiometricPrompt readiness",
            description = "Confirm biometric or device credential to test local hardware readiness.",
            allowDeviceCredentialFallback = true,
          )
      ) {
        is BiometricAuthOutcome.Success ->
          _uiState.update {
            it.copy(
              biometricStep = BiometricStepStatus.LocalDeviceAuthenticated(outcome.methodLabel),
              settingsBiometricTestMessage =
                "OS Biometric check succeeded (${outcome.methodLabel}). Cryptographic state stays in Android Keystore.",
              feedbackBanner = "Device biometric verification succeeded (${outcome.methodLabel}).",
            )
          }
        is BiometricAuthOutcome.Cancelled ->
          _uiState.update {
            it.copy(
              biometricStep = BiometricStepStatus.Cancelled(outcome.reason),
              settingsBiometricTestMessage = outcome.reason,
            )
          }
        is BiometricAuthOutcome.LockedOut ->
          _uiState.update {
            it.copy(
              biometricStep = BiometricStepStatus.Error(outcome.message),
              settingsBiometricTestMessage = outcome.message,
            )
          }
        BiometricAuthOutcome.NotEnrolled ->
          _uiState.update {
            val msg = "No biometric or screen lock credential enrolled in Android Security Settings."
            it.copy(
              biometricStep = BiometricStepStatus.Unavailable(msg),
              settingsBiometricTestMessage = msg,
            )
          }
        is BiometricAuthOutcome.HardwareUnavailable ->
          _uiState.update {
            it.copy(
              biometricStep = BiometricStepStatus.Unavailable(outcome.message),
              settingsBiometricTestMessage = outcome.message,
            )
          }
        is BiometricAuthOutcome.Error ->
          _uiState.update {
            it.copy(
              biometricStep = BiometricStepStatus.Error(outcome.message),
              settingsBiometricTestMessage = outcome.message,
            )
          }
      }
    }
  }

  // --- Auth & Onboarding ---

  fun signIn(
    email: String,
    password: String,
    onAuthenticated: (CivicRole) -> Unit,
  ) {
    val cleanEmail = email.trim()
    if (!InvitationTokenUtils.isValidEmail(cleanEmail)) {
      _uiState.update { it.copy(errorBanner = "Enter a valid email address.") }
      return
    }
    if (password.length < 6) {
      _uiState.update { it.copy(errorBanner = "Enter your account password.") }
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorBanner = null, feedbackBanner = null) }
      when (val result = onboardingRepository.signIn(cleanEmail, password)) {
        is BackendResult.NotConfigured ->
          _uiState.update {
            it.copy(
              isSubmitting = false,
              errorBanner = "Configure SUPABASE_URL and SUPABASE_ANON_KEY in AI Studio Secrets first.",
            )
          }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = result.message) }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = result.message) }
        is BackendResult.Success -> {
          preferences.saveAuthSession(result.data)
          routeSessionToWorkspace(result.data, onAuthenticated)
        }
      }
    }
  }

  private suspend fun routeSessionToWorkspace(
    session: AuthSession,
    onAuthenticated: (CivicRole) -> Unit,
  ) {
    _uiState.update {
      it.copy(
        activeSession = session,
        currentUserId = session.userId,
        currentAccountEmail = session.email,
      )
    }

    // Resolve organisation membership first (Owner or Supervisor)
    when (val ctxResult = onboardingRepository.resolveUserContext(session)) {
      is BackendResult.Success -> {
        val ctx = ctxResult.data
        applyResolvedProfile(session, ctx)
        val primaryMembership = ctx.primaryMembership
        val ownedOrg = ctx.ownedOrganizations.firstOrNull() ?: primaryMembership?.organizations

        when {
          primaryMembership?.parsedRole == OrgMemberRole.OWNER ||
            primaryMembership?.parsedRole == OrgMemberRole.ADMIN ||
            (primaryMembership == null && ownedOrg != null) -> {
            val org =
              ownedOrg
                ?: throw IllegalStateException("Owner account has no organisation record.")
            loadOwnerWorkspace(session, org, ctx.profile?.fullName ?: session.email)
            _uiState.update { it.copy(isSubmitting = false, activeRole = CivicRole.OWNER) }
            onAuthenticated(CivicRole.OWNER)
            return
          }
          primaryMembership?.parsedRole == OrgMemberRole.SUPERVISOR -> {
            loadSupervisorWorkspace(session, primaryMembership)
            _uiState.update { it.copy(isSubmitting = false, activeRole = CivicRole.SUPERVISOR) }
            onAuthenticated(CivicRole.SUPERVISOR)
            return
          }
          else -> {
            // Fall through to employee profile lookup
          }
        }
      }
      is BackendResult.NotConfigured -> {
        _uiState.update { it.copy(isSubmitting = false, errorBanner = "Supabase secrets not configured.") }
        return
      }
      else -> {
        // Proceed to try employee profile lookup if context failed or empty
      }
    }

    // Check employee profile record
    when (val empResult = civicRepository.loadEmployeeProfile(session)) {
      is BackendResult.Success -> {
        loadEmployeeWorkspace(session, empResult.data)
        _uiState.update { it.copy(isSubmitting = false, activeRole = CivicRole.EMPLOYEE) }
        onAuthenticated(CivicRole.EMPLOYEE)
      }
      is BackendResult.ApiFailure ->
        _uiState.update {
          it.copy(
            isSubmitting = false,
            errorBanner =
              "Signed in as ${session.email}, but no active Owner, Supervisor, or Employee role was found for this account yet.",
          )
        }
      is BackendResult.NetworkFailure ->
        _uiState.update { it.copy(isSubmitting = false, errorBanner = empResult.message) }
      is BackendResult.NotConfigured ->
        _uiState.update { it.copy(isSubmitting = false, errorBanner = "Supabase secrets not configured.") }
    }
  }

  private fun applyResolvedProfile(session: AuthSession, ctx: ResolvedUserContext) {
    val name =
      ctx.profile?.fullName?.takeIf { it.isNotBlank() }
        ?: session.email
    _uiState.update {
      it.copy(
        currentUserId = session.userId,
        currentAccountName = name,
        currentAccountEmail = ctx.profile?.email ?: session.email,
        currentAccountPhone = ctx.profile?.phone.orEmpty(),
        currentAccountAvatarUrl = ctx.profile?.avatarUrl,
      )
    }
  }

  fun registerOwner(
    fullName: String,
    email: String,
    phone: String,
    orgName: String,
    registrationNumber: String,
    countryCode: String,
    currencyCode: String,
    password: String,
    confirmPassword: String,
    onEmailVerificationNeeded: () -> Unit,
    onOwnerCreated: () -> Unit,
  ) {
    val cleanName = fullName.trim()
    val cleanEmail = email.trim()
    val cleanOrg = orgName.trim()
    if (cleanName.length < 2) {
      _uiState.update { it.copy(errorBanner = "Enter the organisation owner's full name.") }
      return
    }
    if (!InvitationTokenUtils.isValidEmail(cleanEmail)) {
      _uiState.update { it.copy(errorBanner = "Enter a valid work email address.") }
      return
    }
    if (cleanOrg.length < 2) {
      _uiState.update { it.copy(errorBanner = "Enter your organisation name.") }
      return
    }
    val pwErr = InvitationTokenUtils.validatePassword(password)
    if (pwErr != null) {
      _uiState.update { it.copy(errorBanner = pwErr) }
      return
    }
    if (password != confirmPassword) {
      _uiState.update { it.copy(errorBanner = "Passwords do not match.") }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorBanner = null, feedbackBanner = null) }
      when (
        val signUp =
          onboardingRepository.signUpAccount(
            fullName = cleanName,
            email = cleanEmail,
            phone = phone.trim().ifBlank { null },
            password = password,
            onboardingFlow = "owner",
          )
      ) {
        is BackendResult.NotConfigured ->
          _uiState.update {
            it.copy(
              isSubmitting = false,
              errorBanner = "Configure SUPABASE_URL and SUPABASE_ANON_KEY in AI Studio Secrets first.",
            )
          }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = signUp.message) }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = signUp.message) }
        is BackendResult.Success -> {
          val draft =
            PendingOwnerDraft(
              fullName = cleanName,
              email = cleanEmail,
              phone = phone.trim(),
              password = password,
              orgName = cleanOrg,
              regNumber = registrationNumber.trim(),
              countryCode = countryCode.trim().uppercase().ifBlank { "UG" },
              currencyCode = currencyCode.trim().uppercase().ifBlank { "UGX" },
            )
          when (val outcome = signUp.data) {
            is AccountSignUpOutcome.EmailConfirmationRequired -> {
              _uiState.update {
                it.copy(
                  isSubmitting = false,
                  pendingEmailVerificationAddress = outcome.email,
                  pendingOwnerDraft = draft,
                  currentAccountName = cleanName,
                  currentAccountEmail = cleanEmail,
                  feedbackBanner = "Confirmation link sent to ${outcome.email}. Verify your email to continue.",
                )
              }
              onEmailVerificationNeeded()
            }
            is AccountSignUpOutcome.SignedIn -> {
              preferences.saveAuthSession(outcome.session)
              completeOwnerOrgCreation(outcome.session, draft, onOwnerCreated)
            }
          }
        }
      }
    }
  }

  fun checkEmailVerifiedAndFinishOwnerSetup(onOwnerCreated: () -> Unit, onNeedsLogin: () -> Unit) {
    val draft = _uiState.value.pendingOwnerDraft
    if (draft == null) {
      onNeedsLogin()
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorBanner = null) }
      when (val signIn = onboardingRepository.signIn(draft.email, draft.password)) {
        is BackendResult.Success -> {
          preferences.saveAuthSession(signIn.data)
          completeOwnerOrgCreation(signIn.data, draft, onOwnerCreated)
        }
        is BackendResult.ApiFailure ->
          _uiState.update {
            it.copy(
              isSubmitting = false,
              errorBanner = "Email confirmation not completed yet or sign-in blocked: ${signIn.message}",
            )
          }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = signIn.message) }
        is BackendResult.NotConfigured ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = "Supabase secrets not configured.") }
      }
    }
  }

  private suspend fun completeOwnerOrgCreation(
    session: AuthSession,
    draft: PendingOwnerDraft,
    onOwnerCreated: () -> Unit,
  ) {
    when (
      val createOrg =
        onboardingRepository.createOrganizationViaRpc(
          session = session,
          name = draft.orgName,
          registrationNumber = draft.regNumber.ifBlank { null },
          countryCode = draft.countryCode,
          currencyCode = draft.currencyCode,
        )
    ) {
      is BackendResult.Success -> {
        val org =
          createOrg.data.organizations
            ?: OrganizationDto(
              id = createOrg.data.organizationId,
              name = draft.orgName,
              registrationNumber = draft.regNumber.ifBlank { null },
              countryCode = draft.countryCode,
              currencyCode = draft.currencyCode,
              ownerId = session.userId,
            )
        _uiState.update {
          it.copy(
            activeSession = session,
            currentUserId = session.userId,
            currentAccountName = draft.fullName,
            currentAccountEmail = draft.email,
            currentAccountPhone = draft.phone,
            pendingOwnerDraft = null,
            pendingEmailVerificationAddress = null,
          )
        }
        loadOwnerWorkspace(session, org, draft.fullName)
        _uiState.update {
          it.copy(
            isSubmitting = false,
            activeRole = CivicRole.OWNER,
            feedbackBanner = "Organisation '${org.name}' registered on Supabase.",
          )
        }
        onOwnerCreated()
      }
      is BackendResult.ApiFailure ->
        _uiState.update { it.copy(isSubmitting = false, errorBanner = createOrg.message) }
      is BackendResult.NetworkFailure ->
        _uiState.update { it.copy(isSubmitting = false, errorBanner = createOrg.message) }
      is BackendResult.NotConfigured ->
        _uiState.update { it.copy(isSubmitting = false, errorBanner = "Supabase secrets not configured.") }
    }
  }

  fun validateInvitationToken(rawTokenOrLink: String, expectedRole: OrgMemberRole? = null) {
    viewModelScope.launch {
      _uiState.update {
        it.copy(
          isSubmitting = true,
          errorBanner = null,
          feedbackBanner = null,
          validatedInvitation = null,
          validatedTokenHash = null,
        )
      }
      when (
        val result =
          onboardingRepository.validateInvitationToken(
            session = _uiState.value.activeSession,
            rawTokenOrLink = rawTokenOrLink,
          )
      ) {
        is BackendResult.NotConfigured ->
          _uiState.update {
            it.copy(
              isSubmitting = false,
              errorBanner = "Configure SUPABASE_URL and SUPABASE_ANON_KEY in AI Studio Secrets first.",
            )
          }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = result.message) }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = result.message) }
        is BackendResult.Success -> {
          when (val outcome = result.data) {
            is InvitationValidationOutcome.Invalid ->
              _uiState.update { it.copy(isSubmitting = false, errorBanner = outcome.reason) }
            is InvitationValidationOutcome.Valid -> {
              val inv = outcome.invitation
              val parsedRole = OrgMemberRole.fromApi(inv.role)
              if (expectedRole != null && parsedRole != expectedRole) {
                _uiState.update {
                  it.copy(
                    isSubmitting = false,
                    errorBanner =
                      "This invitation is for ${parsedRole.displayLabel}, not ${expectedRole.displayLabel}.",
                  )
                }
                return@launch
              }
              val details =
                CivicInvitationDetails(
                  invitationId = inv.id,
                  organizationId = inv.organizationId,
                  tokenCode = InvitationTokenUtils.extractInvitationToken(rawTokenOrLink),
                  organizationName = inv.organizations?.name ?: inv.organizationId,
                  invitedEmail = inv.email,
                  role = inv.role,
                  departmentId = inv.departmentId,
                  departmentName = inv.departments?.name ?: (inv.departmentId ?: "General"),
                  workplaceId = inv.workplaceId,
                  workplaceName = inv.workplaces?.name ?: (inv.workplaceId ?: "Assigned Site"),
                  expiresLabel = inv.expiresAt?.take(10) ?: "Active",
                  status = inv.status,
                )
              _uiState.update {
                it.copy(
                  isSubmitting = false,
                  validatedInvitation = details,
                  validatedTokenHash = outcome.tokenHash,
                  feedbackBanner = "Validated invitation for ${details.organizationName}.",
                )
              }
            }
          }
        }
      }
    }
  }

  fun acceptSupervisorInvitation(
    fullName: String,
    password: String,
    confirmPassword: String,
    isExistingAccountSignIn: Boolean,
    onAccepted: () -> Unit,
  ) {
    val details = _uiState.value.validatedInvitation
    val tokenHash = _uiState.value.validatedTokenHash
    if (details == null || tokenHash == null) {
      _uiState.update { it.copy(errorBanner = "Validate your invitation token first.") }
      return
    }
    if (!isExistingAccountSignIn) {
      if (fullName.trim().length < 2) {
        _uiState.update { it.copy(errorBanner = "Enter your full name.") }
        return
      }
      val pwErr = InvitationTokenUtils.validatePassword(password)
      if (pwErr != null) {
        _uiState.update { it.copy(errorBanner = pwErr) }
        return
      }
      if (password != confirmPassword) {
        _uiState.update { it.copy(errorBanner = "Passwords do not match.") }
        return
      }
    } else if (password.length < 6) {
      _uiState.update { it.copy(errorBanner = "Enter your existing account password.") }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorBanner = null) }
      val session: AuthSession =
        if (isExistingAccountSignIn) {
          when (val s = onboardingRepository.signIn(details.invitedEmail, password)) {
            is BackendResult.Success -> s.data
            is BackendResult.ApiFailure -> {
              _uiState.update { it.copy(isSubmitting = false, errorBanner = s.message) }
              return@launch
            }
            is BackendResult.NetworkFailure -> {
              _uiState.update { it.copy(isSubmitting = false, errorBanner = s.message) }
              return@launch
            }
            is BackendResult.NotConfigured -> {
              _uiState.update { it.copy(isSubmitting = false, errorBanner = "Supabase secrets not configured.") }
              return@launch
            }
          }
        } else {
          when (
            val s =
              onboardingRepository.signUpAccount(
                fullName = fullName.trim(),
                email = details.invitedEmail,
                phone = null,
                password = password,
                onboardingFlow = "supervisor",
              )
          ) {
            is BackendResult.Success -> {
              when (val outcome = s.data) {
                is AccountSignUpOutcome.SignedIn -> outcome.session
                is AccountSignUpOutcome.EmailConfirmationRequired -> {
                  _uiState.update {
                    it.copy(
                      isSubmitting = false,
                      pendingEmailVerificationAddress = outcome.email,
                      feedbackBanner =
                        "Account created for ${outcome.email}. Verify your email, then return and sign in on this screen to accept the invitation.",
                    )
                  }
                  return@launch
                }
              }
            }
            is BackendResult.ApiFailure -> {
              _uiState.update { it.copy(isSubmitting = false, errorBanner = s.message) }
              return@launch
            }
            is BackendResult.NetworkFailure -> {
              _uiState.update { it.copy(isSubmitting = false, errorBanner = s.message) }
              return@launch
            }
            is BackendResult.NotConfigured -> {
              _uiState.update { it.copy(isSubmitting = false, errorBanner = "Supabase secrets not configured.") }
              return@launch
            }
          }
        }

      preferences.saveAuthSession(session)
      val dto =
        com.example.data.model.InvitationDto(
          id = details.invitationId,
          organizationId = details.organizationId,
          email = details.invitedEmail,
          role = details.role,
          departmentId = details.departmentId,
          workplaceId = details.workplaceId,
          expiresAt = details.expiresLabel,
          status = details.status,
        )
      when (val acc = onboardingRepository.acceptSupervisorInvitation(session, dto, tokenHash)) {
        is BackendResult.Success -> {
          loadSupervisorWorkspace(session, acc.data)
          _uiState.update {
            it.copy(
              isSubmitting = false,
              activeSession = session,
              activeRole = CivicRole.SUPERVISOR,
              currentUserId = session.userId,
              currentAccountName = fullName.trim().ifBlank { session.email },
              currentAccountEmail = session.email,
              feedbackBanner = "Joined ${details.organizationName} as Supervisor.",
            )
          }
          onAccepted()
        }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = acc.message) }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = acc.message) }
        is BackendResult.NotConfigured ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = "Supabase secrets not configured.") }
      }
    }
  }

  fun acceptEmployeeInvitation(
    fullName: String,
    employeeCode: String,
    jobTitle: String,
    password: String,
    confirmPassword: String,
    isExistingAccountSignIn: Boolean,
    onAccepted: () -> Unit,
  ) {
    val details = _uiState.value.validatedInvitation
    if (details == null) {
      _uiState.update { it.copy(errorBanner = "Validate your invitation token first.") }
      return
    }
    if (!isExistingAccountSignIn) {
      if (fullName.trim().length < 2) {
        _uiState.update { it.copy(errorBanner = "Enter your full name.") }
        return
      }
      val pwErr = InvitationTokenUtils.validatePassword(password)
      if (pwErr != null) {
        _uiState.update { it.copy(errorBanner = pwErr) }
        return
      }
      if (password != confirmPassword) {
        _uiState.update { it.copy(errorBanner = "Passwords do not match.") }
        return
      }
    } else if (password.length < 6) {
      _uiState.update { it.copy(errorBanner = "Enter your existing account password.") }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorBanner = null) }
      val session: AuthSession =
        if (isExistingAccountSignIn) {
          when (val s = civicRepository.signIn(details.invitedEmail, password)) {
            is BackendResult.Success -> s.data
            is BackendResult.ApiFailure -> {
              _uiState.update { it.copy(isSubmitting = false, errorBanner = s.message) }
              return@launch
            }
            is BackendResult.NetworkFailure -> {
              _uiState.update { it.copy(isSubmitting = false, errorBanner = s.message) }
              return@launch
            }
            is BackendResult.NotConfigured -> {
              _uiState.update { it.copy(isSubmitting = false, errorBanner = "Supabase secrets not configured.") }
              return@launch
            }
          }
        } else {
          when (
            val reg =
              civicRepository.registerEmployee(
                email = details.invitedEmail,
                password = password,
                fullName = fullName.trim(),
                employeeCode = employeeCode.trim().ifBlank { "EMP-${details.invitationId.take(4).uppercase()}" },
                department = details.departmentName,
                role = jobTitle.trim().ifBlank { "Workforce Member" },
                workplaceId = details.workplaceId,
              )
          ) {
            is BackendResult.Success -> {
              when (val o = reg.data) {
                is RegistrationOutcome.SignedIn -> o.session
                is RegistrationOutcome.EmailConfirmationRequired -> {
                  _uiState.update {
                    it.copy(
                      isSubmitting = false,
                      pendingEmailVerificationAddress = o.email,
                      feedbackBanner =
                        "Account registered for ${o.email}. Confirm your email, then sign in on this screen.",
                    )
                  }
                  return@launch
                }
              }
            }
            is BackendResult.ApiFailure -> {
              _uiState.update { it.copy(isSubmitting = false, errorBanner = reg.message) }
              return@launch
            }
            is BackendResult.NetworkFailure -> {
              _uiState.update { it.copy(isSubmitting = false, errorBanner = reg.message) }
              return@launch
            }
            is BackendResult.NotConfigured -> {
              _uiState.update { it.copy(isSubmitting = false, errorBanner = "Supabase secrets not configured.") }
              return@launch
            }
          }
        }

      preferences.saveAuthSession(session)
      when (val empRes = civicRepository.loadEmployeeProfile(session)) {
        is BackendResult.Success -> {
          loadEmployeeWorkspace(session, empRes.data)
          _uiState.update {
            it.copy(
              isSubmitting = false,
              activeSession = session,
              activeRole = CivicRole.EMPLOYEE,
              feedbackBanner = "Joined ${details.organizationName} as Employee.",
            )
          }
          onAccepted()
        }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = empRes.message) }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = empRes.message) }
        is BackendResult.NotConfigured ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = "Supabase secrets not configured.") }
      }
    }
  }

  fun declineValidatedInvitation(onDone: () -> Unit) {
    val details = _uiState.value.validatedInvitation
    val tokenHash = _uiState.value.validatedTokenHash
    val session = _uiState.value.activeSession
    if (details != null && tokenHash != null && session != null) {
      viewModelScope.launch {
        val dto =
          com.example.data.model.InvitationDto(
            id = details.invitationId,
            organizationId = details.organizationId,
            email = details.invitedEmail,
            role = details.role,
            status = details.status,
          )
        onboardingRepository.declineSupervisorInvitation(session, dto, tokenHash)
        _uiState.update {
          it.copy(
            validatedInvitation = null,
            validatedTokenHash = null,
            feedbackBanner = "Invitation declined.",
          )
        }
        onDone()
      }
    } else {
      _uiState.update {
        it.copy(validatedInvitation = null, validatedTokenHash = null, feedbackBanner = "Invitation cleared.")
      }
      onDone()
    }
  }

  fun requestPasswordReset(email: String, onDispatched: () -> Unit) {
    val clean = email.trim()
    if (!InvitationTokenUtils.isValidEmail(clean)) {
      _uiState.update { it.copy(errorBanner = "Enter a valid registered email address.") }
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorBanner = null) }
      when (val r = onboardingRepository.requestPasswordReset(clean)) {
        is BackendResult.Success -> {
          _uiState.update {
            it.copy(
              isSubmitting = false,
              passwordResetEmail = clean,
              feedbackBanner = "Password recovery email requested via Supabase for $clean.",
            )
          }
          onDispatched()
        }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = r.message) }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = r.message) }
        is BackendResult.NotConfigured ->
          _uiState.update {
            it.copy(
              isSubmitting = false,
              errorBanner = "Configure SUPABASE_URL and SUPABASE_ANON_KEY in AI Studio Secrets first.",
            )
          }
      }
    }
  }

  fun submitPasswordReset(newPassword: String, confirmPassword: String, onUpdated: () -> Unit) {
    val session = _uiState.value.activeSession
    if (session == null) {
      _uiState.update {
        it.copy(
          errorBanner =
            "Open your password recovery link from email or sign in first before updating your password.",
        )
      }
      return
    }
    val pwErr = InvitationTokenUtils.validatePassword(newPassword)
    if (pwErr != null) {
      _uiState.update { it.copy(errorBanner = pwErr) }
      return
    }
    if (newPassword != confirmPassword) {
      _uiState.update { it.copy(errorBanner = "Passwords do not match.") }
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorBanner = null) }
      when (val r = onboardingRepository.updatePassword(session, newPassword)) {
        is BackendResult.Success -> {
          _uiState.update {
            it.copy(
              isSubmitting = false,
              feedbackBanner = "Password updated on Supabase. Please sign in with your new password.",
            )
          }
          onUpdated()
        }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = r.message) }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = r.message) }
        is BackendResult.NotConfigured ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = "Supabase secrets not configured.") }
      }
    }
  }

  fun logout() {
    activeOrgDto = null
    activeOrgMember = null
    viewModelScope.launch { preferences.clearAuthSession() }
    _uiState.update {
      it.copy(
        activeSession = null,
        activeRole = null,
        currentUserId = "",
        currentAccountName = "",
        currentAccountEmail = "",
        currentAccountPhone = "",
        currentAccountAvatarUrl = null,
        organization = null,
        departments = emptyList(),
        workplaces = emptyList(),
        schedules = emptyList(),
        supervisors = emptyList(),
        employees = emptyList(),
        attendanceRecords = emptyList(),
        leaveRequests = emptyList(),
        auditEvents = emptyList(),
        employeeDocuments = emptyList(),
        biometricStep = BiometricStepStatus.Idle,
        geofenceStep = GeofenceStepStatus.Idle,
        submissionStep = AttendanceSubmissionStatus.Idle,
        feedbackBanner = "Signed out.",
        errorBanner = null,
      )
    }
  }

  // --- Owner Workspace Actions ---

  fun refreshActiveWorkspace() {
    val state = _uiState.value
    val session = state.activeSession ?: return
    viewModelScope.launch {
      when (state.activeRole) {
        CivicRole.OWNER ->
          activeOrgDto?.let { org ->
            loadOwnerWorkspace(session, org, state.currentAccountName.ifBlank { session.email })
          }
        CivicRole.SUPERVISOR ->
          activeOrgMember?.let { member ->
            loadSupervisorWorkspace(session, member)
          }
        CivicRole.EMPLOYEE ->
          when (val r = civicRepository.loadEmployeeProfile(session)) {
            is BackendResult.Success -> loadEmployeeWorkspace(session, r.data)
            else -> Unit
          }
        null -> Unit
      }
    }
  }

  fun updateOrganizationProfile(
    name: String,
    registrationNumber: String,
    industry: String,
    description: String,
    contactEmail: String,
    contactPhone: String,
    address: String,
  ) {
    val session = _uiState.value.activeSession ?: return
    val orgId = _uiState.value.organization?.id ?: return
    if (name.trim().length < 2) {
      _uiState.update { it.copy(errorBanner = "Organisation name is required.") }
      return
    }
    if (contactEmail.isNotBlank() && !InvitationTokenUtils.isValidEmail(contactEmail.trim())) {
      _uiState.update { it.copy(errorBanner = "Enter a valid official contact email.") }
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorBanner = null) }
      val patch =
        OrganizationProfilePatch(
          registrationNumber = registrationNumber.trim().ifBlank { null },
          description = description.trim().ifBlank { null },
          contactEmail = contactEmail.trim().ifBlank { null },
          contactPhone = contactPhone.trim().ifBlank { null },
          address = address.trim().ifBlank { null },
        )
      when (val res = onboardingRepository.updateOrganizationProfile(session, orgId, patch)) {
        is BackendResult.Success -> {
          activeOrgDto = res.data
          loadOwnerWorkspace(session, res.data, _uiState.value.currentAccountName)
          _uiState.update {
            it.copy(isSubmitting = false, feedbackBanner = "Organisation profile saved to Supabase.")
          }
        }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = res.message) }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = res.message) }
        is BackendResult.NotConfigured ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = "Supabase secrets not configured.") }
      }
    }
  }

  fun uploadOrganizationLogoFromUri(context: Context, uri: Uri) {
    val session = _uiState.value.activeSession ?: return
    val orgId = _uiState.value.organization?.id ?: return
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorBanner = null) }
      when (val processed = ImageProcessor.processPickedUri(context, uri, ImageUploadPreset.PROFILE_PHOTO)) {
        is ImageProcessingResult.ValidationError ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = processed.message) }
        is ImageProcessingResult.Success -> {
          when (
            val res =
              onboardingRepository.uploadOrganizationLogo(
                session = session,
                organizationId = orgId,
                imageFile = processed.asset.file,
                mimeType = processed.asset.mimeType,
              )
          ) {
            is BackendResult.Success -> {
              activeOrgDto = res.data
              loadOwnerWorkspace(session, res.data, _uiState.value.currentAccountName)
              _uiState.update {
                it.copy(isSubmitting = false, feedbackBanner = "Organisation logo uploaded to Supabase Storage.")
              }
            }
            is BackendResult.ApiFailure ->
              _uiState.update { it.copy(isSubmitting = false, errorBanner = res.message) }
            is BackendResult.NetworkFailure ->
              _uiState.update { it.copy(isSubmitting = false, errorBanner = res.message) }
            is BackendResult.NotConfigured ->
              _uiState.update { it.copy(isSubmitting = false, errorBanner = "Supabase secrets not configured.") }
          }
        }
      }
    }
  }

  fun inviteEmployee(
    email: String,
    departmentId: String,
    workplaceId: String,
  ) {
    val session = _uiState.value.activeSession ?: return
    val orgId = _uiState.value.organization?.id ?: return
    if (!InvitationTokenUtils.isValidEmail(email.trim())) {
      _uiState.update { it.copy(errorBanner = "Enter a valid employee email address.") }
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorBanner = null) }
      when (
        val res =
          onboardingRepository.createInvitation(
            session = session,
            organizationId = orgId,
            email = email.trim(),
            role = OrgMemberRole.EMPLOYEE,
            departmentId = departmentId.ifBlank { null },
            workplaceId = workplaceId.ifBlank { null },
          )
      ) {
        is BackendResult.Success -> {
          activeOrgDto?.let { loadOwnerWorkspace(session, it, _uiState.value.currentAccountName) }
          _uiState.update {
            it.copy(
              isSubmitting = false,
              lastGeneratedEmployeeInviteLink = res.data.oneTimeDeepLink,
              feedbackBanner = "Employee invitation created for ${email.trim()}.",
            )
          }
        }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = res.message) }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = res.message) }
        is BackendResult.NotConfigured ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = "Supabase secrets not configured.") }
      }
    }
  }

  fun inviteSupervisor(
    email: String,
    departmentId: String,
    workplaceId: String,
  ) {
    val session = _uiState.value.activeSession ?: return
    val orgId = _uiState.value.organization?.id ?: return
    if (!InvitationTokenUtils.isValidEmail(email.trim())) {
      _uiState.update { it.copy(errorBanner = "Enter a valid supervisor email address.") }
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorBanner = null) }
      when (
        val res =
          onboardingRepository.createInvitation(
            session = session,
            organizationId = orgId,
            email = email.trim(),
            role = OrgMemberRole.SUPERVISOR,
            departmentId = departmentId.ifBlank { null },
            workplaceId = workplaceId.ifBlank { null },
          )
      ) {
        is BackendResult.Success -> {
          activeOrgDto?.let { loadOwnerWorkspace(session, it, _uiState.value.currentAccountName) }
          _uiState.update {
            it.copy(
              isSubmitting = false,
              lastGeneratedSupervisorInviteLink = res.data.oneTimeDeepLink,
              feedbackBanner = "Supervisor invitation created for ${email.trim()}.",
            )
          }
        }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = res.message) }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = res.message) }
        is BackendResult.NotConfigured ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = "Supabase secrets not configured.") }
      }
    }
  }

  fun addDepartment(name: String, code: String, description: String) {
    val session = _uiState.value.activeSession ?: return
    val orgId = _uiState.value.organization?.id ?: return
    if (name.trim().length < 2) {
      _uiState.update { it.copy(errorBanner = "Department name is required.") }
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorBanner = null) }
      val request =
        CreateDepartmentRequest(
          organizationId = orgId,
          name = name.trim(),
          description = description.trim().ifBlank { null },
        )
      when (val res = onboardingRepository.createDepartment(session, request)) {
        is BackendResult.Success -> {
          activeOrgDto?.let { loadOwnerWorkspace(session, it, _uiState.value.currentAccountName) }
          _uiState.update {
            it.copy(isSubmitting = false, feedbackBanner = "Department '${res.data.name}' saved.")
          }
        }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = res.message) }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = res.message) }
        is BackendResult.NotConfigured ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = "Supabase secrets not configured.") }
      }
    }
  }

  fun requestWorkplaceGpsCapture(activity: Activity) {
    viewModelScope.launch {
      val previouslyAsked = preferences.wasPermissionRequested(Manifest.permission.ACCESS_FINE_LOCATION)
      val snap = capabilityChecker.inspectCapabilities(activity, previouslyAsked, false)
      _uiState.update { it.copy(capabilities = snap) }
      when (snap.locationPermission) {
        LocationPermissionState.FINE_GRANTED, LocationPermissionState.COARSE_ONLY_GRANTED -> {
          if (!snap.locationServicesEnabled) {
            _uiState.update {
              it.copy(
                blockedPermissionRequiresLocationSettings = true,
                permissionBlockedMessage =
                  capabilityChecker.userMessageForLocationIssue(snap.locationPermission, false),
              )
            }
          } else {
            captureWorkplaceGpsInternal()
          }
        }
        LocationPermissionState.DENIED_CAN_REQUEST ->
          _uiState.update {
            it.copy(
              activeRationale = RationaleType.LOCATION,
              pendingLocationAction = LocationActionKind.WORKPLACE_GPS_CAPTURE,
            )
          }
        LocationPermissionState.PERMANENTLY_DENIED ->
          _uiState.update {
            it.copy(
              blockedPermissionRequiresLocationSettings = false,
              permissionBlockedMessage =
                capabilityChecker.userMessageForLocationIssue(snap.locationPermission, snap.locationServicesEnabled),
            )
          }
      }
    }
  }

  private fun captureWorkplaceGpsInternal() {
    val perm = _uiState.value.capabilities?.locationPermission ?: LocationPermissionState.FINE_GRANTED
    viewModelScope.launch {
      _uiState.update { it.copy(isCapturingWorkplaceGps = true, errorBanner = null) }
      when (val loc = locationProvider.fetchCurrentLocation(perm)) {
        is LocationFetchResult.Success ->
          _uiState.update {
            it.copy(
              isCapturingWorkplaceGps = false,
              capturedWorkplaceLat = loc.location.latitude,
              capturedWorkplaceLng = loc.location.longitude,
              capturedWorkplaceAccuracy = loc.location.accuracyMeters,
              feedbackBanner =
                "Captured GPS coordinates (%.5f, %.5f, ±%.1fm).".format(
                  loc.location.latitude,
                  loc.location.longitude,
                  loc.location.accuracyMeters,
                ),
            )
          }
        is LocationFetchResult.ServicesDisabled ->
          _uiState.update {
            it.copy(
              isCapturingWorkplaceGps = false,
              blockedPermissionRequiresLocationSettings = true,
              permissionBlockedMessage = "Enable Android Location Services to capture site GPS coordinates.",
            )
          }
        is LocationFetchResult.PermissionDenied ->
          _uiState.update {
            it.copy(
              isCapturingWorkplaceGps = false,
              errorBanner = "Location permission is required to read GPS coordinates.",
            )
          }
        is LocationFetchResult.Unavailable ->
          _uiState.update {
            it.copy(isCapturingWorkplaceGps = false, errorBanner = loc.reason)
          }
      }
    }
  }

  fun addWorkplace(
    name: String,
    address: String,
    latText: String,
    lngText: String,
    radiusText: String,
    maxAccuracyText: String = "60",
  ) {
    val session = _uiState.value.activeSession ?: return
    val orgId = _uiState.value.organization?.id ?: return
    if (name.trim().length < 2) {
      _uiState.update { it.copy(errorBanner = "Workplace name is required.") }
      return
    }
    val lat = latText.toDoubleOrNull()
    val lng = lngText.toDoubleOrNull()
    val radius = radiusText.toDoubleOrNull() ?: 150.0
    val maxAcc = maxAccuracyText.toDoubleOrNull() ?: 60.0
    if (lat == null || lng == null || lat !in -90.0..90.0 || lng !in -180.0..180.0) {
      _uiState.update { it.copy(errorBanner = "Enter or capture valid latitude & longitude coordinates.") }
      return
    }
    if (radius !in 20.0..5000.0) {
      _uiState.update { it.copy(errorBanner = "Geofence radius must be between 20m and 5000m.") }
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorBanner = null) }
      val req =
        CreateWorkplaceRequest(
          organizationId = orgId,
          name = name.trim(),
          address = address.trim().ifBlank { null },
          latitude = lat,
          longitude = lng,
          geofenceRadiusMeters = radius,
          maxLocationAccuracyMeters = maxAcc.coerceIn(10.0, 250.0),
        )
      when (val res = onboardingRepository.createWorkplace(session, req)) {
        is BackendResult.Success -> {
          activeOrgDto?.let { loadOwnerWorkspace(session, it, _uiState.value.currentAccountName) }
          _uiState.update {
            it.copy(
              isSubmitting = false,
              capturedWorkplaceLat = null,
              capturedWorkplaceLng = null,
              capturedWorkplaceAccuracy = null,
              feedbackBanner = "Workplace '${res.data.name}' saved to Supabase.",
            )
          }
        }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = res.message) }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = res.message) }
        is BackendResult.NotConfigured ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = "Supabase secrets not configured.") }
      }
    }
  }

  fun saveOrganizationSettingsAndSchedule(
    startTime: String,
    endTime: String,
    graceMinutes: Int,
    workingDays: List<String>,
    requireLocation: Boolean,
    requireBiometric: Boolean,
    allowOutsideGeofence: Boolean,
  ) {
    val session = _uiState.value.activeSession ?: return
    val orgId = _uiState.value.organization?.id ?: return
    val currentSettingsId = _uiState.value.orgSettings.settingsId.ifBlank { null }
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorBanner = null) }
      val dto =
        OrganizationSettingsDto(
          id = currentSettingsId,
          organizationId = orgId,
          requireLocationVerification = requireLocation,
          requireBiometricForAttendance = requireBiometric,
          allowCheckInOutsideGeofence = allowOutsideGeofence,
          workStartTime = startTime.ifBlank { "08:00" },
          workEndTime = endTime.ifBlank { "17:00" },
          lateGraceMinutes = graceMinutes.coerceIn(0, 120),
          workingDays = workingDays.ifEmpty { listOf("MON", "TUE", "WED", "THU", "FRI") },
        )
      when (val res = onboardingRepository.saveOrganizationSettings(session, dto)) {
        is BackendResult.Success -> {
          activeOrgDto?.let { loadOwnerWorkspace(session, it, _uiState.value.currentAccountName) }
          _uiState.update {
            it.copy(
              isSubmitting = false,
              orgSettings = mapOrgSettings(res.data),
              feedbackBanner = "Organisation schedule & policies saved to Supabase.",
            )
          }
        }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = res.message) }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = res.message) }
        is BackendResult.NotConfigured ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = "Supabase secrets not configured.") }
      }
    }
  }

  fun reviewAttendanceRecord(
    recordId: String,
    geofenceStatus: String?,
    reviewStatus: String,
    notes: String,
  ) {
    val session = _uiState.value.activeSession ?: return
    val orgId = _uiState.value.organization?.id ?: activeOrgMember?.organizationId ?: return
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorBanner = null) }
      when (
        val res =
          onboardingRepository.reviewAttendanceRecord(
            session = session,
            organizationId = orgId,
            recordId = recordId,
            geofenceStatus = geofenceStatus,
            reviewStatus = reviewStatus,
            notes = notes,
          )
      ) {
        is BackendResult.Success -> {
          refreshActiveWorkspace()
          _uiState.update {
            it.copy(
              isSubmitting = false,
              feedbackBanner = "Attendance record updated ($reviewStatus).",
            )
          }
        }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = res.message) }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = res.message) }
        is BackendResult.NotConfigured ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = "Supabase secrets not configured.") }
      }
    }
  }

  fun decideLeaveRequest(leaveId: String, approved: Boolean) {
    val session = _uiState.value.activeSession ?: return
    val orgId = _uiState.value.organization?.id ?: activeOrgMember?.organizationId ?: return
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorBanner = null) }
      when (val res = onboardingRepository.decideLeaveRequest(session, orgId, leaveId, approved)) {
        is BackendResult.Success -> {
          refreshActiveWorkspace()
          _uiState.update {
            it.copy(
              isSubmitting = false,
              feedbackBanner =
                if (approved) "Leave request approved in Supabase."
                else "Leave request rejected in Supabase.",
            )
          }
        }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = res.message) }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = res.message) }
        is BackendResult.NotConfigured ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = "Supabase secrets not configured.") }
      }
    }
  }

  fun markNotificationRead(id: String) {
    _uiState.update { it.copy(readNotificationIds = it.readNotificationIds + id) }
  }

  fun markAllNotificationsRead() {
    val allIds = _uiState.value.notifications.map { it.id }.toSet()
    _uiState.update {
      it.copy(
        readNotificationIds = it.readNotificationIds + allIds,
        feedbackBanner = "All notifications marked as read.",
      )
    }
  }

  fun updateCurrentProfile(fullName: String, phone: String) {
    val session = _uiState.value.activeSession ?: return
    val cleanName = fullName.trim()
    if (cleanName.length < 2) {
      _uiState.update { it.copy(errorBanner = "Full name cannot be empty.") }
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorBanner = null) }
      when (
        val res =
          onboardingRepository.upsertProfile(
            session = session,
            fullName = cleanName,
            phone = phone.trim().ifBlank { null },
            avatarUrl = _uiState.value.currentAccountAvatarUrl,
          )
      ) {
        is BackendResult.Success -> {
          _uiState.update {
            it.copy(
              isSubmitting = false,
              currentAccountName = res.data.fullName ?: cleanName,
              currentAccountPhone = res.data.phone.orEmpty(),
              feedbackBanner = "Profile saved to Supabase.",
            )
          }
        }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = res.message) }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = res.message) }
        is BackendResult.NotConfigured ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = "Supabase secrets not configured.") }
      }
    }
  }

  fun toggleSupervisorPrefs(exceptionAlerts: Boolean, leaveDigest: Boolean) {
    _uiState.update {
      it.copy(
        supervisorExceptionAlertsEnabled = exceptionAlerts,
        supervisorLeaveDigestEnabled = leaveDigest,
        feedbackBanner = "Supervisor notification preferences updated.",
      )
    }
  }

  fun toggleEmployeeAppSettings(alerts: Boolean, reminders: Boolean) {
    _uiState.update {
      it.copy(
        employeeNotificationAlertsEnabled = alerts,
        employeeScheduleRemindersEnabled = reminders,
        feedbackBanner = "Notification preferences updated.",
      )
    }
  }

  // --- Employee Hardware Attendance Pipeline (Location + Biometric + Supabase) ---

  fun confirmRationaleAndRequestPermission(
    onLaunchLocationPermissions: () -> Unit,
    onLaunchCameraPermission: () -> Unit,
  ) {
    val type = _uiState.value.activeRationale ?: return
    _uiState.update { it.copy(activeRationale = null) }
    viewModelScope.launch {
      when (type) {
        RationaleType.LOCATION -> {
          preferences.markPermissionRequested(Manifest.permission.ACCESS_FINE_LOCATION)
          onLaunchLocationPermissions()
        }
        RationaleType.CAMERA -> {
          preferences.markPermissionRequested(Manifest.permission.CAMERA)
          onLaunchCameraPermission()
        }
      }
    }
  }

  fun dismissRationale() {
    _uiState.update {
      it.copy(
        activeRationale = null,
        pendingLocationAction = null,
        pendingCameraTarget = null,
      )
    }
  }

  fun dismissPermissionBlockedBanner() {
    _uiState.update { it.copy(permissionBlockedMessage = null, blockedPermissionRequiresLocationSettings = false) }
  }

  fun onLocationPermissionResult(granted: Boolean, activity: FragmentActivity) {
    refreshCapabilities(activity)
    val action = _uiState.value.pendingLocationAction
    _uiState.update { it.copy(pendingLocationAction = null) }
    if (!granted) {
      val permanent = _uiState.value.capabilities?.locationPermission == LocationPermissionState.PERMANENTLY_DENIED
      _uiState.update {
        it.copy(
          geofenceStep = GeofenceStepStatus.PermissionDenied(isPermanent = permanent),
          blockedPermissionRequiresLocationSettings = false,
          permissionBlockedMessage =
            if (permanent) {
              "Location permission permanently denied. Open Android App Settings to allow location access."
            } else {
              "Location permission was denied. CivicTrack requires location access to verify attendance."
            },
        )
      }
      return
    }
    when (action) {
      LocationActionKind.CHECK_IN -> proceedWithBiometricThenLocation(AttendanceCheckType.CHECK_IN, activity)
      LocationActionKind.CHECK_OUT -> proceedWithBiometricThenLocation(AttendanceCheckType.CHECK_OUT, activity)
      LocationActionKind.LIVE_LOCATION_CHECK -> runLiveLocationCheckInternal()
      LocationActionKind.WORKPLACE_GPS_CAPTURE -> captureWorkplaceGpsInternal()
      null -> Unit
    }
  }

  fun initiateAttendanceAction(
    checkType: AttendanceCheckType,
    activity: FragmentActivity,
  ) {
    val actionKind =
      if (checkType == AttendanceCheckType.CHECK_IN) LocationActionKind.CHECK_IN
      else LocationActionKind.CHECK_OUT

    viewModelScope.launch {
      val locAsked = preferences.wasPermissionRequested(Manifest.permission.ACCESS_FINE_LOCATION)
      val camAsked = preferences.wasPermissionRequested(Manifest.permission.CAMERA)
      val snap = capabilityChecker.inspectCapabilities(activity, locAsked, camAsked)
      _uiState.update {
        it.copy(
          capabilities = snap,
          activeCheckType = checkType,
          permissionBlockedMessage = null,
          biometricStep = BiometricStepStatus.Idle,
          geofenceStep = GeofenceStepStatus.Idle,
          submissionStep = AttendanceSubmissionStatus.Idle,
        )
      }
      when (snap.locationPermission) {
        LocationPermissionState.FINE_GRANTED, LocationPermissionState.COARSE_ONLY_GRANTED -> {
          if (!snap.locationServicesEnabled) {
            _uiState.update {
              it.copy(
                geofenceStep = GeofenceStepStatus.ServicesDisabled,
                blockedPermissionRequiresLocationSettings = true,
                permissionBlockedMessage =
                  capabilityChecker.userMessageForLocationIssue(snap.locationPermission, false),
              )
            }
          } else {
            proceedWithBiometricThenLocation(checkType, activity)
          }
        }
        LocationPermissionState.DENIED_CAN_REQUEST ->
          _uiState.update {
            it.copy(
              activeRationale = RationaleType.LOCATION,
              pendingLocationAction = actionKind,
            )
          }
        LocationPermissionState.PERMANENTLY_DENIED ->
          _uiState.update {
            it.copy(
              geofenceStep = GeofenceStepStatus.PermissionDenied(isPermanent = true),
              blockedPermissionRequiresLocationSettings = false,
              permissionBlockedMessage =
                capabilityChecker.userMessageForLocationIssue(snap.locationPermission, snap.locationServicesEnabled),
            )
          }
      }
    }
  }

  fun runLiveLocationCheck(activity: Activity) {
    viewModelScope.launch {
      val locAsked = preferences.wasPermissionRequested(Manifest.permission.ACCESS_FINE_LOCATION)
      val snap = capabilityChecker.inspectCapabilities(activity, locAsked, false)
      _uiState.update { it.copy(capabilities = snap, permissionBlockedMessage = null) }
      when (snap.locationPermission) {
        LocationPermissionState.FINE_GRANTED, LocationPermissionState.COARSE_ONLY_GRANTED ->
          if (!snap.locationServicesEnabled) {
            _uiState.update {
              it.copy(
                geofenceStep = GeofenceStepStatus.ServicesDisabled,
                blockedPermissionRequiresLocationSettings = true,
                permissionBlockedMessage =
                  capabilityChecker.userMessageForLocationIssue(snap.locationPermission, false),
              )
            }
          } else {
            runLiveLocationCheckInternal()
          }
        LocationPermissionState.DENIED_CAN_REQUEST ->
          _uiState.update {
            it.copy(
              activeRationale = RationaleType.LOCATION,
              pendingLocationAction = LocationActionKind.LIVE_LOCATION_CHECK,
            )
          }
        LocationPermissionState.PERMANENTLY_DENIED ->
          _uiState.update {
            it.copy(
              geofenceStep = GeofenceStepStatus.PermissionDenied(isPermanent = true),
              blockedPermissionRequiresLocationSettings = false,
              permissionBlockedMessage =
                capabilityChecker.userMessageForLocationIssue(snap.locationPermission, snap.locationServicesEnabled),
            )
          }
      }
    }
  }

  private fun runLiveLocationCheckInternal() {
    val emp = _uiState.value.activeEmployee
    val wp = _uiState.value.workplaces.firstOrNull { it.id == emp?.workplaceId }
      ?: _uiState.value.workplaces.firstOrNull()
    val perm = _uiState.value.capabilities?.locationPermission ?: LocationPermissionState.FINE_GRANTED
    viewModelScope.launch {
      _uiState.update { it.copy(geofenceStep = GeofenceStepStatus.AcquiringLocation) }
      when (val fetch = locationProvider.fetchCurrentLocation(perm)) {
        is LocationFetchResult.Success -> {
          val fix = fetch.location
          if (wp == null) {
            _uiState.update {
              it.copy(
                geofenceStep =
                  GeofenceStepStatus.LocationUnavailable(
                    "GPS fix received (%.5f, %.5f, ±%.1fm), but no assigned workplace exists to evaluate geofence.".format(
                      fix.latitude,
                      fix.longitude,
                      fix.accuracyMeters,
                    )
                  )
              )
            }
            return@launch
          }
          val workplaceDto =
            WorkplaceDto(
              id = wp.id,
              name = wp.name,
              address = wp.address,
              latitude = wp.latitude,
              longitude = wp.longitude,
              geofenceRadiusMeters = wp.geofenceRadiusMeters.toDouble(),
              maxLocationAccuracyMeters = wp.maxAccuracyMeters.toDouble(),
            )
          val eval = GeofenceEvaluator.evaluate(fix, workplaceDto)
          val step =
            when (eval) {
              is GeofenceEvaluationResult.Verified ->
                GeofenceStepStatus.Verified(
                  latitude = fix.latitude,
                  longitude = fix.longitude,
                  accuracyMeters = fix.accuracyMeters,
                  distanceMeters = eval.distanceMeters,
                  permittedRadiusMeters = wp.geofenceRadiusMeters.toDouble(),
                  timestampIso = fix.timestampIso,
                )
              is GeofenceEvaluationResult.OutsideGeofence ->
                GeofenceStepStatus.OutsideGeofence(
                  latitude = fix.latitude,
                  longitude = fix.longitude,
                  accuracyMeters = fix.accuracyMeters,
                  distanceMeters = eval.distanceMeters,
                  permittedRadiusMeters = wp.geofenceRadiusMeters.toDouble(),
                  timestampIso = fix.timestampIso,
                )
              is GeofenceEvaluationResult.InsufficientAccuracy ->
                GeofenceStepStatus.InsufficientAccuracy(
                  accuracyMeters = fix.accuracyMeters,
                  maxAllowedMeters = eval.maxAllowedAccuracyMeters,
                )
            }
          _uiState.update { it.copy(geofenceStep = step) }
        }
        is LocationFetchResult.ServicesDisabled ->
          _uiState.update {
            it.copy(
              geofenceStep = GeofenceStepStatus.ServicesDisabled,
              blockedPermissionRequiresLocationSettings = true,
              permissionBlockedMessage = "Location Services are disabled.",
            )
          }
        is LocationFetchResult.PermissionDenied ->
          _uiState.update {
            it.copy(geofenceStep = GeofenceStepStatus.PermissionDenied(isPermanent = fetch.isPermanent))
          }
        is LocationFetchResult.Unavailable ->
          _uiState.update {
            it.copy(geofenceStep = GeofenceStepStatus.LocationUnavailable(fetch.reason))
          }
      }
    }
  }

  private fun proceedWithBiometricThenLocation(
    checkType: AttendanceCheckType,
    activity: FragmentActivity,
  ) {
    val requiresBiometric = _uiState.value.orgSettings.requireBiometric
    if (!requiresBiometric) {
      _uiState.update { it.copy(biometricStep = BiometricStepStatus.NotRequired) }
      captureLocationAndSubmitAttendance(checkType, biometricVerified = false)
      return
    }
    val bioCap = capabilityChecker.checkBiometricAvailability(allowDeviceCredentialFallback = true)
    if (bioCap !is BiometricCapabilityState.Available) {
      val msg = capabilityChecker.userMessageForBiometricState(bioCap)
      _uiState.update {
        it.copy(biometricStep = BiometricStepStatus.Unavailable(msg), errorBanner = msg)
      }
      return
    }
    _uiState.update { it.copy(biometricStep = BiometricStepStatus.Prompting) }
    viewModelScope.launch {
      when (
        val outcome =
          biometricAuthenticator.authenticate(
            activity = activity,
            title = "Confirm ${checkType.displayLabel}",
            subtitle = "Verify on this device before capturing workplace location",
            description = "Authenticate on this Android device to proceed to workplace geofence verification.",
            allowDeviceCredentialFallback = true,
          )
      ) {
        is BiometricAuthOutcome.Success -> {
          _uiState.update {
            it.copy(biometricStep = BiometricStepStatus.LocalDeviceAuthenticated(outcome.methodLabel))
          }
          captureLocationAndSubmitAttendance(checkType, biometricVerified = true)
        }
        is BiometricAuthOutcome.Cancelled ->
          _uiState.update { it.copy(biometricStep = BiometricStepStatus.Cancelled(outcome.reason)) }
        is BiometricAuthOutcome.LockedOut ->
          _uiState.update { it.copy(biometricStep = BiometricStepStatus.Error(outcome.message)) }
        BiometricAuthOutcome.NotEnrolled ->
          _uiState.update {
            it.copy(
              biometricStep =
                BiometricStepStatus.Unavailable(
                  "No biometric or device lock credential enrolled on this device."
                )
            )
          }
        is BiometricAuthOutcome.HardwareUnavailable ->
          _uiState.update { it.copy(biometricStep = BiometricStepStatus.Unavailable(outcome.message)) }
        is BiometricAuthOutcome.Error ->
          _uiState.update { it.copy(biometricStep = BiometricStepStatus.Error(outcome.message)) }
      }
    }
  }

  private fun captureLocationAndSubmitAttendance(
    checkType: AttendanceCheckType,
    biometricVerified: Boolean,
  ) {
    val state = _uiState.value
    val session = state.activeSession ?: return
    val emp = state.activeEmployee
    val wp = state.workplaces.firstOrNull { it.id == emp?.workplaceId } ?: state.workplaces.firstOrNull()
    if (emp == null || wp == null) {
      _uiState.update {
        it.copy(
          geofenceStep =
            GeofenceStepStatus.LocationUnavailable(
              "No employee profile or assigned workplace record loaded from Supabase."
            )
        )
      }
      return
    }

    val perm = state.capabilities?.locationPermission ?: LocationPermissionState.FINE_GRANTED
    viewModelScope.launch {
      _uiState.update { it.copy(geofenceStep = GeofenceStepStatus.AcquiringLocation) }
      when (val fetch = locationProvider.fetchCurrentLocation(perm)) {
        is LocationFetchResult.ServicesDisabled ->
          _uiState.update {
            it.copy(
              geofenceStep = GeofenceStepStatus.ServicesDisabled,
              blockedPermissionRequiresLocationSettings = true,
              permissionBlockedMessage = "Location Services turned off. Enable GPS to check in/out.",
            )
          }
        is LocationFetchResult.PermissionDenied ->
          _uiState.update {
            it.copy(geofenceStep = GeofenceStepStatus.PermissionDenied(isPermanent = fetch.isPermanent))
          }
        is LocationFetchResult.Unavailable -> {
          _uiState.update {
            it.copy(geofenceStep = GeofenceStepStatus.LocationUnavailable(fetch.reason))
          }
          val payload =
            AttendanceSubmissionPayload(
              employeeId = emp.id,
              workplaceId = wp.id,
              checkType = checkType,
              biometricVerifiedLocally = biometricVerified,
              latitude = null,
              longitude = null,
              accuracyMeters = null,
              locationTimestampIso = null,
              distanceMeters = null,
              permittedRadiusMeters = wp.geofenceRadiusMeters.toDouble(),
              verificationOutcome = LocationVerificationOutcome.LOCATION_UNAVAILABLE,
              failureReason = fetch.reason,
            )
          submitAttendancePayload(session, payload)
        }
        is LocationFetchResult.Success -> {
          val fix = fetch.location
          val workplaceDto =
            WorkplaceDto(
              id = wp.id,
              name = wp.name,
              address = wp.address,
              latitude = wp.latitude,
              longitude = wp.longitude,
              geofenceRadiusMeters = wp.geofenceRadiusMeters.toDouble(),
              maxLocationAccuracyMeters = wp.maxAccuracyMeters.toDouble(),
            )
          val eval = GeofenceEvaluator.evaluate(fix, workplaceDto)
          val outcome: LocationVerificationOutcome = eval.outcome
          val distanceMeters: Double? = eval.distanceMeters
          val failureReason: String? = eval.failureReason
          when (eval) {
            is GeofenceEvaluationResult.Verified -> {
              _uiState.update {
                it.copy(
                  geofenceStep =
                    GeofenceStepStatus.Verified(
                      latitude = fix.latitude,
                      longitude = fix.longitude,
                      accuracyMeters = fix.accuracyMeters,
                      distanceMeters = eval.distanceMeters,
                      permittedRadiusMeters = wp.geofenceRadiusMeters.toDouble(),
                      timestampIso = fix.timestampIso,
                    )
                )
              }
            }
            is GeofenceEvaluationResult.OutsideGeofence -> {
              _uiState.update {
                it.copy(
                  geofenceStep =
                    GeofenceStepStatus.OutsideGeofence(
                      latitude = fix.latitude,
                      longitude = fix.longitude,
                      accuracyMeters = fix.accuracyMeters,
                      distanceMeters = eval.distanceMeters,
                      permittedRadiusMeters = wp.geofenceRadiusMeters.toDouble(),
                      timestampIso = fix.timestampIso,
                    )
                )
              }
            }
            is GeofenceEvaluationResult.InsufficientAccuracy -> {
              _uiState.update {
                it.copy(
                  geofenceStep =
                    GeofenceStepStatus.InsufficientAccuracy(
                      accuracyMeters = fix.accuracyMeters,
                      maxAllowedMeters = eval.maxAllowedAccuracyMeters,
                    )
                )
              }
            }
          }
          val payload =
            AttendanceSubmissionPayload(
              employeeId = emp.id,
              workplaceId = wp.id,
              checkType = checkType,
              biometricVerifiedLocally = biometricVerified,
              latitude = fix.latitude,
              longitude = fix.longitude,
              accuracyMeters = fix.accuracyMeters,
              locationTimestampIso = fix.timestampIso,
              distanceMeters = distanceMeters,
              permittedRadiusMeters = wp.geofenceRadiusMeters.toDouble(),
              verificationOutcome = outcome,
              failureReason = failureReason,
            )
          submitAttendancePayload(session, payload)
        }
      }
    }
  }

  private suspend fun submitAttendancePayload(
    session: AuthSession,
    payload: AttendanceSubmissionPayload,
  ) {
    _uiState.update {
      it.copy(
        submissionStep = AttendanceSubmissionStatus.Submitting,
        pendingRetryPayload = payload,
      )
    }
    when (val res = civicRepository.submitAttendanceWithLocationVerification(session, payload)) {
      is BackendResult.Success -> {
        refreshActiveWorkspace()
        _uiState.update {
          it.copy(
            submissionStep =
              AttendanceSubmissionStatus.Recorded(
                attendanceRecordId = res.data.attendanceRecordId,
                locationVerificationId = res.data.locationVerificationId,
                checkType = payload.checkType,
                geofenceOutcome = payload.verificationOutcome,
                recordedAtIso = res.data.recordedAtIso,
              ),
            pendingRetryPayload = null,
            feedbackBanner = "${payload.checkType.displayLabel} recorded in Supabase (${payload.verificationOutcome.apiValue}).",
          )
        }
      }
      is BackendResult.NetworkFailure ->
        _uiState.update {
          it.copy(submissionStep = AttendanceSubmissionStatus.NetworkError(res.message))
        }
      is BackendResult.ApiFailure ->
        _uiState.update {
          it.copy(
            submissionStep = AttendanceSubmissionStatus.BackendError(res.message),
            pendingRetryPayload = null,
          )
        }
      is BackendResult.NotConfigured ->
        _uiState.update {
          it.copy(
            submissionStep =
              AttendanceSubmissionStatus.BackendError("Supabase backend secrets are not configured.")
          )
        }
    }
  }

  fun retryPendingAttendanceSubmission() {
    val session = _uiState.value.activeSession ?: return
    val pending = _uiState.value.pendingRetryPayload ?: return
    viewModelScope.launch { submitAttendancePayload(session, pending) }
  }

  // --- CameraX & Supabase Storage Media ---

  fun requestCameraCapture(target: CaptureTarget, activity: Activity) {
    viewModelScope.launch {
      val camAsked = preferences.wasPermissionRequested(Manifest.permission.CAMERA)
      val snap = capabilityChecker.inspectCapabilities(activity, false, camAsked)
      _uiState.update { it.copy(capabilities = snap, permissionBlockedMessage = null) }
      if (!snap.cameraAvailable) {
        _uiState.update {
          it.copy(
            errorBanner =
              capabilityChecker.userMessageForCameraIssue(false, snap.cameraPermission)
          )
        }
        return@launch
      }
      when (snap.cameraPermission) {
        PermissionAccessState.GRANTED ->
          _uiState.update { it.copy(activeCameraTarget = target, pendingUploadAsset = null) }
        PermissionAccessState.DENIED_CAN_REQUEST ->
          _uiState.update {
            it.copy(activeRationale = RationaleType.CAMERA, pendingCameraTarget = target)
          }
        PermissionAccessState.PERMANENTLY_DENIED ->
          _uiState.update {
            it.copy(
              blockedPermissionRequiresLocationSettings = false,
              permissionBlockedMessage =
                capabilityChecker.userMessageForCameraIssue(true, snap.cameraPermission),
            )
          }
      }
    }
  }

  fun onCameraPermissionResult(granted: Boolean, activity: Activity) {
    refreshCapabilities(activity)
    val target = _uiState.value.pendingCameraTarget
    _uiState.update { it.copy(pendingCameraTarget = null) }
    if (!granted) {
      val permanent = _uiState.value.capabilities?.cameraPermission == PermissionAccessState.PERMANENTLY_DENIED
      _uiState.update {
        it.copy(
          blockedPermissionRequiresLocationSettings = false,
          permissionBlockedMessage =
            if (permanent) "Camera permission permanently denied. Enable Camera in Android App Settings."
            else "Camera permission was denied.",
        )
      }
      return
    }
    if (target != null) {
      _uiState.update { it.copy(activeCameraTarget = target, pendingUploadAsset = null) }
    }
  }

  fun onImageCapturedFromCamera(context: Context, rawFile: File) {
    val target = _uiState.value.activeCameraTarget ?: CaptureTarget.DOCUMENT
    val preset =
      if (target == CaptureTarget.PROFILE_PHOTO) ImageUploadPreset.PROFILE_PHOTO
      else ImageUploadPreset.WORKFORCE_DOCUMENT
    when (val res = ImageProcessor.processCapturedFile(context, rawFile, preset)) {
      is ImageProcessingResult.ValidationError ->
        _uiState.update { it.copy(errorBanner = res.message) }
      is ImageProcessingResult.Success ->
        _uiState.update { it.copy(pendingUploadAsset = res.asset, errorBanner = null) }
    }
  }

  fun onDocumentImagePicked(context: Context, uri: Uri) {
    when (val res = ImageProcessor.processPickedUri(context, uri, ImageUploadPreset.WORKFORCE_DOCUMENT)) {
      is ImageProcessingResult.ValidationError ->
        _uiState.update { it.copy(errorBanner = res.message) }
      is ImageProcessingResult.Success ->
        _uiState.update {
          it.copy(
            activeCameraTarget = CaptureTarget.DOCUMENT,
            pendingUploadAsset = res.asset,
            errorBanner = null,
          )
        }
    }
  }

  fun updateDocumentCategory(category: DocumentCaptureCategory) {
    _uiState.update { it.copy(selectedDocumentCategory = category) }
  }

  fun updateDocumentTitle(title: String) {
    _uiState.update { it.copy(documentTitleInput = title) }
  }

  fun retakePhoto() {
    _uiState.update { it.copy(pendingUploadAsset = null, errorBanner = null) }
  }

  fun cancelCameraCapture() {
    _uiState.update {
      it.copy(
        activeCameraTarget = null,
        pendingUploadAsset = null,
        documentTitleInput = "",
        errorBanner = null,
      )
    }
  }

  fun confirmUploadPendingAsset() {
    val state = _uiState.value
    val session = state.activeSession ?: return
    val emp = state.activeEmployee ?: return
    val asset = state.pendingUploadAsset ?: return
    val target = state.activeCameraTarget ?: CaptureTarget.DOCUMENT

    viewModelScope.launch {
      _uiState.update { it.copy(isUploadingMedia = true, errorBanner = null) }
      val result: BackendResult<String> =
        if (target == CaptureTarget.PROFILE_PHOTO) {
          when (val r = civicRepository.uploadProfilePhoto(session, emp.id, asset.file, asset.mimeType)) {
            is BackendResult.Success -> BackendResult.Success("Profile photo uploaded to Supabase Storage.")
            is BackendResult.ApiFailure -> r
            is BackendResult.NetworkFailure -> r
            BackendResult.NotConfigured -> BackendResult.NotConfigured
          }
        } else {
          val docTitle =
            state.documentTitleInput.trim().ifBlank {
              "${state.selectedDocumentCategory.label} Upload"
            }
          when (
            val r =
              civicRepository.uploadEmployeeDocument(
                session = session,
                employeeId = emp.id,
                category = state.selectedDocumentCategory,
                title = docTitle,
                imageFile = asset.file,
                mimeType = asset.mimeType,
              )
          ) {
            is BackendResult.Success -> BackendResult.Success("Document uploaded & saved to employee_documents.")
            is BackendResult.ApiFailure -> r
            is BackendResult.NetworkFailure -> r
            BackendResult.NotConfigured -> BackendResult.NotConfigured
          }
        }

      when (result) {
        is BackendResult.Success -> {
          refreshActiveWorkspace()
          _uiState.update {
            it.copy(
              isUploadingMedia = false,
              activeCameraTarget = null,
              pendingUploadAsset = null,
              documentTitleInput = "",
              feedbackBanner = result.data,
            )
          }
        }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isUploadingMedia = false, errorBanner = result.message) }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isUploadingMedia = false, errorBanner = result.message) }
        is BackendResult.NotConfigured ->
          _uiState.update {
            it.copy(isUploadingMedia = false, errorBanner = "Supabase secrets not configured.")
          }
      }
    }
  }

  // --- Employee Leave Requests ---

  fun submitEmployeeLeave(
    leaveType: String,
    startDate: String,
    endDate: String,
    daysText: String,
    reason: String,
    onSubmitted: () -> Unit,
  ) {
    val state = _uiState.value
    val session = state.activeSession ?: return
    val emp = state.activeEmployee ?: return
    val orgId = state.organization?.id ?: ""
    if (startDate.isBlank() || endDate.isBlank()) {
      _uiState.update { it.copy(errorBanner = "Enter start and end dates (YYYY-MM-DD).") }
      return
    }
    val days = daysText.toIntOrNull()
    if (days == null || days <= 0) {
      _uiState.update { it.copy(errorBanner = "Enter a valid number of working days.") }
      return
    }
    if (reason.trim().length < 5) {
      _uiState.update { it.copy(errorBanner = "Provide a brief reason for your leave request.") }
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorBanner = null) }
      val payload =
        CreateLeaveRequestPayload(
          organizationId = orgId,
          employeeId = emp.id,
          leaveType = leaveType,
          startDate = startDate.trim(),
          endDate = endDate.trim(),
          daysCount = days,
          reason = reason.trim(),
          status = "pending",
        )
      when (val res = onboardingRepository.submitLeaveRequest(session, payload)) {
        is BackendResult.Success -> {
          refreshActiveWorkspace()
          _uiState.update {
            it.copy(isSubmitting = false, feedbackBanner = "Leave application submitted to Supabase.")
          }
          onSubmitted()
        }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = res.message) }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = res.message) }
        is BackendResult.NotConfigured ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = "Supabase secrets not configured.") }
      }
    }
  }

  fun cancelEmployeeLeave(leaveId: String) {
    val session = _uiState.value.activeSession ?: return
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorBanner = null) }
      when (val res = onboardingRepository.cancelLeaveRequest(session, leaveId)) {
        is BackendResult.Success -> {
          refreshActiveWorkspace()
          _uiState.update {
            it.copy(isSubmitting = false, feedbackBanner = "Pending leave application withdrawn.")
          }
        }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = res.message) }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = res.message) }
        is BackendResult.NotConfigured ->
          _uiState.update { it.copy(isSubmitting = false, errorBanner = "Supabase secrets not configured.") }
      }
    }
  }

  // --- Workspace Snapshot Mapping ---

  private suspend fun loadOwnerWorkspace(
    session: AuthSession,
    organization: OrganizationDto,
    ownerName: String,
  ) {
    activeOrgDto = organization
    when (val res = onboardingRepository.loadOwnerWorkspaceSnapshot(session, organization, ownerName)) {
      is BackendResult.Success -> applyOwnerSnapshot(res.data)
      is BackendResult.ApiFailure ->
        _uiState.update {
          it.copy(
            organization = mapOrganization(organization),
            errorBanner = "Loaded organisation '${organization.name}', partial workspace sync error: ${res.message}",
          )
        }
      else -> Unit
    }
  }

  private fun applyOwnerSnapshot(snapshot: OwnerWorkspaceSnapshot) {
    val deptMap = snapshot.departments.associateBy { it.id }
    val wpMap = snapshot.workplaces.associateBy { it.id }

    val supervisorRows =
      snapshot.members
        .filter { it.parsedRole == OrgMemberRole.SUPERVISOR }
        .map { member ->
          val deptName = deptMap[member.departmentId]?.name ?: "All Departments"
          val wpName = wpMap[member.workplaceId]?.name ?: "All Workplaces"
          val staffCount =
            snapshot.employees.count { emp ->
              (member.departmentId == null || emp.departmentId == member.departmentId) &&
                (member.workplaceId == null || emp.workplaceId == member.workplaceId)
            }
          CivicSupervisor(
            id = member.id,
            userId = member.userId,
            fullName = member.profiles?.fullName ?: member.profiles?.email ?: member.userId,
            email = member.profiles?.email.orEmpty(),
            phone = member.profiles?.phone.orEmpty(),
            departmentId = member.departmentId.orEmpty(),
            departmentName = deptName,
            workplaceId = member.workplaceId.orEmpty(),
            workplaceName = wpName,
            assignedStaffCount = staffCount,
            status = member.status.replaceFirstChar { it.uppercase() },
            avatarUrl = member.profiles?.avatarUrl,
          )
        } +
        snapshot.pendingInvitations
          .filter { OrgMemberRole.fromApi(it.role) == OrgMemberRole.SUPERVISOR }
          .map { inv ->
            CivicSupervisor(
              id = "inv_${inv.id}",
              userId = "",
              fullName = inv.email,
              email = inv.email,
              phone = "",
              departmentId = inv.departmentId.orEmpty(),
              departmentName = deptMap[inv.departmentId]?.name ?: "Assigned Dept",
              workplaceId = inv.workplaceId.orEmpty(),
              workplaceName = wpMap[inv.workplaceId]?.name ?: "Assigned Site",
              assignedStaffCount = 0,
              status = "Invited (${inv.status})",
            )
          }

    val mappedDepts =
      snapshot.departments.map { dept ->
        val supName =
          supervisorRows.firstOrNull { it.departmentId == dept.id }?.fullName ?: "Unassigned"
        mapDepartment(dept, supName, snapshot.employees.count { it.departmentId == dept.id })
      }
    val mappedWps =
      snapshot.workplaces.map { wp ->
        mapWorkplace(wp, snapshot.employees.count { it.workplaceId == wp.id }, snapshot.settings)
      }
    val mappedEmployees =
      snapshot.employees.map { emp ->
        mapEmployeeProfile(emp, deptMap, wpMap, snapshot.attendanceRecords)
      }
    val empLookup = mappedEmployees.associateBy { it.id }
    val mappedRecords =
      snapshot.attendanceRecords.map { rec -> mapAttendanceRecord(rec, empLookup, wpMap) }
    val mappedLeaves =
      snapshot.leaveRequests.map { lv -> mapLeaveRequest(lv, empLookup) }
    val mappedAudit = snapshot.auditLogs.map { mapAuditLog(it) }
    val settings = snapshot.settings?.let(::mapOrgSettings) ?: CivicOrgSettings()

    _uiState.update {
      it.copy(
        organization = mapOrganization(snapshot.organization),
        orgSettings = settings,
        departments = mappedDepts,
        workplaces = mappedWps,
        schedules = buildSchedules(settings, snapshot.departments),
        supervisors = supervisorRows,
        employees = mappedEmployees,
        attendanceRecords = mappedRecords,
        leaveRequests = mappedLeaves,
        leaveFeatureAvailable = snapshot.leaveFeatureAvailable,
        auditEvents = mappedAudit,
      )
    }
  }

  private suspend fun loadSupervisorWorkspace(
    session: AuthSession,
    membership: OrganizationMemberDto,
  ) {
    activeOrgMember = membership
    activeOrgDto = membership.organizations
    when (val res = onboardingRepository.loadSupervisorWorkspaceSnapshot(session, membership)) {
      is BackendResult.Success -> applySupervisorSnapshot(res.data)
      is BackendResult.ApiFailure ->
        _uiState.update { it.copy(errorBanner = res.message) }
      else -> Unit
    }
  }

  private fun applySupervisorSnapshot(snapshot: SupervisorWorkspaceSnapshot) {
    activeOrgDto = snapshot.organization
    val deptMap = snapshot.departments.associateBy { it.id }
    val wpMap = snapshot.workplaces.associateBy { it.id }
    val member = snapshot.membership
    val supName =
      snapshot.supervisorProfile?.fullName
        ?: _uiState.value.currentAccountName.ifBlank { member.userId }

    val supervisorEntry =
      CivicSupervisor(
        id = member.id,
        userId = member.userId,
        fullName = supName,
        email = snapshot.supervisorProfile?.email ?: _uiState.value.currentAccountEmail,
        phone = snapshot.supervisorProfile?.phone.orEmpty(),
        departmentId = member.departmentId.orEmpty(),
        departmentName = deptMap[member.departmentId]?.name ?: "Assigned Department",
        workplaceId = member.workplaceId.orEmpty(),
        workplaceName = wpMap[member.workplaceId]?.name ?: "Assigned Workplace",
        assignedStaffCount = snapshot.assignedEmployees.size,
        status = member.status.replaceFirstChar { it.uppercase() },
        avatarUrl = snapshot.supervisorProfile?.avatarUrl,
      )

    val mappedDepts =
      snapshot.departments.map { dept ->
        mapDepartment(dept, supName, snapshot.assignedEmployees.count { it.departmentId == dept.id })
      }
    val mappedWps =
      snapshot.workplaces.map { wp ->
        mapWorkplace(wp, snapshot.assignedEmployees.count { it.workplaceId == wp.id }, snapshot.settings)
      }
    val mappedEmployees =
      snapshot.assignedEmployees.map { emp ->
        mapEmployeeProfile(emp, deptMap, wpMap, snapshot.attendanceRecords)
      }
    val empLookup = mappedEmployees.associateBy { it.id }
    val mappedRecords =
      snapshot.attendanceRecords.map { rec -> mapAttendanceRecord(rec, empLookup, wpMap) }
    val mappedLeaves = snapshot.leaveRequests.map { lv -> mapLeaveRequest(lv, empLookup) }
    val settings = snapshot.settings?.let(::mapOrgSettings) ?: CivicOrgSettings()

    _uiState.update {
      it.copy(
        organization = mapOrganization(snapshot.organization),
        orgSettings = settings,
        departments = mappedDepts,
        workplaces = mappedWps,
        schedules = buildSchedules(settings, snapshot.departments),
        supervisors = listOf(supervisorEntry),
        currentSupervisorId = member.id,
        employees = mappedEmployees,
        attendanceRecords = mappedRecords,
        leaveRequests = mappedLeaves,
        leaveFeatureAvailable = snapshot.leaveFeatureAvailable,
        auditEvents = snapshot.recentActivities.map(::mapAuditLog),
      )
    }
  }

  private suspend fun loadEmployeeWorkspace(session: AuthSession, emp: EmployeeDto) {
    val recentAtt =
      when (val r = civicRepository.loadRecentAttendance(session, emp.id)) {
        is BackendResult.Success -> r.data
        else -> emptyList()
      }
    val docs =
      when (val r = civicRepository.loadEmployeeDocuments(session, emp.id)) {
        is BackendResult.Success -> r.data
        else -> emptyList()
      }
    val leaves =
      when (val r = onboardingRepository.loadEmployeeLeaveRequests(session, emp.id)) {
        is BackendResult.Success -> r.data
        else -> emptyList()
      }

    val wpList = emp.workplaces?.let { listOf(it) } ?: emptyList()
    val wpMap = wpList.associateBy { it.id }

    val mappedEmp =
      CivicEmployee(
        id = emp.id,
        userId = emp.userId,
        fullName = emp.fullName,
        employeeCode = emp.employeeCode,
        email = session.email,
        phone = _uiState.value.currentAccountPhone,
        jobTitle = emp.role ?: "Workforce Member",
        departmentId = "",
        departmentName = emp.department ?: "General",
        workplaceId = emp.workplaceId.orEmpty(),
        workplaceName = emp.workplaces?.name ?: "Unassigned Workplace",
        status = "Active",
        attendanceRatePercent = computeRatePercent(recentAtt),
        supervisorId = null,
        avatarUrl = emp.profilePhotoUrl,
      )
    val empLookup = mapOf(mappedEmp.id to mappedEmp)
    val mappedRecords = recentAtt.map { mapAttendanceRecord(it, empLookup, wpMap) }
    val mappedLeaves = leaves.map { mapLeaveRequest(it, empLookup) }
    val mappedWps = wpList.map { mapWorkplace(it, 1, null) }
    val empOrgSettings =
      emp.attendanceSettings?.let { s ->
        _uiState.value.orgSettings.copy(
          requireLocation = s.requireLocationVerification,
          requireBiometric = s.requireBiometricForAttendance,
          allowOutsideGeofenceWithNote = s.allowCheckInOutsideGeofence,
        )
      } ?: _uiState.value.orgSettings

    _uiState.update { state ->
      state.copy(
        currentUserId = session.userId,
        currentAccountName = emp.fullName,
        currentAccountEmail = session.email,
        currentAccountAvatarUrl = emp.profilePhotoUrl ?: state.currentAccountAvatarUrl,
        currentEmployeeId = emp.id,
        orgSettings = empOrgSettings,
        employees = listOf(mappedEmp),
        workplaces = mappedWps,
        attendanceRecords = mappedRecords,
        leaveRequests = mappedLeaves,
        employeeDocuments = docs,
      )
    }
  }

  private fun mapOrganization(dto: OrganizationDto) =
    CivicOrganization(
      id = dto.id,
      name = dto.name,
      registrationNumber = dto.registrationNumber.orEmpty(),
      countryCode = dto.countryCode ?: "UG",
      currencyCode = dto.currencyCode ?: "UGX",
      industry = "",
      description = dto.description.orEmpty(),
      contactEmail = dto.contactEmail.orEmpty(),
      contactPhone = dto.contactPhone.orEmpty(),
      headquartersAddress = dto.address.orEmpty(),
      logoUrl = dto.logoUrl,
    )

  private fun mapDepartment(
    dto: DepartmentDto,
    supervisorName: String,
    staffCount: Int,
  ) = CivicDepartment(
    id = dto.id,
    name = dto.name,
    code = dto.name.take(3).uppercase(),
    headSupervisorName = supervisorName,
    staffCount = staffCount,
    description = dto.description.orEmpty(),
  )

  private fun mapWorkplace(
    dto: WorkplaceDto,
    staffCount: Int,
    settings: OrganizationSettingsDto?,
  ) = CivicWorkplace(
    id = dto.id,
    name = dto.name,
    address = dto.address.orEmpty(),
    latitude = dto.latitude,
    longitude = dto.longitude,
    geofenceRadiusMeters = dto.geofenceRadiusMeters.toInt(),
    maxAccuracyMeters = dto.maxLocationAccuracyMeters.toInt(),
    assignedStaffCount = staffCount,
    activeShiftWindow =
      if (settings != null) "${settings.workStartTime ?: "08:00"} - ${settings.workEndTime ?: "17:00"}"
      else "08:00 - 17:00",
  )

  private fun mapOrgSettings(dto: OrganizationSettingsDto) =
    CivicOrgSettings(
      settingsId = dto.id.orEmpty(),
      requireLocation = dto.requireLocationVerification,
      requireBiometric = dto.requireBiometricForAttendance,
      allowOutsideGeofenceWithNote = dto.allowCheckInOutsideGeofence,
      workStartTime = dto.workStartTime ?: "08:00",
      workEndTime = dto.workEndTime ?: "17:00",
      lateGraceMinutes = dto.lateGraceMinutes,
      workingDays = dto.workingDays.ifEmpty { listOf("MON", "TUE", "WED", "THU", "FRI") },
    )

  private fun buildSchedules(
    settings: CivicOrgSettings,
    departments: List<DepartmentDto>,
  ): List<CivicWorkSchedule> {
    if (departments.isEmpty()) {
      return listOf(
        CivicWorkSchedule(
          id = settings.settingsId.ifBlank { "org-default" },
          title = "Standard Organisation Shift",
          workingDays = settings.workingDays,
          startTime = settings.workStartTime,
          endTime = settings.workEndTime,
          lateGraceMinutes = settings.lateGraceMinutes,
          departmentName = "All Departments",
        )
      )
    }
    return departments.map { dept ->
      CivicWorkSchedule(
        id = "sch-${dept.id}",
        title = "${dept.name} Standard Shift",
        workingDays = settings.workingDays,
        startTime = settings.workStartTime,
        endTime = settings.workEndTime,
        lateGraceMinutes = settings.lateGraceMinutes,
        departmentName = dept.name,
      )
    }
  }

  private fun mapEmployeeProfile(
    emp: EmployeeProfileRecordDto,
    deptMap: Map<String, DepartmentDto>,
    wpMap: Map<String, WorkplaceDto>,
    records: List<AttendanceRecordDto>,
  ): CivicEmployee {
    val empRecs = records.filter { it.employeeId == emp.id }
    val deptName = deptMap[emp.departmentId]?.name ?: emp.department ?: "Unassigned"
    val wpName = wpMap[emp.workplaceId]?.name ?: "Unassigned"
    return CivicEmployee(
      id = emp.id,
      userId = emp.userId,
      fullName = emp.fullName,
      employeeCode = emp.employeeCode.orEmpty(),
      email = "",
      phone = "",
      jobTitle = emp.role ?: "Workforce Member",
      departmentId = emp.departmentId.orEmpty(),
      departmentName = deptName,
      workplaceId = emp.workplaceId.orEmpty(),
      workplaceName = wpName,
      status = emp.status?.replaceFirstChar { it.uppercase() } ?: "Active",
      attendanceRatePercent = computeRatePercent(empRecs),
      supervisorId = null,
      avatarUrl = emp.profilePhotoUrl,
    )
  }

  private fun computeRatePercent(records: List<AttendanceRecordDto>): Int {
    if (records.isEmpty()) return 100
    val compliant =
      records.count {
        !it.isLate &&
          (it.geofenceStatus == LocationVerificationOutcome.VERIFIED.apiValue ||
            it.reviewStatus.equals("approved", ignoreCase = true))
      }
    return ((compliant.toDouble() / records.size) * 100).toInt().coerceIn(0, 100)
  }

  private fun mapAttendanceRecord(
    rec: AttendanceRecordDto,
    empLookup: Map<String, CivicEmployee>,
    wpMap: Map<String, WorkplaceDto>,
  ): CivicAttendanceRecord {
    val emp = empLookup[rec.employeeId]
    val wpName = wpMap[rec.workplaceId]?.name ?: emp?.workplaceName ?: rec.workplaceId
    val locVer = rec.locationVerifications.firstOrNull()
    val status =
      when {
        rec.reviewStatus.equals("approved", ignoreCase = true) -> CivicAttendanceStatus.ON_TIME
        rec.geofenceStatus != LocationVerificationOutcome.VERIFIED.apiValue ->
          CivicAttendanceStatus.EXCEPTION
        rec.isLate -> CivicAttendanceStatus.LATE
        else -> CivicAttendanceStatus.ON_TIME
      }
    val dateOnly = rec.recordedAtIso.take(10)
    val timeOnly = formatTimeShort(rec.recordedAtIso)
    val isOut = rec.checkType.equals("CHECK_OUT", ignoreCase = true)
    return CivicAttendanceRecord(
      id = rec.id,
      employeeId = rec.employeeId,
      employeeName = emp?.fullName ?: rec.employeeId.take(8),
      employeeCode = emp?.employeeCode ?: "",
      departmentName = emp?.departmentName ?: "",
      workplaceName = wpName,
      dateLabel = dateOnly,
      checkType = rec.checkType,
      checkInTime = if (!isOut) timeOnly else null,
      checkOutTime = if (isOut) timeOnly else null,
      status = status,
      locationNote = "${rec.geofenceStatus} (${rec.checkType})",
      biometricNote =
        if (rec.biometricVerifiedLocally) "OS-local BiometricPrompt passed"
        else "Not locally verified",
      geofenceStatusRaw = rec.geofenceStatus,
      reviewStatusRaw = rec.reviewStatus ?: "auto",
      distanceMeters = locVer?.distanceMeters,
      accuracyMeters = locVer?.accuracyMeters,
      exceptionReason = rec.notes ?: locVer?.failureReason,
      reviewedBy = rec.reviewStatus?.takeIf { it != "auto" }?.let { "Review: $it" },
    )
  }

  private fun mapLeaveRequest(
    dto: LeaveRequestDto,
    empLookup: Map<String, CivicEmployee>,
  ): CivicLeaveRequest {
    val emp = dto.employeeId?.let { empLookup[it] }
    val status =
      when (dto.status.lowercase()) {
        "approved" -> CivicLeaveStatus.APPROVED
        "rejected" -> CivicLeaveStatus.REJECTED
        else -> CivicLeaveStatus.PENDING
      }
    return CivicLeaveRequest(
      id = dto.id,
      employeeId = dto.employeeId.orEmpty(),
      employeeName = emp?.fullName ?: dto.employeeId?.take(8) ?: "Employee",
      departmentName = emp?.departmentName ?: "",
      leaveType = dto.leaveType ?: "Leave",
      startDate = dto.startDate.orEmpty(),
      endDate = dto.endDate.orEmpty(),
      daysCount = dto.daysCount ?: 1,
      reason = dto.reason.orEmpty(),
      status = status,
      submittedDate = dto.createdAt?.take(10) ?: dto.startDate.orEmpty(),
      reviewerNote = dto.reviewerNote,
    )
  }

  private fun mapAuditLog(dto: AuditLogDto) =
    CivicAuditEvent(
      id = dto.id,
      actorName = dto.actorUserId?.take(8) ?: "System",
      actorRole = "Member",
      action = dto.action,
      category = dto.entityType ?: "audit",
      summary = dto.summary.orEmpty(),
      timestampLabel = dto.createdAt?.take(16)?.replace('T', ' ') ?: "",
    )

  private fun formatTimeShort(iso: String): String =
    runCatching {
      val inst = Instant.parse(iso)
      DateTimeFormatter.ofPattern("HH:mm 'UTC'", Locale.US)
        .withZone(ZoneOffset.UTC)
        .format(inst)
    }.getOrElse { iso.drop(11).take(5) }

  class Factory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      val appCtx = context.applicationContext
      val api = SupabaseClientFactory.create()
      val capabilityChecker = AndroidDeviceCapabilityManager(appCtx)
      return FrontendViewModel(
        onboardingRepository = SupabaseOnboardingRepository(api),
        civicRepository = SupabaseCivicTrackRepository(api),
        preferences = DataStorePreferencesRepository(appCtx),
        capabilityChecker = capabilityChecker,
        locationProvider = FusedAttendanceLocationProvider(appCtx, capabilityChecker),
        biometricAuthenticator = AndroidBiometricAuthenticator(capabilityChecker),
      ) as T
    }
  }

  companion object {
    fun createForTesting(
      onboardingRepository: OnboardingRepository,
      civicRepository: CivicTrackRepository,
      preferences: PreferencesRepository,
      capabilityChecker: DeviceCapabilityChecker,
      locationProvider: AttendanceLocationProvider,
      biometricAuthenticator: BiometricAuthenticator,
    ): FrontendViewModel =
      FrontendViewModel(
        onboardingRepository = onboardingRepository,
        civicRepository = civicRepository,
        preferences = preferences,
        capabilityChecker = capabilityChecker,
        locationProvider = locationProvider,
        biometricAuthenticator = biometricAuthenticator,
      )
  }
}
