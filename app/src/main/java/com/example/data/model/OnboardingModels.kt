package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class OrgMemberRole(val apiValue: String, val displayLabel: String) {
  OWNER("owner", "Organisation Owner"),
  ADMIN("admin", "Administrator"),
  SUPERVISOR("supervisor", "Supervisor"),
  EMPLOYEE("employee", "Employee");

  val isOwnerOrAdmin: Boolean
    get() = this == OWNER || this == ADMIN

  companion object {
    fun fromApi(value: String?): OrgMemberRole =
      when (value?.trim()?.lowercase()) {
        "owner" -> OWNER
        "admin", "administrator", "org_admin" -> ADMIN
        "supervisor" -> SUPERVISOR
        else -> EMPLOYEE
      }
  }
}

enum class EntryAuthDestination {
  LANDING,
  SIGN_IN,
  FORGOT_PASSWORD,
  OWNER_ONBOARDING,
  SUPERVISOR_ONBOARDING,
}

enum class OwnerOnboardingStep(val stepNumber: Int, val label: String) {
  ACCOUNT_REGISTRATION(1, "Account"),
  ORGANISATION_CREATION(2, "Organisation"),
  ORGANISATION_PROFILE(3, "Org Profile"),
  STRUCTURE_SETUP(4, "Setup & Invites"),
  DASHBOARD(5, "Dashboard"),
}

enum class SupervisorOnboardingStep(val stepNumber: Int, val label: String) {
  INVITATION_LOOKUP(1, "Invitation"),
  ACCOUNT_AUTH(2, "Account"),
  INVITATION_ACCEPTANCE(3, "Acceptance"),
  PROFILE_CONFIRMATION(4, "Profile"),
  DASHBOARD(5, "Dashboard"),
}

@JsonClass(generateAdapter = true)
data class AuthRecoverRequest(
  val email: String,
)

@JsonClass(generateAdapter = true)
data class OnboardingAuthSignUpMetadata(
  @Json(name = "full_name") val fullName: String,
  val phone: String? = null,
  @Json(name = "onboarding_flow") val onboardingFlow: String,
)

@JsonClass(generateAdapter = true)
data class OnboardingAuthSignUpRequest(
  val email: String,
  val password: String,
  val data: OnboardingAuthSignUpMetadata,
)

@JsonClass(generateAdapter = true)
data class AuthUserDetailDto(
  val id: String,
  val email: String? = null,
  @Json(name = "email_confirmed_at") val emailConfirmedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class ProfileDto(
  val id: String,
  @Json(name = "full_name") val fullName: String? = null,
  val email: String? = null,
  val phone: String? = null,
  @Json(name = "avatar_url") val avatarUrl: String? = null,
  @Json(name = "updated_at") val updatedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class UpsertProfileRequest(
  val id: String,
  @Json(name = "full_name") val fullName: String,
  val email: String,
  val phone: String? = null,
  @Json(name = "avatar_url") val avatarUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class CreateOrganizationRpcRequest(
  @Json(name = "p_name") val name: String,
  @Json(name = "p_country_code") val countryCode: String = "UG",
  @Json(name = "p_currency_code") val currencyCode: String = "UGX",
)

@JsonClass(generateAdapter = true)
data class OrganizationDto(
  val id: String,
  val name: String,
  @Json(name = "registration_number") val registrationNumber: String? = null,
  @Json(name = "country_code") val countryCode: String? = "UG",
  @Json(name = "currency_code") val currencyCode: String? = "UGX",
  val description: String? = null,
  @Json(name = "contact_email") val contactEmail: String? = null,
  @Json(name = "contact_phone") val contactPhone: String? = null,
  val address: String? = null,
  @Json(name = "logo_url") val logoUrl: String? = null,
  @Json(name = "owner_id") val ownerId: String? = null,
  @Json(name = "created_at") val createdAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class OrganizationProfilePatch(
  val description: String? = null,
  @Json(name = "contact_email") val contactEmail: String? = null,
  @Json(name = "contact_phone") val contactPhone: String? = null,
  val address: String? = null,
  @Json(name = "logo_url") val logoUrl: String? = null,
  @Json(name = "registration_number") val registrationNumber: String? = null,
)

@JsonClass(generateAdapter = true)
data class DepartmentDto(
  val id: String,
  @Json(name = "organization_id") val organizationId: String,
  val name: String,
  val description: String? = null,
  @Json(name = "created_at") val createdAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class CreateDepartmentRequest(
  @Json(name = "organization_id") val organizationId: String,
  val name: String,
  val description: String? = null,
)

@JsonClass(generateAdapter = true)
data class CreateWorkplaceRequest(
  @Json(name = "organization_id") val organizationId: String,
  val name: String,
  val address: String? = null,
  val latitude: Double,
  val longitude: Double,
  @Json(name = "geofence_radius_meters") val geofenceRadiusMeters: Double = 150.0,
  @Json(name = "max_location_accuracy_meters") val maxLocationAccuracyMeters: Double = 60.0,
)

@JsonClass(generateAdapter = true)
data class OrganizationMemberDto(
  val id: String,
  @Json(name = "organization_id") val organizationId: String,
  @Json(name = "user_id") val userId: String,
  val role: String,
  val status: String = "active",
  @Json(name = "department_id") val departmentId: String? = null,
  @Json(name = "workplace_id") val workplaceId: String? = null,
  @Json(name = "joined_at") val joinedAt: String? = null,
  val organizations: OrganizationDto? = null,
  val departments: DepartmentDto? = null,
  val workplaces: WorkplaceDto? = null,
  val profiles: ProfileDto? = null,
) {
  val parsedRole: OrgMemberRole
    get() = OrgMemberRole.fromApi(role)
}

@JsonClass(generateAdapter = true)
data class OrganizationSettingsDto(
  val id: String? = null,
  @Json(name = "organization_id") val organizationId: String,
  @Json(name = "require_biometric_for_attendance") val requireBiometricForAttendance: Boolean = false,
  @Json(name = "allow_check_in_outside_geofence") val allowCheckInOutsideGeofence: Boolean = false,
  @Json(name = "require_location_verification") val requireLocationVerification: Boolean = true,
  @Json(name = "working_days") val workingDays: List<String> = listOf("MON", "TUE", "WED", "THU", "FRI"),
  @Json(name = "work_start_time") val workStartTime: String? = "08:30",
  @Json(name = "work_end_time") val workEndTime: String? = "17:00",
  @Json(name = "late_grace_minutes") val lateGraceMinutes: Int = 15,
)

@JsonClass(generateAdapter = true)
data class InvitationDto(
  val id: String,
  @Json(name = "organization_id") val organizationId: String,
  val email: String,
  val role: String,
  @Json(name = "department_id") val departmentId: String? = null,
  @Json(name = "workplace_id") val workplaceId: String? = null,
  @Json(name = "token_hash") val tokenHash: String? = null,
  val status: String = "pending",
  @Json(name = "invited_by") val invitedBy: String? = null,
  @Json(name = "expires_at") val expiresAt: String? = null,
  @Json(name = "accepted_at") val acceptedAt: String? = null,
  @Json(name = "created_at") val createdAt: String? = null,
  val organizations: OrganizationDto? = null,
  val departments: DepartmentDto? = null,
  val workplaces: WorkplaceDto? = null,
)

@JsonClass(generateAdapter = true)
data class CreateInvitationRequest(
  @Json(name = "organization_id") val organizationId: String,
  val email: String,
  val role: String,
  @Json(name = "department_id") val departmentId: String? = null,
  @Json(name = "workplace_id") val workplaceId: String? = null,
  @Json(name = "token_hash") val tokenHash: String,
  @Json(name = "invited_by") val invitedBy: String,
  @Json(name = "expires_at") val expiresAt: String,
  val status: String = "pending",
)

@JsonClass(generateAdapter = true)
data class ValidateInvitationRpcRequest(
  @Json(name = "p_token_hash") val tokenHash: String,
)

@JsonClass(generateAdapter = true)
data class AcceptInvitationRpcRequest(
  @Json(name = "p_token_hash") val tokenHash: String,
)

@JsonClass(generateAdapter = true)
data class DeclineInvitationRpcRequest(
  @Json(name = "p_token_hash") val tokenHash: String,
)

@JsonClass(generateAdapter = true)
data class InvitationStatusPatch(
  val status: String,
)

@JsonClass(generateAdapter = true)
data class EmployeeProfileRecordDto(
  val id: String,
  @Json(name = "organization_id") val organizationId: String? = null,
  @Json(name = "user_id") val userId: String? = null,
  @Json(name = "full_name") val fullName: String,
  @Json(name = "employee_code") val employeeCode: String? = null,
  @Json(name = "department_id") val departmentId: String? = null,
  val department: String? = null,
  val role: String? = null,
  @Json(name = "workplace_id") val workplaceId: String? = null,
  @Json(name = "profile_photo_url") val profilePhotoUrl: String? = null,
  val status: String? = "active",
)

@JsonClass(generateAdapter = true)
data class MembershipHistoryDto(
  val id: String,
  @Json(name = "organization_id") val organizationId: String,
  @Json(name = "user_id") val userId: String,
  val action: String,
  @Json(name = "previous_role") val previousRole: String? = null,
  @Json(name = "new_role") val newRole: String? = null,
  @Json(name = "created_at") val createdAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class AuditLogDto(
  val id: String,
  @Json(name = "organization_id") val organizationId: String,
  @Json(name = "actor_user_id") val actorUserId: String? = null,
  val action: String,
  @Json(name = "entity_type") val entityType: String? = null,
  @Json(name = "entity_id") val entityId: String? = null,
  @Json(name = "summary") val summary: String? = null,
  @Json(name = "created_at") val createdAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class CreateAuditLogRequest(
  @Json(name = "organization_id") val organizationId: String,
  @Json(name = "actor_user_id") val actorUserId: String,
  val action: String,
  @Json(name = "entity_type") val entityType: String,
  @Json(name = "entity_id") val entityId: String? = null,
  @Json(name = "summary") val summary: String? = null,
)

@JsonClass(generateAdapter = true)
data class LeaveRequestDto(
  val id: String,
  @Json(name = "organization_id") val organizationId: String? = null,
  @Json(name = "employee_id") val employeeId: String? = null,
  @Json(name = "department_id") val departmentId: String? = null,
  @Json(name = "workplace_id") val workplaceId: String? = null,
  @Json(name = "leave_type") val leaveType: String? = null,
  val status: String = "pending",
  @Json(name = "start_date") val startDate: String? = null,
  @Json(name = "end_date") val endDate: String? = null,
  val reason: String? = null,
  @Json(name = "days_count") val daysCount: Int? = null,
  @Json(name = "reviewer_note") val reviewerNote: String? = null,
  @Json(name = "created_at") val createdAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class UpdateAuthPasswordRequest(
  val password: String,
)

@JsonClass(generateAdapter = true)
data class AttendanceRecordPatchRequest(
  @Json(name = "review_status") val reviewStatus: String,
  @Json(name = "geofence_status") val geofenceStatus: String? = null,
  val notes: String? = null,
)

@JsonClass(generateAdapter = true)
data class CreateLeaveRequestPayload(
  @Json(name = "organization_id") val organizationId: String? = null,
  @Json(name = "employee_id") val employeeId: String,
  @Json(name = "department_id") val departmentId: String? = null,
  @Json(name = "workplace_id") val workplaceId: String? = null,
  @Json(name = "leave_type") val leaveType: String,
  @Json(name = "start_date") val startDate: String,
  @Json(name = "end_date") val endDate: String,
  @Json(name = "days_count") val daysCount: Int? = null,
  val reason: String,
  val status: String = "pending",
)

@JsonClass(generateAdapter = true)
data class LeaveRequestDecisionPatch(
  val status: String,
  @Json(name = "reviewed_by") val reviewedBy: String? = null,
)

data class SupabaseConnectionReport(
  val isConfigured: Boolean = false,
  val isConnected: Boolean = false,
  val projectHost: String? = null,
  val authEndpointReachable: Boolean = false,
  val restEndpointReachable: Boolean = false,
  val restApiStatus: String = "Not yet verified",
  val latencyMs: Long? = null,
  val statusSummary: String = "Checking Supabase configuration...",
  val guidance: String = "",
)

data class OwnerWorkspaceSnapshot(
  val organization: OrganizationDto,
  val ownerName: String,
  val settings: OrganizationSettingsDto?,
  val workplaces: List<WorkplaceDto>,
  val departments: List<DepartmentDto>,
  val members: List<OrganizationMemberDto>,
  val employees: List<EmployeeProfileRecordDto>,
  val pendingInvitations: List<InvitationDto>,
  val attendanceRecords: List<AttendanceRecordDto>,
  val leaveRequests: List<LeaveRequestDto>,
  val leaveFeatureAvailable: Boolean,
  val auditLogs: List<AuditLogDto>,
)

data class SupervisorWorkspaceSnapshot(
  val organization: OrganizationDto,
  val membership: OrganizationMemberDto,
  val supervisorProfile: ProfileDto?,
  val settings: OrganizationSettingsDto?,
  val departments: List<DepartmentDto>,
  val workplaces: List<WorkplaceDto>,
  val assignedEmployees: List<EmployeeProfileRecordDto>,
  val attendanceRecords: List<AttendanceRecordDto>,
  val leaveRequests: List<LeaveRequestDto>,
  val leaveFeatureAvailable: Boolean,
  val recentActivities: List<AuditLogDto>,
)

@JsonClass(generateAdapter = true)
data class PendingOwnerOnboarding(
  val email: String,
  val fullName: String,
  val phone: String?,
)

@JsonClass(generateAdapter = true)
data class PendingSupervisorInvitation(
  val tokenHash: String,
  val invitedEmail: String,
  val organizationId: String,
  val organizationName: String,
  val role: String,
  val departmentName: String?,
  val workplaceName: String?,
)

data class CreatedInvitationResult(
  val invitation: InvitationDto,
  val oneTimeRawToken: String,
  val oneTimeDeepLink: String,
)

data class OwnerDashboardData(
  val organization: OrganizationDto,
  val ownerName: String,
  val settings: OrganizationSettingsDto?,
  val workplaces: List<WorkplaceDto>,
  val departments: List<DepartmentDto>,
  val employees: List<EmployeeProfileRecordDto>,
  val pendingInvitations: List<InvitationDto>,
  val todayAttendanceCount: Int,
  val todayVerifiedCount: Int,
  val recentActivities: List<AuditLogDto>,
) {
  val setupCompletedSteps: Int
    get() {
      var done = 1 // Organisation created
      if (!organization.description.isNullOrBlank() || !organization.contactEmail.isNullOrBlank()) done++
      if (workplaces.isNotEmpty()) done++
      if (departments.isNotEmpty()) done++
      if (settings != null) done++
      if (pendingInvitations.isNotEmpty() || employees.isNotEmpty()) done++
      return done
    }
  val setupTotalSteps: Int get() = 6
}

data class SupervisorDashboardData(
  val organization: OrganizationDto,
  val membership: OrganizationMemberDto,
  val supervisorProfile: ProfileDto?,
  val assignedEmployees: List<EmployeeProfileRecordDto>,
  val todayAttendanceRecords: List<AttendanceRecordDto>,
  val presentEmployeesCount: Int,
  val lateArrivalsCount: Int,
  val incompleteRecordsCount: Int,
  val pendingReviewsCount: Int,
  val pendingLeaveRequests: List<LeaveRequestDto>,
  val leaveFeatureAvailable: Boolean,
  val recentActivities: List<AuditLogDto>,
)
