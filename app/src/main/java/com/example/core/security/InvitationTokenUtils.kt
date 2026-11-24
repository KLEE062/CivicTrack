package com.example.core.security

import android.net.Uri
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

object InvitationTokenUtils {
  private val EMAIL_REGEX =
    Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

  private val secureRandom = SecureRandom()

  /**
   * Generates a 256-bit cryptographically random URL-safe invitation token.
   * Only the SHA-256 hash should be persisted in the database; never log raw tokens.
   */
  fun generateRawToken(): String {
    val bytes = ByteArray(24)
    secureRandom.nextBytes(bytes)
    return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
  }

  /**
   * Computes lowercase hexadecimal SHA-256 digest of a raw invitation token.
   */
  fun sha256Hex(rawToken: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(rawToken.trim().toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
  }

  /**
   * Extracts a raw invitation token from either a pasted code or a civictrack:// / https:// link.
   */
  fun extractInvitationToken(input: String): String {
    val trimmed = input.trim()
    if (trimmed.contains("token=")) {
      val parsed = runCatching { Uri.parse(trimmed).getQueryParameter("token") }.getOrNull()
      if (!parsed.isNullOrBlank()) return parsed.trim()
      val afterParam = trimmed.substringAfter("token=").substringBefore("&").trim()
      if (afterParam.isNotEmpty()) return afterParam
    }
    return trimmed
  }

  fun buildInviteDeepLink(rawToken: String): String = "civictrack://invite?token=$rawToken"

  fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

  /**
   * Enforces institutional password strength policy:
   * minimum 8 characters with at least one letter and one digit or symbol.
   */
  fun validatePassword(password: String): String? {
    if (password.length < 8) {
      return "Password must be at least 8 characters long."
    }
    if (!password.any { it.isLetter() }) {
      return "Password must include at least one letter."
    }
    if (!password.any { it.isDigit() || !it.isLetterOrDigit() }) {
      return "Password must include at least one number or symbol."
    }
    return null
  }

  fun parseReadableBackendError(rawBody: String?, code: Int?): String {
    val text = rawBody.orEmpty()
    return when {
      text.contains("User already registered", ignoreCase = true) ||
        text.contains("already been registered", ignoreCase = true) ->
        "An account with this email already exists. Use Sign In or Forgot Password instead."
      text.contains("Email not confirmed", ignoreCase = true) ->
        "Your email address is not confirmed yet. Open the verification link sent to your inbox, then sign in."
      text.contains("Invalid login credentials", ignoreCase = true) ->
        "Invalid email or password. Check your credentials or reset your password."
      text.contains("JWT expired", ignoreCase = true) || code == 401 ->
        "Your session has expired. Please sign in again to continue securely."
      text.contains("duplicate key", ignoreCase = true) ||
        text.contains("23505", ignoreCase = true) ->
        "A matching record already exists for this entry."
      text.contains("permission denied", ignoreCase = true) || code == 403 ->
        "Access denied by organisation security policy (RLS). Confirm your role and invitation status."
      text.isNotBlank() ->
        "Server rejected request (${code ?: "ERR"}): ${text.take(180)}"
      else ->
        "Server rejected request (HTTP ${code ?: 500})."
    }
  }
}
