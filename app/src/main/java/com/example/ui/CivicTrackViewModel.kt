package com.example.ui

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
import com.example.data.local.DataStorePreferencesRepository
import com.example.data.local.PreferencesRepository
import com.example.data.model.AttendanceCheckType
import com.example.data.model.AttendanceRecordDto
import com.example.data.model.AttendanceSettingsDto
import com.example.data.model.AttendanceSubmissionStatus
import com.example.data.model.AuthSession
import com.example.data.model.BiometricStepStatus
import com.example.data.model.CreateEmployeeRequest
import com.example.data.model.DocumentCaptureCategory
import com.example.data.model.EmployeeDocumentDto
import com.example.data.model.EmployeeDto
import com.example.data.model.GeofenceStepStatus
import com.example.data.model.LocationVerificationOutcome
import com.example.data.model.PendingEmployeeProfile
import com.example.data.model.RegistrationOutcome
import com.example.data.model.WorkplaceDto
import com.example.data.remote.SupabaseClientFactory
import com.example.data.repository.AttendanceSubmissionPayload
import com.example.data.repository.BackendResult
import com.example.data.repository.CivicTrackRepository
import com.example.data.repository.SupabaseCivicTrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class CaptureTarget {
  PROFILE_PHOTO,
  DOCUMENT,
}

enum class RationaleType {
  LOCATION,
  CAMERA,
}

data class CivicTrackUiState(
  val isBackendConfigured: Boolean = false,
  val session: AuthSession? = null,
  val isRegisterMode: Boolean = true,
  val availableWorkplaces: List<WorkplaceDto> = emptyList(),
  val hasStoredSessionForBiometricUnlock: Boolean = false,
  val isAuthLoading: Boolean = false,
  val authError: String? = null,
  val authSuccessMessage: String? = null,
  val employee: EmployeeDto? = null,
  val isProfileLoading: Boolean = false,
  val profileError: String? = null,
  val recentAttendance: List<AttendanceRecordDto> = emptyList(),
  val employeeDocuments: List<EmployeeDocumentDto> = emptyList(),
  val capabilities: DeviceCapabilitySnapshot? = null,
  val biometricLoginEnabled: Boolean = false,
  // 3 distinct attendance verification pipeline states
  val activeCheckType: AttendanceCheckType? = null,
  val biometricStep: BiometricStepStatus = BiometricStepStatus.Idle,
  val geofenceStep: GeofenceStepStatus = GeofenceStepStatus.Idle,
  val submissionStep: AttendanceSubmissionStatus = AttendanceSubmissionStatus.Idle,
  val pendingRetryPayload: AttendanceSubmissionPayload? = null,
  // Permission rationale / settings guidance state
  val activeRationale: RationaleType? = null,
  val pendingActionAfterLocationPermission: AttendanceCheckType? = null,
  val pendingCaptureAfterCameraPermission: CaptureTarget? = null,
  val permissionBlockedMessage: String? = null,
  val blockedPermissionRequiresLocationSettings: Boolean = false,
  // Camera & Document upload state
  val activeCameraTarget: CaptureTarget? = null,
  val selectedDocumentCategory: DocumentCaptureCategory = DocumentCaptureCategory.ID_BADGE,
  val documentTitleInput: String = "",
  val pendingUploadAsset: ProcessedImageAsset? = null,
  val isUploadingMedia: Boolean = false,
  val mediaStatusMessage: String? = null,
  val mediaErrorMessage: String? = null,
  // Settings test prompt feedback
  val settingsBiometricTestMessage: String? = null,
)

class CivicTrackViewModel(
  private val repository: CivicTrackRepository,
  private val preferences: PreferencesRepository,
  private val capabilityChecker: DeviceCapabilityChecker,
  private val locationProvider: AttendanceLocationProvider,
  private val biometricAuthenticator: BiometricAuthenticator,
) : ViewModel() {

  private val _uiState =
    MutableStateFlow(CivicTrackUiState(isBackendConfigured = repository.isBackendConfigured()))
  val uiState: StateFlow<CivicTrackUiState> = _uiState.asStateFlow()

  private var storedSessionForUnlock: AuthSession? = null

  init {
    viewModelScope.launch {
      preferences.biometricLoginEnabled.collectLatest { enabled ->
        _uiState.update { it.copy(biometricLoginEnabled = enabled) }
      }
    }
    viewModelScope.launch {
      preferences.authSession.collectLatest { saved ->
        storedSessionForUnlock = saved
        val bioEnabled = _uiState.value.biometricLoginEnabled
        if (saved != null && _uiState.value.session == null) {
          if (bioEnabled) {
            _uiState.update {
              it.copy(hasStoredSessionForBiometricUnlock = true, isRegisterMode = false)
            }
          } else {
            _uiState.update { it.copy(session = saved, hasStoredSessionForBiometricUnlock = false) }
            refreshEmployeeData(saved)
          }
        }
      }
    }
    if (repository.isBackendConfigured()) {
      refreshWorkplaces()
    }
  }

  fun refreshWorkplaces() {
    viewModelScope.launch {
      when (val res = repository.loadWorkplaces()) {
        is BackendResult.Success -> _uiState.update { it.copy(availableWorkplaces = res.data) }
        else -> Unit
      }
    }
  }

  fun setAuthMode(registerMode: Boolean) {
    _uiState.update {
      it.copy(isRegisterMode = registerMode, authError = null, authSuccessMessage = null)
    }
  }

  fun refreshCapabilities(activity: Activity?) {
    viewModelScope.launch {
      val locRequested =
        preferences.wasPermissionRequested(Manifest.permission.ACCESS_FINE_LOCATION)
      val camRequested = preferences.wasPermissionRequested(Manifest.permission.CAMERA)
      val snapshot =
        capabilityChecker.inspectCapabilities(
          activity = activity,
          locationPreviouslyRequested = locRequested,
          cameraPreviouslyRequested = camRequested,
        )
      _uiState.update { it.copy(capabilities = snapshot) }
    }
  }

  fun registerEmployee(
    fullName: String,
    employeeCode: String,
    email: String,
    password: String,
    confirmPassword: String,
    department: String,
    role: String,
    workplaceId: String?,
  ) {
    when {
      fullName.isBlank() -> {
        _uiState.update { it.copy(authError = "Enter your full name to register.") }
        return
      }
      employeeCode.isBlank() -> {
        _uiState.update { it.copy(authError = "Enter your assigned employee code.") }
        return
      }
      email.isBlank() || !email.contains("@") -> {
        _uiState.update { it.copy(authError = "Enter a valid work email address.") }
        return
      }
      password.length < 6 -> {
        _uiState.update { it.copy(authError = "Password must be at least 6 characters.") }
        return
      }
      password != confirmPassword -> {
        _uiState.update { it.copy(authError = "Passwords do not match.") }
        return
      }
    }

    viewModelScope.launch {
      _uiState.update {
        it.copy(isAuthLoading = true, authError = null, authSuccessMessage = null)
      }
      val resolvedWorkplaceId =
        workplaceId ?: _uiState.value.availableWorkplaces.firstOrNull()?.id

      when (
        val result =
          repository.registerEmployee(
            email = email,
            password = password,
            fullName = fullName,
            employeeCode = employeeCode,
            department = department,
            role = role,
            workplaceId = resolvedWorkplaceId,
          )
      ) {
        is BackendResult.Success -> {
          when (val outcome = result.data) {
            is RegistrationOutcome.SignedIn -> {
              preferences.clearPendingEmployeeProfile()
              preferences.saveAuthSession(outcome.session)
              _uiState.update {
                it.copy(
                  isAuthLoading = false,
                  session = outcome.session,
                  employee = outcome.employee,
                  hasStoredSessionForBiometricUnlock = false,
                  authError = null,
                  authSuccessMessage = null,
                )
              }
              if (outcome.employee != null) {
                loadHistoryAndDocs(outcome.session, outcome.employee.id)
              } else {
                refreshEmployeeData(outcome.session)
              }
            }
            is RegistrationOutcome.EmailConfirmationRequired -> {
              preferences.savePendingEmployeeProfile(
                PendingEmployeeProfile(
                  email = outcome.email,
                  fullName = fullName.trim(),
                  employeeCode = employeeCode.trim(),
                  department = department.trim().takeIf { it.isNotEmpty() },
                  role = role.trim().takeIf { it.isNotEmpty() },
                  workplaceId = resolvedWorkplaceId,
                )
              )
              _uiState.update {
                it.copy(
                  isAuthLoading = false,
                  isRegisterMode = false,
                  authError = null,
                  authSuccessMessage =
                    "Account registered for ${outcome.email}. Check your email to confirm if required, then sign in.",
                )
              }
            }
          }
        }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isAuthLoading = false, authError = result.message) }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isAuthLoading = false, authError = result.message) }
        BackendResult.NotConfigured ->
          _uiState.update {
            it.copy(
              isAuthLoading = false,
              authError =
                "Supabase is not configured. Add SUPABASE_URL and SUPABASE_ANON_KEY in AI Studio Secrets.",
            )
          }
      }
    }
  }

  fun signInWithPassword(email: String, password: String) {
    if (email.isBlank() || password.isBlank()) {
      _uiState.update { it.copy(authError = "Enter both your work email and password.") }
      return
    }
    viewModelScope.launch {
      _uiState.update {
        it.copy(isAuthLoading = true, authError = null, authSuccessMessage = null)
      }
      when (val result = repository.signIn(email, password)) {
        is BackendResult.Success -> {
          preferences.saveAuthSession(result.data)
          _uiState.update {
            it.copy(
              isAuthLoading = false,
              session = result.data,
              hasStoredSessionForBiometricUnlock = false,
              authError = null,
              authSuccessMessage = null,
            )
          }
          refreshEmployeeData(result.data)
        }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isAuthLoading = false, authError = result.message) }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isAuthLoading = false, authError = result.message) }
        BackendResult.NotConfigured ->
          _uiState.update {
            it.copy(
              isAuthLoading = false,
              authError =
                "Supabase is not configured. Add SUPABASE_URL and SUPABASE_ANON_KEY in AI Studio Secrets.",
            )
          }
      }
    }
  }

  fun unlockSavedSessionWithBiometric(activity: FragmentActivity) {
    val saved = storedSessionForUnlock ?: return
    viewModelScope.launch {
      _uiState.update { it.copy(isAuthLoading = true, authError = null) }
      when (
        val bioResult =
          biometricAuthenticator.authenticate(
            activity = activity,
            title = "Unlock CivicTrack",
            subtitle = "Local device authentication for ${saved.email}",
            description = "Confirm device owner credential to resume your CivicTrack session.",
            allowDeviceCredentialFallback = true,
          )
      ) {
        is BiometricAuthOutcome.Success -> {
          _uiState.update {
            it.copy(
              isAuthLoading = false,
              session = saved,
              hasStoredSessionForBiometricUnlock = false,
              authError = null,
            )
          }
          refreshEmployeeData(saved)
        }
        is BiometricAuthOutcome.Cancelled ->
          _uiState.update { it.copy(isAuthLoading = false, authError = bioResult.reason) }
        is BiometricAuthOutcome.LockedOut ->
          _uiState.update { it.copy(isAuthLoading = false, authError = bioResult.message) }
        BiometricAuthOutcome.NotEnrolled ->
          _uiState.update {
            it.copy(
              isAuthLoading = false,
              authError =
                "No biometric or screen-lock credential enrolled. Sign in with your work password instead.",
            )
          }
        is BiometricAuthOutcome.HardwareUnavailable ->
          _uiState.update { it.copy(isAuthLoading = false, authError = bioResult.message) }
        is BiometricAuthOutcome.Error ->
          _uiState.update { it.copy(isAuthLoading = false, authError = bioResult.message) }
      }
    }
  }

  fun signOut() {
    viewModelScope.launch {
      preferences.clearAuthSession()
      _uiState.update {
        it.copy(
          session = null,
          hasStoredSessionForBiometricUnlock = false,
          employee = null,
          recentAttendance = emptyList(),
          employeeDocuments = emptyList(),
          biometricStep = BiometricStepStatus.Idle,
          geofenceStep = GeofenceStepStatus.Idle,
          submissionStep = AttendanceSubmissionStatus.Idle,
          pendingRetryPayload = null,
        )
      }
    }
  }

  fun refreshEmployeeData(sessionOverride: AuthSession? = null) {
    val activeSession = sessionOverride ?: _uiState.value.session ?: return
    viewModelScope.launch {
      _uiState.update { it.copy(isProfileLoading = true, profileError = null) }
      when (val profileRes = repository.loadEmployeeProfile(activeSession)) {
        is BackendResult.Success -> {
          preferences.clearPendingEmployeeProfile()
          val emp = profileRes.data
          _uiState.update {
            it.copy(isProfileLoading = false, employee = emp, profileError = null)
          }
          loadHistoryAndDocs(activeSession, emp.id)
        }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isProfileLoading = false, profileError = profileRes.message) }
        is BackendResult.ApiFailure -> {
          val pending = preferences.getPendingEmployeeProfile()
          if (pending != null && pending.email.equals(activeSession.email, ignoreCase = true)) {
            when (
              val createdRes =
                repository.createEmployeeProfile(
                  session = activeSession,
                  request =
                    CreateEmployeeRequest(
                      userId = activeSession.userId,
                      fullName = pending.fullName,
                      employeeCode = pending.employeeCode,
                      department = pending.department,
                      role = pending.role,
                      workplaceId = pending.workplaceId,
                    ),
                )
            ) {
              is BackendResult.Success -> {
                preferences.clearPendingEmployeeProfile()
                val emp = createdRes.data
                _uiState.update {
                  it.copy(isProfileLoading = false, employee = emp, profileError = null)
                }
                loadHistoryAndDocs(activeSession, emp.id)
              }
              is BackendResult.NetworkFailure ->
                _uiState.update {
                  it.copy(isProfileLoading = false, profileError = createdRes.message)
                }
              is BackendResult.ApiFailure ->
                _uiState.update {
                  it.copy(isProfileLoading = false, profileError = createdRes.message)
                }
              BackendResult.NotConfigured ->
                _uiState.update {
                  it.copy(
                    isProfileLoading = false,
                    profileError = "Configure Supabase credentials in AI Studio Secrets.",
                  )
                }
            }
          } else {
            _uiState.update { it.copy(isProfileLoading = false, profileError = profileRes.message) }
          }
        }
        BackendResult.NotConfigured ->
          _uiState.update {
            it.copy(
              isProfileLoading = false,
              profileError = "Configure Supabase credentials in AI Studio Secrets.",
            )
          }
      }
    }
  }

  private suspend fun loadHistoryAndDocs(session: AuthSession, employeeId: String) {
    when (val attRes = repository.loadRecentAttendance(session, employeeId)) {
      is BackendResult.Success -> _uiState.update { it.copy(recentAttendance = attRes.data) }
      else -> Unit
    }
    when (val docRes = repository.loadEmployeeDocuments(session, employeeId)) {
      is BackendResult.Success -> _uiState.update { it.copy(employeeDocuments = docRes.data) }
      else -> Unit
    }
  }

  /**
   * Entry point for employee Check-In / Check-Out workflow (Section 21.F).
   */
  fun initiateAttendanceAction(
    checkType: AttendanceCheckType,
    activity: FragmentActivity,
    onRequestLocationPermissions: () -> Unit,
  ) {
    val currentState = _uiState.value
    val session = currentState.session
    val employee = currentState.employee
    val workplace = employee?.workplaces
    if (session == null || employee == null || workplace == null) {
      _uiState.update {
        it.copy(
          profileError = "Employee profile and assigned workplace must be loaded before check-in."
        )
      }
      return
    }

    refreshCapabilitiesAndProceedAttendance(
      checkType = checkType,
      activity = activity,
      session = session,
      employee = employee,
      workplace = workplace,
      onRequestLocationPermissions = onRequestLocationPermissions,
    )
  }

  private fun refreshCapabilitiesAndProceedAttendance(
    checkType: AttendanceCheckType,
    activity: FragmentActivity,
    session: AuthSession,
    employee: EmployeeDto,
    workplace: WorkplaceDto,
    onRequestLocationPermissions: () -> Unit,
  ) {
    viewModelScope.launch {
      val locRequested =
        preferences.wasPermissionRequested(Manifest.permission.ACCESS_FINE_LOCATION)
      val camRequested = preferences.wasPermissionRequested(Manifest.permission.CAMERA)
      val snapshot = capabilityChecker.inspectCapabilities(activity, locRequested, camRequested)
      _uiState.update {
        it.copy(
          capabilities = snapshot,
          activeCheckType = checkType,
          permissionBlockedMessage = null,
          blockedPermissionRequiresLocationSettings = false,
        )
      }

      when (snapshot.locationPermission) {
        LocationPermissionState.DENIED_CAN_REQUEST -> {
          _uiState.update {
            it.copy(
              activeRationale = RationaleType.LOCATION,
              pendingActionAfterLocationPermission = checkType,
            )
          }
          return@launch
        }
        LocationPermissionState.PERMANENTLY_DENIED -> {
          _uiState.update {
            it.copy(
              biometricStep = BiometricStepStatus.Idle,
              geofenceStep = GeofenceStepStatus.PermissionDenied(isPermanent = true),
              submissionStep = AttendanceSubmissionStatus.Idle,
              permissionBlockedMessage =
                capabilityChecker.userMessageForLocationIssue(
                  snapshot.locationPermission,
                  snapshot.locationServicesEnabled,
                ),
              blockedPermissionRequiresLocationSettings = false,
            )
          }
          return@launch
        }
        LocationPermissionState.FINE_GRANTED,
        LocationPermissionState.COARSE_ONLY_GRANTED -> {
          if (!snapshot.locationServicesEnabled) {
            _uiState.update {
              it.copy(
                biometricStep = BiometricStepStatus.Idle,
                geofenceStep = GeofenceStepStatus.ServicesDisabled,
                submissionStep = AttendanceSubmissionStatus.Idle,
                permissionBlockedMessage =
                  capabilityChecker.userMessageForLocationIssue(
                    snapshot.locationPermission,
                    servicesEnabled = false,
                  ),
                blockedPermissionRequiresLocationSettings = true,
              )
            }
            return@launch
          }
        }
      }

      executeAttendancePipeline(
        checkType = checkType,
        activity = activity,
        session = session,
        employee = employee,
        workplace = workplace,
        snapshot = snapshot,
      )
    }
  }

  fun confirmRationaleAndRequestPermission(
    onLaunchLocationPermission: () -> Unit,
    onLaunchCameraPermission: () -> Unit,
  ) {
    val rationale = _uiState.value.activeRationale ?: return
    _uiState.update { it.copy(activeRationale = null) }
    viewModelScope.launch {
      when (rationale) {
        RationaleType.LOCATION -> {
          preferences.markPermissionRequested(Manifest.permission.ACCESS_FINE_LOCATION)
          onLaunchLocationPermission()
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
        pendingActionAfterLocationPermission = null,
        pendingCaptureAfterCameraPermission = null,
      )
    }
  }

  fun onLocationPermissionResult(
    granted: Boolean,
    activity: FragmentActivity,
  ) {
    val pendingCheck = _uiState.value.pendingActionAfterLocationPermission
    _uiState.update { it.copy(pendingActionAfterLocationPermission = null) }
    refreshCapabilities(activity)

    if (!granted) {
      viewModelScope.launch {
        val locRequested =
          preferences.wasPermissionRequested(Manifest.permission.ACCESS_FINE_LOCATION)
        val camRequested = preferences.wasPermissionRequested(Manifest.permission.CAMERA)
        val updated = capabilityChecker.inspectCapabilities(activity, locRequested, camRequested)
        val isPermanent =
          updated.locationPermission == LocationPermissionState.PERMANENTLY_DENIED
        _uiState.update {
          it.copy(
            capabilities = updated,
            geofenceStep = GeofenceStepStatus.PermissionDenied(isPermanent = isPermanent),
            permissionBlockedMessage =
              capabilityChecker.userMessageForLocationIssue(
                updated.locationPermission,
                updated.locationServicesEnabled,
              ),
            blockedPermissionRequiresLocationSettings = false,
          )
        }
      }
      return
    }

    if (pendingCheck != null) {
      val session = _uiState.value.session ?: return
      val employee = _uiState.value.employee ?: return
      val workplace = employee.workplaces ?: return
      viewModelScope.launch {
        val locRequested =
          preferences.wasPermissionRequested(Manifest.permission.ACCESS_FINE_LOCATION)
        val camRequested = preferences.wasPermissionRequested(Manifest.permission.CAMERA)
        val updated = capabilityChecker.inspectCapabilities(activity, locRequested, camRequested)
        _uiState.update { it.copy(capabilities = updated) }
        if (!updated.locationServicesEnabled) {
          _uiState.update {
            it.copy(
              geofenceStep = GeofenceStepStatus.ServicesDisabled,
              permissionBlockedMessage =
                capabilityChecker.userMessageForLocationIssue(
                  updated.locationPermission,
                  servicesEnabled = false,
                ),
              blockedPermissionRequiresLocationSettings = true,
            )
          }
        } else {
          executeAttendancePipeline(
            checkType = pendingCheck,
            activity = activity,
            session = session,
            employee = employee,
            workplace = workplace,
            snapshot = updated,
          )
        }
      }
    }
  }

  private suspend fun executeAttendancePipeline(
    checkType: AttendanceCheckType,
    activity: FragmentActivity,
    session: AuthSession,
    employee: EmployeeDto,
    workplace: WorkplaceDto,
    snapshot: DeviceCapabilitySnapshot,
  ) {
    val settings = employee.attendanceSettings ?: AttendanceSettingsDto()
    val requireBiometric =
      settings.requireBiometricForAttendance || _uiState.value.biometricLoginEnabled

    // Step 1: Device-level Biometric Authentication (distinct from server identity & geofence)
    var biometricVerifiedLocally = false
    if (requireBiometric) {
      _uiState.update {
        it.copy(
          biometricStep = BiometricStepStatus.Prompting,
          geofenceStep = GeofenceStepStatus.Idle,
          submissionStep = AttendanceSubmissionStatus.Idle,
        )
      }
      when (
        val bioOutcome =
          biometricAuthenticator.authenticate(
            activity = activity,
            title = "Confirm ${checkType.displayLabel}",
            subtitle = "Local device authentication for ${workplace.name}",
            description =
              "Verifies local device owner presence before reading location for attendance.",
            allowDeviceCredentialFallback = true,
          )
      ) {
        is BiometricAuthOutcome.Success -> {
          biometricVerifiedLocally = true
          _uiState.update {
            it.copy(biometricStep = BiometricStepStatus.LocalDeviceAuthenticated(bioOutcome.methodLabel))
          }
        }
        is BiometricAuthOutcome.Cancelled -> {
          _uiState.update {
            it.copy(biometricStep = BiometricStepStatus.Cancelled(bioOutcome.reason))
          }
          return
        }
        is BiometricAuthOutcome.LockedOut -> {
          _uiState.update {
            it.copy(biometricStep = BiometricStepStatus.Error(bioOutcome.message))
          }
          return
        }
        BiometricAuthOutcome.NotEnrolled -> {
          _uiState.update {
            it.copy(
              biometricStep =
                BiometricStepStatus.Unavailable(
                  "No biometric or screen lock credential enrolled on this device."
                )
            )
          }
          return
        }
        is BiometricAuthOutcome.HardwareUnavailable -> {
          _uiState.update {
            it.copy(biometricStep = BiometricStepStatus.Unavailable(bioOutcome.message))
          }
          return
        }
        is BiometricAuthOutcome.Error -> {
          _uiState.update {
            it.copy(biometricStep = BiometricStepStatus.Error(bioOutcome.message))
          }
          return
        }
      }
    } else {
      _uiState.update { it.copy(biometricStep = BiometricStepStatus.NotRequired) }
    }

    // Step 2: Acquire device coordinates & evaluate workplace geofence
    _uiState.update {
      it.copy(
        geofenceStep = GeofenceStepStatus.AcquiringLocation,
        submissionStep = AttendanceSubmissionStatus.Idle,
      )
    }

    val locationResult = locationProvider.fetchCurrentLocation(snapshot.locationPermission)
    val payload: AttendanceSubmissionPayload =
      when (locationResult) {
        is LocationFetchResult.PermissionDenied -> {
          _uiState.update {
            it.copy(
              geofenceStep = GeofenceStepStatus.PermissionDenied(locationResult.isPermanent),
              permissionBlockedMessage =
                capabilityChecker.userMessageForLocationIssue(
                  snapshot.locationPermission,
                  snapshot.locationServicesEnabled,
                ),
            )
          }
          return
        }
        LocationFetchResult.ServicesDisabled -> {
          _uiState.update {
            it.copy(
              geofenceStep = GeofenceStepStatus.ServicesDisabled,
              permissionBlockedMessage =
                capabilityChecker.userMessageForLocationIssue(
                  snapshot.locationPermission,
                  servicesEnabled = false,
                ),
              blockedPermissionRequiresLocationSettings = true,
            )
          }
          return
        }
        is LocationFetchResult.Unavailable -> {
          _uiState.update {
            it.copy(geofenceStep = GeofenceStepStatus.LocationUnavailable(locationResult.reason))
          }
          return
        }
        is LocationFetchResult.Success -> {
          val captured = locationResult.location
          when (val eval = GeofenceEvaluator.evaluate(captured, workplace)) {
            is GeofenceEvaluationResult.Verified -> {
              _uiState.update {
                it.copy(
                  geofenceStep =
                    GeofenceStepStatus.Verified(
                      latitude = captured.latitude,
                      longitude = captured.longitude,
                      accuracyMeters = captured.accuracyMeters,
                      distanceMeters = eval.distanceMeters,
                      permittedRadiusMeters = eval.permittedRadiusMeters,
                      timestampIso = captured.timestampIso,
                    )
                )
              }
              AttendanceSubmissionPayload(
                employeeId = employee.id,
                workplaceId = workplace.id,
                checkType = checkType,
                biometricVerifiedLocally = biometricVerifiedLocally,
                latitude = captured.latitude,
                longitude = captured.longitude,
                accuracyMeters = captured.accuracyMeters,
                locationTimestampIso = captured.timestampIso,
                distanceMeters = eval.distanceMeters,
                permittedRadiusMeters = eval.permittedRadiusMeters,
                verificationOutcome = LocationVerificationOutcome.VERIFIED,
              )
            }
            is GeofenceEvaluationResult.OutsideGeofence -> {
              _uiState.update {
                it.copy(
                  geofenceStep =
                    GeofenceStepStatus.OutsideGeofence(
                      latitude = captured.latitude,
                      longitude = captured.longitude,
                      accuracyMeters = captured.accuracyMeters,
                      distanceMeters = eval.distanceMeters,
                      permittedRadiusMeters = eval.permittedRadiusMeters,
                      timestampIso = captured.timestampIso,
                    )
                )
              }
              if (!settings.allowCheckInOutsideGeofence) {
                return
              }
              AttendanceSubmissionPayload(
                employeeId = employee.id,
                workplaceId = workplace.id,
                checkType = checkType,
                biometricVerifiedLocally = biometricVerifiedLocally,
                latitude = captured.latitude,
                longitude = captured.longitude,
                accuracyMeters = captured.accuracyMeters,
                locationTimestampIso = captured.timestampIso,
                distanceMeters = eval.distanceMeters,
                permittedRadiusMeters = eval.permittedRadiusMeters,
                verificationOutcome = LocationVerificationOutcome.OUTSIDE_GEOFENCE,
                failureReason = eval.failureReason,
              )
            }
            is GeofenceEvaluationResult.InsufficientAccuracy -> {
              _uiState.update {
                it.copy(
                  geofenceStep =
                    GeofenceStepStatus.InsufficientAccuracy(
                      accuracyMeters = captured.accuracyMeters,
                      maxAllowedMeters = eval.maxAllowedAccuracyMeters,
                    )
                )
              }
              return
            }
          }
        }
      }

    // Step 3: Submit attendance_records + location_verifications to Supabase
    submitAttendancePayload(session, payload)
  }

  fun retryPendingAttendanceSubmission() {
    val session = _uiState.value.session ?: return
    val payload = _uiState.value.pendingRetryPayload ?: return
    viewModelScope.launch { submitAttendancePayload(session, payload) }
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
    when (val res = repository.submitAttendanceWithLocationVerification(session, payload)) {
      is BackendResult.Success -> {
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
          )
        }
        loadHistoryAndDocs(session, payload.employeeId)
      }
      is BackendResult.NetworkFailure -> {
        _uiState.update {
          it.copy(submissionStep = AttendanceSubmissionStatus.NetworkError(res.message))
        }
      }
      is BackendResult.ApiFailure -> {
        _uiState.update {
          it.copy(submissionStep = AttendanceSubmissionStatus.BackendError(res.message))
        }
      }
      BackendResult.NotConfigured -> {
        _uiState.update {
          it.copy(
            submissionStep =
              AttendanceSubmissionStatus.BackendError(
                "Supabase credentials are not configured in AI Studio Secrets."
              )
          )
        }
      }
    }
  }

  // Camera & Media Capture Workflow (Section 21.B)

  fun requestCameraCapture(target: CaptureTarget, activity: Activity) {
    viewModelScope.launch {
      val locRequested =
        preferences.wasPermissionRequested(Manifest.permission.ACCESS_FINE_LOCATION)
      val camRequested = preferences.wasPermissionRequested(Manifest.permission.CAMERA)
      val snapshot = capabilityChecker.inspectCapabilities(activity, locRequested, camRequested)
      _uiState.update {
        it.copy(
          capabilities = snapshot,
          mediaErrorMessage = null,
          mediaStatusMessage = null,
          permissionBlockedMessage = null,
        )
      }

      if (!snapshot.cameraAvailable) {
        _uiState.update {
          it.copy(
            mediaErrorMessage =
              capabilityChecker.userMessageForCameraIssue(
                available = false,
                permission = snapshot.cameraPermission,
              )
          )
        }
        return@launch
      }

      when (snapshot.cameraPermission) {
        PermissionAccessState.GRANTED -> {
          _uiState.update { it.copy(activeCameraTarget = target) }
        }
        PermissionAccessState.DENIED_CAN_REQUEST -> {
          _uiState.update {
            it.copy(
              activeRationale = RationaleType.CAMERA,
              pendingCaptureAfterCameraPermission = target,
            )
          }
        }
        PermissionAccessState.PERMANENTLY_DENIED -> {
          _uiState.update {
            it.copy(
              mediaErrorMessage =
                capabilityChecker.userMessageForCameraIssue(
                  available = true,
                  permission = PermissionAccessState.PERMANENTLY_DENIED,
                ),
              permissionBlockedMessage =
                capabilityChecker.userMessageForCameraIssue(
                  available = true,
                  permission = PermissionAccessState.PERMANENTLY_DENIED,
                ),
            )
          }
        }
      }
    }
  }

  fun onCameraPermissionResult(granted: Boolean, activity: Activity) {
    val pendingTarget = _uiState.value.pendingCaptureAfterCameraPermission
    _uiState.update { it.copy(pendingCaptureAfterCameraPermission = null) }
    refreshCapabilities(activity)

    if (granted && pendingTarget != null) {
      _uiState.update { it.copy(activeCameraTarget = pendingTarget) }
    } else if (!granted) {
      viewModelScope.launch {
        val locRequested =
          preferences.wasPermissionRequested(Manifest.permission.ACCESS_FINE_LOCATION)
        val camRequested = preferences.wasPermissionRequested(Manifest.permission.CAMERA)
        val updated = capabilityChecker.inspectCapabilities(activity, locRequested, camRequested)
        _uiState.update {
          it.copy(
            capabilities = updated,
            mediaErrorMessage =
              capabilityChecker.userMessageForCameraIssue(
                available = updated.cameraAvailable,
                permission = updated.cameraPermission,
              ),
            permissionBlockedMessage =
              if (updated.cameraPermission == PermissionAccessState.PERMANENTLY_DENIED) {
                capabilityChecker.userMessageForCameraIssue(
                  available = updated.cameraAvailable,
                  permission = updated.cameraPermission,
                )
              } else {
                null
              },
          )
        }
      }
    }
  }

  fun cancelCameraCapture() {
    _uiState.value.pendingUploadAsset?.file?.delete()
    _uiState.update {
      it.copy(activeCameraTarget = null, pendingUploadAsset = null)
    }
  }

  fun onImageCapturedFromCamera(context: Context, rawFile: File) {
    val target = _uiState.value.activeCameraTarget ?: CaptureTarget.DOCUMENT
    val preset =
      if (target == CaptureTarget.PROFILE_PHOTO) {
        ImageUploadPreset.PROFILE_PHOTO
      } else {
        ImageUploadPreset.WORKFORCE_DOCUMENT
      }

    when (val processed = ImageProcessor.processCapturedFile(context, rawFile, preset)) {
      is ImageProcessingResult.Success -> {
        rawFile.delete()
        _uiState.update {
          it.copy(
            pendingUploadAsset = processed.asset,
            mediaErrorMessage = null,
          )
        }
      }
      is ImageProcessingResult.ValidationError -> {
        rawFile.delete()
        _uiState.update {
          it.copy(mediaErrorMessage = processed.message, activeCameraTarget = null)
        }
      }
    }
  }

  fun onImagePickedFromGallery(context: Context, uri: Uri) {
    when (
      val processed =
        ImageProcessor.processPickedUri(context, uri, ImageUploadPreset.WORKFORCE_DOCUMENT)
    ) {
      is ImageProcessingResult.Success -> {
        _uiState.update {
          it.copy(
            activeCameraTarget = CaptureTarget.DOCUMENT,
            pendingUploadAsset = processed.asset,
            mediaErrorMessage = null,
            mediaStatusMessage = null,
          )
        }
      }
      is ImageProcessingResult.ValidationError -> {
        _uiState.update { it.copy(mediaErrorMessage = processed.message) }
      }
    }
  }

  fun retakePhoto() {
    _uiState.value.pendingUploadAsset?.file?.delete()
    _uiState.update { it.copy(pendingUploadAsset = null) }
  }

  fun updateDocumentCategory(category: DocumentCaptureCategory) {
    _uiState.update { it.copy(selectedDocumentCategory = category) }
  }

  fun updateDocumentTitle(title: String) {
    _uiState.update { it.copy(documentTitleInput = title) }
  }

  fun confirmUploadPendingAsset() {
    val state = _uiState.value
    val asset = state.pendingUploadAsset ?: return
    val session = state.session
    val employee = state.employee
    val target = state.activeCameraTarget ?: CaptureTarget.DOCUMENT

    if (session == null || employee == null) {
      _uiState.update {
        it.copy(
          mediaErrorMessage = "Sign in with an employee account before uploading to Supabase Storage."
        )
      }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isUploadingMedia = true, mediaErrorMessage = null) }
      val result =
        if (target == CaptureTarget.PROFILE_PHOTO) {
          when (
            val res =
              repository.uploadProfilePhoto(session, employee.id, asset.file, asset.mimeType)
          ) {
            is BackendResult.Success -> {
              _uiState.update {
                it.copy(
                  employee = res.data,
                  mediaStatusMessage =
                    "Profile photo compressed (${asset.sizeBytes / 1024} KB) and uploaded.",
                )
              }
              true
            }
            is BackendResult.NetworkFailure -> {
              _uiState.update { it.copy(mediaErrorMessage = res.message) }
              false
            }
            is BackendResult.ApiFailure -> {
              _uiState.update { it.copy(mediaErrorMessage = res.message) }
              false
            }
            BackendResult.NotConfigured -> {
              _uiState.update {
                it.copy(
                  mediaErrorMessage =
                    "Configure SUPABASE_URL and SUPABASE_ANON_KEY in AI Studio Secrets to upload files."
                )
              }
              false
            }
          }
        } else {
          when (
            val res =
              repository.uploadEmployeeDocument(
                session = session,
                employeeId = employee.id,
                category = state.selectedDocumentCategory,
                title = state.documentTitleInput,
                imageFile = asset.file,
                mimeType = asset.mimeType,
              )
          ) {
            is BackendResult.Success -> {
              _uiState.update {
                it.copy(
                  employeeDocuments = listOf(res.data) + it.employeeDocuments,
                  documentTitleInput = "",
                  mediaStatusMessage =
                    "Document '${res.data.title}' uploaded (${asset.sizeBytes / 1024} KB).",
                )
              }
              true
            }
            is BackendResult.NetworkFailure -> {
              _uiState.update { it.copy(mediaErrorMessage = res.message) }
              false
            }
            is BackendResult.ApiFailure -> {
              _uiState.update { it.copy(mediaErrorMessage = res.message) }
              false
            }
            BackendResult.NotConfigured -> {
              _uiState.update {
                it.copy(
                  mediaErrorMessage =
                    "Configure SUPABASE_URL and SUPABASE_ANON_KEY in AI Studio Secrets to upload files."
                )
              }
              false
            }
          }
        }

      if (result) {
        asset.file.delete()
        _uiState.update {
          it.copy(
            isUploadingMedia = false,
            pendingUploadAsset = null,
            activeCameraTarget = null,
          )
        }
      } else {
        _uiState.update { it.copy(isUploadingMedia = false) }
      }
    }
  }

  // Device & Security Settings

  fun setBiometricLoginEnabled(enabled: Boolean) {
    viewModelScope.launch {
      val available = _uiState.value.capabilities?.biometricCapability is BiometricCapabilityState.Available
      if (enabled && !available) {
        _uiState.update {
          it.copy(
            settingsBiometricTestMessage =
              "Cannot enable biometric login until a biometric or screen-lock credential is available and enrolled."
          )
        }
        return@launch
      }
      preferences.setBiometricLoginEnabled(enabled)
      _uiState.update {
        it.copy(
          settingsBiometricTestMessage =
            if (enabled) "Optional local biometric unlock enabled."
            else "Biometric unlock disabled."
        )
      }
    }
  }

  fun testBiometricPromptInSettings(activity: FragmentActivity) {
    viewModelScope.launch {
      val outcome =
        biometricAuthenticator.authenticate(
          activity = activity,
          title = "Test Biometric Hardware",
          subtitle = "CivicTrack local device check",
          description =
            "Confirms Android BiometricPrompt works on this device. No biometric data leaves the OS.",
          allowDeviceCredentialFallback = true,
        )
      val message =
        when (outcome) {
          is BiometricAuthOutcome.Success ->
            "Local OS authentication succeeded via ${outcome.methodLabel}."
          is BiometricAuthOutcome.Cancelled -> outcome.reason
          is BiometricAuthOutcome.LockedOut -> outcome.message
          BiometricAuthOutcome.NotEnrolled ->
            "No biometric or screen-lock credential enrolled. Open Device Security Settings."
          is BiometricAuthOutcome.HardwareUnavailable -> outcome.message
          is BiometricAuthOutcome.Error -> outcome.message
        }
      _uiState.update { it.copy(settingsBiometricTestMessage = message) }
    }
  }

  fun dismissPermissionBlockedBanner() {
    _uiState.update {
      it.copy(permissionBlockedMessage = null, blockedPermissionRequiresLocationSettings = false)
    }
  }

  class Factory(private val appContext: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      val capabilityManager = AndroidDeviceCapabilityManager(appContext.applicationContext)
      val preferences = DataStorePreferencesRepository(appContext.applicationContext)
      val api = SupabaseClientFactory.create()
      val repository = SupabaseCivicTrackRepository(api)
      val locationProvider = FusedAttendanceLocationProvider(appContext.applicationContext, capabilityManager)
      val biometricAuth = AndroidBiometricAuthenticator(capabilityManager)
      return CivicTrackViewModel(
        repository = repository,
        preferences = preferences,
        capabilityChecker = capabilityManager,
        locationProvider = locationProvider,
        biometricAuthenticator = biometricAuth,
      ) as T
    }
  }
}
