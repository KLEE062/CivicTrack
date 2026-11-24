package com.example.data.repository

import com.example.core.security.InvitationTokenUtils
import com.example.data.model.AcceptInvitationRpcRequest
import com.example.data.model.AttendanceRecordDto
import com.example.data.model.AttendanceRecordPatchRequest
import com.example.data.model.AuthRecoverRequest
import com.example.data.model.AuthSession
import com.example.data.model.AuthTokenRequest
import com.example.data.model.AuthUserDetailDto
import com.example.data.model.CreateAuditLogRequest
import com.example.data.model.CreateDepartmentRequest
import com.example.data.model.CreateInvitationRequest
import com.example.data.model.CreateLeaveRequestPayload
import com.example.data.model.CreateOrganizationRpcRequest
import com.example.data.model.CreateWorkplaceRequest
import com.example.data.model.CreatedInvitationResult
import com.example.data.model.DeclineInvitationRpcRequest
import com.example.data.model.DepartmentDto
import com.example.data.model.EmployeeProfileRecordDto
import com.example.data.model.InvitationDto
import com.example.data.model.InvitationStatusPatch
import com.example.data.model.LeaveRequestDecisionPatch
import com.example.data.model.LeaveRequestDto
import com.example.data.model.LocationVerificationOutcome
import com.example.data.model.OnboardingAuthSignUpMetadata
import com.example.data.model.OnboardingAuthSignUpRequest
import com.example.data.model.OrgMemberRole
import com.example.data.model.OrganizationDto
import com.example.data.model.OrganizationMemberDto
import com.example.data.model.OrganizationProfilePatch
import com.example.data.model.OrganizationSettingsDto
import com.example.data.model.OwnerDashboardData
import com.example.data.model.OwnerWorkspaceSnapshot
import com.example.data.model.ProfileDto
import com.example.data.model.SupabaseConnectionReport
import com.example.data.model.SupervisorDashboardData
import com.example.data.model.SupervisorWorkspaceSnapshot
import com.example.data.model.UpdateAuthPasswordRequest
import com.example.data.model.UpsertProfileRequest
import com.example.data.model.ValidateInvitationRpcRequest
import com.example.data.model.VerifyTokenRequest
import com.example.data.model.WorkplaceDto
import com.example.data.remote.SupabaseApiService
import com.example.data.remote.SupabaseEnvironment
import java.net.URI
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

sealed interface AccountSignUpOutcome {
  data class SignedIn(val session: AuthSession) : AccountSignUpOutcome
  data class EmailConfirmationRequired(val email: String) : AccountSignUpOutcome
}

data class ResolvedUserContext(
  val authUser: AuthUserDetailDto?,
  val profile: ProfileDto?,
  val memberships: List<OrganizationMemberDto>,
  val ownedOrganizations: List<OrganizationDto>,
) {
  val primaryMembership: OrganizationMemberDto?
    get() =
      memberships.firstOrNull { it.status.equals("active", ignoreCase = true) }
        ?: memberships.firstOrNull()
}

sealed interface InvitationValidationOutcome {
  data class Valid(
    val invitation: InvitationDto,
    val tokenHash: String,
  ) : InvitationValidationOutcome

  data class Invalid(val reason: String) : InvitationValidationOutcome
}

interface OnboardingRepository {
  fun isBackendConfigured(): Boolean

  suspend fun signUpAccount(
    fullName: String,
    email: String,
    phone: String?,
    password: String,
    onboardingFlow: String,
  ): BackendResult<AccountSignUpOutcome>

  suspend fun signIn(email: String, password: String): BackendResult<AuthSession>

  suspend fun requestPasswordReset(email: String): BackendResult<Unit>

  suspend fun resolveUserContext(session: AuthSession): BackendResult<ResolvedUserContext>

  suspend fun upsertProfile(
    session: AuthSession,
    fullName: String,
    phone: String?,
    avatarUrl: String? = null,
  ): BackendResult<ProfileDto>

  suspend fun createOrganizationViaRpc(
    session: AuthSession,
    name: String,
    registrationNumber: String?,
    countryCode: String,
    currencyCode: String,
  ): BackendResult<OrganizationMemberDto>

  suspend fun updateOrganizationProfile(
    session: AuthSession,
    organizationId: String,
    patch: OrganizationProfilePatch,
  ): BackendResult<OrganizationDto>

  suspend fun uploadOrganizationLogo(
    session: AuthSession,
    organizationId: String,
    imageFile: File,
    mimeType: String,
  ): BackendResult<OrganizationDto>

  suspend fun uploadSupervisorAvatar(
    session: AuthSession,
    userId: String,
    fullName: String,
    email: String,
    imageFile: File,
    mimeType: String,
  ): BackendResult<ProfileDto>

  suspend fun createWorkplace(
    session: AuthSession,
    request: CreateWorkplaceRequest,
  ): BackendResult<WorkplaceDto>

  suspend fun createDepartment(
    session: AuthSession,
    request: CreateDepartmentRequest,
  ): BackendResult<DepartmentDto>

  suspend fun saveOrganizationSettings(
    session: AuthSession,
    settings: OrganizationSettingsDto,
  ): BackendResult<OrganizationSettingsDto>

  suspend fun createInvitation(
    session: AuthSession,
    organizationId: String,
    email: String,
    role: OrgMemberRole,
    departmentId: String?,
    workplaceId: String?,
    expiresInDays: Long = 7,
  ): BackendResult<CreatedInvitationResult>

  suspend fun revokeInvitation(
    session: AuthSession,
    organizationId: String,
    invitationId: String,
  ): BackendResult<InvitationDto>

  suspend fun validateInvitationToken(
    session: AuthSession?,
    rawTokenOrLink: String,
  ): BackendResult<InvitationValidationOutcome>

  suspend fun acceptSupervisorInvitation(
    session: AuthSession,
    invitation: InvitationDto,
    tokenHash: String,
  ): BackendResult<OrganizationMemberDto>

  suspend fun declineSupervisorInvitation(
    session: AuthSession,
    invitation: InvitationDto,
    tokenHash: String,
  ): BackendResult<Unit>

  suspend fun loadOwnerDashboard(
    session: AuthSession,
    organization: OrganizationDto,
    ownerName: String,
  ): BackendResult<OwnerDashboardData>

  suspend fun loadSupervisorDashboard(
    session: AuthSession,
    membership: OrganizationMemberDto,
  ): BackendResult<SupervisorDashboardData>

  suspend fun checkSupabaseConnection(session: AuthSession? = null): SupabaseConnectionReport

  suspend fun verifyEmailTokenHash(
    tokenHash: String,
    type: String = "signup",
  ): BackendResult<AuthSession>

  suspend fun hydrateSession(
    accessToken: String,
    refreshToken: String?,
  ): BackendResult<AuthSession>

  suspend fun updatePassword(
    session: AuthSession,
    newPassword: String,
  ): BackendResult<Unit>

  suspend fun loadOwnerWorkspaceSnapshot(
    session: AuthSession,
    organization: OrganizationDto,
    ownerName: String,
  ): BackendResult<OwnerWorkspaceSnapshot>

  suspend fun loadSupervisorWorkspaceSnapshot(
    session: AuthSession,
    membership: OrganizationMemberDto,
  ): BackendResult<SupervisorWorkspaceSnapshot>

  suspend fun reviewAttendanceRecord(
    session: AuthSession,
    organizationId: String,
    recordId: String,
    geofenceStatus: String?,
    reviewStatus: String,
    notes: String?,
  ): BackendResult<AttendanceRecordDto>

  suspend fun submitLeaveRequest(
    session: AuthSession,
    payload: CreateLeaveRequestPayload,
  ): BackendResult<LeaveRequestDto>

  suspend fun decideLeaveRequest(
    session: AuthSession,
    organizationId: String,
    leaveId: String,
    approved: Boolean,
  ): BackendResult<LeaveRequestDto>

  suspend fun cancelLeaveRequest(
    session: AuthSession,
    leaveId: String,
  ): BackendResult<Unit>

  suspend fun loadEmployeeLeaveRequests(
    session: AuthSession,
    employeeId: String,
  ): BackendResult<List<LeaveRequestDto>>
}

class SupabaseOnboardingRepository(
  private val api: SupabaseApiService,
  private val environment: SupabaseEnvironment = SupabaseEnvironment(),
) : OnboardingRepository {

  companion object {
    const val PROFILE_BUCKET = "employee-profiles"
    const val ORG_LOGO_BUCKET = "org-logos"
  }

  private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  private val orgAdapter = moshi.adapter(OrganizationDto::class.java)
  private val orgListAdapter =
    moshi.adapter<List<OrganizationDto>>(
      Types.newParameterizedType(List::class.java, OrganizationDto::class.java)
    )
  private val inviteAdapter = moshi.adapter(InvitationDto::class.java)
  private val inviteListAdapter =
    moshi.adapter<List<InvitationDto>>(
      Types.newParameterizedType(List::class.java, InvitationDto::class.java)
    )

  override fun isBackendConfigured(): Boolean = environment.isConfigured

  override suspend fun signUpAccount(
    fullName: String,
    email: String,
    phone: String?,
    password: String,
    onboardingFlow: String,
  ): BackendResult<AccountSignUpOutcome> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      val cleanEmail = email.trim()
      val cleanName = fullName.trim()
      val cleanPhone = phone?.trim()?.takeIf { it.isNotEmpty() }

      val response =
        api.signUpOnboarding(
          OnboardingAuthSignUpRequest(
            email = cleanEmail,
            password = password,
            data =
              OnboardingAuthSignUpMetadata(
                fullName = cleanName,
                phone = cleanPhone,
                onboardingFlow = onboardingFlow,
              ),
          )
        )

      val token = response.accessToken
      val userId = response.user?.id
      if (!token.isNullOrBlank() && !userId.isNullOrBlank()) {
        val session =
          AuthSession(
            accessToken = token,
            userId = userId,
            email = response.user.email ?: cleanEmail,
          )
        runCatching {
          api.upsertProfile(
            bearerAuth = "Bearer ${session.accessToken}",
            body =
              UpsertProfileRequest(
                id = session.userId,
                fullName = cleanName,
                email = session.email,
                phone = cleanPhone,
              ),
          )
        }
        AccountSignUpOutcome.SignedIn(session)
      } else {
        AccountSignUpOutcome.EmailConfirmationRequired(cleanEmail)
      }
    }
  }

  override suspend fun signIn(email: String, password: String): BackendResult<AuthSession> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      val response = api.signInWithPassword(request = AuthTokenRequest(email.trim(), password))
      val userId =
        response.user?.id
          ?: throw IllegalStateException("Supabase sign-in response missing user ID.")
      AuthSession(
        accessToken = response.accessToken,
        userId = userId,
        email = response.user.email ?: email.trim(),
      )
    }
  }

  override suspend fun verifyEmailTokenHash(
    tokenHash: String,
    type: String,
  ): BackendResult<AuthSession> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      val response = api.verifyAuthToken(VerifyTokenRequest(tokenHash = tokenHash.trim(), type = type))
      val userId =
        response.user?.id
          ?: throw IllegalStateException("Supabase verify token response missing user ID.")
      AuthSession(
        accessToken = response.accessToken,
        refreshToken = response.refreshToken,
        userId = userId,
        email = response.user.email ?: "",
      )
    }
  }

  override suspend fun hydrateSession(
    accessToken: String,
    refreshToken: String?,
  ): BackendResult<AuthSession> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      val token = accessToken.trim()
      val user = api.getCurrentAuthUser("Bearer $token")
      AuthSession(
        accessToken = token,
        refreshToken = refreshToken,
        userId = user.id,
        email = user.email.orEmpty(),
      )
    }
  }

  override suspend fun requestPasswordReset(email: String): BackendResult<Unit> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      val resp = api.recoverPassword(AuthRecoverRequest(email.trim()))
      if (!resp.isSuccessful) {
        throw HttpException(resp)
      }
    }
  }

  override suspend fun resolveUserContext(
    session: AuthSession,
  ): BackendResult<ResolvedUserContext> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      val bearer = "Bearer ${session.accessToken}"
      val authUser = api.getCurrentAuthUser(bearer)
      val profile =
        runCatching {
          api.getProfile(bearerAuth = bearer, idFilter = "eq.${session.userId}").firstOrNull()
        }.getOrNull()

      val memberships = api.getOrganizationMembers(
        bearerAuth = bearer,
        userIdFilter = "eq.${session.userId}",
      )
      val ownedOrgs = api.getOrganizations(
        bearerAuth = bearer,
        ownerIdFilter = "eq.${session.userId}",
      )
      ResolvedUserContext(
        authUser = authUser,
        profile = profile,
        memberships = memberships,
        ownedOrganizations = ownedOrgs,
      )
    }
  }

  override suspend fun upsertProfile(
    session: AuthSession,
    fullName: String,
    phone: String?,
    avatarUrl: String?,
  ): BackendResult<ProfileDto> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      api
        .upsertProfile(
          bearerAuth = "Bearer ${session.accessToken}",
          body =
            UpsertProfileRequest(
              id = session.userId,
              fullName = fullName.trim(),
              email = session.email,
              phone = phone?.trim()?.takeIf { it.isNotEmpty() },
              avatarUrl = avatarUrl,
            ),
        )
        .firstOrNull()
        ?: ProfileDto(
          id = session.userId,
          fullName = fullName.trim(),
          email = session.email,
          phone = phone,
          avatarUrl = avatarUrl,
        )
    }
  }

  override suspend fun createOrganizationViaRpc(
    session: AuthSession,
    name: String,
    registrationNumber: String?,
    countryCode: String,
    currencyCode: String,
  ): BackendResult<OrganizationMemberDto> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      val bearer = "Bearer ${session.accessToken}"
      val cleanName = name.trim()

      // Guard against duplicate creation if interrupted before navigation completed
      val existingMembers =
        runCatching {
          api.getOrganizationMembers(bearerAuth = bearer, userIdFilter = "eq.${session.userId}")
        }.getOrDefault(emptyList())
      val existingOwnedMember =
        existingMembers.firstOrNull {
          it.parsedRole == OrgMemberRole.OWNER &&
            it.organizations?.name?.equals(cleanName, ignoreCase = true) == true
        }
      if (existingOwnedMember != null) {
        return@executeSafely existingOwnedMember
      }

      // Invoke protected existing RPC: public.create_organization(p_name, p_country_code, p_currency_code)
      val rpcResp =
        api.createOrganizationRpc(
          bearerAuth = bearer,
          body =
            CreateOrganizationRpcRequest(
              name = cleanName,
              countryCode = countryCode.trim().uppercase().ifBlank { "UG" },
              currencyCode = currencyCode.trim().uppercase().ifBlank { "UGX" },
            ),
        )
      if (!rpcResp.isSuccessful) {
        throw HttpException(rpcResp)
      }

      val rawBody = rpcResp.body()?.string()?.trim().orEmpty()
      val parsedOrgId = extractOrganizationId(rawBody)

      val cleanRegNumber = registrationNumber?.trim()?.takeIf { it.isNotEmpty() }
      if (cleanRegNumber != null && parsedOrgId != null) {
        runCatching {
          api.patchOrganization(
            bearerAuth = bearer,
            idFilter = "eq.$parsedOrgId",
            body = OrganizationProfilePatch(registrationNumber = cleanRegNumber),
          )
        }
      }

      val confirmedMembers =
        api.getOrganizationMembers(bearerAuth = bearer, userIdFilter = "eq.${session.userId}")
      val matchedMember =
        confirmedMembers.firstOrNull {
          parsedOrgId != null && it.organizationId == parsedOrgId
        }
          ?: confirmedMembers.firstOrNull { it.parsedRole == OrgMemberRole.OWNER }
          ?: run {
            val orgs =
              if (parsedOrgId != null) {
                api.getOrganizations(bearerAuth = bearer, idFilter = "eq.$parsedOrgId")
              } else {
                api.getOrganizations(bearerAuth = bearer, ownerIdFilter = "eq.${session.userId}")
              }
            val org =
              orgs.firstOrNull()
                ?: throw IllegalStateException(
                  "Organisation creation completed, but ownership membership could not be verified from Supabase."
                )
            OrganizationMemberDto(
              id = "owner-${org.id}",
              organizationId = org.id,
              userId = session.userId,
              role = OrgMemberRole.OWNER.apiValue,
              status = "active",
              organizations = org,
            )
          }

      logAuditSafely(
        bearer = bearer,
        orgId = matchedMember.organizationId,
        userId = session.userId,
        action = "ORGANIZATION_CREATED",
        entityType = "organizations",
        entityId = matchedMember.organizationId,
        summary = "Organisation '${matchedMember.organizations?.name ?: cleanName}' registered",
      )

      matchedMember
    }
  }

  private fun extractOrganizationId(rawBody: String): String? {
    if (rawBody.isBlank() || rawBody == "null") return null
    if (rawBody.startsWith("\"") && rawBody.endsWith("\"")) {
      return rawBody.trim('"').takeIf { it.isNotBlank() }
    }
    if (rawBody.startsWith("{")) {
      return runCatching { orgAdapter.fromJson(rawBody)?.id }.getOrNull()
    }
    if (rawBody.startsWith("[")) {
      return runCatching { orgListAdapter.fromJson(rawBody)?.firstOrNull()?.id }.getOrNull()
    }
    return rawBody.takeIf { it.length in 8..64 && !it.contains(" ") }
  }

  override suspend fun updateOrganizationProfile(
    session: AuthSession,
    organizationId: String,
    patch: OrganizationProfilePatch,
  ): BackendResult<OrganizationDto> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      val bearer = "Bearer ${session.accessToken}"
      val updated =
        api
          .patchOrganization(
            bearerAuth = bearer,
            idFilter = "eq.$organizationId",
            body = patch,
          )
          .firstOrNull()
          ?: throw IllegalStateException("Failed to update organisation profile.")
      logAuditSafely(
        bearer = bearer,
        orgId = organizationId,
        userId = session.userId,
        action = "ORGANIZATION_PROFILE_UPDATED",
        entityType = "organizations",
        entityId = organizationId,
        summary = "Organisation profile details updated",
      )
      updated
    }
  }

  override suspend fun uploadOrganizationLogo(
    session: AuthSession,
    organizationId: String,
    imageFile: File,
    mimeType: String,
  ): BackendResult<OrganizationDto> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      val bearer = "Bearer ${session.accessToken}"
      val ext = when {
        mimeType.contains("png", ignoreCase = true) -> "png"
        mimeType.contains("webp", ignoreCase = true) -> "webp"
        else -> "jpg"
      }
      val objectPath = "$organizationId/logo_${System.currentTimeMillis()}.$ext"
      val uploadResp =
        api.uploadStorageObject(
          bearerAuth = bearer,
          bucket = ORG_LOGO_BUCKET,
          objectPath = objectPath,
          fileBody = imageFile.asRequestBody(mimeType.toMediaType()),
        )
      if (!uploadResp.isSuccessful) {
        throw HttpException(uploadResp)
      }
      val publicUrl = environment.publicStorageUrl(ORG_LOGO_BUCKET, objectPath)
      api
        .patchOrganization(
          bearerAuth = bearer,
          idFilter = "eq.$organizationId",
          body = OrganizationProfilePatch(logoUrl = publicUrl),
        )
        .firstOrNull()
        ?: throw IllegalStateException("Logo uploaded but organisation record could not be updated.")
    }
  }

  override suspend fun uploadSupervisorAvatar(
    session: AuthSession,
    userId: String,
    fullName: String,
    email: String,
    imageFile: File,
    mimeType: String,
  ): BackendResult<ProfileDto> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      val bearer = "Bearer ${session.accessToken}"
      val objectPath = "supervisors/$userId/avatar_${System.currentTimeMillis()}.jpg"
      val uploadResp =
        api.uploadStorageObject(
          bearerAuth = bearer,
          bucket = PROFILE_BUCKET,
          objectPath = objectPath,
          fileBody = imageFile.asRequestBody(mimeType.toMediaType()),
        )
      if (!uploadResp.isSuccessful) {
        throw HttpException(uploadResp)
      }
      val publicUrl = environment.publicStorageUrl(PROFILE_BUCKET, objectPath)
      api
        .upsertProfile(
          bearerAuth = bearer,
          body =
            UpsertProfileRequest(
              id = userId,
              fullName = fullName.trim(),
              email = email,
              avatarUrl = publicUrl,
            ),
        )
        .firstOrNull()
        ?: ProfileDto(id = userId, fullName = fullName, email = email, avatarUrl = publicUrl)
    }
  }

  override suspend fun createWorkplace(
    session: AuthSession,
    request: CreateWorkplaceRequest,
  ): BackendResult<WorkplaceDto> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      val bearer = "Bearer ${session.accessToken}"
      val created =
        api.createWorkplace(bearerAuth = bearer, body = request).firstOrNull()
          ?: throw IllegalStateException("Supabase did not return the created workplace.")
      logAuditSafely(
        bearer = bearer,
        orgId = request.organizationId,
        userId = session.userId,
        action = "WORKPLACE_CREATED",
        entityType = "workplaces",
        entityId = created.id,
        summary = "Added workplace '${created.name}' (${created.geofenceRadiusMeters.toInt()}m geofence)",
      )
      created
    }
  }

  override suspend fun createDepartment(
    session: AuthSession,
    request: CreateDepartmentRequest,
  ): BackendResult<DepartmentDto> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      val bearer = "Bearer ${session.accessToken}"
      val created =
        api.createDepartment(bearerAuth = bearer, body = request).firstOrNull()
          ?: throw IllegalStateException("Supabase did not return the created department.")
      logAuditSafely(
        bearer = bearer,
        orgId = request.organizationId,
        userId = session.userId,
        action = "DEPARTMENT_CREATED",
        entityType = "departments",
        entityId = created.id,
        summary = "Added department '${created.name}'",
      )
      created
    }
  }

  override suspend fun saveOrganizationSettings(
    session: AuthSession,
    settings: OrganizationSettingsDto,
  ): BackendResult<OrganizationSettingsDto> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      val bearer = "Bearer ${session.accessToken}"
      val saved =
        api.upsertOrganizationSettings(bearerAuth = bearer, body = settings).firstOrNull()
          ?: settings
      logAuditSafely(
        bearer = bearer,
        orgId = settings.organizationId,
        userId = session.userId,
        action = "ORGANIZATION_SETTINGS_UPDATED",
        entityType = "organization_settings",
        entityId = saved.id,
        summary = "Updated attendance rules & working schedule",
      )
      saved
    }
  }

  override suspend fun createInvitation(
    session: AuthSession,
    organizationId: String,
    email: String,
    role: OrgMemberRole,
    departmentId: String?,
    workplaceId: String?,
    expiresInDays: Long,
  ): BackendResult<CreatedInvitationResult> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    if (role == OrgMemberRole.OWNER) {
      return BackendResult.ApiFailure("Owner role cannot be assigned via member invitations.")
    }
    return executeSafely {
      val bearer = "Bearer ${session.accessToken}"
      val rawToken = InvitationTokenUtils.generateRawToken()
      val tokenHash = InvitationTokenUtils.sha256Hex(rawToken)
      val expiresAt = Instant.now().plus(expiresInDays, ChronoUnit.DAYS).toString()

      val created =
        api
          .createInvitation(
            bearerAuth = bearer,
            body =
              CreateInvitationRequest(
                organizationId = organizationId,
                email = email.trim().lowercase(),
                role = role.apiValue,
                departmentId = departmentId?.takeIf { it.isNotBlank() },
                workplaceId = workplaceId?.takeIf { it.isNotBlank() },
                tokenHash = tokenHash,
                invitedBy = session.userId,
                expiresAt = expiresAt,
                status = "pending",
              ),
          )
          .firstOrNull()
          ?: throw IllegalStateException("Supabase did not return the created invitation.")

      logAuditSafely(
        bearer = bearer,
        orgId = organizationId,
        userId = session.userId,
        action = "INVITATION_CREATED",
        entityType = "invitations",
        entityId = created.id,
        summary = "Invited ${created.email} as ${role.displayLabel}",
      )

      CreatedInvitationResult(
        invitation = created,
        oneTimeRawToken = rawToken,
        oneTimeDeepLink = InvitationTokenUtils.buildInviteDeepLink(rawToken),
      )
    }
  }

  override suspend fun revokeInvitation(
    session: AuthSession,
    organizationId: String,
    invitationId: String,
  ): BackendResult<InvitationDto> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      val bearer = "Bearer ${session.accessToken}"
      val patched =
        api
          .patchInvitationStatus(
            bearerAuth = bearer,
            idFilter = "eq.$invitationId",
            body = InvitationStatusPatch(status = "revoked"),
          )
          .firstOrNull()
          ?: throw IllegalStateException("Could not revoke invitation.")

      logAuditSafely(
        bearer = bearer,
        orgId = organizationId,
        userId = session.userId,
        action = "INVITATION_REVOKED",
        entityType = "invitations",
        entityId = invitationId,
        summary = "Revoked invitation for ${patched.email}",
      )
      patched
    }
  }

  override suspend fun validateInvitationToken(
    session: AuthSession?,
    rawTokenOrLink: String,
  ): BackendResult<InvitationValidationOutcome> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      val extractedToken = InvitationTokenUtils.extractInvitationToken(rawTokenOrLink)
      if (extractedToken.length < 8) {
        return@executeSafely InvitationValidationOutcome.Invalid(
          "Enter a valid invitation token or link provided by your organisation administrator."
        )
      }
      val tokenHash = InvitationTokenUtils.sha256Hex(extractedToken)
      val bearer = session?.let { "Bearer ${it.accessToken}" }

      // Prefer backend RPC validate_invitation if provisioned, otherwise query invitations by token_hash
      val invitation: InvitationDto? =
        fetchInvitationViaRpcOrTable(bearer = bearer, tokenHash = tokenHash)

      if (invitation == null) {
        return@executeSafely InvitationValidationOutcome.Invalid(
          "Invitation was not found or the token is invalid."
        )
      }

      if (invitation.organizationId.isBlank()) {
        return@executeSafely InvitationValidationOutcome.Invalid(
          "Invitation does not reference a valid CivicTrack organisation."
        )
      }

      val parsedRole = OrgMemberRole.fromApi(invitation.role)
      if (parsedRole == OrgMemberRole.OWNER || parsedRole == OrgMemberRole.EMPLOYEE) {
        if (parsedRole == OrgMemberRole.OWNER) {
          return@executeSafely InvitationValidationOutcome.Invalid(
            "Owner privileges cannot be claimed through supervisor onboarding."
          )
        }
      }

      when (invitation.status.lowercase()) {
        "revoked" ->
          return@executeSafely InvitationValidationOutcome.Invalid(
            "This invitation has been revoked by the organisation owner."
          )
        "declined" ->
          return@executeSafely InvitationValidationOutcome.Invalid(
            "This invitation was previously declined."
          )
        "accepted" ->
          return@executeSafely InvitationValidationOutcome.Invalid(
            "This invitation has already been accepted."
          )
        "expired" ->
          return@executeSafely InvitationValidationOutcome.Invalid(
            "This invitation has expired. Ask your organisation owner to issue a new one."
          )
      }

      val expiresAtInstant =
        invitation.expiresAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
      if (expiresAtInstant != null && expiresAtInstant.isBefore(Instant.now())) {
        return@executeSafely InvitationValidationOutcome.Invalid(
          "This invitation expired on ${invitation.expiresAt}. Request a new invitation."
        )
      }

      InvitationValidationOutcome.Valid(invitation = invitation, tokenHash = tokenHash)
    }
  }

  private suspend fun fetchInvitationViaRpcOrTable(
    bearer: String?,
    tokenHash: String,
  ): InvitationDto? {
    val rpcResp =
      runCatching {
        api.validateInvitationRpc(
          bearerAuth = bearer,
          body = ValidateInvitationRpcRequest(tokenHash = tokenHash),
        )
      }.getOrNull()

    if (rpcResp != null && rpcResp.isSuccessful) {
      val body = rpcResp.body()?.string()?.trim().orEmpty()
      if (body.startsWith("{")) {
        runCatching { inviteAdapter.fromJson(body) }.getOrNull()?.let { return it }
      } else if (body.startsWith("[")) {
        runCatching { inviteListAdapter.fromJson(body)?.firstOrNull() }.getOrNull()?.let { return it }
      }
    }

    // Fallback to RLS-scoped SELECT on invitations filtered by token_hash
    return api
      .getInvitations(
        bearerAuth = bearer,
        tokenHashFilter = "eq.$tokenHash",
      )
      .firstOrNull()
  }

  override suspend fun acceptSupervisorInvitation(
    session: AuthSession,
    invitation: InvitationDto,
    tokenHash: String,
  ): BackendResult<OrganizationMemberDto> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      if (!session.email.equals(invitation.email.trim(), ignoreCase = true)) {
        throw IllegalStateException(
          "Email mismatch: invitation is for ${invitation.email}, but you are signed in as ${session.email}."
        )
      }
      if (OrgMemberRole.fromApi(invitation.role) == OrgMemberRole.OWNER) {
        throw IllegalStateException("Owner role assignment is prohibited via invitation acceptance.")
      }

      val bearer = "Bearer ${session.accessToken}"
      // Prevent duplicate membership
      val existingMemberships =
        runCatching {
          api.getOrganizationMembers(
            bearerAuth = bearer,
            userIdFilter = "eq.${session.userId}",
            orgIdFilter = "eq.${invitation.organizationId}",
          )
        }.getOrDefault(emptyList())

      val alreadyActive =
        existingMemberships.firstOrNull {
          it.organizationId == invitation.organizationId &&
            it.status.equals("active", ignoreCase = true)
        }
      if (alreadyActive != null) {
        return@executeSafely alreadyActive
      }

      // Invoke atomic server-side acceptance RPC (backend validates token_hash, email, expiry & assigns role)
      val rpcResp =
        api.acceptSupervisorInvitationRpc(
          bearerAuth = bearer,
          body = AcceptInvitationRpcRequest(tokenHash = tokenHash),
        )
      if (!rpcResp.isSuccessful) {
        val code = rpcResp.code()
        val errorBody = rpcResp.errorBody()?.string()
        if (code == 404 || errorBody?.contains("PGRST202") == true) {
          throw IllegalStateException(
            "Server RPC accept_supervisor_invitation is required for secure atomic membership activation. Apply the proposed SQL migration on Supabase."
          )
        }
        throw HttpException(rpcResp)
      }

      // Confirm membership directly from backend rather than fabricating local state
      val confirmedMembers =
        api.getOrganizationMembers(
          bearerAuth = bearer,
          userIdFilter = "eq.${session.userId}",
          orgIdFilter = "eq.${invitation.organizationId}",
        )
      confirmedMembers.firstOrNull()
        ?: throw IllegalStateException(
          "Invitation acceptance did not produce an active organisation membership record."
        )
    }
  }

  override suspend fun declineSupervisorInvitation(
    session: AuthSession,
    invitation: InvitationDto,
    tokenHash: String,
  ): BackendResult<Unit> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      val bearer = "Bearer ${session.accessToken}"
      val rpcResp =
        runCatching {
          api.declineSupervisorInvitationRpc(
            bearerAuth = bearer,
            body = DeclineInvitationRpcRequest(tokenHash = tokenHash),
          )
        }.getOrNull()

      if (rpcResp != null && rpcResp.isSuccessful) {
        return@executeSafely Unit
      }

      api.patchInvitationStatus(
        bearerAuth = bearer,
        idFilter = "eq.${invitation.id}",
        body = InvitationStatusPatch(status = "declined"),
      )
    }
  }

  override suspend fun loadOwnerDashboard(
    session: AuthSession,
    organization: OrganizationDto,
    ownerName: String,
  ): BackendResult<OwnerDashboardData> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      val bearer = "Bearer ${session.accessToken}"
      val orgId = organization.id
      val freshOrg =
        runCatching {
          api.getOrganizations(bearerAuth = bearer, idFilter = "eq.$orgId").firstOrNull()
        }.getOrNull() ?: organization

      val settings =
        runCatching {
          api.getOrganizationSettings(bearerAuth = bearer, orgIdFilter = "eq.$orgId").firstOrNull()
        }.getOrNull()

      val workplaces =
        runCatching {
          api.getOrganizationWorkplaces(bearerAuth = bearer, orgIdFilter = "eq.$orgId")
        }.getOrDefault(emptyList())

      val departments =
        runCatching {
          api.getDepartments(bearerAuth = bearer, orgIdFilter = "eq.$orgId")
        }.getOrDefault(emptyList())

      val employees =
        runCatching {
          api.getEmployeeProfiles(bearerAuth = bearer, orgIdFilter = "eq.$orgId")
        }.getOrElse {
          runCatching { api.getOrgEmployeesFallback(bearerAuth = bearer) }.getOrDefault(emptyList())
        }

      val pendingInvites =
        runCatching {
          api.getInvitations(
            bearerAuth = bearer,
            orgIdFilter = "eq.$orgId",
            statusFilter = "eq.pending",
          )
        }.getOrDefault(emptyList())

      val todayPrefix = LocalDate.now(ZoneOffset.UTC).toString()
      val todayRecords =
        runCatching {
          api.getScopedAttendanceRecords(
            bearerAuth = bearer,
            orgIdFilter = "eq.$orgId",
            recordedAtFilter = "gte.${todayPrefix}T00:00:00Z",
          )
        }.getOrDefault(emptyList())

      val logs =
        runCatching {
          api.getAuditLogs(bearerAuth = bearer, orgIdFilter = "eq.$orgId", limit = 15)
        }.getOrDefault(emptyList())

      OwnerDashboardData(
        organization = freshOrg,
        ownerName = ownerName,
        settings = settings,
        workplaces = workplaces,
        departments = departments,
        employees = employees,
        pendingInvitations = pendingInvites,
        todayAttendanceCount = todayRecords.size,
        todayVerifiedCount =
          todayRecords.count { it.geofenceStatus == LocationVerificationOutcome.VERIFIED.apiValue },
        recentActivities = logs,
      )
    }
  }

  override suspend fun loadSupervisorDashboard(
    session: AuthSession,
    membership: OrganizationMemberDto,
  ): BackendResult<SupervisorDashboardData> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      val bearer = "Bearer ${session.accessToken}"
      val orgId = membership.organizationId
      val deptFilter = membership.departmentId?.let { "eq.$it" }
      val wpFilter = membership.workplaceId?.let { "eq.$it" }

      val org =
        membership.organizations
          ?: api.getOrganizations(bearerAuth = bearer, idFilter = "eq.$orgId").firstOrNull()
          ?: throw IllegalStateException("Assigned organisation record could not be loaded.")

      val profile =
        membership.profiles
          ?: runCatching {
            api.getProfile(bearerAuth = bearer, idFilter = "eq.${session.userId}").firstOrNull()
          }.getOrNull()

      val scopedEmployees =
        runCatching {
          api.getEmployeeProfiles(
            bearerAuth = bearer,
            orgIdFilter = "eq.$orgId",
            departmentIdFilter = deptFilter,
            workplaceIdFilter = wpFilter,
          )
        }.getOrDefault(emptyList())

      val todayPrefix = LocalDate.now(ZoneOffset.UTC).toString()
      val todayRecords =
        runCatching {
          api.getScopedAttendanceRecords(
            bearerAuth = bearer,
            orgIdFilter = "eq.$orgId",
            workplaceIdFilter = wpFilter,
            departmentIdFilter = deptFilter,
            recordedAtFilter = "gte.${todayPrefix}T00:00:00Z",
          )
        }.getOrDefault(emptyList())

      val employeeIdsWithCheckIn =
        todayRecords.filter { it.checkType == "CHECK_IN" }.map { it.employeeId }.toSet()
      val employeeIdsWithCheckOut =
        todayRecords.filter { it.checkType == "CHECK_OUT" }.map { it.employeeId }.toSet()

      val presentCount = employeeIdsWithCheckIn.size
      val lateCount = todayRecords.count { it.isLate }
      val incompleteCount = (employeeIdsWithCheckIn - employeeIdsWithCheckOut).size
      val pendingReviewsCount =
        todayRecords.count {
          it.reviewStatus.equals("pending", ignoreCase = true) ||
            it.geofenceStatus != LocationVerificationOutcome.VERIFIED.apiValue
        }

      var leaveFeatureAvailable = true
      val leaveRequests: List<LeaveRequestDto> =
        try {
          api.getLeaveRequests(
            bearerAuth = bearer,
            orgIdFilter = "eq.$orgId",
            departmentIdFilter = deptFilter,
            workplaceIdFilter = wpFilter,
          )
        } catch (_: HttpException) {
          leaveFeatureAvailable = false
          emptyList()
        } catch (_: Exception) {
          emptyList()
        }

      val activities =
        runCatching {
          api.getAuditLogs(bearerAuth = bearer, orgIdFilter = "eq.$orgId", limit = 15)
        }.getOrDefault(emptyList())

      SupervisorDashboardData(
        organization = org,
        membership = membership,
        supervisorProfile = profile,
        assignedEmployees = scopedEmployees,
        todayAttendanceRecords = todayRecords,
        presentEmployeesCount = presentCount,
        lateArrivalsCount = lateCount,
        incompleteRecordsCount = incompleteCount,
        pendingReviewsCount = pendingReviewsCount,
        pendingLeaveRequests = leaveRequests,
        leaveFeatureAvailable = leaveFeatureAvailable,
        recentActivities = activities,
      )
    }
  }

  override suspend fun checkSupabaseConnection(session: AuthSession?): SupabaseConnectionReport {
    if (!environment.isConfigured) {
      return SupabaseConnectionReport(
        isConfigured = false,
        isConnected = false,
        projectHost = null,
        authEndpointReachable = false,
        restEndpointReachable = false,
        restApiStatus = "Not yet verified",
        latencyMs = null,
        statusSummary = "Supabase Secrets Not Configured",
        guidance =
          "Configure SUPABASE_URL and SUPABASE_ANON_KEY in the AI Studio Secrets panel (.env file), then rebuild the app to connect to your live Supabase backend.",
      )
    }

    if (!environment.isRecognizedKeyFormat) {
      val host = runCatching { URI(environment.normalizedUrl).host }.getOrNull()
      return SupabaseConnectionReport(
        isConfigured = true,
        isConnected = false,
        projectHost = host,
        authEndpointReachable = false,
        restEndpointReachable = false,
        restApiStatus = "Not yet verified",
        latencyMs = null,
        statusSummary = "Invalid Supabase Publishable Key",
        guidance =
          "SUPABASE_ANON_KEY doesn't look like a valid Supabase key. Copy the full value (starting with sb_publishable_) from Project Settings → API Keys in your Supabase dashboard and update your .env file, then sync and rebuild.",
      )
    }

    val host = runCatching { URI(environment.normalizedUrl).host }.getOrNull()
    val startMs = System.currentTimeMillis()
    var authOk = false
    var restOk = false
    var restApiStatus = "Not yet verified"
    var errorDetail: String? = null

    try {
      val authResp = api.checkAuthHealth()
      authOk = authResp.isSuccessful
      if (!authOk) {
        errorDetail = "Auth health probe HTTP ${authResp.code()}"
      }
    } catch (e: Exception) {
      errorDetail = e.localizedMessage ?: "Auth endpoint unreachable"
    }

    if (session != null) {
      try {
        val restResp = api.checkRestRoot(bearerAuth = "Bearer ${session.accessToken}")
        restOk = restResp.isSuccessful || restResp.code() == 401 || restResp.code() == 403
        restApiStatus = if (restOk) "Reachable" else "Unreachable (HTTP ${restResp.code()})"
        if (!restOk && errorDetail == null) {
          errorDetail = "PostgREST probe HTTP ${restResp.code()}"
        }
      } catch (e: Exception) {
        restApiStatus = "Unreachable (${e.localizedMessage ?: "network error"})"
        if (errorDetail == null) {
          errorDetail = e.localizedMessage ?: "PostgREST endpoint unreachable"
        }
      }
    } else {
      restApiStatus = "Not yet verified"
    }

    val latency = (System.currentTimeMillis() - startMs).coerceAtLeast(1L)
    val connected = authOk
    val summary =
      when {
        connected -> "Connected to Supabase ($latency ms)"
        else -> "Supabase Unreachable"
      }
    val guidance =
      when {
        connected ->
          "Supabase Auth service (/auth/v1/health) is online for ${host ?: environment.normalizedUrl}. ${if (session != null) "PostgREST API status: $restApiStatus." else "PostgREST database checks will be verified after logging in."}"
        else ->
          "Probe issue${errorDetail?.let { ": $it" } ?: ""}. Verify SUPABASE_URL, SUPABASE_ANON_KEY in your .env file, and device network connectivity."
      }

    return SupabaseConnectionReport(
      isConfigured = true,
      isConnected = connected,
      projectHost = host,
      authEndpointReachable = authOk,
      restEndpointReachable = restOk,
      restApiStatus = restApiStatus,
      latencyMs = latency,
      statusSummary = summary,
      guidance = guidance,
    )
  }

  override suspend fun updatePassword(
    session: AuthSession,
    newPassword: String,
  ): BackendResult<Unit> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      api.updateAuthUser(
        bearerAuth = "Bearer ${session.accessToken}",
        request = UpdateAuthPasswordRequest(password = newPassword),
      )
    }
  }

  override suspend fun loadOwnerWorkspaceSnapshot(
    session: AuthSession,
    organization: OrganizationDto,
    ownerName: String,
  ): BackendResult<OwnerWorkspaceSnapshot> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      val bearer = "Bearer ${session.accessToken}"
      val orgId = organization.id

      val freshOrg =
        runCatching {
          api.getOrganizations(bearerAuth = bearer, idFilter = "eq.$orgId").firstOrNull()
        }.getOrNull() ?: organization

      val settings =
        runCatching {
          api.getOrganizationSettings(bearerAuth = bearer, orgIdFilter = "eq.$orgId").firstOrNull()
        }.getOrNull()

      val workplaces =
        runCatching {
          api.getOrganizationWorkplaces(bearerAuth = bearer, orgIdFilter = "eq.$orgId")
        }.getOrDefault(emptyList())

      val departments =
        runCatching {
          api.getDepartments(bearerAuth = bearer, orgIdFilter = "eq.$orgId")
        }.getOrDefault(emptyList())

      val members =
        runCatching {
          api.getOrganizationMembers(bearerAuth = bearer, orgIdFilter = "eq.$orgId")
        }.getOrDefault(emptyList())

      val employees =
        runCatching {
          api.getEmployeeProfiles(bearerAuth = bearer, orgIdFilter = "eq.$orgId")
        }.getOrElse {
          runCatching { api.getOrgEmployeesFallback(bearerAuth = bearer) }.getOrDefault(emptyList())
        }

      val pendingInvites =
        runCatching {
          api.getInvitations(
            bearerAuth = bearer,
            orgIdFilter = "eq.$orgId",
            statusFilter = "eq.pending",
          )
        }.getOrDefault(emptyList())

      val thirtyDaysAgo = LocalDate.now(ZoneOffset.UTC).minusDays(30).toString()
      val attendanceRecords =
        runCatching {
          api.getScopedAttendanceRecords(
            bearerAuth = bearer,
            orgIdFilter = "eq.$orgId",
            recordedAtFilter = "gte.${thirtyDaysAgo}T00:00:00Z",
          )
        }.getOrDefault(emptyList())

      var leaveFeatureAvailable = true
      val leaveRequests: List<LeaveRequestDto> =
        try {
          api.getLeaveRequests(
            bearerAuth = bearer,
            orgIdFilter = "eq.$orgId",
            statusFilter = null,
          )
        } catch (e: HttpException) {
          if (e.code() == 404) leaveFeatureAvailable = false
          emptyList()
        } catch (_: Exception) {
          emptyList()
        }

      val logs =
        runCatching {
          api.getAuditLogs(bearerAuth = bearer, orgIdFilter = "eq.$orgId", limit = 40)
        }.getOrDefault(emptyList())

      OwnerWorkspaceSnapshot(
        organization = freshOrg,
        ownerName = ownerName,
        settings = settings,
        workplaces = workplaces,
        departments = departments,
        members = members,
        employees = employees,
        pendingInvitations = pendingInvites,
        attendanceRecords = attendanceRecords,
        leaveRequests = leaveRequests,
        leaveFeatureAvailable = leaveFeatureAvailable,
        auditLogs = logs,
      )
    }
  }

  override suspend fun loadSupervisorWorkspaceSnapshot(
    session: AuthSession,
    membership: OrganizationMemberDto,
  ): BackendResult<SupervisorWorkspaceSnapshot> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      val bearer = "Bearer ${session.accessToken}"
      val orgId = membership.organizationId
      val deptFilter = membership.departmentId?.let { "eq.$it" }
      val wpFilter = membership.workplaceId?.let { "eq.$it" }

      val org =
        membership.organizations
          ?: api.getOrganizations(bearerAuth = bearer, idFilter = "eq.$orgId").firstOrNull()
          ?: throw IllegalStateException("Assigned organisation record could not be loaded.")

      val profile =
        membership.profiles
          ?: runCatching {
            api.getProfile(bearerAuth = bearer, idFilter = "eq.${session.userId}").firstOrNull()
          }.getOrNull()

      val settings =
        runCatching {
          api.getOrganizationSettings(bearerAuth = bearer, orgIdFilter = "eq.$orgId").firstOrNull()
        }.getOrNull()

      val departments =
        runCatching {
          api.getDepartments(bearerAuth = bearer, orgIdFilter = "eq.$orgId")
        }.getOrDefault(emptyList())

      val workplaces =
        runCatching {
          api.getOrganizationWorkplaces(bearerAuth = bearer, orgIdFilter = "eq.$orgId")
        }.getOrDefault(emptyList())

      val scopedEmployees =
        runCatching {
          api.getEmployeeProfiles(
            bearerAuth = bearer,
            orgIdFilter = "eq.$orgId",
            departmentIdFilter = deptFilter,
            workplaceIdFilter = wpFilter,
          )
        }.getOrDefault(emptyList())

      val thirtyDaysAgo = LocalDate.now(ZoneOffset.UTC).minusDays(30).toString()
      val attendanceRecords =
        runCatching {
          api.getScopedAttendanceRecords(
            bearerAuth = bearer,
            orgIdFilter = "eq.$orgId",
            workplaceIdFilter = wpFilter,
            departmentIdFilter = deptFilter,
            recordedAtFilter = "gte.${thirtyDaysAgo}T00:00:00Z",
          )
        }.getOrDefault(emptyList())

      var leaveFeatureAvailable = true
      val leaveRequests: List<LeaveRequestDto> =
        try {
          api.getLeaveRequests(
            bearerAuth = bearer,
            orgIdFilter = "eq.$orgId",
            departmentIdFilter = deptFilter,
            workplaceIdFilter = wpFilter,
            statusFilter = null,
          )
        } catch (e: HttpException) {
          if (e.code() == 404) leaveFeatureAvailable = false
          emptyList()
        } catch (_: Exception) {
          emptyList()
        }

      val logs =
        runCatching {
          api.getAuditLogs(bearerAuth = bearer, orgIdFilter = "eq.$orgId", limit = 25)
        }.getOrDefault(emptyList())

      SupervisorWorkspaceSnapshot(
        organization = org,
        membership = membership,
        supervisorProfile = profile,
        settings = settings,
        departments = departments,
        workplaces = workplaces,
        assignedEmployees = scopedEmployees,
        attendanceRecords = attendanceRecords,
        leaveRequests = leaveRequests,
        leaveFeatureAvailable = leaveFeatureAvailable,
        recentActivities = logs,
      )
    }
  }

  override suspend fun reviewAttendanceRecord(
    session: AuthSession,
    organizationId: String,
    recordId: String,
    geofenceStatus: String?,
    reviewStatus: String,
    notes: String?,
  ): BackendResult<AttendanceRecordDto> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      val bearer = "Bearer ${session.accessToken}"
      val updated =
        api
          .patchAttendanceRecord(
            bearerAuth = bearer,
            idFilter = "eq.$recordId",
            body =
              AttendanceRecordPatchRequest(
                geofenceStatus = geofenceStatus,
                reviewStatus = reviewStatus,
                notes = notes?.trim()?.takeIf { it.isNotEmpty() },
              ),
          )
          .firstOrNull()
          ?: throw IllegalStateException("Attendance record update did not return a row.")

      logAuditSafely(
        bearer = bearer,
        orgId = organizationId,
        userId = session.userId,
        action = "ATTENDANCE_REVIEWED",
        entityType = "attendance_records",
        entityId = recordId,
        summary = "Marked attendance record ${recordId.take(8)} as $reviewStatus",
      )
      updated
    }
  }

  override suspend fun submitLeaveRequest(
    session: AuthSession,
    payload: CreateLeaveRequestPayload,
  ): BackendResult<LeaveRequestDto> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      val bearer = "Bearer ${session.accessToken}"
      val created =
        api.createLeaveRequest(bearerAuth = bearer, body = payload).firstOrNull()
          ?: throw IllegalStateException("Leave request submission did not return a record.")
      payload.organizationId?.takeIf { it.isNotBlank() }?.let { orgId ->
        logAuditSafely(
          bearer = bearer,
          orgId = orgId,
          userId = session.userId,
          action = "LEAVE_SUBMITTED",
          entityType = "leave_requests",
          entityId = created.id,
          summary = "Submitted ${payload.leaveType} leave request (${payload.startDate} to ${payload.endDate})",
        )
      }
      created
    }
  }

  override suspend fun decideLeaveRequest(
    session: AuthSession,
    organizationId: String,
    leaveId: String,
    approved: Boolean,
  ): BackendResult<LeaveRequestDto> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      val bearer = "Bearer ${session.accessToken}"
      val status = if (approved) "approved" else "rejected"
      val updated =
        api
          .patchLeaveRequest(
            bearerAuth = bearer,
            idFilter = "eq.$leaveId",
            body = LeaveRequestDecisionPatch(status = status, reviewedBy = session.userId),
          )
          .firstOrNull()
          ?: throw IllegalStateException("Leave decision update did not return a record.")

      logAuditSafely(
        bearer = bearer,
        orgId = organizationId,
        userId = session.userId,
        action = if (approved) "LEAVE_APPROVED" else "LEAVE_REJECTED",
        entityType = "leave_requests",
        entityId = leaveId,
        summary = "Marked leave request ${leaveId.take(8)} as $status",
      )
      updated
    }
  }

  override suspend fun cancelLeaveRequest(
    session: AuthSession,
    leaveId: String,
  ): BackendResult<Unit> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      val resp =
        api.deleteLeaveRequest(
          bearerAuth = "Bearer ${session.accessToken}",
          idFilter = "eq.$leaveId",
        )
      if (!resp.isSuccessful) {
        throw HttpException(resp)
      }
    }
  }

  override suspend fun loadEmployeeLeaveRequests(
    session: AuthSession,
    employeeId: String,
  ): BackendResult<List<LeaveRequestDto>> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      try {
        api.getLeaveRequests(
          bearerAuth = "Bearer ${session.accessToken}",
          employeeIdFilter = "eq.$employeeId",
          statusFilter = null,
        )
      } catch (e: HttpException) {
        if (e.code() == 404) emptyList() else throw e
      }
    }
  }

  private suspend fun logAuditSafely(
    bearer: String,
    orgId: String,
    userId: String,
    action: String,
    entityType: String,
    entityId: String?,
    summary: String,
  ) {
    runCatching {
      api.createAuditLog(
        bearerAuth = bearer,
        body =
          CreateAuditLogRequest(
            organizationId = orgId,
            actorUserId = userId,
            action = action,
            entityType = entityType,
            entityId = entityId,
            summary = summary,
          ),
      )
    }
  }

  private suspend fun <T> executeSafely(block: suspend () -> T): BackendResult<T> {
    return try {
      BackendResult.Success(block())
    } catch (e: IOException) {
      BackendResult.NetworkFailure(
        e.localizedMessage ?: "Network unavailable while contacting CivicTrack server."
      )
    } catch (e: HttpException) {
      val errorBody = e.response()?.errorBody()?.string()
      BackendResult.ApiFailure(
        message = InvitationTokenUtils.parseReadableBackendError(errorBody, e.code()),
        code = e.code(),
      )
    } catch (e: Exception) {
      BackendResult.ApiFailure(e.localizedMessage ?: "Unexpected backend error.")
    }
  }
}
