package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.InvitationDto
import com.example.data.model.OrgMemberRole
import com.example.data.model.SupervisorOnboardingStep
import com.example.ui.OnboardingUiState
import com.example.ui.components.FeedbackMessageCard
import com.example.ui.components.OnboardingStepIndicator
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalCivicStatusPalette

@Composable
fun SupervisorOnboardingFlowScreen(
  uiState: OnboardingUiState,
  initialTokenInput: String?,
  onBackToEntry: () -> Unit,
  onValidateInvitation: (String) -> Unit,
  onToggleAuthMode: (Boolean) -> Unit,
  onRegisterSupervisorAccount: (String, String, String) -> Unit,
  onSignInSupervisorAccount: (String, String) -> Unit,
  onAcceptInvitation: () -> Unit,
  onDeclineInvitation: () -> Unit,
  onSignOutToSwitchAccount: () -> Unit,
  onPickSupervisorPhoto: () -> Unit,
  onConfirmSupervisorProfile: (String, String) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (uiState.supervisorStep == SupervisorOnboardingStep.INVITATION_LOOKUP) {
    BackHandler { onBackToEntry() }
  }

  val steps =
    remember {
      listOf(
        1 to "Invite Check",
        2 to "Account",
        3 to "Accept",
        4 to "Profile",
        5 to "Dashboard",
      )
    }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
            ),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Supervisor Invitation Onboarding",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
              )
              StatusPill(
                label = "SUPERVISOR FLOW",
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
              )
            }
            OnboardingStepIndicator(
              steps = steps,
              currentStepNumber = uiState.supervisorStep.stepNumber,
              accentColor = MaterialTheme.colorScheme.tertiary,
            )
          }
        }
      }

      if (
        uiState.errorMessage != null ||
          uiState.infoMessage != null ||
          uiState.pendingEmailVerificationAddress != null
      ) {
        item {
          FeedbackMessageCard(
            errorMessage = uiState.errorMessage,
            infoMessage = uiState.infoMessage,
            pendingVerificationEmail = uiState.pendingEmailVerificationAddress,
            onSignInAfterVerification = { onToggleAuthMode(true) },
          )
        }
      }

      when (uiState.supervisorStep) {
        SupervisorOnboardingStep.INVITATION_LOOKUP -> {
          item {
            SupervisorStep1InvitationLookupCard(
              initialToken = initialTokenInput.orEmpty(),
              isLoading = uiState.isSubmitting,
              onValidate = onValidateInvitation,
              onBack = onBackToEntry,
            )
          }
        }
        SupervisorOnboardingStep.ACCOUNT_AUTH -> {
          val invite = uiState.validatedInvitation
          if (invite != null) {
            item { InvitationSummaryCard(invite = invite) }
            item {
              SupervisorStep2AccountAuthCard(
                invitedEmail = invite.email,
                useSignIn = uiState.supervisorAuthUseSignIn,
                isLoading = uiState.isSubmitting,
                onToggleAuthMode = onToggleAuthMode,
                onRegister = onRegisterSupervisorAccount,
                onSignIn = onSignInSupervisorAccount,
              )
            }
          }
        }
        SupervisorOnboardingStep.INVITATION_ACCEPTANCE -> {
          val invite = uiState.validatedInvitation
          if (invite != null) {
            item {
              SupervisorStep3AcceptanceCard(
                invite = invite,
                signedInEmail = uiState.session?.email,
                isLoading = uiState.isSubmitting,
                onAccept = onAcceptInvitation,
                onDecline = onDeclineInvitation,
                onSwitchAccount = onSignOutToSwitchAccount,
              )
            }
          }
        }
        SupervisorOnboardingStep.PROFILE_CONFIRMATION -> {
          item {
            SupervisorStep4ProfileConfirmationCard(
              uiState = uiState,
              onPickPhoto = onPickSupervisorPhoto,
              onConfirm = onConfirmSupervisorProfile,
            )
          }
        }
        SupervisorOnboardingStep.DASHBOARD -> Unit
      }
    }
  }
}

@Composable
private fun SupervisorStep1InvitationLookupCard(
  initialToken: String,
  isLoading: Boolean,
  onValidate: (String) -> Unit,
  onBack: () -> Unit,
) {
  var tokenInput by rememberSaveable(initialToken) { mutableStateOf(initialToken) }

  Card(
    modifier = Modifier.fillMaxWidth().testTag("supervisor_step1_lookup_card"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      TextButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text("Back to Welcome")
      }
      Text(
        text = "Step 1: Verify Your Supervisor Invitation",
        style = MaterialTheme.typography.headlineMedium,
      )
      Text(
        text =
          "Enter the invitation token or open the civictrack:// invite link issued by your Organisation Owner. Supervisors cannot create new organisations from this flow.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      OutlinedTextField(
        value = tokenInput,
        onValueChange = { tokenInput = it },
        label = { Text("Invitation Token or Link") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("supervisor_invite_token_input"),
      )
      Button(
        onClick = { onValidate(tokenInput) },
        enabled = !isLoading,
        modifier =
          Modifier.fillMaxWidth()
            .height(50.dp)
            .testTag("validate_supervisor_invite_button"),
      ) {
        if (isLoading) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary,
          )
        } else {
          Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Validate Invitation")
        }
      }
    }
  }
}

@Composable
private fun InvitationSummaryCard(invite: InvitationDto) {
  Card(
    modifier = Modifier.fillMaxWidth().testTag("validated_invitation_summary_card"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = invite.organizations?.name ?: "Organisation ${invite.organizationId.take(8)}",
          style = MaterialTheme.typography.titleLarge,
        )
        StatusPill(
          label = OrgMemberRole.fromApi(invite.role).displayLabel,
          containerColor = MaterialTheme.colorScheme.tertiaryContainer,
          contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        )
      }
      Text(
        text = "Invited Email: ${invite.email}",
        style = MaterialTheme.typography.bodyMedium,
      )
      invite.departments?.let {
        Text("Assigned Department: ${it.name}", style = MaterialTheme.typography.bodyMedium)
      }
      invite.workplaces?.let {
        Text("Assigned Workplace: ${it.name}", style = MaterialTheme.typography.bodyMedium)
      }
      invite.expiresAt?.let {
        Text(
          text = "Expires: ${it.take(19).replace('T', ' ')} UTC",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun SupervisorStep2AccountAuthCard(
  invitedEmail: String,
  useSignIn: Boolean,
  isLoading: Boolean,
  onToggleAuthMode: (Boolean) -> Unit,
  onRegister: (String, String, String) -> Unit,
  onSignIn: (String, String) -> Unit,
) {
  var fullName by rememberSaveable { mutableStateOf("") }
  var password by rememberSaveable { mutableStateOf("") }
  var confirmPassword by rememberSaveable { mutableStateOf("") }

  Card(
    modifier = Modifier.fillMaxWidth().testTag("supervisor_step2_auth_card"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
          selected = !useSignIn,
          onClick = { onToggleAuthMode(false) },
          shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
          modifier = Modifier.testTag("supervisor_tab_register"),
        ) {
          Text("Create Account")
        }
        SegmentedButton(
          selected = useSignIn,
          onClick = { onToggleAuthMode(true) },
          shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
          modifier = Modifier.testTag("supervisor_tab_signin"),
        ) {
          Text("Existing Account")
        }
      }

      Text(
        text =
          if (useSignIn) "Step 2: Sign In with Invited Account"
          else "Step 2: Register Supervisor Account",
        style = MaterialTheme.typography.headlineMedium,
      )

      OutlinedTextField(
        value = invitedEmail,
        onValueChange = {},
        readOnly = true,
        enabled = false,
        label = { Text("Invited Email (Locked to Invitation)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("supervisor_locked_email_input"),
      )

      if (!useSignIn) {
        OutlinedTextField(
          value = fullName,
          onValueChange = { fullName = it },
          label = { Text(stringResource(R.string.full_name_label)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("supervisor_full_name_input"),
        )
      }

      OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth().testTag("supervisor_password_input"),
      )

      if (!useSignIn) {
        OutlinedTextField(
          value = confirmPassword,
          onValueChange = { confirmPassword = it },
          label = { Text(stringResource(R.string.confirm_password_label)) },
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          modifier = Modifier.fillMaxWidth().testTag("supervisor_confirm_password_input"),
        )
      }

      Button(
        onClick = {
          if (useSignIn) onSignIn(invitedEmail, password)
          else onRegister(fullName, password, confirmPassword)
        },
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("supervisor_auth_submit_button"),
      ) {
        if (isLoading) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary,
          )
        } else {
          Icon(
            imageVector = if (useSignIn) Icons.AutoMirrored.Filled.Login else Icons.Filled.PersonAdd,
            contentDescription = null,
          )
          Spacer(Modifier.width(8.dp))
          Text(if (useSignIn) "Sign In & Review Invitation" else "Register & Continue")
        }
      }
    }
  }
}

@Composable
private fun SupervisorStep3AcceptanceCard(
  invite: InvitationDto,
  signedInEmail: String?,
  isLoading: Boolean,
  onAccept: () -> Unit,
  onDecline: () -> Unit,
  onSwitchAccount: () -> Unit,
) {
  val palette = LocalCivicStatusPalette.current
  val emailMatches = signedInEmail != null && signedInEmail.equals(invite.email, ignoreCase = true)

  Card(
    modifier = Modifier.fillMaxWidth().testTag("supervisor_step3_acceptance_card"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "Step 3: Review & Accept Invitation",
        style = MaterialTheme.typography.headlineMedium,
      )
      Text(
        text =
          "The server independently verifies your invitation and assigns your Supervisor role upon acceptance.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      HorizontalDivider()
      DetailLine("Organisation", invite.organizations?.name ?: invite.organizationId)
      DetailLine("Invitation Role", "${OrgMemberRole.fromApi(invite.role).displayLabel} (Locked)")
      DetailLine("Invited Email", invite.email)
      DetailLine("Signed-In Account", signedInEmail ?: "Not signed in")
      invite.departments?.let { DetailLine("Assigned Department", it.name) }
      invite.workplaces?.let { DetailLine("Assigned Workplace", it.name) }
      invite.expiresAt?.let { DetailLine("Expires At", it.take(19).replace('T', ' ')) }
      HorizontalDivider()

      if (!emailMatches) {
        Card(
          colors = CardDefaults.cardColors(containerColor = palette.errorContainer),
          modifier = Modifier.fillMaxWidth().testTag("email_mismatch_warning"),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text =
                "Email mismatch: this invitation is issued to ${invite.email}, but you are signed in as ${signedInEmail ?: "unknown"}.",
              style = MaterialTheme.typography.bodyMedium,
              color = palette.error,
            )
            OutlinedButton(
              onClick = onSwitchAccount,
              modifier = Modifier.testTag("switch_account_button"),
            ) {
              Text("Sign Out & Switch Account")
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = onDecline,
          enabled = !isLoading,
          modifier = Modifier.weight(1f).height(50.dp).testTag("decline_invitation_button"),
        ) {
          Icon(Icons.Filled.Close, contentDescription = null)
          Spacer(Modifier.width(6.dp))
          Text("Decline Invitation")
        }
        Button(
          onClick = onAccept,
          enabled = !isLoading && emailMatches,
          modifier = Modifier.weight(1f).height(50.dp).testTag("accept_invitation_button"),
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.onPrimary,
            )
          } else {
            Icon(Icons.Filled.CheckCircle, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Accept Invitation")
          }
        }
      }
    }
  }
}

@Composable
private fun SupervisorStep4ProfileConfirmationCard(
  uiState: OnboardingUiState,
  onPickPhoto: () -> Unit,
  onConfirm: (String, String) -> Unit,
) {
  val membership = uiState.activeMembership
  val profile = uiState.userProfile
  val initialName = profile?.fullName ?: ""
  val initialPhone = profile?.phone ?: ""
  var fullName by rememberSaveable(initialName) { mutableStateOf(initialName) }
  var phone by rememberSaveable(initialPhone) { mutableStateOf(initialPhone) }

  Card(
    modifier = Modifier.fillMaxWidth().testTag("supervisor_step4_profile_card"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "Step 4: Confirm Supervisor Profile",
        style = MaterialTheme.typography.headlineMedium,
      )
      Text(
        text =
          "Review your organisational authority and confirm your supervisor profile. Role and unit assignments are managed by your Organisation Owner.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        if (!profile?.avatarUrl.isNullOrBlank()) {
          AsyncImage(
            model = profile?.avatarUrl,
            contentDescription = "Supervisor avatar",
            modifier = Modifier.size(56.dp).clip(CircleShape),
            contentScale = ContentScale.Crop,
          )
        } else {
          Icon(
            imageVector = Icons.Filled.AccountCircle,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.tertiary,
          )
        }
        OutlinedButton(onClick = onPickPhoto, modifier = Modifier.testTag("supervisor_photo_button")) {
          Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Upload Profile Photo")
        }
      }

      OutlinedTextField(
        value = fullName,
        onValueChange = { fullName = it },
        label = { Text("Full Name (Editable)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("supervisor_profile_name_input"),
      )
      OutlinedTextField(
        value = phone,
        onValueChange = { phone = it },
        label = { Text("Phone Number (Optional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )

      HorizontalDivider()
      DetailLine("Email", uiState.session?.email ?: profile?.email ?: "-")
      DetailLine("Organisation", uiState.activeOrganization?.name ?: membership?.organizations?.name ?: "-")
      DetailLine("Role", membership?.parsedRole?.displayLabel ?: "Supervisor")
      DetailLine("Assigned Department", membership?.departments?.name ?: "All Authorised Units")
      DetailLine("Assigned Workplace", membership?.workplaces?.name ?: "All Assigned Sites")

      Button(
        onClick = { onConfirm(fullName, phone) },
        enabled = !uiState.isSubmitting,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
        modifier =
          Modifier.fillMaxWidth()
            .height(50.dp)
            .testTag("supervisor_confirm_profile_button"),
      ) {
        Icon(Icons.Filled.Dashboard, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Continue to Supervisor Dashboard")
      }
    }
  }
}

@Composable
private fun DetailLine(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}
