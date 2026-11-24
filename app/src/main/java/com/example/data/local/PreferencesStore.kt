package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AuthSession
import com.example.data.model.PendingEmployeeProfile
import com.example.data.model.PendingOwnerOnboarding
import com.example.data.model.PendingSupervisorInvitation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.civicTrackDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "civictrack_preferences")

interface PreferencesRepository {
  val biometricLoginEnabled: Flow<Boolean>
  val authSession: Flow<AuthSession?>
  val themeModePreference: Flow<String>
    get() = kotlinx.coroutines.flow.flowOf("SYSTEM")

  suspend fun setBiometricLoginEnabled(enabled: Boolean)
  suspend fun setThemeModePreference(mode: String) {}
  suspend fun wasPermissionRequested(permission: String): Boolean
  suspend fun markPermissionRequested(permission: String)
  suspend fun saveAuthSession(session: AuthSession)
  suspend fun clearAuthSession()
  suspend fun savePendingEmployeeProfile(profile: PendingEmployeeProfile)
  suspend fun getPendingEmployeeProfile(): PendingEmployeeProfile?
  suspend fun clearPendingEmployeeProfile()

  suspend fun savePendingOwnerOnboarding(pending: PendingOwnerOnboarding) {}
  suspend fun getPendingOwnerOnboarding(): PendingOwnerOnboarding? = null
  suspend fun clearPendingOwnerOnboarding() {}

  suspend fun savePendingSupervisorInvitation(pending: PendingSupervisorInvitation) {}
  suspend fun getPendingSupervisorInvitation(): PendingSupervisorInvitation? = null
  suspend fun clearPendingSupervisorInvitation() {}
}

class DataStorePreferencesRepository(private val context: Context) : PreferencesRepository {

  private object Keys {
    val BIOMETRIC_LOGIN_ENABLED = booleanPreferencesKey("biometric_login_enabled")
    val THEME_MODE = stringPreferencesKey("theme_mode_preference")
    val ACCESS_TOKEN = stringPreferencesKey("supabase_access_token")
    val USER_ID = stringPreferencesKey("supabase_user_id")
    val USER_EMAIL = stringPreferencesKey("supabase_user_email")

    val PENDING_EMAIL = stringPreferencesKey("pending_reg_email")
    val PENDING_FULL_NAME = stringPreferencesKey("pending_reg_full_name")
    val PENDING_EMPLOYEE_CODE = stringPreferencesKey("pending_reg_employee_code")
    val PENDING_DEPARTMENT = stringPreferencesKey("pending_reg_department")
    val PENDING_ROLE = stringPreferencesKey("pending_reg_role")
    val PENDING_WORKPLACE_ID = stringPreferencesKey("pending_reg_workplace_id")

    val OWNER_PENDING_EMAIL = stringPreferencesKey("owner_pending_email")
    val OWNER_PENDING_NAME = stringPreferencesKey("owner_pending_name")
    val OWNER_PENDING_PHONE = stringPreferencesKey("owner_pending_phone")

    val SUP_PENDING_TOKEN_HASH = stringPreferencesKey("sup_pending_token_hash")
    val SUP_PENDING_EMAIL = stringPreferencesKey("sup_pending_email")
    val SUP_PENDING_ORG_ID = stringPreferencesKey("sup_pending_org_id")
    val SUP_PENDING_ORG_NAME = stringPreferencesKey("sup_pending_org_name")
    val SUP_PENDING_ROLE = stringPreferencesKey("sup_pending_role")
    val SUP_PENDING_DEPT = stringPreferencesKey("sup_pending_dept")
    val SUP_PENDING_WORKPLACE = stringPreferencesKey("sup_pending_workplace")

    fun permissionRequested(permission: String) =
      booleanPreferencesKey("permission_requested_${permission.replace('.', '_')}")
  }

  override val biometricLoginEnabled: Flow<Boolean> =
    context.civicTrackDataStore.data.map { prefs -> prefs[Keys.BIOMETRIC_LOGIN_ENABLED] ?: false }

  override val themeModePreference: Flow<String> =
    context.civicTrackDataStore.data.map { prefs -> prefs[Keys.THEME_MODE] ?: "SYSTEM" }

  override val authSession: Flow<AuthSession?> =
    context.civicTrackDataStore.data.map { prefs ->
      val token = prefs[Keys.ACCESS_TOKEN]
      val userId = prefs[Keys.USER_ID]
      val email = prefs[Keys.USER_EMAIL]
      if (!token.isNullOrBlank() && !userId.isNullOrBlank() && !email.isNullOrBlank()) {
        AuthSession(accessToken = token, userId = userId, email = email)
      } else {
        null
      }
    }

  override suspend fun setBiometricLoginEnabled(enabled: Boolean) {
    context.civicTrackDataStore.edit { prefs -> prefs[Keys.BIOMETRIC_LOGIN_ENABLED] = enabled }
  }

  override suspend fun setThemeModePreference(mode: String) {
    context.civicTrackDataStore.edit { prefs -> prefs[Keys.THEME_MODE] = mode }
  }

  override suspend fun wasPermissionRequested(permission: String): Boolean {
    val prefs = context.civicTrackDataStore.data.first()
    return prefs[Keys.permissionRequested(permission)] ?: false
  }

  override suspend fun markPermissionRequested(permission: String) {
    context.civicTrackDataStore.edit { prefs ->
      prefs[Keys.permissionRequested(permission)] = true
    }
  }

  override suspend fun saveAuthSession(session: AuthSession) {
    context.civicTrackDataStore.edit { prefs ->
      prefs[Keys.ACCESS_TOKEN] = session.accessToken
      prefs[Keys.USER_ID] = session.userId
      prefs[Keys.USER_EMAIL] = session.email
    }
  }

  override suspend fun clearAuthSession() {
    context.civicTrackDataStore.edit { prefs ->
      prefs.remove(Keys.ACCESS_TOKEN)
      prefs.remove(Keys.USER_ID)
      prefs.remove(Keys.USER_EMAIL)
    }
  }

  override suspend fun savePendingEmployeeProfile(profile: PendingEmployeeProfile) {
    context.civicTrackDataStore.edit { prefs ->
      prefs[Keys.PENDING_EMAIL] = profile.email
      prefs[Keys.PENDING_FULL_NAME] = profile.fullName
      prefs[Keys.PENDING_EMPLOYEE_CODE] = profile.employeeCode
      if (profile.department != null) prefs[Keys.PENDING_DEPARTMENT] = profile.department
      else prefs.remove(Keys.PENDING_DEPARTMENT)
      if (profile.role != null) prefs[Keys.PENDING_ROLE] = profile.role
      else prefs.remove(Keys.PENDING_ROLE)
      if (profile.workplaceId != null) prefs[Keys.PENDING_WORKPLACE_ID] = profile.workplaceId
      else prefs.remove(Keys.PENDING_WORKPLACE_ID)
    }
  }

  override suspend fun getPendingEmployeeProfile(): PendingEmployeeProfile? {
    val prefs = context.civicTrackDataStore.data.first()
    val email = prefs[Keys.PENDING_EMAIL] ?: return null
    val fullName = prefs[Keys.PENDING_FULL_NAME] ?: return null
    val code = prefs[Keys.PENDING_EMPLOYEE_CODE] ?: return null
    return PendingEmployeeProfile(
      email = email,
      fullName = fullName,
      employeeCode = code,
      department = prefs[Keys.PENDING_DEPARTMENT],
      role = prefs[Keys.PENDING_ROLE],
      workplaceId = prefs[Keys.PENDING_WORKPLACE_ID],
    )
  }

  override suspend fun clearPendingEmployeeProfile() {
    context.civicTrackDataStore.edit { prefs ->
      prefs.remove(Keys.PENDING_EMAIL)
      prefs.remove(Keys.PENDING_FULL_NAME)
      prefs.remove(Keys.PENDING_EMPLOYEE_CODE)
      prefs.remove(Keys.PENDING_DEPARTMENT)
      prefs.remove(Keys.PENDING_ROLE)
      prefs.remove(Keys.PENDING_WORKPLACE_ID)
    }
  }

  override suspend fun savePendingOwnerOnboarding(pending: PendingOwnerOnboarding) {
    context.civicTrackDataStore.edit { prefs ->
      prefs[Keys.OWNER_PENDING_EMAIL] = pending.email
      prefs[Keys.OWNER_PENDING_NAME] = pending.fullName
      if (pending.phone != null) prefs[Keys.OWNER_PENDING_PHONE] = pending.phone
      else prefs.remove(Keys.OWNER_PENDING_PHONE)
    }
  }

  override suspend fun getPendingOwnerOnboarding(): PendingOwnerOnboarding? {
    val prefs = context.civicTrackDataStore.data.first()
    val email = prefs[Keys.OWNER_PENDING_EMAIL] ?: return null
    val fullName = prefs[Keys.OWNER_PENDING_NAME] ?: return null
    return PendingOwnerOnboarding(
      email = email,
      fullName = fullName,
      phone = prefs[Keys.OWNER_PENDING_PHONE],
    )
  }

  override suspend fun clearPendingOwnerOnboarding() {
    context.civicTrackDataStore.edit { prefs ->
      prefs.remove(Keys.OWNER_PENDING_EMAIL)
      prefs.remove(Keys.OWNER_PENDING_NAME)
      prefs.remove(Keys.OWNER_PENDING_PHONE)
    }
  }

  override suspend fun savePendingSupervisorInvitation(pending: PendingSupervisorInvitation) {
    context.civicTrackDataStore.edit { prefs ->
      prefs[Keys.SUP_PENDING_TOKEN_HASH] = pending.tokenHash
      prefs[Keys.SUP_PENDING_EMAIL] = pending.invitedEmail
      prefs[Keys.SUP_PENDING_ORG_ID] = pending.organizationId
      prefs[Keys.SUP_PENDING_ORG_NAME] = pending.organizationName
      prefs[Keys.SUP_PENDING_ROLE] = pending.role
      if (pending.departmentName != null) prefs[Keys.SUP_PENDING_DEPT] = pending.departmentName
      else prefs.remove(Keys.SUP_PENDING_DEPT)
      if (pending.workplaceName != null) prefs[Keys.SUP_PENDING_WORKPLACE] = pending.workplaceName
      else prefs.remove(Keys.SUP_PENDING_WORKPLACE)
    }
  }

  override suspend fun getPendingSupervisorInvitation(): PendingSupervisorInvitation? {
    val prefs = context.civicTrackDataStore.data.first()
    val tokenHash = prefs[Keys.SUP_PENDING_TOKEN_HASH] ?: return null
    val email = prefs[Keys.SUP_PENDING_EMAIL] ?: return null
    val orgId = prefs[Keys.SUP_PENDING_ORG_ID] ?: return null
    val orgName = prefs[Keys.SUP_PENDING_ORG_NAME] ?: return null
    val role = prefs[Keys.SUP_PENDING_ROLE] ?: return null
    return PendingSupervisorInvitation(
      tokenHash = tokenHash,
      invitedEmail = email,
      organizationId = orgId,
      organizationName = orgName,
      role = role,
      departmentName = prefs[Keys.SUP_PENDING_DEPT],
      workplaceName = prefs[Keys.SUP_PENDING_WORKPLACE],
    )
  }

  override suspend fun clearPendingSupervisorInvitation() {
    context.civicTrackDataStore.edit { prefs ->
      prefs.remove(Keys.SUP_PENDING_TOKEN_HASH)
      prefs.remove(Keys.SUP_PENDING_EMAIL)
      prefs.remove(Keys.SUP_PENDING_ORG_ID)
      prefs.remove(Keys.SUP_PENDING_ORG_NAME)
      prefs.remove(Keys.SUP_PENDING_ROLE)
      prefs.remove(Keys.SUP_PENDING_DEPT)
      prefs.remove(Keys.SUP_PENDING_WORKPLACE)
    }
  }
}
