package com.example.data.remote

import com.example.BuildConfig
import com.example.data.model.AcceptInvitationRpcRequest
import com.example.data.model.AttendanceRecordDto
import com.example.data.model.AuditLogDto
import com.example.data.model.AuthRecoverRequest
import com.example.data.model.AuthSignUpRequest
import com.example.data.model.AuthSignUpResponse
import com.example.data.model.AuthTokenRequest
import com.example.data.model.AuthTokenResponse
import com.example.data.model.AuthUserDetailDto
import com.example.data.model.CreateAttendanceRecordRequest
import com.example.data.model.CreateAuditLogRequest
import com.example.data.model.CreateDepartmentRequest
import com.example.data.model.CreateEmployeeDocumentRequest
import com.example.data.model.CreateEmployeeRequest
import com.example.data.model.CreateInvitationRequest
import com.example.data.model.CreateLocationVerificationRequest
import com.example.data.model.CreateOrganizationRpcRequest
import com.example.data.model.CreateWorkplaceRequest
import com.example.data.model.DeclineInvitationRpcRequest
import com.example.data.model.DepartmentDto
import com.example.data.model.EmployeeDocumentDto
import com.example.data.model.EmployeeDto
import com.example.data.model.EmployeeProfilePhotoPatch
import com.example.data.model.EmployeeProfileRecordDto
import com.example.data.model.InvitationDto
import com.example.data.model.InvitationStatusPatch
import com.example.data.model.LeaveRequestDto
import com.example.data.model.LocationVerificationDto
import com.example.data.model.MembershipHistoryDto
import com.example.data.model.OnboardingAuthSignUpRequest
import com.example.data.model.OrganizationDto
import com.example.data.model.OrganizationMemberDto
import com.example.data.model.OrganizationProfilePatch
import com.example.data.model.OrganizationSettingsDto
import com.example.data.model.ProfileDto
import com.example.data.model.UpsertProfileRequest
import com.example.data.model.ValidateInvitationRpcRequest
import com.example.data.model.WorkplaceDto
import com.example.data.model.AttendanceRecordPatchRequest
import com.example.data.model.CreateLeaveRequestPayload
import com.example.data.model.LeaveRequestDecisionPatch
import com.example.data.model.UpdateAuthPasswordRequest
import com.example.data.model.VerifyTokenRequest
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

const val AUTH_REDIRECT_URL = "civictrack://auth/callback"
data class SupabaseEnvironment(
  val url: String = BuildConfig.SUPABASE_URL,
  val anonKey: String = BuildConfig.SUPABASE_ANON_KEY,
) {
  val normalizedUrl: String
    get() {
      var cleaned = url.trim().trimEnd('/')
      val serviceSuffixes =
        listOf("/rest/v1", "/auth/v1", "/storage/v1", "/functions/v1", "/realtime/v1")
      for (suffix in serviceSuffixes) {
        if (cleaned.endsWith(suffix, ignoreCase = true)) {
          cleaned = cleaned.dropLast(suffix.length).trimEnd('/')
          break
        }
      }
      return cleaned
    }

  val isJwtAnonKey: Boolean
    get() {
      val trimmed = anonKey.trim()
      return trimmed.count { it == '.' } == 2 && trimmed.startsWith("eyJ")
    }

  val isRecognizedKeyFormat: Boolean
    get() {
      val trimmed = anonKey.trim()
      return isJwtAnonKey || trimmed.startsWith("sb_publishable_") || trimmed.startsWith("test-") || trimmed.length >= 20
    }

  val isConfigured: Boolean
    get() =
      (normalizedUrl.startsWith("https://") || normalizedUrl.startsWith("http://")) &&
        !normalizedUrl.contains("your-project.supabase.co") &&
        anonKey.isNotBlank() &&
        anonKey != "your_supabase_anon_key"

  fun publicStorageUrl(bucket: String, objectPath: String): String =
    "$normalizedUrl/storage/v1/object/public/$bucket/$objectPath"
}

interface SupabaseApiService {
  @POST("auth/v1/signup")
  suspend fun signUp(
    @Body request: AuthSignUpRequest,
    @Query("redirect_to") redirectTo: String? = AUTH_REDIRECT_URL,
  ): AuthSignUpResponse

  @POST("auth/v1/signup")
  suspend fun signUpOnboarding(
    @Body request: OnboardingAuthSignUpRequest,
    @Query("redirect_to") redirectTo: String? = AUTH_REDIRECT_URL,
  ): AuthSignUpResponse

  @POST("auth/v1/recover")
  suspend fun recoverPassword(
    @Body request: AuthRecoverRequest,
    @Query("redirect_to") redirectTo: String? = AUTH_REDIRECT_URL,
  ): Response<ResponseBody>
  //@POST("auth/v1/signup")
  //suspend fun signUp(@Body request: AuthSignUpRequest): AuthSignUpResponse

  //@POST("auth/v1/signup")
  //suspend fun signUpOnboarding(@Body request: OnboardingAuthSignUpRequest): AuthSignUpResponse

  @POST("auth/v1/token")
  suspend fun signInWithPassword(
    @Query("grant_type") grantType: String = "password",
    @Body request: AuthTokenRequest,
  ): AuthTokenResponse

  //@POST("auth/v1/recover")
  //suspend fun recoverPassword(@Body request: AuthRecoverRequest): Response<ResponseBody>

  @POST("auth/v1/verify")
  suspend fun verifyAuthToken(@Body request: VerifyTokenRequest): AuthTokenResponse

  @GET("auth/v1/health")
  suspend fun checkAuthHealth(): Response<ResponseBody>

  @GET("auth/v1/settings")
  suspend fun checkAuthSettings(): Response<ResponseBody>

  @GET("rest/v1/organizations")
  suspend fun checkRestRoot(
    @Header("Authorization") bearerAuth: String,
    @Query("select") select: String = "id",
    @Query("limit") limit: Int = 1,
  ): Response<ResponseBody>

  @GET("auth/v1/user")
  suspend fun getCurrentAuthUser(
    @Header("Authorization") bearerAuth: String,
  ): AuthUserDetailDto

  @PUT("auth/v1/user")
  suspend fun updateAuthUser(
    @Header("Authorization") bearerAuth: String,
    @Body request: UpdateAuthPasswordRequest,
  ): AuthUserDetailDto

  // --- Profiles ---

  @GET("rest/v1/profiles")
  suspend fun getProfile(
    @Header("Authorization") bearerAuth: String,
    @Query("id") idFilter: String,
    @Query("select") select: String = "*",
  ): List<ProfileDto>

  @POST("rest/v1/profiles")
  suspend fun upsertProfile(
    @Header("Authorization") bearerAuth: String,
    @Header("Prefer") prefer: String = "resolution=merge-duplicates,return=representation",
    @Body body: UpsertProfileRequest,
  ): List<ProfileDto>

  // --- Protected create_organization RPC & Organizations ---

  @POST("rest/v1/rpc/create_organization")
  suspend fun createOrganizationRpc(
    @Header("Authorization") bearerAuth: String,
    @Body body: CreateOrganizationRpcRequest,
  ): Response<ResponseBody>

  @GET("rest/v1/organizations")
  suspend fun getOrganizations(
    @Header("Authorization") bearerAuth: String,
    @Query("id") idFilter: String? = null,
    @Query("owner_id") ownerIdFilter: String? = null,
    @Query("select") select: String = "*",
    @Query("order") order: String = "created_at.desc",
  ): List<OrganizationDto>

  @PATCH("rest/v1/organizations")
  suspend fun patchOrganization(
    @Header("Authorization") bearerAuth: String,
    @Header("Prefer") prefer: String = "return=representation",
    @Query("id") idFilter: String,
    @Body body: OrganizationProfilePatch,
  ): List<OrganizationDto>

  // --- Memberships ---

  @GET("rest/v1/organization_members")
  suspend fun getOrganizationMembers(
    @Header("Authorization") bearerAuth: String,
    @Query("user_id") userIdFilter: String? = null,
    @Query("organization_id") orgIdFilter: String? = null,
    @Query("select") select: String = "*,organizations(*),departments(*),workplaces(*),profiles(*)",
    @Query("order") order: String = "joined_at.desc",
  ): List<OrganizationMemberDto>

  // --- Organization Settings ---

  @GET("rest/v1/organization_settings")
  suspend fun getOrganizationSettings(
    @Header("Authorization") bearerAuth: String,
    @Query("organization_id") orgIdFilter: String,
    @Query("select") select: String = "*",
  ): List<OrganizationSettingsDto>

  @POST("rest/v1/organization_settings")
  suspend fun upsertOrganizationSettings(
    @Header("Authorization") bearerAuth: String,
    @Header("Prefer") prefer: String = "resolution=merge-duplicates,return=representation",
    @Body body: OrganizationSettingsDto,
  ): List<OrganizationSettingsDto>

  // --- Departments ---

  @GET("rest/v1/departments")
  suspend fun getDepartments(
    @Header("Authorization") bearerAuth: String,
    @Query("organization_id") orgIdFilter: String,
    @Query("select") select: String = "*",
    @Query("order") order: String = "name.asc",
  ): List<DepartmentDto>

  @POST("rest/v1/departments")
  suspend fun createDepartment(
    @Header("Authorization") bearerAuth: String,
    @Header("Prefer") prefer: String = "return=representation",
    @Body body: CreateDepartmentRequest,
  ): List<DepartmentDto>

  // --- Workplaces ---

  @GET("rest/v1/workplaces")
  suspend fun getWorkplaces(
    @Query("select") select: String = "*",
    @Query("order") order: String = "name.asc",
  ): List<WorkplaceDto>

  @GET("rest/v1/workplaces")
  suspend fun getOrganizationWorkplaces(
    @Header("Authorization") bearerAuth: String,
    @Query("organization_id") orgIdFilter: String,
    @Query("select") select: String = "*",
    @Query("order") order: String = "name.asc",
  ): List<WorkplaceDto>

  @POST("rest/v1/workplaces")
  suspend fun createWorkplace(
    @Header("Authorization") bearerAuth: String,
    @Header("Prefer") prefer: String = "return=representation",
    @Body body: CreateWorkplaceRequest,
  ): List<WorkplaceDto>

  // --- Invitations & Secure RPCs ---

  @GET("rest/v1/invitations")
  suspend fun getInvitations(
    @Header("Authorization") bearerAuth: String? = null,
    @Query("organization_id") orgIdFilter: String? = null,
    @Query("token_hash") tokenHashFilter: String? = null,
    @Query("status") statusFilter: String? = null,
    @Query("select") select: String = "*,organizations(*),departments(*),workplaces(*)",
    @Query("order") order: String = "created_at.desc",
  ): List<InvitationDto>

  @POST("rest/v1/invitations")
  suspend fun createInvitation(
    @Header("Authorization") bearerAuth: String,
    @Header("Prefer") prefer: String = "return=representation",
    @Body body: CreateInvitationRequest,
  ): List<InvitationDto>

  @PATCH("rest/v1/invitations")
  suspend fun patchInvitationStatus(
    @Header("Authorization") bearerAuth: String,
    @Header("Prefer") prefer: String = "return=representation",
    @Query("id") idFilter: String,
    @Body body: InvitationStatusPatch,
  ): List<InvitationDto>

  @POST("rest/v1/rpc/validate_invitation")
  suspend fun validateInvitationRpc(
    @Header("Authorization") bearerAuth: String? = null,
    @Body body: ValidateInvitationRpcRequest,
  ): Response<ResponseBody>

  @POST("rest/v1/rpc/accept_supervisor_invitation")
  suspend fun acceptSupervisorInvitationRpc(
    @Header("Authorization") bearerAuth: String,
    @Body body: AcceptInvitationRpcRequest,
  ): Response<ResponseBody>

  @POST("rest/v1/rpc/decline_supervisor_invitation")
  suspend fun declineSupervisorInvitationRpc(
    @Header("Authorization") bearerAuth: String,
    @Body body: DeclineInvitationRpcRequest,
  ): Response<ResponseBody>

  // --- Employee Profiles & Scoped Queries ---

  @GET("rest/v1/employee_profiles")
  suspend fun getEmployeeProfiles(
    @Header("Authorization") bearerAuth: String,
    @Query("organization_id") orgIdFilter: String,
    @Query("department_id") departmentIdFilter: String? = null,
    @Query("workplace_id") workplaceIdFilter: String? = null,
    @Query("select") select: String = "*",
    @Query("order") order: String = "full_name.asc",
  ): List<EmployeeProfileRecordDto>

  @GET("rest/v1/employees")
  suspend fun getOrgEmployeesFallback(
    @Header("Authorization") bearerAuth: String,
    @Query("workplace_id") workplaceIdFilter: String? = null,
    @Query("select") select: String = "*",
  ): List<EmployeeProfileRecordDto>

  @POST("rest/v1/employees")
  suspend fun createEmployee(
    @Header("Authorization") bearerAuth: String,
    @Header("Prefer") prefer: String = "return=representation",
    @Body body: CreateEmployeeRequest,
  ): List<EmployeeDto>

  @GET("rest/v1/employees")
  suspend fun getEmployeeByUserId(
    @Header("Authorization") bearerAuth: String,
    @Query("user_id") userIdFilter: String,
    @Query("select") select: String = "*,workplaces(*),attendance_settings(*)",
  ): List<EmployeeDto>

  @GET("rest/v1/attendance_records")
  suspend fun getRecentAttendance(
    @Header("Authorization") bearerAuth: String,
    @Query("employee_id") employeeIdFilter: String,
    @Query("select") select: String = "*,location_verifications(*)",
    @Query("order") order: String = "recorded_at.desc",
    @Query("limit") limit: Int = 15,
  ): List<AttendanceRecordDto>

  @GET("rest/v1/attendance_records")
  suspend fun getScopedAttendanceRecords(
    @Header("Authorization") bearerAuth: String,
    @Query("organization_id") orgIdFilter: String? = null,
    @Query("workplace_id") workplaceIdFilter: String? = null,
    @Query("department_id") departmentIdFilter: String? = null,
    @Query("recorded_at") recordedAtFilter: String? = null,
    @Query("select") select: String = "*,location_verifications(*)",
    @Query("order") order: String = "recorded_at.desc",
    @Query("limit") limit: Int = 100,
  ): List<AttendanceRecordDto>

  @POST("rest/v1/attendance_records")
  suspend fun createAttendanceRecord(
    @Header("Authorization") bearerAuth: String,
    @Header("Prefer") prefer: String = "return=representation",
    @Body body: CreateAttendanceRecordRequest,
  ): List<AttendanceRecordDto>

  @PATCH("rest/v1/attendance_records")
  suspend fun patchAttendanceRecord(
    @Header("Authorization") bearerAuth: String,
    @Header("Prefer") prefer: String = "return=representation",
    @Query("id") idFilter: String,
    @Body body: AttendanceRecordPatchRequest,
  ): List<AttendanceRecordDto>

  @POST("rest/v1/location_verifications")
  suspend fun createLocationVerification(
    @Header("Authorization") bearerAuth: String,
    @Header("Prefer") prefer: String = "return=representation",
    @Body body: CreateLocationVerificationRequest,
  ): List<LocationVerificationDto>

  @PATCH("rest/v1/employees")
  suspend fun updateEmployeeProfilePhoto(
    @Header("Authorization") bearerAuth: String,
    @Header("Prefer") prefer: String = "return=representation",
    @Query("id") employeeIdFilter: String,
    @Body body: EmployeeProfilePhotoPatch,
  ): List<EmployeeDto>

  @GET("rest/v1/employee_documents")
  suspend fun getEmployeeDocuments(
    @Header("Authorization") bearerAuth: String,
    @Query("employee_id") employeeIdFilter: String,
    @Query("order") order: String = "uploaded_at.desc",
  ): List<EmployeeDocumentDto>

  @POST("rest/v1/employee_documents")
  suspend fun createEmployeeDocument(
    @Header("Authorization") bearerAuth: String,
    @Header("Prefer") prefer: String = "return=representation",
    @Body body: CreateEmployeeDocumentRequest,
  ): List<EmployeeDocumentDto>

  @GET("rest/v1/leave_requests")
  suspend fun getLeaveRequests(
    @Header("Authorization") bearerAuth: String,
    @Query("organization_id") orgIdFilter: String? = null,
    @Query("department_id") departmentIdFilter: String? = null,
    @Query("workplace_id") workplaceIdFilter: String? = null,
    @Query("employee_id") employeeIdFilter: String? = null,
    @Query("status") statusFilter: String? = "eq.pending",
    @Query("select") select: String = "*",
  ): List<LeaveRequestDto>

  @POST("rest/v1/leave_requests")
  suspend fun createLeaveRequest(
    @Header("Authorization") bearerAuth: String,
    @Header("Prefer") prefer: String = "return=representation",
    @Body body: CreateLeaveRequestPayload,
  ): List<LeaveRequestDto>

  @PATCH("rest/v1/leave_requests")
  suspend fun patchLeaveRequest(
    @Header("Authorization") bearerAuth: String,
    @Header("Prefer") prefer: String = "return=representation",
    @Query("id") idFilter: String,
    @Body body: LeaveRequestDecisionPatch,
  ): List<LeaveRequestDto>

  @DELETE("rest/v1/leave_requests")
  suspend fun deleteLeaveRequest(
    @Header("Authorization") bearerAuth: String,
    @Query("id") idFilter: String,
  ): Response<ResponseBody>

  @GET("rest/v1/audit_logs")
  suspend fun getAuditLogs(
    @Header("Authorization") bearerAuth: String,
    @Query("organization_id") orgIdFilter: String,
    @Query("select") select: String = "*",
    @Query("order") order: String = "created_at.desc",
    @Query("limit") limit: Int = 20,
  ): List<AuditLogDto>

  @POST("rest/v1/audit_logs")
  suspend fun createAuditLog(
    @Header("Authorization") bearerAuth: String,
    @Header("Prefer") prefer: String = "return=representation",
    @Body body: CreateAuditLogRequest,
  ): List<AuditLogDto>

  @GET("rest/v1/membership_history")
  suspend fun getMembershipHistory(
    @Header("Authorization") bearerAuth: String,
    @Query("organization_id") orgIdFilter: String,
    @Query("select") select: String = "*",
    @Query("order") order: String = "created_at.desc",
    @Query("limit") limit: Int = 20,
  ): List<MembershipHistoryDto>

  @POST("storage/v1/object/{bucket}/{objectPath}")
  suspend fun uploadStorageObject(
    @Header("Authorization") bearerAuth: String,
    @Header("x-upsert") upsert: String = "true",
    @Path("bucket") bucket: String,
    @Path("objectPath", encoded = true) objectPath: String,
    @Body fileBody: RequestBody,
  ): Response<ResponseBody>
}

object SupabaseClientFactory {
  fun create(environment: SupabaseEnvironment = SupabaseEnvironment()): SupabaseApiService {
    val baseUrl =
      if (environment.normalizedUrl.startsWith("http")) {
        "${environment.normalizedUrl}/"
      } else {
        "https://your-project.supabase.co/"
      }

    val logging =
      HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }

    val okHttpClient =
      OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
          val original = chain.request()
          val trimmedKey = environment.anonKey.trim()
          val builder =
            original
              .newBuilder()
              .header("apikey", trimmedKey)
              .header("Accept", "application/json")
          val existingAuth = original.header("Authorization")
          if (existingAuth != null && (existingAuth.isBlank() || existingAuth.trim() == "Bearer" || existingAuth.trim() == "Bearer null")) {
            builder.removeHeader("Authorization")
          } else if (existingAuth == null && environment.isJwtAnonKey) {
            builder.header("Authorization", "Bearer $trimmedKey")
          }
          chain.proceed(builder.build())
        }
        .addInterceptor(logging)
        .build()

    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    return Retrofit.Builder()
      .baseUrl(baseUrl)
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(SupabaseApiService::class.java)
  }
}
