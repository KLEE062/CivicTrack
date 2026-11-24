package com.example.data.repository

import com.example.data.model.AttendanceCheckType
import com.example.data.model.AttendanceRecordDto
import com.example.data.model.AuthSession
import com.example.data.model.AuthSignUpRequest
import com.example.data.model.AuthSignUpUserMetadata
import com.example.data.model.AuthTokenRequest
import com.example.data.model.CreateAttendanceRecordRequest
import com.example.data.model.CreateEmployeeDocumentRequest
import com.example.data.model.CreateEmployeeRequest
import com.example.data.model.CreateLocationVerificationRequest
import com.example.data.model.DocumentCaptureCategory
import com.example.data.model.EmployeeDocumentDto
import com.example.data.model.EmployeeDto
import com.example.data.model.EmployeeProfilePhotoPatch
import com.example.data.model.LocationVerificationOutcome
import com.example.data.model.RegistrationOutcome
import com.example.data.model.WorkplaceDto
import com.example.data.remote.SupabaseApiService
import com.example.data.remote.SupabaseEnvironment
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import java.time.Instant

sealed interface BackendResult<out T> {
  data class Success<T>(val data: T) : BackendResult<T>
  data class NetworkFailure(val message: String) : BackendResult<Nothing>
  data class ApiFailure(val message: String, val code: Int? = null) : BackendResult<Nothing>
  data object NotConfigured : BackendResult<Nothing>
}

data class AttendanceSubmissionPayload(
  val employeeId: String,
  val workplaceId: String,
  val checkType: AttendanceCheckType,
  val biometricVerifiedLocally: Boolean,
  val latitude: Double?,
  val longitude: Double?,
  val accuracyMeters: Double?,
  val locationTimestampIso: String?,
  val distanceMeters: Double?,
  val permittedRadiusMeters: Double,
  val verificationOutcome: LocationVerificationOutcome,
  val failureReason: String? = null,
  val notes: String? = null,
)

data class RecordedAttendanceOutcome(
  val attendanceRecordId: String,
  val locationVerificationId: String,
  val recordedAtIso: String,
)

interface CivicTrackRepository {
  fun isBackendConfigured(): Boolean
  suspend fun loadWorkplaces(): BackendResult<List<WorkplaceDto>>
  suspend fun registerEmployee(
    email: String,
    password: String,
    fullName: String,
    employeeCode: String,
    department: String?,
    role: String?,
    workplaceId: String?,
  ): BackendResult<RegistrationOutcome>
  suspend fun createEmployeeProfile(
    session: AuthSession,
    request: CreateEmployeeRequest,
  ): BackendResult<EmployeeDto>
  suspend fun signIn(email: String, password: String): BackendResult<AuthSession>
  suspend fun loadEmployeeProfile(session: AuthSession): BackendResult<EmployeeDto>
  suspend fun loadRecentAttendance(
    session: AuthSession,
    employeeId: String,
  ): BackendResult<List<AttendanceRecordDto>>
  suspend fun submitAttendanceWithLocationVerification(
    session: AuthSession,
    payload: AttendanceSubmissionPayload,
  ): BackendResult<RecordedAttendanceOutcome>
  suspend fun uploadProfilePhoto(
    session: AuthSession,
    employeeId: String,
    imageFile: File,
    mimeType: String,
  ): BackendResult<EmployeeDto>
  suspend fun uploadEmployeeDocument(
    session: AuthSession,
    employeeId: String,
    category: DocumentCaptureCategory,
    title: String,
    imageFile: File,
    mimeType: String,
  ): BackendResult<EmployeeDocumentDto>
  suspend fun loadEmployeeDocuments(
    session: AuthSession,
    employeeId: String,
  ): BackendResult<List<EmployeeDocumentDto>>
}

class SupabaseCivicTrackRepository(
  private val api: SupabaseApiService,
  private val environment: SupabaseEnvironment = SupabaseEnvironment(),
) : CivicTrackRepository {

  companion object {
    const val PROFILE_BUCKET = "employee-profiles"
    const val DOCUMENTS_BUCKET = "employee-documents"
  }

  override fun isBackendConfigured(): Boolean = environment.isConfigured

  override suspend fun loadWorkplaces(): BackendResult<List<WorkplaceDto>> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely { api.getWorkplaces() }
  }

  override suspend fun registerEmployee(
    email: String,
    password: String,
    fullName: String,
    employeeCode: String,
    department: String?,
    role: String?,
    workplaceId: String?,
  ): BackendResult<RegistrationOutcome> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      val trimmedEmail = email.trim()
      val trimmedName = fullName.trim()
      val trimmedCode = employeeCode.trim()
      val cleanDept = department?.trim()?.takeIf { it.isNotEmpty() }
      val cleanRole = role?.trim()?.takeIf { it.isNotEmpty() }
      val cleanWorkplaceId = workplaceId?.trim()?.takeIf { it.isNotEmpty() }

      val response =
        api.signUp(
          AuthSignUpRequest(
            email = trimmedEmail,
            password = password,
            data =
              AuthSignUpUserMetadata(
                fullName = trimmedName,
                employeeCode = trimmedCode,
                department = cleanDept,
                role = cleanRole,
                workplaceId = cleanWorkplaceId,
              ),
          )
        )

      val accessToken = response.accessToken
      val userId = response.user?.id
      if (!accessToken.isNullOrBlank() && !userId.isNullOrBlank()) {
        val session =
          AuthSession(
            accessToken = accessToken,
            userId = userId,
            email = response.user.email ?: trimmedEmail,
          )
        val bearer = "Bearer ${session.accessToken}"
        val existing =
          api.getEmployeeByUserId(bearerAuth = bearer, userIdFilter = "eq.${session.userId}")
        val createdOrExisting =
          existing.firstOrNull()
            ?: run {
              api.createEmployee(
                bearerAuth = bearer,
                body =
                  CreateEmployeeRequest(
                    userId = session.userId,
                    fullName = trimmedName,
                    employeeCode = trimmedCode,
                    department = cleanDept,
                    role = cleanRole,
                    workplaceId = cleanWorkplaceId,
                  ),
              )
              api
                .getEmployeeByUserId(bearerAuth = bearer, userIdFilter = "eq.${session.userId}")
                .firstOrNull()
            }
        RegistrationOutcome.SignedIn(session = session, employee = createdOrExisting)
      } else {
        RegistrationOutcome.EmailConfirmationRequired(email = trimmedEmail)
      }
    }
  }

  override suspend fun createEmployeeProfile(
    session: AuthSession,
    request: CreateEmployeeRequest,
  ): BackendResult<EmployeeDto> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      val bearer = "Bearer ${session.accessToken}"
      api.createEmployee(bearerAuth = bearer, body = request)
      api
        .getEmployeeByUserId(bearerAuth = bearer, userIdFilter = "eq.${session.userId}")
        .firstOrNull()
        ?: throw IllegalStateException("Employee profile created but could not be reloaded.")
    }
  }

  override suspend fun signIn(email: String, password: String): BackendResult<AuthSession> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      val response = api.signInWithPassword(request = AuthTokenRequest(email.trim(), password))
      val userId =
        response.user?.id
          ?: throw IllegalStateException("Supabase auth response missing user identifier.")
      AuthSession(
        accessToken = response.accessToken,
        userId = userId,
        email = response.user.email ?: email.trim(),
      )
    }
  }

  override suspend fun loadEmployeeProfile(session: AuthSession): BackendResult<EmployeeDto> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      val employees =
        api.getEmployeeByUserId(
          bearerAuth = "Bearer ${session.accessToken}",
          userIdFilter = "eq.${session.userId}",
        )
      employees.firstOrNull()
        ?: throw IllegalStateException("No employee record found for signed-in user.")
    }
  }

  override suspend fun loadRecentAttendance(
    session: AuthSession,
    employeeId: String,
  ): BackendResult<List<AttendanceRecordDto>> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      api.getRecentAttendance(
        bearerAuth = "Bearer ${session.accessToken}",
        employeeIdFilter = "eq.$employeeId",
      )
    }
  }

  override suspend fun submitAttendanceWithLocationVerification(
    session: AuthSession,
    payload: AttendanceSubmissionPayload,
  ): BackendResult<RecordedAttendanceOutcome> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      val bearer = "Bearer ${session.accessToken}"
      val recordedAt = Instant.now().toString()

      val createdRecord =
        api
          .createAttendanceRecord(
            bearerAuth = bearer,
            body =
              CreateAttendanceRecordRequest(
                employeeId = payload.employeeId,
                workplaceId = payload.workplaceId,
                checkType = payload.checkType.apiValue,
                recordedAtIso = recordedAt,
                biometricVerifiedLocally = payload.biometricVerifiedLocally,
                geofenceStatus = payload.verificationOutcome.apiValue,
                notes = payload.notes,
              ),
          )
          .firstOrNull()
          ?: throw IllegalStateException("Supabase did not return created attendance record.")

      val createdVerification =
        api
          .createLocationVerification(
            bearerAuth = bearer,
            body =
              CreateLocationVerificationRequest(
                attendanceRecordId = createdRecord.id,
                employeeId = payload.employeeId,
                workplaceId = payload.workplaceId,
                latitude = payload.latitude,
                longitude = payload.longitude,
                accuracyMeters = payload.accuracyMeters,
                locationTimestampIso = payload.locationTimestampIso,
                distanceMeters = payload.distanceMeters,
                permittedRadiusMeters = payload.permittedRadiusMeters,
                verificationStatus = payload.verificationOutcome.apiValue,
                failureReason = payload.failureReason,
              ),
          )
          .firstOrNull()
          ?: throw IllegalStateException("Supabase did not return created location verification.")

      RecordedAttendanceOutcome(
        attendanceRecordId = createdRecord.id,
        locationVerificationId = createdVerification.id,
        recordedAtIso = createdRecord.recordedAtIso,
      )
    }
  }

  override suspend fun uploadProfilePhoto(
    session: AuthSession,
    employeeId: String,
    imageFile: File,
    mimeType: String,
  ): BackendResult<EmployeeDto> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      val bearer = "Bearer ${session.accessToken}"
      val objectPath = "$employeeId/profile_${System.currentTimeMillis()}.jpg"
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
        .updateEmployeeProfilePhoto(
          bearerAuth = bearer,
          employeeIdFilter = "eq.$employeeId",
          body = EmployeeProfilePhotoPatch(profilePhotoPath = objectPath, profilePhotoUrl = publicUrl),
        )
        .firstOrNull()
        ?: throw IllegalStateException("Failed to update employee profile photo reference.")
    }
  }

  override suspend fun uploadEmployeeDocument(
    session: AuthSession,
    employeeId: String,
    category: DocumentCaptureCategory,
    title: String,
    imageFile: File,
    mimeType: String,
  ): BackendResult<EmployeeDocumentDto> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      val bearer = "Bearer ${session.accessToken}"
      val objectPath =
        "$employeeId/${category.apiValue.lowercase()}_${System.currentTimeMillis()}.jpg"
      val uploadResp =
        api.uploadStorageObject(
          bearerAuth = bearer,
          bucket = DOCUMENTS_BUCKET,
          objectPath = objectPath,
          fileBody = imageFile.asRequestBody(mimeType.toMediaType()),
        )
      if (!uploadResp.isSuccessful) {
        throw HttpException(uploadResp)
      }

      api
        .createEmployeeDocument(
          bearerAuth = bearer,
          body =
            CreateEmployeeDocumentRequest(
              employeeId = employeeId,
              documentType = category.apiValue,
              title = title.ifBlank { category.label },
              storageBucket = DOCUMENTS_BUCKET,
              storagePath = objectPath,
              mimeType = mimeType,
              fileSizeBytes = imageFile.length(),
              uploadedAtIso = Instant.now().toString(),
            ),
        )
        .firstOrNull()
        ?: throw IllegalStateException("Failed to save document metadata in Supabase.")
    }
  }

  override suspend fun loadEmployeeDocuments(
    session: AuthSession,
    employeeId: String,
  ): BackendResult<List<EmployeeDocumentDto>> {
    if (!environment.isConfigured) return BackendResult.NotConfigured
    return executeSafely {
      api.getEmployeeDocuments(
        bearerAuth = "Bearer ${session.accessToken}",
        employeeIdFilter = "eq.$employeeId",
      )
    }
  }

  private suspend fun <T> executeSafely(block: suspend () -> T): BackendResult<T> {
    return try {
      BackendResult.Success(block())
    } catch (e: IOException) {
      BackendResult.NetworkFailure(
        e.localizedMessage ?: "Network connection unavailable while contacting CivicTrack server."
      )
    } catch (e: HttpException) {
      val errorBody = e.response()?.errorBody()?.string()?.take(200)
      BackendResult.ApiFailure(
        message =
          if (!errorBody.isNullOrBlank()) "Server error (${e.code()}): $errorBody"
          else "Server rejected request (HTTP ${e.code()}).",
        code = e.code(),
      )
    } catch (e: Exception) {
      BackendResult.ApiFailure(e.localizedMessage ?: "Unexpected backend error.")
    }
  }
}
