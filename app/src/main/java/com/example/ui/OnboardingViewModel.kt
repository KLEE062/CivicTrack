package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.camera.ImageProcessingResult
import com.example.core.camera.ImageProcessor
import com.example.core.camera.ImageUploadPreset
import com.example.core.capabilities.AndroidDeviceCapabilityManager
import com.example.core.location.AttendanceLocationProvider
import com.example.core.location.FusedAttendanceLocationProvider
import com.example.core.location.LocationFetchResult
import com.example.core.security.InvitationTokenUtils
import com.example.data.local.DataStorePreferencesRepository
import com.example.data.local.PreferencesRepository
import com.example.data.model.AuthSession
import com.example.data.model.CreateDepartmentRequest
import com.example.data.model.CreateWorkplaceRequest
import com.example.data.model.CreatedInvitationResult
import com.example.data.model.EntryAuthDestination
import com.example.data.model.OrgMemberRole
import com.example.data.model.OrganizationDto
import com.example.data.model.OrganizationMemberDto
import com.example.data.model.OrganizationProfilePatch
import com.example.data.model.OrganizationSettingsDto
import com.example.data.model.OwnerDashboardData
import com.example.data.model.OwnerOnboardingStep
import com.example.data.model.PendingOwnerOnboarding
import com.example.data.model.PendingSupervisorInvitation
import com.example.data.model.ProfileDto
import com.example.data.model.SupervisorDashboardData
import com.example.data.model.SupervisorOnboardingStep
import com.example.data.remote.SupabaseClientFactory
import com.example.data.repository.AccountSignUpOutcome
import com.example.data.repository.BackendResult
import com.example.data.repository.InvitationValidationOutcome
import com.example.data.repository.OnboardingRepository
import com.example.data.repository.ResolvedUserContext
import com.example.data.repository.SupabaseOnboardingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ActiveWorkspaceMode {
  UNAUTHENTICATED_OR_ONBOARDING,
  OWNER_DASHBOARD,
  SUPERVISOR_DASHBOARD,
  EMPLOYEE_WORKSPACE,
}

data class OnboardingUiState(
  val isBackendConfigured: Boolean = false,
  val session: AuthSession? = null,
  val isRestoringSession: Boolean = false,
  val workspaceMode: ActiveWorkspaceMode = ActiveWorkspaceMode.UNAUTHENTICATED_OR_ONBOARDING,
  val entryDestination: EntryAuthDestination = EntryAuthDestination.LANDING,
  val ownerStep: OwnerOnboardingStep = OwnerOnboardingStep.ACCOUNT_REGISTRATION,
  val supervisorStep: SupervisorOnboardingStep = SupervisorOnboardingStep.INVITATION_LOOKUP,
  // Account & Auth state
  val isSubmitting: Boolean = false,
  val errorMessage: String? = null,
  val infoMessage: String? = null,
  val pendingEmailVerificationAddress: String? = null,
  val userProfile: ProfileDto? = null,
  val activeMembership: OrganizationMemberDto? = null,
  val activeOrganization: OrganizationDto? = null,
  // Owner structure & dashboard
  val ownerDashboard: OwnerDashboardData? = null,
  val lastCreatedInvitation: CreatedInvitationResult? = null,
  val gpsCaptureStatus: String? = null,
  val capturedWorkplaceLat: Double? = null,
  val capturedWorkplaceLng: Double? = null,
  // Supervisor invitation & dashboard
  val validatedInvitation: com.example.data.model.InvitationDto? = null,
  val validatedTokenHash: String? = null,
  val supervisorAuthUseSignIn: Boolean = false,
  val supervisorDashboard: SupervisorDashboardData? = null,
  val supervisorProfileConfirmed: Boolean = false,
)

class OnboardingViewModel(
  private val repository: OnboardingRepository,
  private val preferences: PreferencesRepository,
  private val locationProvider: AttendanceLocationProvider? = null,
) : ViewModel() {

  private val _uiState =
    MutableStateFlow(OnboardingUiState(isBackendConfigured = repository.isBackendConfigured()))
  val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

  private var biometricGateActive = false

  init {
    viewModelScope.launch {
      preferences.biometricLoginEnabled.collectLatest { enabled ->
        biometricGateActive = enabled
      }
    }
    viewModelScope.launch {
      preferences.authSession.collectLatest { savedSession ->
        if (savedSession != null && _uiState.value.session == null && !biometricGateActive) {
          restoreAuthenticatedSession(savedSession)
        } else if (savedSession == null && _uiState.value.session != null) {
          resetToSignedOutState()
        }
      }
    }
  }

  // --- Entry & Navigation Controls ---

  fun selectEntryDestination(destination: EntryAuthDestination) {
    _uiState.update {
      it.copy(
        entryDestination = destination,
        errorMessage = null,
        infoMessage = null,
        pendingEmailVerificationAddress = null,
      )
    }
  }

  fun startOwnerOnboardingFlow() {
    _uiState.update {
      it.copy(
        entryDestination = EntryAuthDestination.OWNER_ONBOARDING,
        ownerStep =
          if (it.session != null) OwnerOnboardingStep.ORGANISATION_CREATION
          else OwnerOnboardingStep.ACCOUNT_REGISTRATION,
        errorMessage = null,
        infoMessage = null,
      )
    }
  }

  fun startSupervisorOnboardingFlow(initialTokenOrLink: String? = null) {
    _uiState.update {
      it.copy(
        entryDestination = EntryAuthDestination.SUPERVISOR_ONBOARDING,
        supervisorStep = SupervisorOnboardingStep.INVITATION_LOOKUP,
        errorMessage = null,
        infoMessage = null,
      )
    }
    if (!initialTokenOrLink.isNullOrBlank()) {
      validateSupervisorInvitation(initialTokenOrLink)
    }
  }

  fun openOwnerStructureWizard() {
    _uiState.update {
      it.copy(
        workspaceMode = ActiveWorkspaceMode.UNAUTHENTICATED_OR_ONBOARDING,
        entryDestination = EntryAuthDestination.OWNER_ONBOARDING,
        ownerStep = OwnerOnboardingStep.STRUCTURE_SETUP,
        errorMessage = null,
        infoMessage = null,
      )
    }
  }

  fun openOwnerOrgProfileStep() {
    _uiState.update {
      it.copy(
        workspaceMode = ActiveWorkspaceMode.UNAUTHENTICATED_OR_ONBOARDING,
        entryDestination = EntryAuthDestination.OWNER_ONBOARDING,
        ownerStep = OwnerOnboardingStep.ORGANISATION_PROFILE,
        errorMessage = null,
        infoMessage = null,
      )
    }
  }

  fun goToOwnerDashboard() {
    val org = _uiState.value.activeOrganization ?: return
    _uiState.update {
      it.copy(
        workspaceMode = ActiveWorkspaceMode.OWNER_DASHBOARD,
        ownerStep = OwnerOnboardingStep.DASHBOARD,
        errorMessage = null,
      )
    }
    refreshOwnerDashboard(org)
  }

  fun clearMessages() {
    _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
  }

  // --- Shared Sign In & Forgot Password ---

  fun signIn(email: String, password: String) {
    if (_uiState.value.isSubmitting) return
    val cleanEmail = email.trim()
    if (!InvitationTokenUtils.isValidEmail(cleanEmail)) {
      _uiState.update { it.copy(errorMessage = "Enter a valid work email address.") }
      return
    }
    if (password.isBlank()) {
      _uiState.update { it.copy(errorMessage = "Enter your account password.") }
      return
    }

    viewModelScope.launch {
      _uiState.update {
        it.copy(isSubmitting = true, errorMessage = null, infoMessage = null)
      }
      when (val result = repository.signIn(cleanEmail, password)) {
        is BackendResult.Success -> {
          _uiState.update { it.copy(session = result.data) }
          preferences.saveAuthSession(result.data)
          onSessionAuthenticated(result.data)
        }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
        BackendResult.NotConfigured ->
          _uiState.update {
            it.copy(
              isSubmitting = false,
              errorMessage = "Configure SUPABASE_URL and SUPABASE_ANON_KEY in AI Studio Secrets.",
            )
          }
      }
    }
  }

  fun requestPasswordReset(email: String) {
    if (_uiState.value.isSubmitting) return
    val cleanEmail = email.trim()
    if (!InvitationTokenUtils.isValidEmail(cleanEmail)) {
      _uiState.update { it.copy(errorMessage = "Enter a valid email address to reset your password.") }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, infoMessage = null) }
      when (val result = repository.requestPasswordReset(cleanEmail)) {
        is BackendResult.Success ->
          _uiState.update {
            it.copy(
              isSubmitting = false,
              infoMessage =
                "Password recovery instructions have been sent to $cleanEmail if an account exists.",
            )
          }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
        BackendResult.NotConfigured ->
          _uiState.update {
            it.copy(
              isSubmitting = false,
              errorMessage = "Supabase credentials are not configured in AI Studio Secrets.",
            )
          }
      }
    }
  }

  fun signOut() {
    viewModelScope.launch {
      preferences.clearAuthSession()
      resetToSignedOutState()
    }
  }

  private fun resetToSignedOutState() {
    _uiState.update {
      OnboardingUiState(
        isBackendConfigured = repository.isBackendConfigured(),
        entryDestination = EntryAuthDestination.LANDING,
      )
    }
  }

  // --- Role-Aware Session Restoration & Workflow Recovery ---

  fun restoreAuthenticatedSession(session: AuthSession) {
    viewModelScope.launch {
      _uiState.update { it.copy(isRestoringSession = true, session = session, errorMessage = null) }
      onSessionAuthenticated(session)
    }
  }

  private suspend fun onSessionAuthenticated(session: AuthSession) {
    when (val ctxRes = repository.resolveUserContext(session)) {
      is BackendResult.Success -> {
        val ctx = ctxRes.data
        routeFromBackendContext(session, ctx)
      }
      is BackendResult.ApiFailure -> {
        if (ctxRes.code == 401) {
          preferences.clearAuthSession()
          _uiState.update {
            it.copy(
              session = null,
              isSubmitting = false,
              isRestoringSession = false,
              workspaceMode = ActiveWorkspaceMode.UNAUTHENTICATED_OR_ONBOARDING,
              entryDestination = EntryAuthDestination.SIGN_IN,
              errorMessage = "Your session expired. Sign in again to resume safely.",
            )
          }
        } else {
          _uiState.update {
            it.copy(
              session = session,
              isSubmitting = false,
              isRestoringSession = false,
              errorMessage = ctxRes.message,
            )
          }
        }
      }
      is BackendResult.NetworkFailure ->
        _uiState.update {
          it.copy(
            session = session,
            isSubmitting = false,
            isRestoringSession = false,
            errorMessage = ctxRes.message,
          )
        }
      BackendResult.NotConfigured ->
        _uiState.update {
          it.copy(
            isSubmitting = false,
            isRestoringSession = false,
            errorMessage = "Supabase credentials are not configured.",
          )
        }
    }
  }

  private suspend fun routeFromBackendContext(
    session: AuthSession,
    ctx: ResolvedUserContext,
  ) {
    val activeValidatedInvite = _uiState.value.validatedInvitation
    val pendingSupInvite = preferences.getPendingSupervisorInvitation()
    val pendingOwner = preferences.getPendingOwnerOnboarding()
    val primaryMembership = ctx.primaryMembership

    // Case A: User is currently in Supervisor onboarding with a validated invitation
    if (
      activeValidatedInvite != null &&
        _uiState.value.entryDestination == EntryAuthDestination.SUPERVISOR_ONBOARDING
    ) {
      _uiState.update {
        it.copy(
          session = session,
          userProfile = ctx.profile,
          isSubmitting = false,
          isRestoringSession = false,
          pendingEmailVerificationAddress = null,
          supervisorStep = SupervisorOnboardingStep.INVITATION_ACCEPTANCE,
        )
      }
      return
    }

    // Case B: Resuming after email confirmation for a saved Supervisor invitation
    if (
      pendingSupInvite != null &&
        pendingSupInvite.invitedEmail.equals(session.email, ignoreCase = true) &&
        primaryMembership == null
    ) {
      val revalidated =
        repository.validateInvitationToken(session, pendingSupInvite.tokenHash).let { res ->
          res
        }
      // Restore invitation display state even if tokenHash needs server lookup
      _uiState.update {
        it.copy(
          session = session,
          userProfile = ctx.profile,
          isSubmitting = false,
          isRestoringSession = false,
          pendingEmailVerificationAddress = null,
          entryDestination = EntryAuthDestination.SUPERVISOR_ONBOARDING,
          supervisorStep = SupervisorOnboardingStep.INVITATION_ACCEPTANCE,
          validatedTokenHash = pendingSupInvite.tokenHash,
          validatedInvitation =
            (revalidated as? BackendResult.Success)?.data
              ?.let { (it as? InvitationValidationOutcome.Valid)?.invitation }
              ?: com.example.data.model.InvitationDto(
                id = "pending",
                organizationId = pendingSupInvite.organizationId,
                email = pendingSupInvite.invitedEmail,
                role = pendingSupInvite.role,
                organizations =
                  OrganizationDto(
                    id = pendingSupInvite.organizationId,
                    name = pendingSupInvite.organizationName,
                  ),
              ),
        )
      }
      return
    }

    // Case C: Existing Owner / Admin membership or owned organization
    val ownerMembership =
      ctx.memberships.firstOrNull { it.parsedRole.isOwnerOrAdmin }
        ?: ctx.ownedOrganizations.firstOrNull()?.let { org ->
          OrganizationMemberDto(
            id = "owner-${org.id}",
            organizationId = org.id,
            userId = session.userId,
            role = OrgMemberRole.OWNER.apiValue,
            status = "active",
            organizations = org,
          )
        }

    if (ownerMembership != null) {
      preferences.clearPendingOwnerOnboarding()
      val org = ownerMembership.organizations ?: ctx.ownedOrganizations.firstOrNull()
      val isNewBlankOrg =
        org != null &&
          org.description.isNullOrBlank() &&
          org.contactEmail.isNullOrBlank() &&
          _uiState.value.entryDestination == EntryAuthDestination.OWNER_ONBOARDING

      if (isNewBlankOrg) {
        _uiState.update {
          it.copy(
            session = session,
            userProfile = ctx.profile,
            activeMembership = ownerMembership,
            activeOrganization = org,
            isSubmitting = false,
            isRestoringSession = false,
            pendingEmailVerificationAddress = null,
            workspaceMode = ActiveWorkspaceMode.UNAUTHENTICATED_OR_ONBOARDING,
            entryDestination = EntryAuthDestination.OWNER_ONBOARDING,
            ownerStep = OwnerOnboardingStep.ORGANISATION_PROFILE,
          )
        }
      } else if (org != null) {
        _uiState.update {
          it.copy(
            session = session,
            userProfile = ctx.profile,
            activeMembership = ownerMembership,
            activeOrganization = org,
            isSubmitting = false,
            isRestoringSession = false,
            pendingEmailVerificationAddress = null,
            workspaceMode = ActiveWorkspaceMode.OWNER_DASHBOARD,
            ownerStep = OwnerOnboardingStep.DASHBOARD,
          )
        }
        refreshOwnerDashboard(org, ctx.profile?.fullName ?: session.email)
      }
      return
    }

    // Case D: Existing Supervisor membership
    val supervisorMembership =
      ctx.memberships.firstOrNull { it.parsedRole == OrgMemberRole.SUPERVISOR }
    if (supervisorMembership != null) {
      preferences.clearPendingSupervisorInvitation()
      val org = supervisorMembership.organizations
      _uiState.update {
        it.copy(
          session = session,
          userProfile = ctx.profile ?: supervisorMembership.profiles,
          activeMembership = supervisorMembership,
          activeOrganization = org,
          isSubmitting = false,
          isRestoringSession = false,
          pendingEmailVerificationAddress = null,
          workspaceMode = ActiveWorkspaceMode.SUPERVISOR_DASHBOARD,
          supervisorStep = SupervisorOnboardingStep.DASHBOARD,
        )
      }
      refreshSupervisorDashboard(supervisorMembership)
      return
    }

    // Case E: Owner signed up + confirmed email, now needs Step 2 (Create Organisation)
    if (
      (pendingOwner != null && pendingOwner.email.equals(session.email, ignoreCase = true)) ||
        _uiState.value.entryDestination == EntryAuthDestination.OWNER_ONBOARDING
    ) {
      if (pendingOwner != null && ctx.profile == null) {
        repository.upsertProfile(session, pendingOwner.fullName, pendingOwner.phone)
      }
      _uiState.update {
        it.copy(
          session = session,
          userProfile =
            ctx.profile
              ?: pendingOwner?.let { p ->
                ProfileDto(id = session.userId, fullName = p.fullName, email = session.email, phone = p.phone)
              },
          isSubmitting = false,
          isRestoringSession = false,
          pendingEmailVerificationAddress = null,
          workspaceMode = ActiveWorkspaceMode.UNAUTHENTICATED_OR_ONBOARDING,
          entryDestination = EntryAuthDestination.OWNER_ONBOARDING,
          ownerStep = OwnerOnboardingStep.ORGANISATION_CREATION,
        )
      }
      return
    }

    // Case F: Employee membership or existing employee record
    _uiState.update {
      it.copy(
        session = session,
        userProfile = ctx.profile,
        activeMembership = primaryMembership,
        activeOrganization = primaryMembership?.organizations,
        isSubmitting = false,
        isRestoringSession = false,
        pendingEmailVerificationAddress = null,
        workspaceMode = ActiveWorkspaceMode.EMPLOYEE_WORKSPACE,
      )
    }
  }

  // --- PHASE 2: OWNER ONBOARDING WORKFLOW ---

  fun registerOwnerAccount(
    fullName: String,
    email: String,
    phone: String,
    password: String,
    confirmPassword: String,
  ) {
    if (_uiState.value.isSubmitting) return

    val cleanName = fullName.trim()
    val cleanEmail = email.trim()
    if (cleanName.length < 2) {
      _uiState.update { it.copy(errorMessage = "Enter your full name.") }
      return
    }
    if (!InvitationTokenUtils.isValidEmail(cleanEmail)) {
      _uiState.update { it.copy(errorMessage = "Enter a valid institutional email address.") }
      return
    }
    val passwordError = InvitationTokenUtils.validatePassword(password)
    if (passwordError != null) {
      _uiState.update { it.copy(errorMessage = passwordError) }
      return
    }
    if (password != confirmPassword) {
      _uiState.update { it.copy(errorMessage = "Passwords do not match.") }
      return
    }

    viewModelScope.launch {
      _uiState.update {
        it.copy(
          isSubmitting = true,
          errorMessage = null,
          infoMessage = null,
          pendingEmailVerificationAddress = null,
        )
      }

      when (
        val result =
          repository.signUpAccount(
            fullName = cleanName,
            email = cleanEmail,
            phone = phone.trim().takeIf { it.isNotEmpty() },
            password = password,
            onboardingFlow = "owner",
          )
      ) {
        is BackendResult.Success -> {
          when (val outcome = result.data) {
            is AccountSignUpOutcome.SignedIn -> {
              _uiState.update {
                it.copy(
                  session = outcome.session,
                  userProfile =
                    ProfileDto(
                      id = outcome.session.userId,
                      fullName = cleanName,
                      email = outcome.session.email,
                      phone = phone.trim().takeIf { p -> p.isNotEmpty() },
                    ),
                  isSubmitting = false,
                  ownerStep = OwnerOnboardingStep.ORGANISATION_CREATION,
                  infoMessage = "Owner account created. Next, register your organisation.",
                )
              }
              preferences.clearPendingOwnerOnboarding()
              preferences.saveAuthSession(outcome.session)
            }
            is AccountSignUpOutcome.EmailConfirmationRequired -> {
              preferences.savePendingOwnerOnboarding(
                PendingOwnerOnboarding(
                  email = outcome.email,
                  fullName = cleanName,
                  phone = phone.trim().takeIf { p -> p.isNotEmpty() },
                )
              )
              _uiState.update {
                it.copy(
                  isSubmitting = false,
                  pendingEmailVerificationAddress = outcome.email,
                  infoMessage =
                    "Confirmation email sent to ${outcome.email}. Verify your email, then sign in to continue creating your organisation.",
                )
              }
            }
          }
        }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
        BackendResult.NotConfigured ->
          _uiState.update {
            it.copy(
              isSubmitting = false,
              errorMessage = "Configure SUPABASE_URL and SUPABASE_ANON_KEY in AI Studio Secrets.",
            )
          }
      }
    }
  }

  fun createOrganization(
    organizationName: String,
    registrationNumber: String,
    countryCode: String = "UG",
    currencyCode: String = "UGX",
  ) {
    if (_uiState.value.isSubmitting) return
    val session =
      _uiState.value.session
        ?: run {
          _uiState.update { it.copy(errorMessage = "Sign in to register your organisation.") }
          return
        }

    val cleanName = organizationName.trim()
    if (cleanName.length < 2) {
      _uiState.update { it.copy(errorMessage = "Enter a valid organisation name.") }
      return
    }
    if (countryCode.trim().length != 2) {
      _uiState.update { it.copy(errorMessage = "Select a 2-letter country code (default UG).") }
      return
    }
    if (currencyCode.trim().length != 3) {
      _uiState.update { it.copy(errorMessage = "Select a 3-letter currency code (default UGX).") }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, infoMessage = null) }
      when (
        val res =
          repository.createOrganizationViaRpc(
            session = session,
            name = cleanName,
            registrationNumber = registrationNumber.trim().takeIf { it.isNotEmpty() },
            countryCode = countryCode.trim().uppercase(),
            currencyCode = currencyCode.trim().uppercase(),
          )
      ) {
        is BackendResult.Success -> {
          preferences.clearPendingOwnerOnboarding()
          val member = res.data
          val org = member.organizations
          _uiState.update {
            it.copy(
              isSubmitting = false,
              activeMembership = member,
              activeOrganization = org,
              ownerStep = OwnerOnboardingStep.ORGANISATION_PROFILE,
              infoMessage = "Organisation '${org?.name ?: cleanName}' created and owner role confirmed.",
            )
          }
        }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = res.message) }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = res.message) }
        BackendResult.NotConfigured ->
          _uiState.update {
            it.copy(
              isSubmitting = false,
              errorMessage = "Supabase credentials are not configured.",
            )
          }
      }
    }
  }

  fun saveOrganizationProfile(
    description: String,
    contactEmail: String,
    contactPhone: String,
    address: String,
    skipToSetup: Boolean = false,
  ) {
    if (skipToSetup) {
      val org = _uiState.value.activeOrganization
      _uiState.update {
        it.copy(ownerStep = OwnerOnboardingStep.STRUCTURE_SETUP, errorMessage = null)
      }
      if (org != null) refreshOwnerDashboard(org)
      return
    }

    if (_uiState.value.isSubmitting) return
    val session = _uiState.value.session ?: return
    val org = _uiState.value.activeOrganization ?: return

    val cleanEmail = contactEmail.trim()
    if (cleanEmail.isNotEmpty() && !InvitationTokenUtils.isValidEmail(cleanEmail)) {
      _uiState.update { it.copy(errorMessage = "Enter a valid official contact email address.") }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, infoMessage = null) }
      when (
        val res =
          repository.updateOrganizationProfile(
            session = session,
            organizationId = org.id,
            patch =
              OrganizationProfilePatch(
                description = description.trim().takeIf { it.isNotEmpty() },
                contactEmail = cleanEmail.takeIf { it.isNotEmpty() },
                contactPhone = contactPhone.trim().takeIf { it.isNotEmpty() },
                address = address.trim().takeIf { it.isNotEmpty() },
              ),
          )
      ) {
        is BackendResult.Success -> {
          _uiState.update {
            it.copy(
              isSubmitting = false,
              activeOrganization = res.data,
              ownerStep = OwnerOnboardingStep.STRUCTURE_SETUP,
              infoMessage = "Organisation profile saved.",
            )
          }
          refreshOwnerDashboard(res.data)
        }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = res.message) }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = res.message) }
        BackendResult.NotConfigured ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = "Supabase not configured.") }
      }
    }
  }

  fun uploadOrganizationLogoFromUri(context: Context, uri: Uri) {
    val session = _uiState.value.session ?: return
    val org = _uiState.value.activeOrganization ?: return
    viewModelScope.launch {
      when (
        val processed = ImageProcessor.processPickedUri(context, uri, ImageUploadPreset.PROFILE_PHOTO)
      ) {
        is ImageProcessingResult.Success -> {
          _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
          when (
            val res =
              repository.uploadOrganizationLogo(
                session = session,
                organizationId = org.id,
                imageFile = processed.asset.file,
                mimeType = processed.asset.mimeType,
              )
          ) {
            is BackendResult.Success -> {
              processed.asset.file.delete()
              _uiState.update {
                it.copy(
                  isSubmitting = false,
                  activeOrganization = res.data,
                  infoMessage = "Organisation logo uploaded.",
                )
              }
            }
            is BackendResult.NetworkFailure -> {
              processed.asset.file.delete()
              _uiState.update { it.copy(isSubmitting = false, errorMessage = res.message) }
            }
            is BackendResult.ApiFailure -> {
              processed.asset.file.delete()
              _uiState.update { it.copy(isSubmitting = false, errorMessage = res.message) }
            }
            BackendResult.NotConfigured -> {
              processed.asset.file.delete()
              _uiState.update { it.copy(isSubmitting = false, errorMessage = "Supabase not configured.") }
            }
          }
        }
        is ImageProcessingResult.ValidationError ->
          _uiState.update { it.copy(errorMessage = processed.message) }
      }
    }
  }

  // --- Owner Structure Setup Wizard & Invitations ---

  fun captureCurrentCoordinatesForWorkplace(context: Context) {
    val provider = locationProvider ?: return
    val checker = AndroidDeviceCapabilityManager(context)
    viewModelScope.launch {
      val permState = checker.inspectCapabilities(null, false, false).locationPermission
      _uiState.update { it.copy(gpsCaptureStatus = "Reading GPS coordinates...") }
      when (val result = provider.fetchCurrentLocation(permState)) {
        is LocationFetchResult.Success -> {
          _uiState.update {
            it.copy(
              capturedWorkplaceLat = result.location.latitude,
              capturedWorkplaceLng = result.location.longitude,
              gpsCaptureStatus =
                "Captured %.5f, %.5f (±%.0fm)".format(
                  result.location.latitude,
                  result.location.longitude,
                  result.location.accuracyMeters,
                ),
            )
          }
        }
        is LocationFetchResult.PermissionDenied ->
          _uiState.update {
            it.copy(gpsCaptureStatus = "Location permission needed to auto-fill coordinates.")
          }
        LocationFetchResult.ServicesDisabled ->
          _uiState.update {
            it.copy(gpsCaptureStatus = "Enable device location services or enter coordinates manually.")
          }
        is LocationFetchResult.Unavailable ->
          _uiState.update { it.copy(gpsCaptureStatus = result.reason) }
      }
    }
  }

  fun addWorkplace(
    name: String,
    address: String,
    latText: String,
    lngText: String,
    radiusText: String,
  ) {
    if (_uiState.value.isSubmitting) return
    val session = _uiState.value.session ?: return
    val org = _uiState.value.activeOrganization ?: return

    val lat = latText.toDoubleOrNull()
    val lng = lngText.toDoubleOrNull()
    val radius = radiusText.toDoubleOrNull() ?: 150.0
    if (name.isBlank()) {
      _uiState.update { it.copy(errorMessage = "Enter a workplace name.") }
      return
    }
    if (lat == null || lng == null || lat !in -90.0..90.0 || lng !in -180.0..180.0) {
      _uiState.update { it.copy(errorMessage = "Enter valid workplace latitude and longitude.") }
      return
    }
    if (radius < 20.0 || radius > 5000.0) {
      _uiState.update { it.copy(errorMessage = "Geofence radius must be between 20m and 5000m.") }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, infoMessage = null) }
      when (
        val res =
          repository.createWorkplace(
            session = session,
            request =
              CreateWorkplaceRequest(
                organizationId = org.id,
                name = name.trim(),
                address = address.trim().takeIf { it.isNotEmpty() },
                latitude = lat,
                longitude = lng,
                geofenceRadiusMeters = radius,
              ),
          )
      ) {
        is BackendResult.Success -> {
          _uiState.update {
            it.copy(
              isSubmitting = false,
              infoMessage = "Workplace '${res.data.name}' added (${res.data.geofenceRadiusMeters.toInt()}m geofence).",
            )
          }
          refreshOwnerDashboard(org)
        }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = res.message) }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = res.message) }
        BackendResult.NotConfigured ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = "Supabase not configured.") }
      }
    }
  }

  fun addDepartment(name: String, description: String) {
    if (_uiState.value.isSubmitting) return
    val session = _uiState.value.session ?: return
    val org = _uiState.value.activeOrganization ?: return
    if (name.isBlank()) {
      _uiState.update { it.copy(errorMessage = "Enter a department name.") }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, infoMessage = null) }
      when (
        val res =
          repository.createDepartment(
            session = session,
            request =
              CreateDepartmentRequest(
                organizationId = org.id,
                name = name.trim(),
                description = description.trim().takeIf { it.isNotEmpty() },
              ),
          )
      ) {
        is BackendResult.Success -> {
          _uiState.update {
            it.copy(isSubmitting = false, infoMessage = "Department '${res.data.name}' added.")
          }
          refreshOwnerDashboard(org)
        }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = res.message) }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = res.message) }
        BackendResult.NotConfigured ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = "Supabase not configured.") }
      }
    }
  }

  fun saveAttendanceSettings(
    requireBiometric: Boolean,
    allowOutsideGeofence: Boolean,
    requireLocation: Boolean,
    workingDays: List<String>,
    workStartTime: String,
    workEndTime: String,
    lateGraceMinutes: Int,
  ) {
    if (_uiState.value.isSubmitting) return
    val session = _uiState.value.session ?: return
    val org = _uiState.value.activeOrganization ?: return

    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, infoMessage = null) }
      when (
        val res =
          repository.saveOrganizationSettings(
            session = session,
            settings =
              OrganizationSettingsDto(
                organizationId = org.id,
                requireBiometricForAttendance = requireBiometric,
                allowCheckInOutsideGeofence = allowOutsideGeofence,
                requireLocationVerification = requireLocation,
                workingDays = workingDays.ifEmpty { listOf("MON", "TUE", "WED", "THU", "FRI") },
                workStartTime = workStartTime.ifBlank { "08:30" },
                workEndTime = workEndTime.ifBlank { "17:00" },
                lateGraceMinutes = lateGraceMinutes.coerceIn(0, 180),
              ),
          )
      ) {
        is BackendResult.Success -> {
          _uiState.update {
            it.copy(isSubmitting = false, infoMessage = "Attendance rules and schedule saved.")
          }
          refreshOwnerDashboard(org)
        }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = res.message) }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = res.message) }
        BackendResult.NotConfigured ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = "Supabase not configured.") }
      }
    }
  }

  fun sendInvitation(
    email: String,
    role: OrgMemberRole,
    departmentId: String?,
    workplaceId: String?,
  ) {
    if (_uiState.value.isSubmitting) return
    val session = _uiState.value.session ?: return
    val org = _uiState.value.activeOrganization ?: return

    val cleanEmail = email.trim()
    if (!InvitationTokenUtils.isValidEmail(cleanEmail)) {
      _uiState.update { it.copy(errorMessage = "Enter a valid recipient email address.") }
      return
    }
    if (role == OrgMemberRole.OWNER) {
      _uiState.update { it.copy(errorMessage = "Owner privileges cannot be invited via member invitations.") }
      return
    }

    viewModelScope.launch {
      _uiState.update {
        it.copy(
          isSubmitting = true,
          errorMessage = null,
          infoMessage = null,
          lastCreatedInvitation = null,
        )
      }
      when (
        val res =
          repository.createInvitation(
            session = session,
            organizationId = org.id,
            email = cleanEmail,
            role = role,
            departmentId = departmentId,
            workplaceId = workplaceId,
          )
      ) {
        is BackendResult.Success -> {
          _uiState.update {
            it.copy(
              isSubmitting = false,
              lastCreatedInvitation = res.data,
              infoMessage =
                "Invitation created for ${res.data.invitation.email}. Share the one-time token or link securely.",
            )
          }
          refreshOwnerDashboard(org)
        }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = res.message) }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = res.message) }
        BackendResult.NotConfigured ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = "Supabase not configured.") }
      }
    }
  }

  fun revokeInvitation(invitationId: String) {
    val session = _uiState.value.session ?: return
    val org = _uiState.value.activeOrganization ?: return
    viewModelScope.launch {
      when (val res = repository.revokeInvitation(session, org.id, invitationId)) {
        is BackendResult.Success -> {
          _uiState.update { it.copy(infoMessage = "Invitation revoked.") }
          refreshOwnerDashboard(org)
        }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(errorMessage = res.message) }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(errorMessage = res.message) }
        BackendResult.NotConfigured -> Unit
      }
    }
  }

  fun refreshOwnerDashboard(
    organizationOverride: OrganizationDto? = null,
    ownerNameOverride: String? = null,
  ) {
    val session = _uiState.value.session ?: return
    val org = organizationOverride ?: _uiState.value.activeOrganization ?: return
    val ownerName =
      ownerNameOverride ?: _uiState.value.userProfile?.fullName ?: session.email
    viewModelScope.launch {
      when (val res = repository.loadOwnerDashboard(session, org, ownerName)) {
        is BackendResult.Success ->
          _uiState.update {
            it.copy(
              ownerDashboard = res.data,
              activeOrganization = res.data.organization,
            )
          }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(errorMessage = res.message) }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(errorMessage = res.message) }
        BackendResult.NotConfigured -> Unit
      }
    }
  }

  // --- PHASE 3 & 4: SUPERVISOR ONBOARDING & DASHBOARD ---

  fun validateSupervisorInvitation(rawTokenOrLink: String) {
    if (_uiState.value.isSubmitting) return
    if (rawTokenOrLink.isBlank()) {
      _uiState.update {
        it.copy(errorMessage = "Paste your invitation token or civictrack:// link to continue.")
      }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, infoMessage = null) }
      when (
        val res =
          repository.validateInvitationToken(
            session = _uiState.value.session,
            rawTokenOrLink = rawTokenOrLink,
          )
      ) {
        is BackendResult.Success -> {
          when (val outcome = res.data) {
            is InvitationValidationOutcome.Valid -> {
              val inv = outcome.invitation
              preferences.savePendingSupervisorInvitation(
                PendingSupervisorInvitation(
                  tokenHash = outcome.tokenHash,
                  invitedEmail = inv.email,
                  organizationId = inv.organizationId,
                  organizationName = inv.organizations?.name ?: "CivicTrack Organisation",
                  role = inv.role,
                  departmentName = inv.departments?.name,
                  workplaceName = inv.workplaces?.name,
                )
              )
              val nextStep =
                if (_uiState.value.session != null) SupervisorOnboardingStep.INVITATION_ACCEPTANCE
                else SupervisorOnboardingStep.ACCOUNT_AUTH
              _uiState.update {
                it.copy(
                  isSubmitting = false,
                  validatedInvitation = inv,
                  validatedTokenHash = outcome.tokenHash,
                  supervisorStep = nextStep,
                  infoMessage =
                    "Invitation verified for ${inv.organizations?.name ?: "organisation"}.",
                )
              }
            }
            is InvitationValidationOutcome.Invalid ->
              _uiState.update {
                it.copy(
                  isSubmitting = false,
                  validatedInvitation = null,
                  validatedTokenHash = null,
                  errorMessage = outcome.reason,
                )
              }
          }
        }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = res.message) }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = res.message) }
        BackendResult.NotConfigured ->
          _uiState.update {
            it.copy(
              isSubmitting = false,
              errorMessage = "Configure SUPABASE_URL and SUPABASE_ANON_KEY in AI Studio Secrets.",
            )
          }
      }
    }
  }

  fun setSupervisorAuthMode(useSignIn: Boolean) {
    _uiState.update {
      it.copy(supervisorAuthUseSignIn = useSignIn, errorMessage = null, infoMessage = null)
    }
  }

  fun registerSupervisorAccount(
    fullName: String,
    password: String,
    confirmPassword: String,
  ) {
    if (_uiState.value.isSubmitting) return
    val invite =
      _uiState.value.validatedInvitation
        ?: run {
          _uiState.update { it.copy(errorMessage = "Validate an invitation first.") }
          return
        }

    val cleanName = fullName.trim()
    if (cleanName.length < 2) {
      _uiState.update { it.copy(errorMessage = "Enter your full name.") }
      return
    }
    val pwError = InvitationTokenUtils.validatePassword(password)
    if (pwError != null) {
      _uiState.update { it.copy(errorMessage = pwError) }
      return
    }
    if (password != confirmPassword) {
      _uiState.update { it.copy(errorMessage = "Passwords do not match.") }
      return
    }

    viewModelScope.launch {
      _uiState.update {
        it.copy(
          isSubmitting = true,
          errorMessage = null,
          infoMessage = null,
          pendingEmailVerificationAddress = null,
        )
      }
      when (
        val res =
          repository.signUpAccount(
            fullName = cleanName,
            email = invite.email,
            phone = null,
            password = password,
            onboardingFlow = "supervisor",
          )
      ) {
        is BackendResult.Success -> {
          when (val outcome = res.data) {
            is AccountSignUpOutcome.SignedIn -> {
              _uiState.update {
                it.copy(
                  session = outcome.session,
                  userProfile =
                    ProfileDto(
                      id = outcome.session.userId,
                      fullName = cleanName,
                      email = outcome.session.email,
                    ),
                  isSubmitting = false,
                  supervisorStep = SupervisorOnboardingStep.INVITATION_ACCEPTANCE,
                )
              }
              preferences.saveAuthSession(outcome.session)
            }
            is AccountSignUpOutcome.EmailConfirmationRequired -> {
              _uiState.update {
                it.copy(
                  isSubmitting = false,
                  supervisorAuthUseSignIn = true,
                  pendingEmailVerificationAddress = outcome.email,
                  infoMessage =
                    "Confirmation email sent to ${outcome.email}. Verify your email, then sign in here to complete invitation acceptance.",
                )
              }
            }
          }
        }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = res.message) }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = res.message) }
        BackendResult.NotConfigured ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = "Supabase not configured.") }
      }
    }
  }

  fun acceptValidatedSupervisorInvitation() {
    if (_uiState.value.isSubmitting) return
    val session = _uiState.value.session ?: return
    val invite = _uiState.value.validatedInvitation ?: return
    val tokenHash = _uiState.value.validatedTokenHash ?: return

    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, infoMessage = null) }
      when (val res = repository.acceptSupervisorInvitation(session, invite, tokenHash)) {
        is BackendResult.Success -> {
          preferences.clearPendingSupervisorInvitation()
          val member = res.data
          _uiState.update {
            it.copy(
              isSubmitting = false,
              activeMembership = member,
              activeOrganization = member.organizations ?: invite.organizations,
              supervisorStep = SupervisorOnboardingStep.PROFILE_CONFIRMATION,
              infoMessage = "Invitation accepted. Your supervisor membership is now active.",
            )
          }
        }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = res.message) }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = res.message) }
        BackendResult.NotConfigured ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = "Supabase not configured.") }
      }
    }
  }

  fun declineValidatedSupervisorInvitation() {
    if (_uiState.value.isSubmitting) return
    val session = _uiState.value.session ?: return
    val invite = _uiState.value.validatedInvitation ?: return
    val tokenHash = _uiState.value.validatedTokenHash ?: return

    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
      when (val res = repository.declineSupervisorInvitation(session, invite, tokenHash)) {
        is BackendResult.Success -> {
          preferences.clearPendingSupervisorInvitation()
          _uiState.update {
            it.copy(
              isSubmitting = false,
              validatedInvitation = null,
              validatedTokenHash = null,
              entryDestination = EntryAuthDestination.LANDING,
              supervisorStep = SupervisorOnboardingStep.INVITATION_LOOKUP,
              infoMessage = "Invitation declined.",
            )
          }
        }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = res.message) }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = res.message) }
        BackendResult.NotConfigured ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = "Supabase not configured.") }
      }
    }
  }

  fun confirmSupervisorProfile(fullName: String, phone: String) {
    if (_uiState.value.isSubmitting) return
    val session = _uiState.value.session ?: return
    val membership = _uiState.value.activeMembership ?: return
    val cleanName = fullName.trim()
    if (cleanName.length < 2) {
      _uiState.update { it.copy(errorMessage = "Enter your full name to continue.") }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
      when (
        val res =
          repository.upsertProfile(
            session = session,
            fullName = cleanName,
            phone = phone.trim().takeIf { it.isNotEmpty() },
            avatarUrl = _uiState.value.userProfile?.avatarUrl,
          )
      ) {
        is BackendResult.Success -> {
          _uiState.update {
            it.copy(
              isSubmitting = false,
              userProfile = res.data,
              supervisorProfileConfirmed = true,
              supervisorStep = SupervisorOnboardingStep.DASHBOARD,
              workspaceMode = ActiveWorkspaceMode.SUPERVISOR_DASHBOARD,
            )
          }
          refreshSupervisorDashboard(membership)
        }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = res.message) }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = res.message) }
        BackendResult.NotConfigured ->
          _uiState.update { it.copy(isSubmitting = false, errorMessage = "Supabase not configured.") }
      }
    }
  }

  fun uploadSupervisorProfilePhoto(context: Context, uri: Uri) {
    val session = _uiState.value.session ?: return
    val currentName = _uiState.value.userProfile?.fullName ?: session.email
    viewModelScope.launch {
      when (
        val processed = ImageProcessor.processPickedUri(context, uri, ImageUploadPreset.PROFILE_PHOTO)
      ) {
        is ImageProcessingResult.Success -> {
          _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
          when (
            val res =
              repository.uploadSupervisorAvatar(
                session = session,
                userId = session.userId,
                fullName = currentName,
                email = session.email,
                imageFile = processed.asset.file,
                mimeType = processed.asset.mimeType,
              )
          ) {
            is BackendResult.Success -> {
              processed.asset.file.delete()
              _uiState.update {
                it.copy(
                  isSubmitting = false,
                  userProfile = res.data,
                  infoMessage = "Supervisor profile photo updated.",
                )
              }
            }
            is BackendResult.NetworkFailure -> {
              processed.asset.file.delete()
              _uiState.update { it.copy(isSubmitting = false, errorMessage = res.message) }
            }
            is BackendResult.ApiFailure -> {
              processed.asset.file.delete()
              _uiState.update { it.copy(isSubmitting = false, errorMessage = res.message) }
            }
            BackendResult.NotConfigured -> {
              processed.asset.file.delete()
              _uiState.update { it.copy(isSubmitting = false, errorMessage = "Supabase not configured.") }
            }
          }
        }
        is ImageProcessingResult.ValidationError ->
          _uiState.update { it.copy(errorMessage = processed.message) }
      }
    }
  }

  fun refreshSupervisorDashboard(membershipOverride: OrganizationMemberDto? = null) {
    val session = _uiState.value.session ?: return
    val membership = membershipOverride ?: _uiState.value.activeMembership ?: return
    viewModelScope.launch {
      when (val res = repository.loadSupervisorDashboard(session, membership)) {
        is BackendResult.Success ->
          _uiState.update {
            it.copy(
              supervisorDashboard = res.data,
              activeOrganization = res.data.organization,
            )
          }
        is BackendResult.NetworkFailure ->
          _uiState.update { it.copy(errorMessage = res.message) }
        is BackendResult.ApiFailure ->
          _uiState.update { it.copy(errorMessage = res.message) }
        BackendResult.NotConfigured -> Unit
      }
    }
  }

  class Factory(private val appContext: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      val capabilityManager = AndroidDeviceCapabilityManager(appContext.applicationContext)
      val preferences = DataStorePreferencesRepository(appContext.applicationContext)
      val api = SupabaseClientFactory.create()
      val repository = SupabaseOnboardingRepository(api)
      val locationProvider =
        FusedAttendanceLocationProvider(appContext.applicationContext, capabilityManager)
      return OnboardingViewModel(
        repository = repository,
        preferences = preferences,
        locationProvider = locationProvider,
      ) as T
    }
  }
}
