package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import com.example.core.security.InvitationTokenUtils
import com.example.data.local.PreferencesRepository
import com.example.data.model.AuthSession
import com.example.data.model.EntryAuthDestination
import com.example.data.model.OrgMemberRole
import com.example.data.model.OwnerOnboardingStep
import com.example.data.model.PendingEmployeeProfile
import com.example.data.model.PendingOwnerOnboarding
import com.example.data.model.PendingSupervisorInvitation
import com.example.data.model.SupervisorOnboardingStep
import com.example.data.remote.SupabaseClientFactory
import com.example.data.remote.SupabaseEnvironment
import com.example.data.repository.OnboardingRepository
import com.example.data.repository.SupabaseOnboardingRepository
import com.example.ui.ActiveWorkspaceMode
import com.example.ui.OnboardingUiState
import com.example.ui.OnboardingViewModel
import com.example.ui.screens.OnboardingEntryScreen
import com.example.ui.theme.CivicTrackTheme
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CivicTrackOnboardingTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private val testDispatcher = UnconfinedTestDispatcher()
  private lateinit var mockWebServer: MockWebServer

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    mockWebServer = MockWebServer()
    mockWebServer.start()
  }

  @After
  fun tearDown() {
    mockWebServer.shutdown()
    Dispatchers.resetMain()
  }

  @Test
  fun `entry screen shows Welcome to CivicTrack with Owner and Supervisor options only`() {
    composeTestRule.setContent {
      CivicTrackTheme {
        OnboardingEntryScreen(
          uiState = OnboardingUiState(isBackendConfigured = true),
          hasBiometricSavedSession = false,
          onStartOwnerOnboarding = {},
          onStartSupervisorOnboarding = {},
          onOpenSignIn = {},
          onOpenForgotPassword = {},
          onBackToLanding = {},
          onSignIn = { _, _ -> },
          onBiometricUnlock = {},
          onSendPasswordReset = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Welcome to CivicTrack").assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("entry_lazy_column")
      .performScrollToNode(hasTestTag("register_owner_entry_button"))
    composeTestRule.onNodeWithTag("register_owner_entry_button").assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("entry_lazy_column")
      .performScrollToNode(hasTestTag("join_supervisor_entry_button"))
    composeTestRule.onNodeWithTag("join_supervisor_entry_button").assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("entry_lazy_column")
      .performScrollToNode(hasTestTag("already_have_account_sign_in_button"))
    composeTestRule.onNodeWithTag("already_have_account_sign_in_button").assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("entry_lazy_column")
      .performScrollToNode(hasTestTag("forgot_password_entry_button"))
    composeTestRule.onNodeWithTag("forgot_password_entry_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("tab_register_button").assertDoesNotExist()
  }

  @Test
  fun `owner registration validates email, password strength, and matching confirmation`() = runTest {
    val vm = createViewModel()

    vm.registerOwnerAccount("Ada", "bad-email", "", "StrongPass1", "StrongPass1")
    assertTrue(vm.uiState.value.errorMessage?.contains("valid") == true)

    vm.registerOwnerAccount("Ada", "ada@kampala.org", "", "short", "short")
    assertTrue(vm.uiState.value.errorMessage?.contains("8 characters") == true)

    vm.registerOwnerAccount("Ada", "ada@kampala.org", "", "StrongPass1", "Mismatch2")
    assertTrue(vm.uiState.value.errorMessage?.contains("do not match") == true)
  }

  @Test
  fun `owner onboarding handles email confirmation pending, resumes to create_organization RPC, and loads Owner Dashboard`() =
    runTest {
      val prefs = FakeOnboardingPrefs()
      var rpcPayloadCaptured = ""
      var createOrgCallCount = 0

      mockWebServer.dispatcher =
        object : Dispatcher() {
          override fun dispatch(request: RecordedRequest): MockResponse {
            val path = request.path.orEmpty()
            return when {
              path.startsWith("/auth/v1/signup") && request.method == "POST" ->
                MockResponse()
                  .setResponseCode(200)
                  .setBody("""{"user":{"id":"owner-u1","email":"owner@kcca.ug"}}""")
              path.startsWith("/auth/v1/token") && request.method == "POST" ->
                MockResponse()
                  .setResponseCode(200)
                  .setBody(
                    """{"access_token":"owner-jwt","user":{"id":"owner-u1","email":"owner@kcca.ug"}}"""
                  )
              path.startsWith("/auth/v1/user") ->
                MockResponse()
                  .setResponseCode(200)
                  .setBody("""{"id":"owner-u1","email":"owner@kcca.ug","email_confirmed_at":"2026-11-18T10:00:00Z"}""")
              path.startsWith("/rest/v1/profiles") && request.method == "POST" ->
                MockResponse()
                  .setResponseCode(201)
                  .setBody("""[{"id":"owner-u1","full_name":"Sarah Namukasa","email":"owner@kcca.ug","phone":"+256700111222"}]""")
              path.startsWith("/rest/v1/profiles") && request.method == "GET" ->
                MockResponse().setResponseCode(200).setBody("[]")
              path.startsWith("/rest/v1/rpc/create_organization") -> {
                createOrgCallCount++
                rpcPayloadCaptured = request.body.readUtf8()
                MockResponse().setResponseCode(200).setBody("\"org-ug-1\"")
              }
              path.startsWith("/rest/v1/organization_members") ->
                if (createOrgCallCount == 0) {
                  MockResponse().setResponseCode(200).setBody("[]")
                } else {
                  MockResponse()
                    .setResponseCode(200)
                    .setBody(
                      """[{"id":"mem-1","organization_id":"org-ug-1","user_id":"owner-u1","role":"owner","status":"active","organizations":{"id":"org-ug-1","name":"Kampala Civic Works","country_code":"UG","currency_code":"UGX","owner_id":"owner-u1"}}]"""
                    )
                }
              path.startsWith("/rest/v1/organizations") && request.method == "GET" ->
                if (createOrgCallCount == 0) {
                  MockResponse().setResponseCode(200).setBody("[]")
                } else {
                  MockResponse()
                    .setResponseCode(200)
                    .setBody(
                      """[{"id":"org-ug-1","name":"Kampala Civic Works","country_code":"UG","currency_code":"UGX","owner_id":"owner-u1"}]"""
                    )
                }
              path.startsWith("/rest/v1/organizations") && request.method == "PATCH" ->
                MockResponse()
                  .setResponseCode(200)
                  .setBody(
                    """[{"id":"org-ug-1","name":"Kampala Civic Works","country_code":"UG","currency_code":"UGX","description":"Municipal services","contact_email":"info@kcca.ug"}]"""
                  )
              else -> MockResponse().setResponseCode(200).setBody("[]")
            }
          }
        }

      val vm = createViewModel(prefs)
      vm.startOwnerOnboardingFlow()

      // Step 1: Register Owner -> Email confirmation required
      vm.registerOwnerAccount(
        fullName = "Sarah Namukasa",
        email = "owner@kcca.ug",
        phone = "+256700111222",
        password = "CivicPassword9",
        confirmPassword = "CivicPassword9",
      )
      val afterSignup = vm.uiState.first { it.pendingEmailVerificationAddress != null }
      assertEquals("owner@kcca.ug", afterSignup.pendingEmailVerificationAddress)
      assertNotNull(prefs.getPendingOwnerOnboarding())

      // Step 1b: Confirm email & Sign In -> automatically resumes at Step 2 (Organisation Creation)
      vm.signIn("owner@kcca.ug", "CivicPassword9")
      val afterVerifySignIn =
        vm.uiState.first { it.ownerStep == OwnerOnboardingStep.ORGANISATION_CREATION }
      assertEquals(OwnerOnboardingStep.ORGANISATION_CREATION, afterVerifySignIn.ownerStep)

      // Step 2: Create Organisation via existing RPC public.create_organization
      vm.createOrganization(
        organizationName = "Kampala Civic Works",
        registrationNumber = "UG-80021",
        countryCode = "UG",
        currencyCode = "UGX",
      )
      val afterOrgCreate =
        vm.uiState.first { it.ownerStep == OwnerOnboardingStep.ORGANISATION_PROFILE }
      assertEquals("org-ug-1", afterOrgCreate.activeOrganization?.id)
      assertEquals(OrgMemberRole.OWNER, afterOrgCreate.activeMembership?.parsedRole)
      assertTrue(rpcPayloadCaptured.contains("\"p_name\":\"Kampala Civic Works\""))
      assertTrue(rpcPayloadCaptured.contains("\"p_country_code\":\"UG\""))
      assertTrue(rpcPayloadCaptured.contains("\"p_currency_code\":\"UGX\""))

      // Step 2 duplicate guard: calling createOrganization again does NOT call RPC a second time
      vm.createOrganization("Kampala Civic Works", "", "UG", "UGX")
      vm.uiState.first { !it.isSubmitting }
      assertEquals(1, createOrgCallCount)

      // Step 3: Save Organisation Profile -> Step 4 Structure Setup -> Dashboard
      vm.saveOrganizationProfile("Municipal services", "info@kcca.ug", "", "", false)
      vm.uiState.first { !it.isSubmitting && it.ownerStep == OwnerOnboardingStep.STRUCTURE_SETUP }
      vm.goToOwnerDashboard()
      val dashboardState =
        vm.uiState.first {
          it.workspaceMode == ActiveWorkspaceMode.OWNER_DASHBOARD && it.ownerDashboard != null
        }
      assertEquals("Kampala Civic Works", dashboardState.ownerDashboard?.organization?.name)
      assertEquals(0, dashboardState.ownerDashboard?.employees?.size)
    }

  @Test
  fun `owner creates supervisor invitation with SHA-256 token hash only and never sends raw token to DB`() =
    runTest {
      val prefs = FakeOnboardingPrefs(AuthSession(accessToken = "owner-jwt", userId = "owner-u1", email = "owner@kcca.ug"))
      var capturedInviteRequestBody = ""

      mockWebServer.dispatcher =
        object : Dispatcher() {
          override fun dispatch(request: RecordedRequest): MockResponse {
            val path = request.path.orEmpty()
            return when {
              path.startsWith("/auth/v1/user") ->
                MockResponse().setResponseCode(200).setBody("""{"id":"owner-u1","email":"owner@kcca.ug"}""")
              path.startsWith("/rest/v1/organization_members") ->
                MockResponse()
                  .setResponseCode(200)
                  .setBody(
                    """[{"id":"mem-1","organization_id":"org-ug-1","user_id":"owner-u1","role":"owner","status":"active","organizations":{"id":"org-ug-1","name":"Kampala Civic Works","country_code":"UG","currency_code":"UGX"}}]"""
                  )
              path.startsWith("/rest/v1/invitations") && request.method == "POST" -> {
                capturedInviteRequestBody = request.body.readUtf8()
                MockResponse()
                  .setResponseCode(201)
                  .setBody(
                    """[{"id":"inv-1","organization_id":"org-ug-1","email":"sup@kcca.ug","role":"supervisor","status":"pending","expires_at":"2026-11-25T00:00:00Z"}]"""
                  )
              }
              else -> MockResponse().setResponseCode(200).setBody("[]")
            }
          }
        }

      val vm = createViewModel(prefs)
      vm.uiState.first { it.workspaceMode == ActiveWorkspaceMode.OWNER_DASHBOARD }

      vm.sendInvitation(
        email = "sup@kcca.ug",
        role = OrgMemberRole.SUPERVISOR,
        departmentId = "dept-1",
        workplaceId = "wp-1",
      )

      val afterInvite = vm.uiState.first { it.lastCreatedInvitation != null }
      val created = afterInvite.lastCreatedInvitation!!
      val expectedHash = InvitationTokenUtils.sha256Hex(created.oneTimeRawToken)

      assertTrue(capturedInviteRequestBody.contains("\"token_hash\":\"$expectedHash\""))
      assertFalse(capturedInviteRequestBody.contains(created.oneTimeRawToken))
      assertTrue(created.oneTimeDeepLink.startsWith("civictrack://invite?token="))
    }

  @Test
  fun `supervisor invitation validation rejects expired, revoked, accepted, and owner-escalation invitations`() =
    runTest {
      val scenarios =
        listOf(
          """[]""" to "not found",
          """[{"id":"inv-exp","organization_id":"org-1","email":"s@org.ug","role":"supervisor","status":"pending","expires_at":"2020-01-01T00:00:00Z","organizations":{"id":"org-1","name":"Org"}}]""" to "expired",
          """[{"id":"inv-rev","organization_id":"org-1","email":"s@org.ug","role":"supervisor","status":"revoked","organizations":{"id":"org-1","name":"Org"}}]""" to "revoked",
          """[{"id":"inv-acc","organization_id":"org-1","email":"s@org.ug","role":"supervisor","status":"accepted","organizations":{"id":"org-1","name":"Org"}}]""" to "already been accepted",
          """[{"id":"inv-own","organization_id":"org-1","email":"s@org.ug","role":"owner","status":"pending","organizations":{"id":"org-1","name":"Org"}}]""" to "Owner privileges cannot",
        )

      for ((responseJson, expectedFragment) in scenarios) {
        mockWebServer.dispatcher =
          object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
              val path = request.path.orEmpty()
              return when {
                path.startsWith("/rest/v1/rpc/validate_invitation") ->
                  MockResponse().setResponseCode(404).setBody("""{"code":"PGRST202"}""")
                path.startsWith("/rest/v1/invitations") ->
                  MockResponse().setResponseCode(200).setBody(responseJson)
                else -> MockResponse().setResponseCode(200).setBody("[]")
              }
            }
          }

        val vm = createViewModel()
        vm.startSupervisorOnboardingFlow()
        vm.validateSupervisorInvitation("civictrack://invite?token=ValidLookingToken12345")
        val state = vm.uiState.first { !it.isSubmitting }
        assertNull(state.validatedInvitation)
        assertTrue(
          "Expected '$expectedFragment' in '${state.errorMessage}'",
          state.errorMessage?.contains(expectedFragment, ignoreCase = true) == true,
        )
      }
    }

  @Test
  fun `supervisor onboarding validates invite, blocks wrong email, accepts atomically, confirms profile, and loads scoped dashboard`() =
    runTest {
      val prefs = FakeOnboardingPrefs()
      val rawToken = "SupervisorTokenABCDEF123456"
      val expectedHash = InvitationTokenUtils.sha256Hex(rawToken)
      var invitationAccepted = false

      mockWebServer.dispatcher =
        object : Dispatcher() {
          override fun dispatch(request: RecordedRequest): MockResponse {
            val path = request.path.orEmpty()
            return when {
              path.startsWith("/rest/v1/rpc/validate_invitation") ->
                MockResponse().setResponseCode(404).setBody("""{"code":"PGRST202"}""")
              path.startsWith("/rest/v1/invitations") && path.contains(expectedHash) ->
                MockResponse()
                  .setResponseCode(200)
                  .setBody(
                    """[{"id":"inv-sup-1","organization_id":"org-ug-1","email":"supervisor@kcca.ug","role":"supervisor","department_id":"dept-ops","workplace_id":"wp-hq","status":"pending","expires_at":"2029-12-31T23:59:59Z","organizations":{"id":"org-ug-1","name":"Kampala Civic Works"},"departments":{"id":"dept-ops","organization_id":"org-ug-1","name":"Field Operations"},"workplaces":{"id":"wp-hq","name":"Kampala HQ","latitude":0.3476,"longitude":32.5825}}]"""
                  )
              path.startsWith("/auth/v1/token") -> {
                val body = request.body.readUtf8()
                if (body.contains("wrong@kcca.ug")) {
                  MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"access_token":"wrong-jwt","user":{"id":"u-wrong","email":"wrong@kcca.ug"}}""")
                } else {
                  MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"access_token":"sup-jwt","user":{"id":"u-sup","email":"supervisor@kcca.ug"}}""")
                }
              }
              path.startsWith("/auth/v1/user") ->
                if (request.getHeader("Authorization")?.contains("wrong-jwt") == true) {
                  MockResponse().setResponseCode(200).setBody("""{"id":"u-wrong","email":"wrong@kcca.ug"}""")
                } else {
                  MockResponse().setResponseCode(200).setBody("""{"id":"u-sup","email":"supervisor@kcca.ug"}""")
                }
              path.startsWith("/rest/v1/rpc/accept_supervisor_invitation") -> {
                invitationAccepted = true
                MockResponse().setResponseCode(200).setBody("""{"accepted":true}""")
              }
              path.startsWith("/rest/v1/organization_members") ->
                if (!invitationAccepted) {
                  MockResponse().setResponseCode(200).setBody("[]")
                } else {
                  MockResponse()
                    .setResponseCode(200)
                    .setBody(
                      """[{"id":"mem-sup-1","organization_id":"org-ug-1","user_id":"u-sup","role":"supervisor","status":"active","department_id":"dept-ops","workplace_id":"wp-hq","organizations":{"id":"org-ug-1","name":"Kampala Civic Works"},"departments":{"id":"dept-ops","organization_id":"org-ug-1","name":"Field Operations"},"workplaces":{"id":"wp-hq","name":"Kampala HQ","latitude":0.3476,"longitude":32.5825}}]"""
                    )
                }
              path.startsWith("/rest/v1/profiles") && request.method == "POST" ->
                MockResponse()
                  .setResponseCode(201)
                  .setBody("""[{"id":"u-sup","full_name":"David Okello","email":"supervisor@kcca.ug"}]""")
              path.startsWith("/rest/v1/employee_profiles") ->
                MockResponse()
                  .setResponseCode(200)
                  .setBody(
                    """[{"id":"ep-1","organization_id":"org-ug-1","full_name":"Grace Atim","department_id":"dept-ops","workplace_id":"wp-hq"}]"""
                  )
              path.startsWith("/rest/v1/attendance_records") ->
                MockResponse()
                  .setResponseCode(200)
                  .setBody(
                    """[{"id":"att-1","employee_id":"ep-1","workplace_id":"wp-hq","check_type":"CHECK_IN","recorded_at":"2026-11-18T08:40:00Z","geofence_status":"VERIFIED","is_late":true,"review_status":"pending"}]"""
                  )
              else -> MockResponse().setResponseCode(200).setBody("[]")
            }
          }
        }

      val vm = createViewModel(prefs)
      vm.startSupervisorOnboardingFlow("civictrack://invite?token=$rawToken")

      val validated =
        vm.uiState.first { it.supervisorStep == SupervisorOnboardingStep.ACCOUNT_AUTH }
      assertEquals("supervisor@kcca.ug", validated.validatedInvitation?.email)

      // Sign in with wrong account -> acceptance must be rejected!
      vm.signIn("wrong@kcca.ug", "Password123")
      vm.uiState.first { it.supervisorStep == SupervisorOnboardingStep.INVITATION_ACCEPTANCE }
      vm.acceptValidatedSupervisorInvitation()
      val mismatchState = vm.uiState.first { it.errorMessage != null }
      assertTrue(mismatchState.errorMessage?.contains("Email mismatch") == true)
      assertFalse(invitationAccepted)

      // Sign out and sign in with the invited supervisor email
      vm.signOut()
      vm.startSupervisorOnboardingFlow("civictrack://invite?token=$rawToken")
      vm.uiState.first { it.supervisorStep == SupervisorOnboardingStep.ACCOUNT_AUTH }
      vm.signIn("supervisor@kcca.ug", "Password123")
      vm.uiState.first { it.supervisorStep == SupervisorOnboardingStep.INVITATION_ACCEPTANCE }

      // Accept invitation atomically
      vm.acceptValidatedSupervisorInvitation()
      val afterAccept =
        vm.uiState.first { it.supervisorStep == SupervisorOnboardingStep.PROFILE_CONFIRMATION }
      assertEquals(OrgMemberRole.SUPERVISOR, afterAccept.activeMembership?.parsedRole)
      assertNull(prefs.getPendingSupervisorInvitation())

      // Confirm profile -> Supervisor Dashboard with scoped counts
      vm.confirmSupervisorProfile("David Okello", "+256777000111")
      val dashboardState =
        vm.uiState.first {
          it.workspaceMode == ActiveWorkspaceMode.SUPERVISOR_DASHBOARD &&
            it.supervisorDashboard != null
        }
      assertNotNull(dashboardState.supervisorDashboard)
      assertEquals(1, dashboardState.supervisorDashboard?.assignedEmployees?.size)
      assertEquals(1, dashboardState.supervisorDashboard?.presentEmployeesCount)
      assertEquals(1, dashboardState.supervisorDashboard?.lateArrivalsCount)
      assertEquals(1, dashboardState.supervisorDashboard?.pendingReviewsCount)
    }

  @Test
  fun `expired session (HTTP 401) recovers safely back to Sign In`() = runTest {
    val prefs = FakeOnboardingPrefs(AuthSession(accessToken = "expired-jwt", userId = "u-1", email = "owner@kcca.ug"))
    mockWebServer.dispatcher =
      object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse =
          MockResponse().setResponseCode(401).setBody("""{"message":"JWT expired"}""")
      }

    val vm = createViewModel(prefs)
    val state = vm.uiState.first { it.entryDestination == EntryAuthDestination.SIGN_IN }
    assertNull(state.session)
    assertTrue(state.errorMessage?.contains("expired") == true)
  }

  @Test
  fun `SupabaseEnvironment strips service path suffixes and preserves publishable key auth behavior`() =
    runTest {
      val envWithRestSuffix =
        SupabaseEnvironment(
          url = "${mockWebServer.url("/").toString().trimEnd('/')}/rest/v1/",
          anonKey = "sb_publishable_test_key_123",
        )
      assertEquals(mockWebServer.url("/").toString().trimEnd('/'), envWithRestSuffix.normalizedUrl)
      assertFalse(envWithRestSuffix.isJwtAnonKey)
      assertTrue(envWithRestSuffix.isConfigured)

      mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

      val api = SupabaseClientFactory.create(envWithRestSuffix)
      val repo = SupabaseOnboardingRepository(api, envWithRestSuffix)
      val report = repo.checkSupabaseConnection()
      assertTrue(report.authEndpointReachable)
      assertTrue(report.isConnected)

      val authProbeReq = mockWebServer.takeRequest()
      assertEquals("/auth/v1/health", authProbeReq.path)
      assertEquals("sb_publishable_test_key_123", authProbeReq.getHeader("apikey"))
      assertNull(authProbeReq.getHeader("Authorization"))

      mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"id":"u-1"}"""))
      api.getCurrentAuthUser("Bearer user-session-jwt")
      val authedReq = mockWebServer.takeRequest()
      assertEquals("Bearer user-session-jwt", authedReq.getHeader("Authorization"))
      assertEquals("sb_publishable_test_key_123", authedReq.getHeader("apikey"))
    }

  @Test
  fun `checkSupabaseConnection verifies auth health and returns connected status`() = runTest {
    val env = SupabaseEnvironment(
      url = mockWebServer.url("/").toString().trimEnd('/'),
      anonKey = "sb_publishable_test_key_123",
    )
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

    val api = SupabaseClientFactory.create(env)
    val repo = SupabaseOnboardingRepository(api, env)
    val report = repo.checkSupabaseConnection()

    assertTrue(report.isConfigured)
    assertTrue(report.authEndpointReachable)
    assertFalse(report.restEndpointReachable)
    assertTrue(report.isConnected)
    assertTrue(report.statusSummary.contains("Connected to Supabase"))
  }

  private fun createViewModel(prefs: PreferencesRepository = FakeOnboardingPrefs()): OnboardingViewModel {
    val env =
      SupabaseEnvironment(
        url = mockWebServer.url("/").toString().trimEnd('/'),
        anonKey = "test-anon-key",
      )
    val api = SupabaseClientFactory.create(env)
    val repo: OnboardingRepository =
      object : OnboardingRepository by SupabaseOnboardingRepository(api, env) {
        override fun isBackendConfigured(): Boolean = true
      }
    return OnboardingViewModel(repository = repo, preferences = prefs)
  }

  private class FakeOnboardingPrefs(savedSession: AuthSession? = null) : PreferencesRepository {
    private val bioFlow = MutableStateFlow(false)
    private val sessionFlow = MutableStateFlow(savedSession)
    private var pendingEmp: PendingEmployeeProfile? = null
    private var pendingOwner: PendingOwnerOnboarding? = null
    private var pendingSup: PendingSupervisorInvitation? = null

    override val biometricLoginEnabled: Flow<Boolean> = bioFlow
    override val authSession: Flow<AuthSession?> = sessionFlow

    override suspend fun setBiometricLoginEnabled(enabled: Boolean) {
      bioFlow.value = enabled
    }

    override suspend fun wasPermissionRequested(permission: String) = false
    override suspend fun markPermissionRequested(permission: String) {}
    override suspend fun saveAuthSession(session: AuthSession) {
      sessionFlow.value = session
    }

    override suspend fun clearAuthSession() {
      sessionFlow.value = null
    }

    override suspend fun savePendingEmployeeProfile(profile: PendingEmployeeProfile) {
      pendingEmp = profile
    }

    override suspend fun getPendingEmployeeProfile() = pendingEmp
    override suspend fun clearPendingEmployeeProfile() {
      pendingEmp = null
    }

    override suspend fun savePendingOwnerOnboarding(pending: PendingOwnerOnboarding) {
      pendingOwner = pending
    }

    override suspend fun getPendingOwnerOnboarding() = pendingOwner
    override suspend fun clearPendingOwnerOnboarding() {
      pendingOwner = null
    }

    override suspend fun savePendingSupervisorInvitation(pending: PendingSupervisorInvitation) {
      pendingSup = pending
    }

    override suspend fun getPendingSupervisorInvitation() = pendingSup
    override suspend fun clearPendingSupervisorInvitation() {
      pendingSup = null
    }
  }
}
