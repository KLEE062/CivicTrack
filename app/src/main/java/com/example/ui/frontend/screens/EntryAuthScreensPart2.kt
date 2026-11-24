package com.example.ui.frontend.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.OrgMemberRole
import com.example.ui.frontend.components.CivicConfirmationDialog
import com.example.ui.frontend.components.CivicDetailFieldRow
import com.example.ui.frontend.components.CivicFormField
import com.example.ui.frontend.components.CivicNoticeBanner
import com.example.ui.frontend.components.CivicPill
import com.example.ui.frontend.components.CivicPrimaryButton
import com.example.ui.frontend.components.CivicResponsiveScreenContainer
import com.example.ui.frontend.components.CivicSecondaryButton
import com.example.ui.frontend.state.FrontendUiState

@Composable
fun SupervisorInvitationAcceptanceScreen(
  state: FrontendUiState,
  onValidateCode: (String) -> Unit,
  onAcceptInvite: (fullName: String, password: String, confirmPassword: String, isExistingAccountSignIn: Boolean) -> Unit,
  onDeclineInvite: () -> Unit,
  onDismissBanner: () -> Unit,
) {
  var tokenInput by rememberSaveable { mutableStateOf(state.incomingInviteToken.orEmpty()) }
  LaunchedEffect(state.incomingInviteToken) {
    val incoming = state.incomingInviteToken
    if (!incoming.isNullOrBlank() && tokenInput.isBlank()) {
      tokenInput = incoming
      onValidateCode(incoming)
    }
  }

  val invite =
    state.validatedInvitation?.takeIf { OrgMemberRole.fromApi(it.role) == OrgMemberRole.SUPERVISOR }
  var isExistingAccountSignIn by rememberSaveable { mutableStateOf(false) }
  var fullName by rememberSaveable { mutableStateOf("") }
  var password by rememberSaveable { mutableStateOf("") }
  var confirmPassword by rememberSaveable { mutableStateOf("") }
  var showDeclineDialog by rememberSaveable { mutableStateOf(false) }

  if (showDeclineDialog && invite != null) {
    CivicConfirmationDialog(
      title = "Decline Supervisor Invitation?",
      message =
        "Declining cancels your supervisor invitation for ${invite.organizationName}. You can return to role selection.",
      confirmLabel = "Decline Invitation",
      isDestructive = true,
      onConfirm = {
        showDeclineDialog = false
        onDeclineInvite()
      },
      onDismiss = { showDeclineDialog = false },
    )
  }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("supervisor_invitation_screen"),
  ) {
    item {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        CivicPill(
          label = "SUPERVISOR INVITATION",
          containerColor = MaterialTheme.colorScheme.tertiaryContainer,
          contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Spacer(Modifier.height(4.dp))
        Text("Accept Supervisor Invitation", style = MaterialTheme.typography.headlineMedium)
        Text(
          text = "Supervisors join only through an authorised Supabase invitation scoped to a specific department and workplace.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          CivicFormField(
            value = tokenInput,
            onValueChange = { tokenInput = it },
            label = "Invitation Token or Deep Link",
            placeholder = "Paste invitation token or civictrack://invite?token=...",
            enabled = !state.isSubmitting,
            testTag = "sup_invite_token_input",
          )
          CivicSecondaryButton(
            text = if (state.isSubmitting && invite == null) "Validating..." else "Validate Invitation",
            enabled = !state.isSubmitting && tokenInput.isNotBlank(),
            onClick = { onValidateCode(tokenInput) },
            testTag = "sup_invite_lookup_button",
          )
        }
      }
    }

    if (invite != null) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Assigned Authority Scope (Locked)", style = MaterialTheme.typography.titleMedium)
            CivicDetailFieldRow("Organisation", invite.organizationName)
            CivicDetailFieldRow("Invited Email", invite.invitedEmail)
            CivicDetailFieldRow("Assigned Department", invite.departmentName)
            CivicDetailFieldRow("Assigned Workplace", invite.workplaceName)
            CivicDetailFieldRow("Status / Expiry", invite.expiresLabel, showDivider = false)
          }
        }
      }

      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Account Setup", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              FilterChip(
                selected = !isExistingAccountSignIn,
                onClick = { isExistingAccountSignIn = false },
                label = { Text("New Account") },
              )
              FilterChip(
                selected = isExistingAccountSignIn,
                onClick = { isExistingAccountSignIn = true },
                label = { Text("Existing Account") },
              )
            }
            if (!isExistingAccountSignIn) {
              CivicFormField(
                value = fullName,
                onValueChange = { fullName = it },
                label = "Full Name",
                enabled = !state.isSubmitting,
                testTag = "sup_invite_name_input",
              )
            }
            CivicFormField(
              value = invite.invitedEmail,
              onValueChange = {},
              label = "Email (Locked to Invitation)",
              readOnly = true,
              enabled = false,
            )
            CivicFormField(
              value = password,
              onValueChange = { password = it },
              label = if (isExistingAccountSignIn) "Account Password" else "Create Password",
              visualTransformation = PasswordVisualTransformation(),
              enabled = !state.isSubmitting,
              testTag = "sup_invite_password_input",
            )
            if (!isExistingAccountSignIn) {
              CivicFormField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "Confirm Password",
                visualTransformation = PasswordVisualTransformation(),
                enabled = !state.isSubmitting,
                testTag = "sup_invite_confirm_input",
              )
            }
            CivicPrimaryButton(
              text = if (state.isSubmitting) "Joining Organisation..." else "Accept & Join as Supervisor",
              icon = Icons.Filled.VerifiedUser,
              enabled = !state.isSubmitting,
              testTag = "sup_invite_accept_button",
              onClick = {
                onAcceptInvite(fullName, password, confirmPassword, isExistingAccountSignIn)
              },
            )
            CivicSecondaryButton(
              text = "Decline Invitation",
              isDestructive = true,
              enabled = !state.isSubmitting,
              testTag = "sup_invite_decline_button",
              onClick = { showDeclineDialog = true },
            )
          }
        }
      }
    }
  }
}

@Composable
fun EmployeeInvitationAcceptanceScreen(
  state: FrontendUiState,
  onValidateCode: (String) -> Unit,
  onAcceptInvite: (
    fullName: String,
    employeeCode: String,
    jobTitle: String,
    password: String,
    confirmPassword: String,
    isExistingAccountSignIn: Boolean,
  ) -> Unit,
  onDeclineInvite: () -> Unit,
  onDismissBanner: () -> Unit,
) {
  var tokenInput by rememberSaveable { mutableStateOf(state.incomingInviteToken.orEmpty()) }
  val invite =
    state.validatedInvitation?.takeIf { OrgMemberRole.fromApi(it.role) == OrgMemberRole.EMPLOYEE }
  var isExistingAccountSignIn by rememberSaveable { mutableStateOf(false) }
  var fullName by rememberSaveable { mutableStateOf("") }
  var employeeCode by rememberSaveable { mutableStateOf("") }
  var jobTitle by rememberSaveable { mutableStateOf("") }
  var password by rememberSaveable { mutableStateOf("") }
  var confirmPassword by rememberSaveable { mutableStateOf("") }
  var showDeclineDialog by rememberSaveable { mutableStateOf(false) }

  if (showDeclineDialog && invite != null) {
    CivicConfirmationDialog(
      title = "Decline Employee Invitation?",
      message = "You will return to role selection without joining ${invite.organizationName}.",
      confirmLabel = "Decline",
      isDestructive = true,
      onConfirm = {
        showDeclineDialog = false
        onDeclineInvite()
      },
      onDismiss = { showDeclineDialog = false },
    )
  }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("employee_invitation_screen"),
  ) {
    item {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        CivicPill(
          label = "EMPLOYEE INVITATION",
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Spacer(Modifier.height(4.dp))
        Text("Accept Employee Invitation", style = MaterialTheme.typography.headlineMedium)
        Text(
          text = "Validate your invitation token to inspect your department and workplace assignment before joining.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          CivicFormField(
            value = tokenInput,
            onValueChange = { tokenInput = it },
            label = "Employee Invitation Token",
            placeholder = "Paste invitation token or civictrack://invite?token=...",
            enabled = !state.isSubmitting,
            testTag = "emp_invite_token_input",
          )
          CivicSecondaryButton(
            text = if (state.isSubmitting && invite == null) "Validating..." else "Validate Invitation",
            enabled = !state.isSubmitting && tokenInput.isNotBlank(),
            onClick = { onValidateCode(tokenInput) },
            testTag = "emp_invite_lookup_button",
          )
        }
      }
    }

    if (invite != null) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Employment Assignment", style = MaterialTheme.typography.titleMedium)
            CivicDetailFieldRow("Organisation", invite.organizationName)
            CivicDetailFieldRow("Invited Email", invite.invitedEmail)
            CivicDetailFieldRow("Department", invite.departmentName)
            CivicDetailFieldRow("Workplace", invite.workplaceName)
            CivicDetailFieldRow("Expires", invite.expiresLabel, showDivider = false)
          }
        }
      }

      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              FilterChip(
                selected = !isExistingAccountSignIn,
                onClick = { isExistingAccountSignIn = false },
                label = { Text("New Account") },
              )
              FilterChip(
                selected = isExistingAccountSignIn,
                onClick = { isExistingAccountSignIn = true },
                label = { Text("Existing Account") },
              )
            }
            if (!isExistingAccountSignIn) {
              CivicFormField(
                value = fullName,
                onValueChange = { fullName = it },
                label = "Full Name",
                enabled = !state.isSubmitting,
                testTag = "emp_invite_name_input",
              )
              CivicFormField(
                value = employeeCode,
                onValueChange = { employeeCode = it },
                label = "Employee Code (Optional)",
                placeholder = "e.g. CT-1042",
                enabled = !state.isSubmitting,
              )
              CivicFormField(
                value = jobTitle,
                onValueChange = { jobTitle = it },
                label = "Job Title",
                placeholder = "e.g. Field Officer",
                enabled = !state.isSubmitting,
              )
            }
            CivicFormField(
              value = invite.invitedEmail,
              onValueChange = {},
              label = "Work Email (Locked)",
              readOnly = true,
              enabled = false,
            )
            CivicFormField(
              value = password,
              onValueChange = { password = it },
              label = if (isExistingAccountSignIn) "Account Password" else "Create Password",
              visualTransformation = PasswordVisualTransformation(),
              enabled = !state.isSubmitting,
              testTag = "emp_invite_password_input",
            )
            if (!isExistingAccountSignIn) {
              CivicFormField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "Confirm Password",
                visualTransformation = PasswordVisualTransformation(),
                enabled = !state.isSubmitting,
                testTag = "emp_invite_confirm_input",
              )
            }
            CivicPrimaryButton(
              text = if (state.isSubmitting) "Joining Organisation..." else "Accept & Join as Employee",
              enabled = !state.isSubmitting,
              testTag = "emp_invite_accept_button",
              onClick = {
                onAcceptInvite(
                  fullName,
                  employeeCode,
                  jobTitle,
                  password,
                  confirmPassword,
                  isExistingAccountSignIn,
                )
              },
            )
            CivicSecondaryButton(
              text = "Decline Invitation",
              isDestructive = true,
              enabled = !state.isSubmitting,
              onClick = { showDeclineDialog = true },
            )
          }
        }
      }
    }
  }
}

@Composable
fun LoginScreen(
  state: FrontendUiState,
  onSignIn: (email: String, password: String) -> Unit,
  onBiometricUnlock: () -> Unit,
  onForgotPassword: () -> Unit,
  onRoleSelection: () -> Unit,
  onDismissBanner: () -> Unit,
) {
  var email by rememberSaveable { mutableStateOf("") }
  var password by rememberSaveable { mutableStateOf("") }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("login_screen"),
  ) {
    item {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Sign In to CivicTrack", style = MaterialTheme.typography.headlineMedium)
        Text(
          text = "Authenticate with Supabase Auth. Your Owner, Supervisor, or Employee workspace loads automatically from your organisational membership.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          CivicFormField(
            value = email,
            onValueChange = { email = it },
            label = "Work Email",
            enabled = !state.isSubmitting,
            testTag = "login_email_input",
          )
          CivicFormField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            visualTransformation = PasswordVisualTransformation(),
            enabled = !state.isSubmitting,
            testTag = "login_password_input",
          )

          Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            TextButton(
              onClick = onForgotPassword,
              enabled = !state.isSubmitting,
              modifier = Modifier.testTag("login_forgot_password_button"),
            ) {
              Text("Forgot Password?")
            }
          }

          CivicPrimaryButton(
            text = if (state.isSubmitting) "Signing In..." else "Sign In",
            icon = Icons.AutoMirrored.Filled.Login,
            enabled = !state.isSubmitting,
            testTag = "login_submit_button",
            onClick = { onSignIn(email, password) },
          )

          if (state.hasStoredSessionForBiometricUnlock) {
            CivicSecondaryButton(
              text = "Unlock Saved Session with Biometrics",
              icon = Icons.Filled.Fingerprint,
              enabled = !state.isSubmitting,
              testTag = "login_biometric_unlock_button",
              onClick = onBiometricUnlock,
            )
          }

          TextButton(
            onClick = onRoleSelection,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth().testTag("login_to_role_selection_button"),
          ) {
            Text("New to CivicTrack? View Onboarding Options")
          }
        }
      }
    }
  }
}

@Composable
fun ForgotPasswordScreen(
  state: FrontendUiState,
  onSubmitForgot: (String) -> Unit,
  onOpenResetPassword: () -> Unit,
  onBackToLogin: () -> Unit,
  onDismissBanner: () -> Unit,
) {
  var email by rememberSaveable { mutableStateOf(state.passwordResetEmail) }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("forgot_password_screen"),
  ) {
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
      ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Icon(
            imageVector = Icons.Filled.LockReset,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Text("Forgot Password", style = MaterialTheme.typography.headlineMedium)
          Text(
            text = "Enter your registered email address to request a Supabase password recovery email.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          CivicFormField(
            value = email,
            onValueChange = { email = it },
            label = "Registered Email",
            enabled = !state.isSubmitting,
            testTag = "forgot_email_input",
          )
          CivicPrimaryButton(
            text = if (state.isSubmitting) "Sending Request..." else "Send Recovery Email",
            enabled = !state.isSubmitting,
            testTag = "forgot_submit_button",
            onClick = { onSubmitForgot(email) },
          )
          if (state.activeSession != null) {
            CivicSecondaryButton(
              text = "Set New Password",
              testTag = "forgot_open_reset_button",
              onClick = onOpenResetPassword,
            )
          }
          TextButton(onClick = onBackToLogin, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Sign In")
          }
        }
      }
    }
  }
}

@Composable
fun PasswordResetScreen(
  state: FrontendUiState,
  onSubmitReset: (String, String) -> Unit,
  onBackToLogin: () -> Unit,
  onDismissBanner: () -> Unit,
) {
  var newPassword by rememberSaveable { mutableStateOf("") }
  var confirmPassword by rememberSaveable { mutableStateOf("") }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("password_reset_screen"),
  ) {
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
      ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Reset Password", style = MaterialTheme.typography.headlineMedium)
          if (state.passwordResetEmail.isNotBlank() || state.currentAccountEmail.isNotBlank()) {
            Text(
              text = "Account: ${state.currentAccountEmail.ifBlank { state.passwordResetEmail }}",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          CivicFormField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = "New Password",
            visualTransformation = PasswordVisualTransformation(),
            enabled = !state.isSubmitting,
            testTag = "reset_new_password_input",
          )
          CivicFormField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = "Confirm New Password",
            visualTransformation = PasswordVisualTransformation(),
            enabled = !state.isSubmitting,
            testTag = "reset_confirm_password_input",
          )
          CivicPrimaryButton(
            text = if (state.isSubmitting) "Updating Password..." else "Update Password",
            enabled = !state.isSubmitting,
            testTag = "reset_submit_button",
            onClick = { onSubmitReset(newPassword, confirmPassword) },
          )
          TextButton(onClick = onBackToLogin, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Sign In")
          }
        }
      }
    }
  }
}

@Composable
fun EmailVerificationInfoScreen(
  state: FrontendUiState,
  onCheckVerifiedAndContinue: () -> Unit,
  onGoToLogin: () -> Unit,
  onDismissBanner: () -> Unit,
) {
  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("email_verification_info_screen"),
  ) {
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
      ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Icon(
            imageVector = Icons.Filled.MarkEmailRead,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Text("Verify Your Email Address", style = MaterialTheme.typography.headlineMedium)
          Text(
            text =
              "Supabase dispatched a verification email to ${state.pendingEmailVerificationAddress ?: state.currentAccountEmail}. Open the confirmation link in your inbox, then continue below to finalise organisation setup.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          CivicNoticeBanner(
            title = "Confirmation Pending",
            message = "Once verified, tap Continue below so CivicTrack can complete organisation creation via Supabase RPC.",
          )
          CivicPrimaryButton(
            text = if (state.isSubmitting) "Checking Verification..." else "I Verified My Email — Continue",
            enabled = !state.isSubmitting,
            testTag = "verify_email_continue_button",
            onClick = onCheckVerifiedAndContinue,
          )
          TextButton(
            onClick = onGoToLogin,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("Go to Sign In")
          }
        }
      }
    }
  }
}
