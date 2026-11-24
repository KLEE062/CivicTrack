package com.example.ui.frontend.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.frontend.components.CivicFeedbackBannerRow
import com.example.ui.frontend.components.CivicFormField
import com.example.ui.frontend.components.CivicPill
import com.example.ui.frontend.components.CivicPrimaryButton
import com.example.ui.frontend.components.CivicResponsiveScreenContainer
import com.example.ui.frontend.components.CivicSecondaryButton
import com.example.ui.frontend.components.SupabaseConnectionCard
import com.example.ui.frontend.state.FrontendUiState
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
  onContinue: () -> Unit,
) {
  var started by remember { mutableStateOf(false) }
  val scale by animateFloatAsState(
    targetValue = if (started) 1f else 0.88f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "splash_scale",
  )

  LaunchedEffect(Unit) {
    started = true
    delay(900)
    onContinue()
  }

  Surface(
    modifier = Modifier.fillMaxSize().testTag("splash_screen"),
    color = MaterialTheme.colorScheme.secondary,
  ) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
      Column(
        modifier = Modifier.widthIn(max = 420.dp).scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Image(
          painter = painterResource(id = R.drawable.ic_civictrack_logo_1795004541308),
          contentDescription = "CivicTrack emblem",
          modifier = Modifier.size(88.dp).clip(RoundedCornerShape(22.dp)),
          contentScale = ContentScale.Crop,
        )
        Text(
          text = "CivicTrack",
          style = MaterialTheme.typography.headlineLarge,
          color = MaterialTheme.colorScheme.onSecondary,
        )
        Text(
          text = "Institutional Workforce Management & Attendance Accountability",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.85f),
          textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        CivicPrimaryButton(
          text = "Enter CivicTrack",
          onClick = onContinue,
          icon = Icons.AutoMirrored.Filled.ArrowForward,
          testTag = "splash_continue_button",
        )
      }
    }
  }
}

@Composable
fun WelcomeScreen(
  state: FrontendUiState,
  onRecheckConnection: () -> Unit,
  onGetStarted: () -> Unit,
  onSignIn: () -> Unit,
  onBiometricUnlock: () -> Unit,
  onDismissBanner: () -> Unit,
) {
  var showDiagnostics by remember { mutableStateOf(true) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .testTag("welcome_screen")
  ) {
    // 1. Full Page Background Hero Image
    Image(
      painter = painterResource(id = R.drawable.img_workplace_hero_1795004542758),
      contentDescription = "CivicTrack Workplace Hero Background",
      modifier = Modifier.fillMaxSize(),
      contentScale = ContentScale.Crop,
    )

    // 2. Downward Color Gradient Overlay (Fades to dark surface)
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color.Black.copy(alpha = 0.25f),
              Color.Black.copy(alpha = 0.65f),
              Color.Black.copy(alpha = 0.95f),
            )
          )
        )
    )

    // 3. Hero Content Overlaid on Gradient
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .testTag("civic_screen_list")
        .padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      CivicFeedbackBannerRow(
        feedbackMessage = state.feedbackBanner,
        errorMessage = state.errorBanner,
        onDismiss = onDismissBanner,
      )

      // Header Branding
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Image(
          painter = painterResource(id = R.drawable.ic_civictrack_logo_1795004541308),
          contentDescription = "CivicTrack logo",
          modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(14.dp)),
          contentScale = ContentScale.Crop,
        )
        Column {
          CivicPill(
            label = "INSTITUTIONAL WORKFORCE PLATFORM",
            containerColor = Color.White.copy(alpha = 0.2f),
            contentColor = Color.White,
          )
          Spacer(Modifier.height(2.dp))
          Text(
            text = "CivicTrack",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
          )
        }
      }

      Spacer(Modifier.height(12.dp))
      Text(
        text = "Workforce Governance & Hardware-Verified Attendance",
        style = MaterialTheme.typography.titleMedium,
        color = Color.White.copy(alpha = 0.95f),
      )
      Spacer(Modifier.height(6.dp))
      Text(
        text = "Register your institution, coordinate supervisors across departments, and verify real-time workforce attendance with precision geofencing.",
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.85f),
      )

      Spacer(Modifier.height(24.dp))

      // Action Buttons
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CivicPrimaryButton(
          text = "Get Started & Register Organisation",
          onClick = onGetStarted,
          icon = Icons.AutoMirrored.Filled.ArrowForward,
          testTag = "welcome_get_started_button",
        )
        CivicSecondaryButton(
          text = "Already Have an Account? Sign In",
          onClick = onSignIn,
          icon = Icons.AutoMirrored.Filled.Login,
          testTag = "welcome_sign_in_button",
        )
        if (state.hasStoredSessionForBiometricUnlock) {
          CivicSecondaryButton(
            text = "Unlock Saved Session with Biometrics",
            onClick = onBiometricUnlock,
            icon = Icons.Filled.Fingerprint,
            testTag = "welcome_biometric_unlock_button",
          )
        }
      }

      // Optional Server Diagnostics at Bottom
      Spacer(Modifier.height(14.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(Color.White.copy(alpha = 0.12f))
          .clickable { showDiagnostics = !showDiagnostics }
          .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = if (state.connectionReport.isConnected) Icons.Filled.CheckCircle else Icons.Filled.Shield,
            contentDescription = null,
            tint = if (state.connectionReport.isConnected) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = "Server Status: ${state.connectionReport.statusSummary}",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.85f),
          )
        }
        Text(
          text = if (showDiagnostics) "Hide" else "Diagnostics",
          style = MaterialTheme.typography.labelSmall,
          color = Color.White.copy(alpha = 0.9f),
        )
      }

      if (showDiagnostics) {
        Spacer(Modifier.height(8.dp))
        SupabaseConnectionCard(
          report = state.connectionReport,
          onRecheck = onRecheckConnection,
          isChecking = state.isCheckingConnection,
          onNavigateToRegistration = onGetStarted,
        )
      }
    }
  }
}

@Composable
fun RoleSelectionScreen(
  state: FrontendUiState,
  onSelectOwnerRegistration: () -> Unit,
  onSelectSupervisorInvite: () -> Unit,
  onSelectEmployeeInvite: () -> Unit,
  onOpenLogin: () -> Unit,
  onDismissBanner: () -> Unit,
) {
  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("role_selection_screen"),
  ) {
    item {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Choose How to Continue", style = MaterialTheme.typography.headlineMedium)
        Text(
          text = "Register an organisation, accept an authorised invitation, or sign in to an existing account.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    item {
      RoleOptionCard(
        badge = "ORGANISATION CREATION",
        title = "Organisation Owner",
        description =
          "Register your institution, configure workplaces, departments & schedules, and oversee all staff.",
        icon = Icons.Filled.Apartment,
        primaryLabel = "Register as Organisation Owner",
        primaryTag = "role_owner_register_button",
        onPrimary = onSelectOwnerRegistration,
      )
    }

    item {
      RoleOptionCard(
        badge = "INVITATION REQUIRED",
        title = "Supervisor",
        description =
          "Join an existing organisation through an authorised invitation scoped to a specific department and workplace.",
        icon = Icons.Filled.SupervisorAccount,
        primaryLabel = "Join with Supervisor Invitation",
        primaryTag = "role_supervisor_invite_button",
        onPrimary = onSelectSupervisorInvite,
      )
    }

    item {
      RoleOptionCard(
        badge = "WORKFORCE INVITATION",
        title = "Employee",
        description =
          "Accept an employee invitation to verify attendance, view schedules, apply for leave, and upload documents.",
        icon = Icons.Filled.Badge,
        primaryLabel = "Join with Employee Invitation",
        primaryTag = "role_employee_invite_button",
        onPrimary = onSelectEmployeeInvite,
      )
    }

    item {
      TextButton(
        onClick = onOpenLogin,
        modifier = Modifier.fillMaxWidth().testTag("role_selection_login_link"),
      ) {
        Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(6.dp))
        Text("Already have an account? Sign In")
      }
    }
  }
}

@Composable
private fun RoleOptionCard(
  badge: String,
  title: String,
  description: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  primaryLabel: String,
  primaryTag: String,
  onPrimary: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Box(
          modifier =
            Modifier.size(42.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center,
        ) {
          Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          CivicPill(
            label = badge,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
          )
          Text(title, style = MaterialTheme.typography.titleLarge)
        }
      }
      Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      CivicPrimaryButton(text = primaryLabel, onClick = onPrimary, testTag = primaryTag)
    }
  }
}

@Composable
fun OwnerRegistrationScreen(
  state: FrontendUiState,
  onSubmitRegistration: (
    fullName: String,
    email: String,
    phone: String,
    orgName: String,
    registrationNumber: String,
    countryCode: String,
    currencyCode: String,
    password: String,
    confirmPassword: String,
  ) -> Unit,
  onGoToLogin: () -> Unit,
  onDismissBanner: () -> Unit,
) {
  var fullName by rememberSaveable { mutableStateOf("") }
  var email by rememberSaveable { mutableStateOf("") }
  var phone by rememberSaveable { mutableStateOf("") }
  var orgName by rememberSaveable { mutableStateOf("") }
  var registrationNumber by rememberSaveable { mutableStateOf("") }
  var countryCode by rememberSaveable { mutableStateOf("UG") }
  var currencyCode by rememberSaveable { mutableStateOf("UGX") }
  var password by rememberSaveable { mutableStateOf("") }
  var confirmPassword by rememberSaveable { mutableStateOf("") }

  CivicResponsiveScreenContainer(
    feedbackMessage = state.feedbackBanner,
    errorMessage = state.errorBanner,
    onDismissBanner = onDismissBanner,
    modifier = Modifier.testTag("owner_registration_screen"),
  ) {
    item {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        CivicPill(
          label = "ORGANISATION OWNER ONBOARDING",
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Spacer(Modifier.height(4.dp))
        Text("Register Organisation & Owner", style = MaterialTheme.typography.headlineMedium)
        Text(
          text = "Create the primary administrator account via Supabase Auth and register your institution.",
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
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text("Owner Account", style = MaterialTheme.typography.titleMedium)
          CivicFormField(
            value = fullName,
            onValueChange = { fullName = it },
            label = "Full Name",
            enabled = !state.isSubmitting,
            testTag = "owner_reg_name_input",
          )
          CivicFormField(
            value = email,
            onValueChange = { email = it },
            label = "Work Email",
            enabled = !state.isSubmitting,
            testTag = "owner_reg_email_input",
          )
          CivicFormField(
            value = phone,
            onValueChange = { phone = it },
            label = "Phone Number (Optional)",
            enabled = !state.isSubmitting,
          )
        }
      }
    }

    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text("Organisation Details", style = MaterialTheme.typography.titleMedium)
          CivicFormField(
            value = orgName,
            onValueChange = { orgName = it },
            label = "Organisation Name",
            enabled = !state.isSubmitting,
            testTag = "owner_reg_org_input",
          )
          CivicFormField(
            value = registrationNumber,
            onValueChange = { registrationNumber = it },
            label = "Registration Number (Optional)",
            enabled = !state.isSubmitting,
            testTag = "owner_reg_number_input",
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Box(modifier = Modifier.weight(1f)) {
              CivicFormField(
                value = countryCode,
                onValueChange = { countryCode = it.take(2).uppercase() },
                label = "Country",
                enabled = !state.isSubmitting,
              )
            }
            Box(modifier = Modifier.weight(1f)) {
              CivicFormField(
                value = currencyCode,
                onValueChange = { currencyCode = it.take(3).uppercase() },
                label = "Currency",
                enabled = !state.isSubmitting,
              )
            }
          }
        }
      }
    }

    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text("Account Security", style = MaterialTheme.typography.titleMedium)
          CivicFormField(
            value = password,
            onValueChange = { password = it },
            label = "Password (min 8 chars, letter + number/symbol)",
            visualTransformation = PasswordVisualTransformation(),
            enabled = !state.isSubmitting,
            testTag = "owner_reg_password_input",
          )
          CivicFormField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = "Confirm Password",
            visualTransformation = PasswordVisualTransformation(),
            enabled = !state.isSubmitting,
            testTag = "owner_reg_confirm_input",
          )
        }
      }
    }

    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CivicPrimaryButton(
          text = if (state.isSubmitting) "Registering Organisation..." else "Create Owner Account & Organisation",
          icon = Icons.Filled.Shield,
          enabled = !state.isSubmitting,
          testTag = "owner_reg_submit_button",
          onClick = {
            onSubmitRegistration(
              fullName,
              email,
              phone,
              orgName,
              registrationNumber,
              countryCode,
              currencyCode,
              password,
              confirmPassword,
            )
          },
        )
        TextButton(
          onClick = onGoToLogin,
          enabled = !state.isSubmitting,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text("Already registered? Sign In")
        }
      }
    }
  }
}
