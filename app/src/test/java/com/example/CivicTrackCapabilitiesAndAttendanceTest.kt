package com.example

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.example.core.biometric.BiometricAuthOutcome
import com.example.core.biometric.BiometricAuthenticator
import com.example.core.capabilities.AndroidDeviceCapabilityManager
import com.example.core.capabilities.BiometricCapabilityState
import com.example.core.capabilities.DeviceCapabilityChecker
import com.example.core.capabilities.DeviceCapabilitySnapshot
import com.example.core.capabilities.DeviceCompatibilityInfo
import com.example.core.capabilities.LocationPermissionState
import com.example.core.capabilities.PermissionAccessState
import com.example.core.location.AttendanceLocationProvider
import com.example.core.location.CapturedLocation
import com.example.core.location.GeofenceEvaluationResult
import com.example.core.location.GeofenceEvaluator
import com.example.core.location.LocationFetchResult
import com.example.data.local.PreferencesRepository
import com.example.data.model.AttendanceCheckType
import com.example.data.model.AttendanceSettingsDto
import com.example.data.model.AttendanceSubmissionStatus
import com.example.data.model.AuthSession
import com.example.data.model.BiometricStepStatus
import com.example.data.model.EmployeeDto
import com.example.data.model.GeofenceStepStatus
import com.example.data.model.LocationVerificationOutcome
import com.example.data.model.PendingEmployeeProfile
import com.example.data.model.WorkplaceDto
import com.example.data.remote.SupabaseClientFactory
import com.example.data.remote.SupabaseEnvironment
import com.example.data.repository.BackendResult
import com.example.data.repository.CivicTrackRepository
import com.example.data.repository.SupabaseCivicTrackRepository
import com.example.ui.CaptureTarget
import com.example.ui.CivicTrackViewModel
import com.example.ui.RationaleType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowCameraCharacteristics
import okhttp3.mockwebserver.SocketPolicy

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CivicTrackCapabilitiesAndAttendanceTest {

  private val testDispatcher = UnconfinedTestDispatcher()
  private lateinit var context: Application
  private lateinit var activity: FragmentActivity
  private lateinit var mockWebServer: MockWebServer

  private val sampleWorkplace =
    WorkplaceDto(
      id = "wp-1",
      name = "City Hall Annex",
      address = "100 Civic Plaza",
      latitude = 37.7749,
      longitude = -122.4194,
      geofenceRadiusMeters = 150.0,
      maxLocationAccuracyMeters = 60.0,
    )

  private val sampleEmployee =
    EmployeeDto(
      id = "emp-1",
      userId = "user-1",
      fullName = "Jordan Rivera",
      employeeCode = "CT-1042",
      department = "Public Works",
      role = "Field Inspector",
      workplaceId = "wp-1",
      workplaces = sampleWorkplace,
      attendanceSettings =
        AttendanceSettingsDto(
          requireBiometricForAttendance = true,
          allowCheckInOutsideGeofence = false,
          requireLocationVerification = true,
        ),
    )

  private val sampleSession =
    AuthSession(accessToken = "jwt-token-123", userId = "user-1", email = "jordan@civictrack.org")

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    context = ApplicationProvider.getApplicationContext()
    activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
    mockWebServer = MockWebServer()
    mockWebServer.start()
  }

  @After
  fun tearDown() {
    mockWebServer.shutdown()
    Dispatchers.resetMain()
  }

  // --- Test Cases 1, 2, 3: Location permission granted, denied, and services disabled ---

  @Test
  fun `case 1 - location permission granted and services enabled marks location ready`() {
    val shadowApp = shadowOf(context)
    shadowApp.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    shadowOf(lm).setLocationEnabled(true)

    val manager = AndroidDeviceCapabilityManager(context)
    val snapshot =
      manager.inspectCapabilities(
        activity = activity,
        locationPreviouslyRequested = false,
        cameraPreviouslyRequested = false,
      )

    assertEquals(LocationPermissionState.FINE_GRANTED, snapshot.locationPermission)
    assertTrue(snapshot.locationServicesEnabled)
    assertTrue(snapshot.isLocationReady)
  }

  @Test
  fun `case 2 - location permission denied shows rationale before requesting and respects permanent denial`() =
    runTest {
      val prefs = FakePreferencesRepository(savedSession = sampleSession)
      val repo = createMockRepository()
      enqueueProfileAndEmptyHistory()

      val checker =
        FakeDeviceCapabilityChecker(
          locationPermission = LocationPermissionState.DENIED_CAN_REQUEST,
          locationServicesEnabled = true,
        )
      val vm =
        CivicTrackViewModel(
          repository = repo,
          preferences = prefs,
          capabilityChecker = checker,
          locationProvider = FakeLocationProvider(LocationFetchResult.PermissionDenied(false)),
          biometricAuthenticator = FakeBiometricAuthenticator(BiometricAuthOutcome.Success("Fingerprint")),
        )

      vm.uiState.first { it.employee != null }
      vm.initiateAttendanceAction(AttendanceCheckType.CHECK_IN, activity) {}
      vm.uiState.first { it.activeRationale == RationaleType.LOCATION }
      assertEquals(RationaleType.LOCATION, vm.uiState.value.activeRationale)

      // Simulate permanent denial
      checker.locationPermission = LocationPermissionState.PERMANENTLY_DENIED
      vm.onLocationPermissionResult(granted = false, activity = activity)

      val geofenceStep = vm.uiState.first { it.geofenceStep is GeofenceStepStatus.PermissionDenied }.geofenceStep
      assertTrue(geofenceStep is GeofenceStepStatus.PermissionDenied && geofenceStep.isPermanent)
      assertNotNull(vm.uiState.value.permissionBlockedMessage)
      assertFalse(vm.uiState.value.blockedPermissionRequiresLocationSettings)
    }

  @Test
  fun `case 3 - location services disabled blocks check-in and points to location settings`() =
    runTest {
      val prefs = FakePreferencesRepository(savedSession = sampleSession)
      val repo = createMockRepository()
      enqueueProfileAndEmptyHistory()

      val checker =
        FakeDeviceCapabilityChecker(
          locationPermission = LocationPermissionState.FINE_GRANTED,
          locationServicesEnabled = false,
        )
      val vm =
        CivicTrackViewModel(
          repository = repo,
          preferences = prefs,
          capabilityChecker = checker,
          locationProvider = FakeLocationProvider(LocationFetchResult.ServicesDisabled),
          biometricAuthenticator = FakeBiometricAuthenticator(BiometricAuthOutcome.Success("Fingerprint")),
        )

      vm.uiState.first { it.employee != null }
      vm.initiateAttendanceAction(AttendanceCheckType.CHECK_IN, activity) {}
      vm.uiState.first { it.geofenceStep == GeofenceStepStatus.ServicesDisabled }

      assertEquals(GeofenceStepStatus.ServicesDisabled, vm.uiState.value.geofenceStep)
      assertTrue(vm.uiState.value.blockedPermissionRequiresLocationSettings)
      assertNotNull(vm.uiState.value.permissionBlockedMessage)
    }

  // --- Test Cases 4, 5, 6: Camera available, unavailable, and permission denied ---

  @Test
  fun `case 4 - camera available and permission granted launches user-initiated capture`() =
    runTest {
      shadowOf(context).grantPermissions(Manifest.permission.CAMERA)
      shadowOf(context.packageManager)
        .setSystemFeature(PackageManager.FEATURE_CAMERA_ANY, true)
      val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
      val shadowCameraManager = shadowOf(cameraManager)
      shadowCameraManager.addCamera("0", ShadowCameraCharacteristics.newCameraCharacteristics())

      val manager = AndroidDeviceCapabilityManager(context)
      assertTrue(manager.hasCameraHardware())

      val prefs = FakePreferencesRepository(savedSession = sampleSession)
      val repo = createMockRepository()
      enqueueProfileAndEmptyHistory()
      val vm =
        CivicTrackViewModel(
          repository = repo,
          preferences = prefs,
          capabilityChecker = manager,
          locationProvider = FakeLocationProvider(LocationFetchResult.ServicesDisabled),
          biometricAuthenticator = FakeBiometricAuthenticator(BiometricAuthOutcome.Success("Fingerprint")),
        )

      vm.requestCameraCapture(CaptureTarget.PROFILE_PHOTO, activity)
      assertEquals(CaptureTarget.PROFILE_PHOTO, vm.uiState.value.activeCameraTarget)
    }

  @Test
  fun `case 5 - camera unavailable handles missing hardware gracefully without crashing`() =
    runTest {
      shadowOf(context.packageManager)
        .setSystemFeature(PackageManager.FEATURE_CAMERA_ANY, false)
      val manager = AndroidDeviceCapabilityManager(context)
      assertFalse(manager.hasCameraHardware())

      val prefs = FakePreferencesRepository(savedSession = sampleSession)
      val repo = createMockRepository()
      enqueueProfileAndEmptyHistory()
      val vm =
        CivicTrackViewModel(
          repository = repo,
          preferences = prefs,
          capabilityChecker = manager,
          locationProvider = FakeLocationProvider(LocationFetchResult.ServicesDisabled),
          biometricAuthenticator = FakeBiometricAuthenticator(BiometricAuthOutcome.Success("Fingerprint")),
        )

      vm.requestCameraCapture(CaptureTarget.DOCUMENT, activity)
      assertNull(vm.uiState.value.activeCameraTarget)
      assertTrue(
        vm.uiState.value.mediaErrorMessage?.contains("No active camera hardware") == true
      )
    }

  @Test
  fun `case 6 - camera permission denied shows rationale then records denial gracefully`() =
    runTest {
      val prefs = FakePreferencesRepository(savedSession = sampleSession)
      val repo = createMockRepository()
      enqueueProfileAndEmptyHistory()

      val checker =
        FakeDeviceCapabilityChecker(
          cameraAvailable = true,
          cameraPermission = PermissionAccessState.DENIED_CAN_REQUEST,
        )
      val vm =
        CivicTrackViewModel(
          repository = repo,
          preferences = prefs,
          capabilityChecker = checker,
          locationProvider = FakeLocationProvider(LocationFetchResult.ServicesDisabled),
          biometricAuthenticator = FakeBiometricAuthenticator(BiometricAuthOutcome.Success("Fingerprint")),
        )

      vm.requestCameraCapture(CaptureTarget.DOCUMENT, activity)
      assertEquals(RationaleType.CAMERA, vm.uiState.value.activeRationale)

      vm.onCameraPermissionResult(granted = false, activity = activity)
      assertNull(vm.uiState.value.activeCameraTarget)
      assertNotNull(vm.uiState.value.mediaErrorMessage)
    }

  // --- Test Cases 7, 8, 9, 10, 11: Biometric hardware, missing hardware, not enrolled, success, cancel/fail ---

  @Test
  fun `case 7 - biometric hardware available exposes Ready capability and allows toggle`() =
    runTest {
      val prefs = FakePreferencesRepository(savedSession = sampleSession)
      val repo = createMockRepository()
      enqueueProfileAndEmptyHistory()
      val checker =
        FakeDeviceCapabilityChecker(
          biometricState =
            BiometricCapabilityState.Available(
              strengthLabel = "Class 3 (Strong Biometric)",
              supportsDeviceCredentialFallback = true,
              allowedAuthenticators = 0,
            )
        )
      val vm =
        CivicTrackViewModel(
          repository = repo,
          preferences = prefs,
          capabilityChecker = checker,
          locationProvider = FakeLocationProvider(LocationFetchResult.ServicesDisabled),
          biometricAuthenticator = FakeBiometricAuthenticator(BiometricAuthOutcome.Success("Device Biometric")),
        )

      vm.refreshCapabilities(activity)
      assertTrue(vm.uiState.value.capabilities?.biometricCapability is BiometricCapabilityState.Available)

      vm.setBiometricLoginEnabled(true)
      assertTrue(vm.uiState.value.biometricLoginEnabled)
    }

  @Test
  fun `case 8 - no biometric hardware available prevents enabling biometric login and reports message`() =
    runTest {
      val prefs = FakePreferencesRepository(savedSession = sampleSession)
      val repo = createMockRepository()
      enqueueProfileAndEmptyHistory()
      val checker = FakeDeviceCapabilityChecker(biometricState = BiometricCapabilityState.NoHardware)

      val vm =
        CivicTrackViewModel(
          repository = repo,
          preferences = prefs,
          capabilityChecker = checker,
          locationProvider = FakeLocationProvider(LocationFetchResult.ServicesDisabled),
          biometricAuthenticator =
            FakeBiometricAuthenticator(BiometricAuthOutcome.HardwareUnavailable("No biometric sensor")),
        )

      vm.refreshCapabilities(activity)
      assertEquals(BiometricCapabilityState.NoHardware, vm.uiState.value.capabilities?.biometricCapability)

      vm.setBiometricLoginEnabled(true)
      assertFalse(vm.uiState.value.biometricLoginEnabled)
      assertNotNull(vm.uiState.value.settingsBiometricTestMessage)
    }

  @Test
  fun `case 9 - biometric credential not enrolled instructs user to enroll in device settings`() =
    runTest {
      val prefs = FakePreferencesRepository(savedSession = sampleSession)
      val repo = createMockRepository()
      enqueueProfileAndEmptyHistory()
      val checker = FakeDeviceCapabilityChecker(biometricState = BiometricCapabilityState.NoneEnrolled)

      val vm =
        CivicTrackViewModel(
          repository = repo,
          preferences = prefs,
          capabilityChecker = checker,
          locationProvider = FakeLocationProvider(LocationFetchResult.ServicesDisabled),
          biometricAuthenticator = FakeBiometricAuthenticator(BiometricAuthOutcome.NotEnrolled),
        )

      vm.refreshCapabilities(activity)
      assertEquals(
        BiometricCapabilityState.NoneEnrolled,
        vm.uiState.value.capabilities?.biometricCapability,
      )

      vm.testBiometricPromptInSettings(activity)
      assertTrue(
        vm.uiState.value.settingsBiometricTestMessage?.contains("Device Security Settings") == true
      )
    }

  @Test
  fun `case 10 & 12 - successful biometric auth + verified workplace location records attendance and location_verifications`() =
    runTest {
      val prefs = FakePreferencesRepository(savedSession = sampleSession)
      val repo = createMockRepository()
      mockWebServer.dispatcher =
        object : Dispatcher() {
          override fun dispatch(request: RecordedRequest): MockResponse {
            val path = request.path.orEmpty()
            return when {
              path.startsWith("/rest/v1/employees") && request.method == "GET" ->
                MockResponse()
                  .setResponseCode(200)
                  .setBody(
                    """[
                      {
                        "id": "emp-1",
                        "user_id": "user-1",
                        "full_name": "Jordan Rivera",
                        "employee_code": "CT-1042",
                        "department": "Public Works",
                        "role": "Field Inspector",
                        "workplace_id": "wp-1",
                        "workplaces": {
                          "id": "wp-1",
                          "name": "City Hall Annex",
                          "address": "100 Civic Plaza",
                          "latitude": 37.7749,
                          "longitude": -122.4194,
                          "geofence_radius_meters": 150.0,
                          "max_location_accuracy_meters": 60.0
                        },
                        "attendance_settings": {
                          "require_biometric_for_attendance": true,
                          "allow_check_in_outside_geofence": false,
                          "require_location_verification": true
                        }
                      }
                    ]"""
                  )
              path.startsWith("/rest/v1/attendance_records") && request.method == "POST" ->
                MockResponse()
                  .setResponseCode(201)
                  .setBody(
                    """[{"id":"att-900","employee_id":"emp-1","workplace_id":"wp-1","check_type":"CHECK_IN","recorded_at":"2026-11-18T12:00:00Z","biometric_verified_locally":true,"geofence_status":"VERIFIED"}]"""
                  )
              path.startsWith("/rest/v1/location_verifications") && request.method == "POST" ->
                MockResponse()
                  .setResponseCode(201)
                  .setBody(
                    """[{"id":"loc-700","attendance_record_id":"att-900","employee_id":"emp-1","workplace_id":"wp-1","verification_status":"VERIFIED"}]"""
                  )
              else -> MockResponse().setResponseCode(200).setBody("[]")
            }
          }
        }

      val checker =
        FakeDeviceCapabilityChecker(
          locationPermission = LocationPermissionState.FINE_GRANTED,
          locationServicesEnabled = true,
        )
      val insideLocation =
        CapturedLocation(
          latitude = 37.7751, // ~22m from 37.7749, -122.4194
          longitude = -122.4194,
          accuracyMeters = 12.0,
          timestampMillis = 1_763_467_200_000L,
          provider = "fused",
        )

      val vm =
        CivicTrackViewModel(
          repository = repo,
          preferences = prefs,
          capabilityChecker = checker,
          locationProvider = FakeLocationProvider(LocationFetchResult.Success(insideLocation)),
          biometricAuthenticator =
            FakeBiometricAuthenticator(BiometricAuthOutcome.Success("Device Biometric")),
        )

      vm.uiState.first { it.employee != null }
      vm.initiateAttendanceAction(AttendanceCheckType.CHECK_IN, activity) {}
      val state = vm.uiState.first { it.submissionStep is AttendanceSubmissionStatus.Recorded }

      assertTrue(state.biometricStep is BiometricStepStatus.LocalDeviceAuthenticated)
      assertTrue(state.geofenceStep is GeofenceStepStatus.Verified)
      val recorded = state.submissionStep as AttendanceSubmissionStatus.Recorded
      assertEquals("att-900", recorded.attendanceRecordId)
      assertEquals("loc-700", recorded.locationVerificationId)
      assertEquals(LocationVerificationOutcome.VERIFIED, recorded.geofenceOutcome)
    }

  @Test
  fun `case 11 - biometric cancellation stops attendance workflow before location or backend call`() =
    runTest {
      val prefs = FakePreferencesRepository(savedSession = sampleSession)
      val repo = createMockRepository()
      enqueueProfileAndEmptyHistory()
      val checker =
        FakeDeviceCapabilityChecker(
          locationPermission = LocationPermissionState.FINE_GRANTED,
          locationServicesEnabled = true,
        )
      val vm =
        CivicTrackViewModel(
          repository = repo,
          preferences = prefs,
          capabilityChecker = checker,
          locationProvider =
            FakeLocationProvider(
              LocationFetchResult.Success(
                CapturedLocation(37.7749, -122.4194, 10.0, 1_763_467_200_000L, "fused")
              )
            ),
          biometricAuthenticator =
            FakeBiometricAuthenticator(BiometricAuthOutcome.Cancelled("Authentication cancelled by user.")),
        )

      vm.uiState.first { it.employee != null }
      vm.initiateAttendanceAction(AttendanceCheckType.CHECK_IN, activity) {}
      vm.uiState.first { it.biometricStep is BiometricStepStatus.Cancelled }

      assertTrue(vm.uiState.value.biometricStep is BiometricStepStatus.Cancelled)
      assertEquals(GeofenceStepStatus.Idle, vm.uiState.value.geofenceStep)
      assertEquals(AttendanceSubmissionStatus.Idle, vm.uiState.value.submissionStep)
    }

  // --- Test Case 13: Attendance outside configured geofence ---

  @Test
  fun `case 13 - attendance outside configured geofence is flagged and never marked verified`() =
    runTest {
      val farLocation =
        CapturedLocation(
          latitude = 37.7850, // > 1.1 km away from 37.7749
          longitude = -122.4194,
          accuracyMeters = 10.0,
          timestampMillis = 1_763_467_200_000L,
          provider = "fused",
        )
      val eval = GeofenceEvaluator.evaluate(farLocation, sampleWorkplace)
      assertTrue(eval is GeofenceEvaluationResult.OutsideGeofence)
      assertEquals(LocationVerificationOutcome.OUTSIDE_GEOFENCE, eval.outcome)

      val prefs = FakePreferencesRepository(savedSession = sampleSession)
      val repo = createMockRepository()
      enqueueProfileAndEmptyHistory()
      val checker =
        FakeDeviceCapabilityChecker(
          locationPermission = LocationPermissionState.FINE_GRANTED,
          locationServicesEnabled = true,
        )
      val vm =
        CivicTrackViewModel(
          repository = repo,
          preferences = prefs,
          capabilityChecker = checker,
          locationProvider = FakeLocationProvider(LocationFetchResult.Success(farLocation)),
          biometricAuthenticator =
            FakeBiometricAuthenticator(BiometricAuthOutcome.Success("Device Biometric")),
        )

      vm.uiState.first { it.employee != null }
      vm.initiateAttendanceAction(AttendanceCheckType.CHECK_IN, activity) {}
      vm.uiState.first { it.geofenceStep is GeofenceStepStatus.OutsideGeofence }

      assertTrue(vm.uiState.value.geofenceStep is GeofenceStepStatus.OutsideGeofence)
      assertEquals(AttendanceSubmissionStatus.Idle, vm.uiState.value.submissionStep)
    }

  // --- Test Case 14: Network failure during attendance submission ---

  @Test
  fun `case 14 - network failure during attendance submission surfaces error and succeeds on retry`() =
    runTest {
      val prefs = FakePreferencesRepository(savedSession = sampleSession)
      val repo = createMockRepository()
      var postAttendanceAttempts = 0

      mockWebServer.dispatcher =
        object : Dispatcher() {
          override fun dispatch(request: RecordedRequest): MockResponse {
            val path = request.path.orEmpty()
            return when {
              path.startsWith("/rest/v1/employees") ->
                MockResponse()
                  .setResponseCode(200)
                  .setBody(
                    """[
                      {
                        "id": "emp-1",
                        "user_id": "user-1",
                        "full_name": "Jordan Rivera",
                        "employee_code": "CT-1042",
                        "department": "Public Works",
                        "role": "Field Inspector",
                        "workplace_id": "wp-1",
                        "workplaces": {
                          "id": "wp-1",
                          "name": "City Hall Annex",
                          "address": "100 Civic Plaza",
                          "latitude": 37.7749,
                          "longitude": -122.4194,
                          "geofence_radius_meters": 150.0,
                          "max_location_accuracy_meters": 60.0
                        },
                        "attendance_settings": {
                          "require_biometric_for_attendance": true,
                          "allow_check_in_outside_geofence": false,
                          "require_location_verification": true
                        }
                      }
                    ]"""
                  )
              path.startsWith("/rest/v1/attendance_records") && request.method == "POST" -> {
                postAttendanceAttempts++
                if (postAttendanceAttempts == 1) {
                  MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
                } else {
                  MockResponse()
                    .setResponseCode(201)
                    .setBody(
                      """[{"id":"att-901","employee_id":"emp-1","workplace_id":"wp-1","check_type":"CHECK_IN","recorded_at":"2026-11-18T12:05:00Z","biometric_verified_locally":true,"geofence_status":"VERIFIED"}]"""
                    )
                }
              }
              path.startsWith("/rest/v1/location_verifications") && request.method == "POST" ->
                MockResponse()
                  .setResponseCode(201)
                  .setBody(
                    """[{"id":"loc-701","attendance_record_id":"att-901","employee_id":"emp-1","workplace_id":"wp-1","verification_status":"VERIFIED"}]"""
                  )
              else -> MockResponse().setResponseCode(200).setBody("[]")
            }
          }
        }

      val checker =
        FakeDeviceCapabilityChecker(
          locationPermission = LocationPermissionState.FINE_GRANTED,
          locationServicesEnabled = true,
        )
      val insideLocation =
        CapturedLocation(37.7749, -122.4194, 8.0, 1_763_467_200_000L, "fused")

      val vm =
        CivicTrackViewModel(
          repository = repo,
          preferences = prefs,
          capabilityChecker = checker,
          locationProvider = FakeLocationProvider(LocationFetchResult.Success(insideLocation)),
          biometricAuthenticator =
            FakeBiometricAuthenticator(BiometricAuthOutcome.Success("Device Biometric")),
        )

      vm.uiState.first { it.employee != null }
      vm.initiateAttendanceAction(AttendanceCheckType.CHECK_IN, activity) {}
      vm.uiState.first { it.submissionStep is AttendanceSubmissionStatus.NetworkError }

      assertTrue(vm.uiState.value.geofenceStep is GeofenceStepStatus.Verified)
      assertTrue(vm.uiState.value.submissionStep is AttendanceSubmissionStatus.NetworkError)
      assertNotNull(vm.uiState.value.pendingRetryPayload)

      vm.retryPendingAttendanceSubmission()
      val afterRetry =
        vm.uiState.first { it.submissionStep is AttendanceSubmissionStatus.Recorded }
      val recorded = afterRetry.submissionStep as AttendanceSubmissionStatus.Recorded
      assertEquals("att-901", recorded.attendanceRecordId)
      assertEquals("loc-701", recorded.locationVerificationId)
      assertNull(afterRetry.pendingRetryPayload)
    }

  @Test
  fun `case 0 - employee registration before sign in supports signup then sign in`() = runTest {
    val prefs = FakePreferencesRepository(savedSession = null)
    val env =
      SupabaseEnvironment(
        url = mockWebServer.url("/").toString().trimEnd('/'),
        anonKey = "test-anon-key",
      )
    val api = SupabaseClientFactory.create(env)
    val repo =
      object : CivicTrackRepository by SupabaseCivicTrackRepository(api, env) {
        override fun isBackendConfigured(): Boolean = true
        override suspend fun loadWorkplaces(): BackendResult<List<WorkplaceDto>> =
          BackendResult.Success(listOf(sampleWorkplace))
      }

    val vm =
      CivicTrackViewModel(
        repository = repo,
        preferences = prefs,
        capabilityChecker = FakeDeviceCapabilityChecker(),
        locationProvider = FakeLocationProvider(LocationFetchResult.ServicesDisabled),
        biometricAuthenticator = FakeBiometricAuthenticator(BiometricAuthOutcome.NotEnrolled),
      )

    // Defaults to Registration tab before Sign In
    assertTrue(vm.uiState.value.isRegisterMode)
    assertNull(vm.uiState.value.session)

    // 1. Signup returns email confirmation required (no access_token yet)
    mockWebServer.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody("""{"user":{"id":"user-new","email":"new@civictrack.org"}}""")
    )

    vm.registerEmployee(
      fullName = "Alex Mercer",
      employeeCode = "CT-2099",
      email = "new@civictrack.org",
      password = "password123",
      confirmPassword = "password123",
      department = "Transit",
      role = "Inspector",
      workplaceId = "wp-1",
    )

    val afterRegister = vm.uiState.first { !it.isRegisterMode }
    assertFalse(afterRegister.isRegisterMode)
    assertNotNull(afterRegister.authSuccessMessage)
    assertNotNull(prefs.getPendingEmployeeProfile())

    // 2. Now user signs in -> missing employee row is auto-created from pending registration profile
    mockWebServer.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(
          """{"access_token":"jwt-new","user":{"id":"user-new","email":"new@civictrack.org"}}"""
        )
    )
    // Initial GET employees returns empty -> triggers POST employees + reload
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
    mockWebServer.enqueue(
      MockResponse()
        .setResponseCode(201)
        .setBody(
          """[{"id":"emp-new","user_id":"user-new","full_name":"Alex Mercer","employee_code":"CT-2099","workplace_id":"wp-1"}]"""
        )
    )
    mockWebServer.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(
          """[{"id":"emp-new","user_id":"user-new","full_name":"Alex Mercer","employee_code":"CT-2099","workplace_id":"wp-1","workplaces":{"id":"wp-1","name":"City Hall Annex","latitude":37.7749,"longitude":-122.4194,"geofence_radius_meters":150.0,"max_location_accuracy_meters":60.0}}]"""
        )
    )
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

    vm.signInWithPassword("new@civictrack.org", "password123")
    val signedIn = vm.uiState.first { it.employee != null }
    assertEquals("Alex Mercer", signedIn.employee?.fullName)
    assertEquals("emp-new", signedIn.employee?.id)
    assertNull(prefs.getPendingEmployeeProfile())
  }

  private fun createMockRepository(): CivicTrackRepository {
    val env =
      SupabaseEnvironment(
        url = mockWebServer.url("/").toString().trimEnd('/'),
        anonKey = "test-anon-key",
      )
    val api = SupabaseClientFactory.create(env)
    return object : CivicTrackRepository by SupabaseCivicTrackRepository(api, env) {
      override fun isBackendConfigured(): Boolean = true
      override suspend fun loadWorkplaces(): BackendResult<List<WorkplaceDto>> =
        BackendResult.Success(listOf(sampleWorkplace))
    }
  }

  private fun enqueueProfileAndEmptyHistory() {
    mockWebServer.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(
          """[
            {
              "id": "emp-1",
              "user_id": "user-1",
              "full_name": "Jordan Rivera",
              "employee_code": "CT-1042",
              "department": "Public Works",
              "role": "Field Inspector",
              "workplace_id": "wp-1",
              "workplaces": {
                "id": "wp-1",
                "name": "City Hall Annex",
                "address": "100 Civic Plaza",
                "latitude": 37.7749,
                "longitude": -122.4194,
                "geofence_radius_meters": 150.0,
                "max_location_accuracy_meters": 60.0
              },
              "attendance_settings": {
                "require_biometric_for_attendance": true,
                "allow_check_in_outside_geofence": false,
                "require_location_verification": true
              }
            }
          ]"""
        )
    )
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
  }

  // --- Test Doubles ---

  private class FakePreferencesRepository(savedSession: AuthSession? = null) : PreferencesRepository {
    private val bioFlow = MutableStateFlow(false)
    private val sessionFlow = MutableStateFlow(savedSession)
    private val requestedPermissions = mutableSetOf<String>()
    private var pendingProfile: PendingEmployeeProfile? = null

    override val biometricLoginEnabled: Flow<Boolean> = bioFlow
    override val authSession: Flow<AuthSession?> = sessionFlow

    override suspend fun setBiometricLoginEnabled(enabled: Boolean) {
      bioFlow.value = enabled
    }

    override suspend fun wasPermissionRequested(permission: String): Boolean =
      permission in requestedPermissions

    override suspend fun markPermissionRequested(permission: String) {
      requestedPermissions.add(permission)
    }

    override suspend fun saveAuthSession(session: AuthSession) {
      sessionFlow.value = session
    }

    override suspend fun clearAuthSession() {
      sessionFlow.value = null
    }

    override suspend fun savePendingEmployeeProfile(profile: PendingEmployeeProfile) {
      pendingProfile = profile
    }

    override suspend fun getPendingEmployeeProfile(): PendingEmployeeProfile? = pendingProfile

    override suspend fun clearPendingEmployeeProfile() {
      pendingProfile = null
    }
  }

  private class FakeDeviceCapabilityChecker(
    var locationPermission: LocationPermissionState = LocationPermissionState.FINE_GRANTED,
    var locationServicesEnabled: Boolean = true,
    var cameraPermission: PermissionAccessState = PermissionAccessState.GRANTED,
    var cameraAvailable: Boolean = true,
    var biometricState: BiometricCapabilityState =
      BiometricCapabilityState.Available("Class 3", true, 0),
    var networkAvailable: Boolean = true,
  ) : DeviceCapabilityChecker {
    override fun inspectCapabilities(
      activity: android.app.Activity?,
      locationPreviouslyRequested: Boolean,
      cameraPreviouslyRequested: Boolean,
    ): DeviceCapabilitySnapshot =
      DeviceCapabilitySnapshot(
        locationPermission = locationPermission,
        locationServicesEnabled = locationServicesEnabled,
        cameraPermission = cameraPermission,
        cameraAvailable = cameraAvailable,
        biometricCapability = biometricState,
        networkAvailable = networkAvailable,
        compatibility =
          DeviceCompatibilityInfo(
            deviceModel = "Test Device",
            androidVersionLabel = "Android 16",
            hasGpsHardware = true,
            hasNetworkLocation = true,
            hasCameraHardware = cameraAvailable,
            cameraCount = if (cameraAvailable) 2 else 0,
            hasFingerprintSensor = biometricState is BiometricCapabilityState.Available,
            hasFaceHardware = false,
            hasIrisHardware = false,
          ),
      )

    override fun isLocationServicesEnabled(): Boolean = locationServicesEnabled
    override fun hasCameraHardware(): Boolean = cameraAvailable
    override fun checkBiometricAvailability(allowDeviceCredentialFallback: Boolean) = biometricState
    override fun isNetworkConnected(): Boolean = networkAvailable
    override fun buildAppSettingsIntent() = android.content.Intent()
    override fun buildLocationSettingsIntent() = android.content.Intent()
    override fun buildBiometricEnrollIntent() = android.content.Intent()
    override fun userMessageForBiometricState(state: BiometricCapabilityState) = state.toString()
    override fun userMessageForLocationIssue(
      permission: LocationPermissionState,
      servicesEnabled: Boolean,
    ) = "Location issue: $permission / enabled=$servicesEnabled"
    override fun userMessageForCameraIssue(
      available: Boolean,
      permission: PermissionAccessState,
    ) = "Camera issue: available=$available / $permission"
  }

  private class FakeLocationProvider(private val result: LocationFetchResult) :
    AttendanceLocationProvider {
    override suspend fun fetchCurrentLocation(permissionState: LocationPermissionState) = result
  }

  private class FakeBiometricAuthenticator(private val outcome: BiometricAuthOutcome) :
    BiometricAuthenticator {
    override suspend fun authenticate(
      activity: FragmentActivity,
      title: String,
      subtitle: String,
      description: String,
      allowDeviceCredentialFallback: Boolean,
    ) = outcome
  }
}
