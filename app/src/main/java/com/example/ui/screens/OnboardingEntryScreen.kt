package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.EntryAuthDestination
import com.example.ui.OnboardingUiState
import com.example.ui.components.FeedbackMessageCard
import com.example.ui.components.StatusPill
import com.example.ui.components.SupabaseConfigBanner

@Composable
fun OnboardingEntryScreen(
  uiState: OnboardingUiState,
  hasBiometricSavedSession: Boolean,
  onStartOwnerOnboarding: () -> Unit,
  onStartSupervisorOnboarding: () -> Unit,
  onOpenSignIn: () -> Unit,
  onOpenForgotPassword: () -> Unit,
  onBackToLanding: () -> Unit,
  onSignIn: (String, String) -> Unit,
  onBiometricUnlock: () -> Unit,
  onSendPasswordReset: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (uiState.entryDestination != EntryAuthDestination.LANDING) {
    BackHandler { onBackToLanding() }
  }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 720.dp).fillMaxSize().testTag("entry_lazy_column"),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      if (!uiState.isBackendConfigured) {
        item { SupabaseConfigBanner() }
      }

      item {
        EntryHeroBanner()
      }

      if (uiState.errorMessage != null || uiState.infoMessage != null) {
        item {
          FeedbackMessageCard(
            errorMessage = uiState.errorMessage,
            infoMessage = uiState.infoMessage,
          )
        }
      }

      when (uiState.entryDestination) {
        EntryAuthDestination.LANDING -> {
          item {
            Card(
              modifier = Modifier.fillMaxWidth().testTag("option_owner_card"),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
              elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
              Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Box(
                    modifier =
                      Modifier.size(44.dp)
                        .background(
                          MaterialTheme.colorScheme.primaryContainer,
                          RoundedCornerShape(12.dp),
                        ),
                    contentAlignment = Alignment.Center,
                  ) {
                    Icon(
                      imageVector = Icons.Filled.Apartment,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                    )
                  }
                  Column(modifier = Modifier.weight(1f)) {
                    StatusPill(
                      label = "ORGANISATION CREATION",
                      containerColor = MaterialTheme.colorScheme.primaryContainer,
                      contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                      text = "Option A — Create an Organisation",
                      style = MaterialTheme.typography.titleLarge,
                    )
                  }
                }
                Text(
                  text = stringResource(R.string.owner_option_desc),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                  onClick = onStartOwnerOnboarding,
                  modifier =
                    Modifier.fillMaxWidth()
                      .height(50.dp)
                      .testTag("register_owner_entry_button"),
                ) {
                  Icon(Icons.Filled.Apartment, contentDescription = null)
                  Spacer(Modifier.width(8.dp))
                  Text(stringResource(R.string.register_owner_button))
                }
              }
            }
          }

          item {
            Card(
              modifier = Modifier.fillMaxWidth().testTag("option_supervisor_card"),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
              elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
              Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Box(
                    modifier =
                      Modifier.size(44.dp)
                        .background(
                          MaterialTheme.colorScheme.tertiaryContainer,
                          RoundedCornerShape(12.dp),
                        ),
                    contentAlignment = Alignment.Center,
                  ) {
                    Icon(
                      imageVector = Icons.Filled.SupervisorAccount,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.tertiary,
                    )
                  }
                  Column(modifier = Modifier.weight(1f)) {
                    StatusPill(
                      label = "INVITATION REQUIRED",
                      containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                      contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                      text = "Option B — Join as a Supervisor",
                      style = MaterialTheme.typography.titleLarge,
                    )
                  }
                }
                Text(
                  text = stringResource(R.string.supervisor_option_desc),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                  onClick = onStartSupervisorOnboarding,
                  modifier =
                    Modifier.fillMaxWidth()
                      .height(50.dp)
                      .testTag("join_supervisor_entry_button"),
                ) {
                  Icon(Icons.Filled.VerifiedUser, contentDescription = null)
                  Spacer(Modifier.width(8.dp))
                  Text(stringResource(R.string.join_supervisor_button))
                }
              }
            }
          }

          item {
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                if (hasBiometricSavedSession) {
                  Button(
                    onClick = onBiometricUnlock,
                    colors =
                      ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                      ),
                    modifier =
                      Modifier.fillMaxWidth()
                        .height(48.dp)
                        .testTag("entry_biometric_unlock_button"),
                  ) {
                    Icon(Icons.Filled.Fingerprint, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Unlock Saved Session with Biometric")
                  }
                }
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  TextButton(
                    onClick = onOpenSignIn,
                    modifier = Modifier.testTag("already_have_account_sign_in_button"),
                  ) {
                    Text(stringResource(R.string.already_have_account_sign_in))
                  }
                  TextButton(
                    onClick = onOpenForgotPassword,
                    modifier = Modifier.testTag("forgot_password_entry_button"),
                  ) {
                    Text(stringResource(R.string.forgot_password))
                  }
                }
              }
            }
          }
        }

        EntryAuthDestination.SIGN_IN -> {
          item {
            SignInFormCard(
              isLoading = uiState.isSubmitting || uiState.isRestoringSession,
              hasBiometricSavedSession = hasBiometricSavedSession,
              onSignIn = onSignIn,
              onBiometricUnlock = onBiometricUnlock,
              onForgotPassword = onOpenForgotPassword,
              onBack = onBackToLanding,
            )
          }
        }

        EntryAuthDestination.FORGOT_PASSWORD -> {
          item {
            ForgotPasswordCard(
              isLoading = uiState.isSubmitting,
              onSendReset = onSendPasswordReset,
              onBackToSignIn = onOpenSignIn,
              onBack = onBackToLanding,
            )
          }
        }

        else -> Unit
      }
    }
  }
}

@Composable
private fun EntryHeroBanner() {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
  ) {
    Box(modifier = Modifier.fillMaxWidth().height(190.dp)) {
      Image(
        painter = painterResource(id = R.drawable.img_workplace_hero_1795004542758),
        contentDescription = "CivicTrack organisational headquarters",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
      )
      Box(
        modifier =
          Modifier.fillMaxSize()
            .background(
              Brush.verticalGradient(
                colors =
                  listOf(
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.92f),
                  )
              )
            )
      )
      Column(
        modifier = Modifier.align(Alignment.BottomStart).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        StatusPill(
          label = "INSTITUTIONAL WORKFORCE PLATFORM",
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
        )
        Text(
          text = stringResource(R.string.welcome_title),
          style = MaterialTheme.typography.headlineLarge,
          color = Color.White,
        )
        Text(
          text = stringResource(R.string.welcome_subtitle),
          style = MaterialTheme.typography.bodyMedium,
          color = Color.White.copy(alpha = 0.9f),
        )
      }
    }
  }
}

@Composable
private fun SignInFormCard(
  isLoading: Boolean,
  hasBiometricSavedSession: Boolean,
  onSignIn: (String, String) -> Unit,
  onBiometricUnlock: () -> Unit,
  onForgotPassword: () -> Unit,
  onBack: () -> Unit,
) {
  var email by rememberSaveable { mutableStateOf("") }
  var password by rememberSaveable { mutableStateOf("") }

  Card(
    modifier = Modifier.fillMaxWidth().testTag("sign_in_form_card"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      TextButton(onClick = onBack, modifier = Modifier.testTag("sign_in_back_button")) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text("Back to Account Options")
      }
      Text(text = "Sign In to CivicTrack", style = MaterialTheme.typography.headlineMedium)
      Text(
        text =
          "Sign in with your verified Owner, Supervisor, or Organisation Employee account. CivicTrack routes directly to your assigned workspace.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      if (hasBiometricSavedSession) {
        Button(
          onClick = onBiometricUnlock,
          enabled = !isLoading,
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
          modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sign_in_biometric_button"),
        ) {
          Icon(Icons.Filled.Fingerprint, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Unlock Saved Session")
        }
      }

      OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text(stringResource(R.string.email_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("sign_in_email_input"),
      )
      OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text(stringResource(R.string.password_label)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth().testTag("sign_in_password_input"),
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
      ) {
        TextButton(onClick = onForgotPassword) {
          Text(stringResource(R.string.forgot_password))
        }
      }

      Button(
        onClick = { onSignIn(email, password) },
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sign_in_submit_button"),
      ) {
        if (isLoading) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary,
          )
        } else {
          Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text(stringResource(R.string.sign_in_button))
        }
      }
    }
  }
}

@Composable
private fun ForgotPasswordCard(
  isLoading: Boolean,
  onSendReset: (String) -> Unit,
  onBackToSignIn: () -> Unit,
  onBack: () -> Unit,
) {
  var email by rememberSaveable { mutableStateOf("") }

  Card(
    modifier = Modifier.fillMaxWidth().testTag("forgot_password_card"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      TextButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text("Back to Account Options")
      }
      Text(text = "Reset Password", style = MaterialTheme.typography.headlineMedium)
      Text(
        text = "Enter the email associated with your CivicTrack account to receive a password recovery link.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text(stringResource(R.string.email_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("forgot_password_email_input"),
      )
      Button(
        onClick = { onSendReset(email) },
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("forgot_password_submit_button"),
      ) {
        if (isLoading) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary,
          )
        } else {
          Icon(Icons.Filled.LockReset, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Send Recovery Link")
        }
      }
      TextButton(onClick = onBackToSignIn, modifier = Modifier.align(Alignment.CenterHorizontally)) {
        Text("Return to Sign In")
      }
    }
  }
}
