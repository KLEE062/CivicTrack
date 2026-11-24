package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class AttendanceCheckType(val apiValue: String, val displayLabel: String) {
  CHECK_IN("CHECK_IN", "Check In"),
  CHECK_OUT("CHECK_OUT", "Check Out"),
}

enum class LocationVerificationOutcome(val apiValue: String) {
  VERIFIED("VERIFIED"),
  OUTSIDE_GEOFENCE("OUTSIDE_GEOFENCE"),
  INSUFFICIENT_ACCURACY("INSUFFICIENT_ACCURACY"),
  LOCATION_UNAVAILABLE("LOCATION_UNAVAILABLE"),
  PERMISSION_DENIED("PERMISSION_DENIED"),
}

enum class DocumentCaptureCategory(val apiValue: String, val label: String) {
  PROFILE_PHOTO("PROFILE_PHOTO", "Employee Profile Photo"),
  ID_BADGE("ID_BADGE", "Municipal ID / Badge"),
  SITE_PERMIT("SITE_PERMIT", "Workforce Site Permit"),
  MEDICAL_CLEARANCE("MEDICAL_CLEARANCE", "Safety / Clearance Form"),
}

@JsonClass(generateAdapter = true)
data class AuthTokenRequest(
  val email: String,
  val password: String,
)

@JsonClass(generateAdapter = true)
data class AuthSignUpUserMetadata(
  @Json(name = "full_name") val fullName: String,
  @Json(name = "employee_code") val employeeCode: String,
  val department: String? = null,
  val role: String? = null,
  @Json(name = "workplace_id") val workplaceId: String? = null,
)

@JsonClass(generateAdapter = true)
data class AuthSignUpRequest(
  val email: String,
  val password: String,
  val data: AuthSignUpUserMetadata? = null,
)

@JsonClass(generateAdapter = true)
data class AuthUserDto(
  val id: String,
  val email: String? = null,
)

@JsonClass(generateAdapter = true)
data class AuthTokenResponse(
  @Json(name = "access_token") val accessToken: String,
  @Json(name = "refresh_token") val refreshToken: String? = null,
  @Json(name = "token_type") val tokenType: String? = null,
  @Json(name = "expires_in") val expiresIn: Long? = null,
  val user: AuthUserDto? = null,
)

@JsonClass(generateAdapter = true)
data class AuthSignUpResponse(
  @Json(name = "access_token") val accessToken: String? = null,
  @Json(name = "refresh_token") val refreshToken: String? = null,
  @Json(name = "token_type") val tokenType: String? = null,
  @Json(name = "expires_in") val expiresIn: Long? = null,
  val user: AuthUserDto? = null,
)

@JsonClass(generateAdapter = true)
data class VerifyTokenRequest(
  @Json(name = "token_hash") val tokenHash: String,
  val type: String = "signup",
)

data class AuthSession(
  val accessToken: String,
  val refreshToken: String? = null,
  val userId: String,
  val email: String,
)

@JsonClass(generateAdapter = true)
data class CreateEmployeeRequest(
  @Json(name = "user_id") val userId: String,
  @Json(name = "full_name") val fullName: String,
  @Json(name = "employee_code") val employeeCode: String,
  val department: String? = null,
  val role: String? = null,
  @Json(name = "workplace_id") val workplaceId: String? = null,
)

data class PendingEmployeeProfile(
  val email: String,
  val fullName: String,
  val employeeCode: String,
  val department: String?,
  val role: String?,
  val workplaceId: String?,
)

sealed interface RegistrationOutcome {
  data class SignedIn(
    val session: AuthSession,
    val employee: EmployeeDto?,
  ) : RegistrationOutcome

  data class EmailConfirmationRequired(val email: String) : RegistrationOutcome
}

@JsonClass(generateAdapter = true)
data class WorkplaceDto(
  val id: String,
  val name: String,
  val address: String? = null,
  val latitude: Double,
  val longitude: Double,
  @Json(name = "geofence_radius_meters") val geofenceRadiusMeters: Double = 150.0,
  @Json(name = "max_location_accuracy_meters") val maxLocationAccuracyMeters: Double = 60.0,
)

@JsonClass(generateAdapter = true)
data class AttendanceSettingsDto(
  @Json(name = "require_biometric_for_attendance") val requireBiometricForAttendance: Boolean = false,
  @Json(name = "allow_check_in_outside_geofence") val allowCheckInOutsideGeofence: Boolean = false,
  @Json(name = "require_location_verification") val requireLocationVerification: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class EmployeeDto(
  val id: String,
  @Json(name = "user_id") val userId: String,
  @Json(name = "full_name") val fullName: String,
  @Json(name = "employee_code") val employeeCode: String,
  val department: String? = null,
  val role: String? = null,
  @Json(name = "workplace_id") val workplaceId: String? = null,
  @Json(name = "profile_photo_path") val profilePhotoPath: String? = null,
  @Json(name = "profile_photo_url") val profilePhotoUrl: String? = null,
  val workplaces: WorkplaceDto? = null,
  @Json(name = "attendance_settings") val attendanceSettings: AttendanceSettingsDto? = null,
)

@JsonClass(generateAdapter = true)
data class CreateAttendanceRecordRequest(
  @Json(name = "employee_id") val employeeId: String,
  @Json(name = "workplace_id") val workplaceId: String,
  @Json(name = "check_type") val checkType: String,
  @Json(name = "recorded_at") val recordedAtIso: String,
  @Json(name = "biometric_verified_locally") val biometricVerifiedLocally: Boolean,
  @Json(name = "geofence_status") val geofenceStatus: String,
  val notes: String? = null,
)

@JsonClass(generateAdapter = true)
data class AttendanceRecordDto(
  val id: String,
  @Json(name = "employee_id") val employeeId: String,
  @Json(name = "workplace_id") val workplaceId: String,
  @Json(name = "check_type") val checkType: String,
  @Json(name = "recorded_at") val recordedAtIso: String,
  @Json(name = "biometric_verified_locally") val biometricVerifiedLocally: Boolean = false,
  @Json(name = "geofence_status") val geofenceStatus: String,
  val notes: String? = null,
  @Json(name = "organization_id") val organizationId: String? = null,
  @Json(name = "department_id") val departmentId: String? = null,
  @Json(name = "review_status") val reviewStatus: String? = null,
  @Json(name = "is_late") val isLate: Boolean = false,
  @Json(name = "location_verifications") val locationVerifications: List<LocationVerificationDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class CreateLocationVerificationRequest(
  @Json(name = "attendance_record_id") val attendanceRecordId: String,
  @Json(name = "employee_id") val employeeId: String,
  @Json(name = "workplace_id") val workplaceId: String,
  val latitude: Double?,
  val longitude: Double?,
  @Json(name = "accuracy_meters") val accuracyMeters: Double?,
  @Json(name = "location_timestamp") val locationTimestampIso: String?,
  @Json(name = "distance_meters") val distanceMeters: Double?,
  @Json(name = "permitted_radius_meters") val permittedRadiusMeters: Double,
  @Json(name = "verification_status") val verificationStatus: String,
  @Json(name = "failure_reason") val failureReason: String? = null,
)

@JsonClass(generateAdapter = true)
data class LocationVerificationDto(
  val id: String,
  @Json(name = "attendance_record_id") val attendanceRecordId: String,
  @Json(name = "employee_id") val employeeId: String,
  @Json(name = "workplace_id") val workplaceId: String,
  val latitude: Double? = null,
  val longitude: Double? = null,
  @Json(name = "accuracy_meters") val accuracyMeters: Double? = null,
  @Json(name = "location_timestamp") val locationTimestampIso: String? = null,
  @Json(name = "distance_meters") val distanceMeters: Double? = null,
  @Json(name = "permitted_radius_meters") val permittedRadiusMeters: Double? = null,
  @Json(name = "verification_status") val verificationStatus: String,
  @Json(name = "failure_reason") val failureReason: String? = null,
)

@JsonClass(generateAdapter = true)
data class EmployeeProfilePhotoPatch(
  @Json(name = "profile_photo_path") val profilePhotoPath: String,
  @Json(name = "profile_photo_url") val profilePhotoUrl: String,
)

@JsonClass(generateAdapter = true)
data class CreateEmployeeDocumentRequest(
  @Json(name = "employee_id") val employeeId: String,
  @Json(name = "document_type") val documentType: String,
  val title: String,
  @Json(name = "storage_bucket") val storageBucket: String,
  @Json(name = "storage_path") val storagePath: String,
  @Json(name = "mime_type") val mimeType: String,
  @Json(name = "file_size_bytes") val fileSizeBytes: Long,
  @Json(name = "uploaded_at") val uploadedAtIso: String,
)

@JsonClass(generateAdapter = true)
data class EmployeeDocumentDto(
  val id: String,
  @Json(name = "employee_id") val employeeId: String,
  @Json(name = "document_type") val documentType: String,
  val title: String,
  @Json(name = "storage_bucket") val storageBucket: String,
  @Json(name = "storage_path") val storagePath: String,
  @Json(name = "mime_type") val mimeType: String,
  @Json(name = "file_size_bytes") val fileSizeBytes: Long,
  @Json(name = "uploaded_at") val uploadedAtIso: String,
)

/**
 * Operation 1: Device-level Android BiometricPrompt authentication.
 * Explicitly distinct from centrally verified identity or workplace geofencing.
 */
sealed interface BiometricStepStatus {
  data object Idle : BiometricStepStatus
  data object NotRequired : BiometricStepStatus
  data object Prompting : BiometricStepStatus
  data class LocalDeviceAuthenticated(val methodLabel: String) : BiometricStepStatus
  data class Cancelled(val message: String) : BiometricStepStatus
  data class Unavailable(val message: String) : BiometricStepStatus
  data class Error(val message: String) : BiometricStepStatus
}

/**
 * Operation 2: Workplace geofence and GPS accuracy verification.
 */
sealed interface GeofenceStepStatus {
  data object Idle : GeofenceStepStatus
  data object AcquiringLocation : GeofenceStepStatus
  data class Verified(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double,
    val distanceMeters: Double,
    val permittedRadiusMeters: Double,
    val timestampIso: String,
  ) : GeofenceStepStatus

  data class OutsideGeofence(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double,
    val distanceMeters: Double,
    val permittedRadiusMeters: Double,
    val timestampIso: String,
  ) : GeofenceStepStatus

  data class InsufficientAccuracy(
    val accuracyMeters: Double,
    val maxAllowedMeters: Double,
  ) : GeofenceStepStatus

  data class LocationUnavailable(val reason: String) : GeofenceStepStatus
  data class PermissionDenied(val isPermanent: Boolean) : GeofenceStepStatus
  data object ServicesDisabled : GeofenceStepStatus
}

/**
 * Operation 3: Backend attendance and location_verifications persistence.
 */
sealed interface AttendanceSubmissionStatus {
  data object Idle : AttendanceSubmissionStatus
  data object Submitting : AttendanceSubmissionStatus
  data class Recorded(
    val attendanceRecordId: String,
    val locationVerificationId: String,
    val checkType: AttendanceCheckType,
    val geofenceOutcome: LocationVerificationOutcome,
    val recordedAtIso: String,
  ) : AttendanceSubmissionStatus

  data class NetworkError(val message: String) : AttendanceSubmissionStatus
  data class BackendError(val message: String) : AttendanceSubmissionStatus
}
