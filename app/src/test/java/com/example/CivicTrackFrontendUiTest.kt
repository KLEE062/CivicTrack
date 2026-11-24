package com.example

import android.app.Activity
import android.content.Intent
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.fragment.app.FragmentActivity
import com.example.core.biometric.BiometricAuthOutcome
import com.example.core.biometric.BiometricAuthenticator
import com.example.core.capabilities.BiometricCapabilityState
import com.example.core.capabilities.DeviceCapabilityChecker
import com.example.core.capabilities.DeviceCapabilitySnapshot
import com.example.core.capabilities.DeviceCompatibilityInfo
import com.example.core.capabilities.LocationPermissionState
import com.example.core.capabilities.PermissionAccessState
import com.example.core.location.AttendanceLocationProvider
import com.example.core.location.CapturedLocation
import com.example.core.location.LocationFetchResult
import com.example.data.local.PreferencesRepository
import com.example.data.model.AuthSession
import com.example.data.model.PendingEmployeeProfile
import com.example.data.remote.SupabaseClientFactory
import com.example.data.remote.SupabaseEnvironment
import com.example.data.repository.SupabaseCivicTrackRepository
import com.example.data.repository.SupabaseOnboardingRepository
import com.example.ui.frontend.CivicTrackFrontendRoot
import com.example.ui.frontend.navigation.FrontendRoutes
import com.example.ui.frontend.state.FrontendViewModel
import com.example.ui.theme.CivicTrackTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CivicTrackFrontendUiTest {

  @get:Rule
  val composeRule = createComposeRule()

  private lateinit var activity: FragmentActivity
  private lateinit var mockWebServer: MockWebServer
  private val recordedPaths = CopyOnWriteArrayList<String>()

  @Before
  fun setUp() {
    activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
    mockWebServer = MockWebServer()
    mockWebServer.start()
    recordedPaths.clear()
  }

  @After
  fun tearDown() {
    shadowOf(Looper.getMainLooper()).idle()
    mockWebServer.shutdown()
  }

  private fun buildViewModel(
    environment: SupabaseEnvironment =
      SupabaseEnvironment(
        url = mockWebServer.url("/").toString().trimEnd('/'),
        anonKey = "test-anon-key",
      ),
    capabilityChecker: FakeDeviceCapabilityChecker = FakeDeviceCapabilityChecker(),
    locationProvider: AttendanceLocationProvider =
      FakeLocationProvider(
        LocationFetchResult.Success(
          CapturedLocation(
            latitude = 0.3136,
            longitude = 32.5811,
            accuracyMeters = 12.0,
            timestampMillis = System.currentTimeMillis(),
            provider = "fused",
          )
        )
      ),
    biometricAuthenticator: BiometricAuthenticator =
      FakeBiometricAuthenticator(BiometricAuthOutcome.Success("Fingerprint")),
  ): FrontendViewModel {
    val api = SupabaseClientFactory.create(environment)
    return FrontendViewModel.createForTesting(
      onboardingRepository = SupabaseOnboardingRepository(api, environment),
      civicRepository = SupabaseCivicTrackRepository(api, environment),
      preferences = FakePreferencesRepository(),
      capabilityChecker = capabilityChecker,
      locationProvider = locationProvider,
      biometricAuthenticator = biometricAuthenticator,
    )
  }

  private fun pumpAndWaitForTag(tag: String) {
    composeRule.waitUntil(timeoutMillis = 10_000) {
      shadowOf(Looper.getMainLooper()).idle()
      composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(tag).assertIsDisplayed()
  }

  private fun assertScreenVisible(screenTag: String) {
    pumpAndWaitForTag(screenTag)
  }

  private fun scrollAndClick(nodeTag: String) {
    shadowOf(Looper.getMainLooper()).idle()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("civic_screen_list").performScrollToNode(hasTestTag(nodeTag))
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(nodeTag).performClick()
    shadowOf(Looper.getMainLooper()).idle()
    composeRule.waitForIdle()
  }

  private fun scrollAndType(nodeTag: String, text: String) {
    shadowOf(Looper.getMainLooper()).idle()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("civic_screen_list").performScrollToNode(hasTestTag(nodeTag))
    composeRule.onNodeWithTag(nodeTag).performTextInput(text)
    shadowOf(Looper.getMainLooper()).idle()
    composeRule.waitForIdle()
  }

  private fun scrollToTextAndAssertVisible(text: String) {
    composeRule.waitUntil(timeoutMillis = 10_000) {
      shadowOf(Looper.getMainLooper()).idle()
      val exists =
        composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
      if (!exists) {
        runCatching {
          composeRule.onNodeWithTag("civic_screen_list").performScrollToNode(hasText(text, substring = true))
        }
      }
      composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    }
    runCatching {
      composeRule.onNodeWithTag("civic_screen_list").performScrollToNode(hasText(text, substring = true))
    }
    val nodes = composeRule.onAllNodesWithText(text, substring = true)
    assertTrue("Expected at least one node with text '$text'", nodes.fetchSemanticsNodes().isNotEmpty())
  }

  @Test
  fun unconfiguredSupabase_welcomeScreenShowsSecretsGuidance() {
    val vm = buildViewModel(environment = SupabaseEnvironment(url = "", anonKey = ""))
    composeRule.setContent {
      CivicTrackTheme {
        CivicTrackFrontendRoot(
          viewModel = vm,
          initialRoute = FrontendRoutes.WELCOME,
          activityOverride = activity,
        )
      }
    }

    assertScreenVisible("welcome_screen")
    scrollToTextAndAssertVisible("Supabase Secrets Not Configured")
    scrollToTextAndAssertVisible("AI Studio Secrets")
  }

  @Test
  fun connectedSupabase_ownerOnboarding_inviteEmployee_andApproveLeave() {
    var leaveStatus = "pending"
    mockWebServer.dispatcher =
      object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
          val path = request.path.orEmpty()
          val method = request.method.orEmpty()
          recordedPaths.add("$method $path")
          return when {
            path == "/auth/v1/health" -> jsonOk("{}")
            path == "/auth/v1/settings" -> jsonOk("{}")
            path == "/rest/v1/" -> jsonOk("{}")
            path.startsWith("/auth/v1/signup") ->
              jsonOk("""{"user":{"id":"u-owner","email":"sarah@kampala.org"}}""")
            path.startsWith("/auth/v1/token") ->
              jsonOk(
                """{"access_token":"tok-owner","token_type":"bearer","expires_in":3600,"user":{"id":"u-owner","email":"sarah@kampala.org"}}"""
              )
            path.startsWith("/auth/v1/user") ->
              jsonOk("""{"id":"u-owner","email":"sarah@kampala.org"}""")
            path.startsWith("/rest/v1/rpc/create_organization") ->
              jsonOk(
                """{"id":"org-1","name":"Kampala Works","country_code":"UG","currency_code":"UGX","owner_id":"u-owner"}"""
              )
            path.startsWith("/rest/v1/organization_members") ->
              jsonOk(
                """[{
                  "id":"mem-owner",
                  "organization_id":"org-1",
                  "user_id":"u-owner",
                  "role":"owner",
                  "status":"active",
                  "organizations":{"id":"org-1","name":"Kampala Works","country_code":"UG","currency_code":"UGX","owner_id":"u-owner"},
                  "profiles":{"id":"u-owner","full_name":"Sarah Namukasa","email":"sarah@kampala.org"}
                }]"""
              )
            path.startsWith("/rest/v1/organizations") ->
              jsonOk(
                """[{"id":"org-1","name":"Kampala Works","country_code":"UG","currency_code":"UGX","owner_id":"u-owner"}]"""
              )
            path.startsWith("/rest/v1/organization_settings") ->
              jsonOk(
                """[{"id":"set-1","organization_id":"org-1","require_location_verification":true,"require_biometric_for_attendance":true,"allow_check_in_outside_geofence":false,"work_start_time":"08:00","work_end_time":"17:00","late_grace_minutes":15,"working_days":["MON","TUE","WED","THU","FRI"]}]"""
              )
            path.startsWith("/rest/v1/departments") ->
              jsonOk("""[{"id":"dept-1","organization_id":"org-1","name":"Operations","description":"Field operations"}]""")
            path.startsWith("/rest/v1/workplaces") ->
              jsonOk(
                """[{"id":"wp-1","organization_id":"org-1","name":"Main Site","address":"Kampala Road","latitude":0.3136,"longitude":32.5811,"geofence_radius_meters":150.0,"max_location_accuracy_meters":60.0}]"""
              )
            path.startsWith("/rest/v1/employee_profiles") ->
              jsonOk(
                """[{"id":"emp-1","organization_id":"org-1","user_id":"u-emp","full_name":"Grace Atim","employee_code":"CT-102","department_id":"dept-1","workplace_id":"wp-1","role":"Field Officer","status":"active"}]"""
              )
            path.startsWith("/rest/v1/invitations") && method == "POST" ->
              jsonOk(
                """[{"id":"inv-1","organization_id":"org-1","email":"amina@kampala.org","role":"employee","department_id":"dept-1","workplace_id":"wp-1","status":"pending"}]"""
              )
            path.startsWith("/rest/v1/invitations") -> jsonOk("[]")
            path.startsWith("/rest/v1/attendance_records") -> jsonOk("[]")
            path.startsWith("/rest/v1/leave_requests") && method == "PATCH" -> {
              leaveStatus = "approved"
              jsonOk(
                """[{"id":"lv-1","organization_id":"org-1","employee_id":"emp-1","department_id":"dept-1","workplace_id":"wp-1","leave_type":"Annual Leave","status":"approved","start_date":"2026-04-01","end_date":"2026-04-02","days_count":2,"reason":"Annual leave"}]"""
              )
            }
            path.startsWith("/rest/v1/leave_requests") ->
              jsonOk(
                """[{"id":"lv-1","organization_id":"org-1","employee_id":"emp-1","department_id":"dept-1","workplace_id":"wp-1","leave_type":"Annual Leave","status":"$leaveStatus","start_date":"2026-04-01","end_date":"2026-04-02","days_count":2,"reason":"Annual leave"}]"""
              )
            path.startsWith("/rest/v1/audit_logs") -> jsonOk("[]")
            else -> jsonOk("[]")
          }
        }
      }

    val vm = buildViewModel()
    composeRule.setContent {
      CivicTrackTheme {
        CivicTrackFrontendRoot(
          viewModel = vm,
          initialRoute = FrontendRoutes.WELCOME,
          activityOverride = activity,
        )
      }
    }

    vm.checkSupabaseConnection()
    assertScreenVisible("welcome_screen")
    shadowOf(Looper.getMainLooper()).idle()
    scrollToTextAndAssertVisible("Connected to Supabase")
    scrollAndClick("welcome_get_started_button")

    assertScreenVisible("role_selection_screen")
    scrollAndClick("role_owner_register_button")

    assertScreenVisible("owner_registration_screen")
    scrollAndClick("owner_reg_submit_button")
    pumpAndWaitForTag("frontend_error_banner")

    scrollAndType("owner_reg_name_input", "Sarah Namukasa")
    scrollAndType("owner_reg_email_input", "sarah@kampala.org")
    scrollAndType("owner_reg_org_input", "Kampala Works")
    scrollAndType("owner_reg_password_input", "StrongPass1")
    scrollAndType("owner_reg_confirm_input", "StrongPass1")
    scrollAndClick("owner_reg_submit_button")

    assertScreenVisible("email_verification_info_screen")
    scrollAndClick("verify_email_continue_button")
    assertScreenVisible("owner_dashboard_screen")

    composeRule.onNodeWithTag("nav_owner_staff").performClick()
    assertScreenVisible("owner_employee_directory_screen")
    scrollAndClick("directory_add_employee_button")
    assertScreenVisible("owner_add_employee_screen")

    scrollAndType("add_emp_email_input", "amina@kampala.org")
    scrollAndClick("add_emp_submit_button")
    scrollToTextAndAssertVisible("One-Time Employee Invite Link")

    composeRule.onNodeWithTag("nav_owner_leave").performClick()
    assertScreenVisible("owner_leave_management_screen")
    scrollAndClick("owner_approve_leave_lv-1")
    composeRule.onNodeWithTag("dialog_confirm_button").performClick()
    pumpAndWaitForTag("frontend_info_banner")
    assertTrue(recordedPaths.any { it.startsWith("PATCH /rest/v1/leave_requests") })
  }

  @Test
  fun loginAsSupervisor_loadsScopedTeam_andResolvesException() {
    var recordGeofenceStatus = "OUTSIDE_GEOFENCE"
    var recordReviewStatus = "pending"
    mockWebServer.dispatcher =
      object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
          val path = request.path.orEmpty()
          val method = request.method.orEmpty()
          recordedPaths.add("$method $path")
          return when {
            path == "/auth/v1/settings" -> jsonOk("{}")
            path == "/rest/v1/" -> jsonOk("{}")
            path.startsWith("/auth/v1/token") ->
              jsonOk(
                """{"access_token":"tok-sup","token_type":"bearer","expires_in":3600,"user":{"id":"u-sup","email":"sup@kampala.org"}}"""
              )
            path.startsWith("/auth/v1/user") ->
              jsonOk("""{"id":"u-sup","email":"sup@kampala.org"}""")
            path.startsWith("/rest/v1/profiles") ->
              jsonOk("""[{"id":"u-sup","full_name":"David Okello","email":"sup@kampala.org"}]""")
            path.startsWith("/rest/v1/organization_members") ->
              jsonOk(
                """[{
                  "id":"mem-sup",
                  "organization_id":"org-1",
                  "user_id":"u-sup",
                  "role":"supervisor",
                  "status":"active",
                  "department_id":"dept-1",
                  "workplace_id":"wp-1",
                  "organizations":{"id":"org-1","name":"Kampala Works","country_code":"UG","currency_code":"UGX","owner_id":"u-owner"},
                  "departments":{"id":"dept-1","organization_id":"org-1","name":"Operations"},
                  "workplaces":{"id":"wp-1","organization_id":"org-1","name":"Main Site","latitude":0.3136,"longitude":32.5811,"geofence_radius_meters":150.0,"max_location_accuracy_meters":60.0},
                  "profiles":{"id":"u-sup","full_name":"David Okello","email":"sup@kampala.org"}
                }]"""
              )
            path.startsWith("/rest/v1/organizations") ->
              jsonOk("""[{"id":"org-1","name":"Kampala Works","country_code":"UG","currency_code":"UGX","owner_id":"u-owner"}]""")
            path.startsWith("/rest/v1/organization_settings") ->
              jsonOk("""[{"id":"set-1","organization_id":"org-1","require_location_verification":true,"require_biometric_for_attendance":true,"allow_check_in_outside_geofence":false}]""")
            path.startsWith("/rest/v1/departments") ->
              jsonOk("""[{"id":"dept-1","organization_id":"org-1","name":"Operations"}]""")
            path.startsWith("/rest/v1/workplaces") ->
              jsonOk("""[{"id":"wp-1","organization_id":"org-1","name":"Main Site","latitude":0.3136,"longitude":32.5811,"geofence_radius_meters":150.0,"max_location_accuracy_meters":60.0}]""")
            path.startsWith("/rest/v1/employee_profiles") ->
              jsonOk(
                """[{"id":"emp-1","organization_id":"org-1","user_id":"u-emp","full_name":"Grace Atim","employee_code":"CT-102","department_id":"dept-1","workplace_id":"wp-1","role":"Field Officer","status":"active"}]"""
              )
            path.startsWith("/rest/v1/attendance_records") && method == "PATCH" -> {
              recordGeofenceStatus = "VERIFIED"
              recordReviewStatus = "approved"
              jsonOk(
                """[{"id":"att-3","organization_id":"org-1","employee_id":"emp-1","workplace_id":"wp-1","department_id":"dept-1","check_type":"CHECK_IN","recorded_at":"2026-03-01T08:40:00Z","biometric_verified_locally":true,"geofence_status":"VERIFIED","review_status":"approved","notes":"Approved by supervisor"}]"""
              )
            }
            path.startsWith("/rest/v1/attendance_records") ->
              jsonOk(
                """[{"id":"att-3","organization_id":"org-1","employee_id":"emp-1","workplace_id":"wp-1","department_id":"dept-1","check_type":"CHECK_IN","recorded_at":"2026-03-01T08:40:00Z","biometric_verified_locally":true,"geofence_status":"$recordGeofenceStatus","review_status":"$recordReviewStatus","notes":"GPS drift near gate"}]"""
              )
            path.startsWith("/rest/v1/leave_requests") -> jsonOk("[]")
            path.startsWith("/rest/v1/audit_logs") -> jsonOk("[]")
            else -> jsonOk("[]")
          }
        }
      }

    val vm = buildViewModel()
    composeRule.setContent {
      CivicTrackTheme {
        CivicTrackFrontendRoot(
          viewModel = vm,
          initialRoute = FrontendRoutes.LOGIN,
          activityOverride = activity,
        )
      }
    }

    assertScreenVisible("login_screen")
    scrollAndType("login_email_input", "sup@kampala.org")
    scrollAndType("login_password_input", "Supervisor1!")
    scrollAndClick("login_submit_button")

    assertScreenVisible("supervisor_dashboard_screen")
    scrollAndClick("sup_nav_workforce")
    assertScreenVisible("supervisor_workforce_screen")
    scrollToTextAndAssertVisible("Grace Atim")

    composeRule.onNodeWithTag("nav_sup_dashboard").performClick()
    assertScreenVisible("supervisor_dashboard_screen")
    scrollAndClick("sup_nav_exceptions")
    assertScreenVisible("supervisor_exceptions_screen")
    scrollAndClick("sup_resolve_exception_att-3")
    pumpAndWaitForTag("frontend_info_banner")
    assertTrue(recordedPaths.any { it.startsWith("PATCH /rest/v1/attendance_records") })
  }

  @Test
  fun loginAsEmployee_runsBiometricAndFusedLocationCheckIn_andSubmitsLeave() {
    val submittedLeaves = mutableListOf<String>()
    mockWebServer.dispatcher =
      object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
          val path = request.path.orEmpty()
          val method = request.method.orEmpty()
          recordedPaths.add("$method $path")
          return when {
            path == "/auth/v1/settings" -> jsonOk("{}")
            path == "/rest/v1/" -> jsonOk("{}")
            path.startsWith("/auth/v1/token") ->
              jsonOk(
                """{"access_token":"tok-emp","token_type":"bearer","expires_in":3600,"user":{"id":"u-emp","email":"grace@kampala.org"}}"""
              )
            path.startsWith("/auth/v1/user") ->
              jsonOk("""{"id":"u-emp","email":"grace@kampala.org"}""")
            path.startsWith("/rest/v1/organization_members") -> jsonOk("[]")
            path.startsWith("/rest/v1/organizations") -> jsonOk("[]")
            path.startsWith("/rest/v1/employees") ->
              jsonOk(
                """[{
                  "id":"emp-1",
                  "user_id":"u-emp",
                  "full_name":"Grace Atim",
                  "employee_code":"CT-102",
                  "department":"Operations",
                  "role":"Field Officer",
                  "workplace_id":"wp-1",
                  "workplaces":{"id":"wp-1","name":"Main Site","address":"Kampala Road","latitude":0.3136,"longitude":32.5811,"geofence_radius_meters":150.0,"max_location_accuracy_meters":60.0},
                  "attendance_settings":{"require_biometric_for_attendance":true,"allow_check_in_outside_geofence":false,"require_location_verification":true,"work_start_time":"08:00","work_end_time":"17:00","late_grace_minutes":15}
                }]"""
              )
            path.startsWith("/rest/v1/workplaces") ->
              jsonOk(
                """[{"id":"wp-1","name":"Main Site","address":"Kampala Road","latitude":0.3136,"longitude":32.5811,"geofence_radius_meters":150.0,"max_location_accuracy_meters":60.0}]"""
              )
            path.startsWith("/rest/v1/attendance_records") && method == "POST" ->
              jsonOk(
                """[{"id":"att-new","employee_id":"emp-1","workplace_id":"wp-1","check_type":"CHECK_IN","recorded_at":"2026-03-01T08:00:00Z","biometric_verified_locally":true,"geofence_status":"VERIFIED"}]"""
              )
            path.startsWith("/rest/v1/location_verifications") && method == "POST" ->
              jsonOk(
                """[{"id":"loc-new","attendance_record_id":"att-new","employee_id":"emp-1","workplace_id":"wp-1","latitude":0.3136,"longitude":32.5811,"accuracy_meters":12.0,"distance_meters":0.0,"permitted_radius_meters":150.0,"verification_outcome":"VERIFIED","verified_at":"2026-03-01T08:00:00Z"}]"""
              )
            path.startsWith("/rest/v1/leave_requests") && method == "POST" -> {
              val createdJson =
                """{"id":"lv-new","organization_id":"org-1","employee_id":"emp-1","leave_type":"Annual Leave","status":"pending","start_date":"2026-04-10","end_date":"2026-04-10","days_count":1,"reason":"Field training workshop in Entebbe"}"""
              submittedLeaves.add(createdJson)
              jsonOk("[$createdJson]")
            }
            path.startsWith("/rest/v1/leave_requests") ->
              jsonOk("[${submittedLeaves.joinToString(",")}]")
            path.startsWith("/rest/v1/attendance_records") -> jsonOk("[]")
            path.startsWith("/rest/v1/employee_documents") -> jsonOk("[]")
            path.startsWith("/rest/v1/audit_logs") -> jsonOk("[]")
            else -> jsonOk("[]")
          }
        }
      }

    val vm = buildViewModel()
    composeRule.setContent {
      CivicTrackTheme {
        CivicTrackFrontendRoot(
          viewModel = vm,
          initialRoute = FrontendRoutes.LOGIN,
          activityOverride = activity,
        )
      }
    }

    assertScreenVisible("login_screen")
    scrollAndType("login_email_input", "grace@kampala.org")
    scrollAndType("login_password_input", "Employee1!")
    scrollAndClick("login_submit_button")

    assertScreenVisible("employee_dashboard_screen")
    scrollAndClick("emp_dash_checkin_button")
    assertScreenVisible("employee_check_in_screen")

    scrollAndClick("checkin_verify_button")
    composeRule.waitUntil(timeoutMillis = 10_000) {
      shadowOf(Looper.getMainLooper()).idle()
      recordedPaths.any { it.startsWith("POST /rest/v1/location_verifications") }
    }
    assertTrue(recordedPaths.any { it.startsWith("POST /rest/v1/attendance_records") })

    scrollAndClick("checkin_open_location_stage")
    assertScreenVisible("employee_location_presentation_screen")
    scrollToTextAndAssertVisible("Verified inside radius")

    composeRule.onNodeWithTag("topbar_back_button").performClick()
    assertScreenVisible("employee_check_in_screen")
    scrollAndClick("checkin_open_biometric_stage")
    assertScreenVisible("employee_biometric_presentation_screen")
    scrollToTextAndAssertVisible("Android Keystore Hardware Isolation")
    scrollToTextAndAssertVisible("Class 3")

    composeRule.onNodeWithTag("nav_emp_leave").performClick()
    assertScreenVisible("employee_leave_history_screen")
    scrollAndClick("emp_leave_history_apply_button")
    assertScreenVisible("employee_leave_application_screen")
    scrollAndType("emp_leave_reason_input", "Field training workshop in Entebbe")
    scrollAndClick("emp_leave_submit_button")

    assertScreenVisible("employee_leave_history_screen")
    scrollToTextAndAssertVisible("Field training workshop in Entebbe")
  }

  private fun jsonOk(body: String): MockResponse =
    MockResponse().setResponseCode(200).addHeader("Content-Type", "application/json").setBody(body)

  private class FakePreferencesRepository(savedSession: AuthSession? = null) : PreferencesRepository {
    private val bioFlow = MutableStateFlow(false)
    private val sessionFlow = MutableStateFlow(savedSession)
    private val themeFlow = MutableStateFlow("SYSTEM")
    private val requestedPermissions = mutableSetOf<String>()
    private var pendingProfile: PendingEmployeeProfile? = null

    override val biometricLoginEnabled: Flow<Boolean> = bioFlow
    override val authSession: Flow<AuthSession?> = sessionFlow
    override val themeModePreference: Flow<String> = themeFlow

    override suspend fun setBiometricLoginEnabled(enabled: Boolean) {
      bioFlow.value = enabled
    }

    override suspend fun setThemeModePreference(mode: String) {
      themeFlow.value = mode
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
      activity: Activity?,
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
    override fun buildAppSettingsIntent() = Intent()
    override fun buildLocationSettingsIntent() = Intent()
    override fun buildBiometricEnrollIntent() = Intent()
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
