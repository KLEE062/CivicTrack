package com.example.core.biometric

import android.os.Build
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.core.capabilities.BiometricCapabilityState
import com.example.core.capabilities.DeviceCapabilityChecker
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed interface BiometricAuthOutcome {
  /**
   * Indicates only that the local Android operating system verified the device owner's
   * biometric or screen-lock credential. Never equates to centralized identity verification.
   */
  data class Success(val methodLabel: String) : BiometricAuthOutcome
  data class Cancelled(val reason: String) : BiometricAuthOutcome
  data class LockedOut(val isPermanent: Boolean, val message: String) : BiometricAuthOutcome
  data object NotEnrolled : BiometricAuthOutcome
  data class HardwareUnavailable(val message: String) : BiometricAuthOutcome
  data class Error(val errorCode: Int, val message: String) : BiometricAuthOutcome
}

interface BiometricAuthenticator {
  suspend fun authenticate(
    activity: FragmentActivity,
    title: String,
    subtitle: String,
    description: String,
    allowDeviceCredentialFallback: Boolean = true,
  ): BiometricAuthOutcome
}

class AndroidBiometricAuthenticator(
  private val capabilityChecker: DeviceCapabilityChecker,
) : BiometricAuthenticator {

  override suspend fun authenticate(
    activity: FragmentActivity,
    title: String,
    subtitle: String,
    description: String,
    allowDeviceCredentialFallback: Boolean,
  ): BiometricAuthOutcome {
    val availability = capabilityChecker.checkBiometricAvailability(allowDeviceCredentialFallback)
    if (availability !is BiometricCapabilityState.Available) {
      return when (availability) {
        BiometricCapabilityState.NoneEnrolled -> BiometricAuthOutcome.NotEnrolled
        BiometricCapabilityState.NoHardware,
        BiometricCapabilityState.HardwareUnavailable,
        BiometricCapabilityState.SecurityUpdateRequired,
        BiometricCapabilityState.Unsupported ->
          BiometricAuthOutcome.HardwareUnavailable(
            capabilityChecker.userMessageForBiometricState(availability)
          )
      }
    }

    return suspendCancellableCoroutine { continuation ->
      val executor = ContextCompat.getMainExecutor(activity)

      val callback =
        object : BiometricPrompt.AuthenticationCallback() {
          override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            if (!continuation.isActive) return
            val method =
              when (result.authenticationType) {
                BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC -> "Device Biometric"
                BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL ->
                  "Device PIN / Pattern / Password"
                else -> "OS Local Credential"
              }
            continuation.resume(BiometricAuthOutcome.Success(method))
          }

          override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            if (!continuation.isActive) return
            val msg = errString.toString()
            val mapped =
              when (errorCode) {
                BiometricPrompt.ERROR_USER_CANCELED,
                BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                BiometricPrompt.ERROR_CANCELED ->
                  BiometricAuthOutcome.Cancelled("Authentication cancelled by user.")
                BiometricPrompt.ERROR_LOCKOUT ->
                  BiometricAuthOutcome.LockedOut(
                    isPermanent = false,
                    message = "Too many failed biometric attempts. Wait 30 seconds or unlock device.",
                  )
                BiometricPrompt.ERROR_LOCKOUT_PERMANENT ->
                  BiometricAuthOutcome.LockedOut(
                    isPermanent = true,
                    message =
                      "Biometric authentication is locked until device screen lock is entered.",
                  )
                BiometricPrompt.ERROR_NO_BIOMETRICS,
                BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> BiometricAuthOutcome.NotEnrolled
                BiometricPrompt.ERROR_HW_NOT_PRESENT,
                BiometricPrompt.ERROR_HW_UNAVAILABLE ->
                  BiometricAuthOutcome.HardwareUnavailable(msg)
                else -> BiometricAuthOutcome.Error(errorCode, msg)
              }
            continuation.resume(mapped)
          }
        }

      val prompt = BiometricPrompt(activity, executor, callback)
      val promptInfoBuilder =
        BiometricPrompt.PromptInfo.Builder()
          .setTitle(title)
          .setSubtitle(subtitle)
          .setDescription(description)
          .setConfirmationRequired(true)

      val usesDeviceCredential =
        (availability.allowedAuthenticators and DEVICE_CREDENTIAL) != 0 &&
          Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

      if (usesDeviceCredential) {
        // AndroidX Biometric prohibits calling setNegativeButtonText when DEVICE_CREDENTIAL is included.
        promptInfoBuilder.setAllowedAuthenticators(availability.allowedAuthenticators)
      } else {
        promptInfoBuilder
          .setAllowedAuthenticators(availability.allowedAuthenticators)
          .setNegativeButtonText("Cancel")
      }

      try {
        prompt.authenticate(promptInfoBuilder.build())
      } catch (e: Exception) {
        if (continuation.isActive) {
          continuation.resume(
            BiometricAuthOutcome.Error(-1, e.localizedMessage ?: "Failed to show biometric prompt.")
          )
        }
      }

      continuation.invokeOnCancellation {
        try {
          prompt.cancelAuthentication()
        } catch (_: Exception) {
          // Ignore cancellation errors if prompt already dismissed
        }
      }
    }
  }
}
