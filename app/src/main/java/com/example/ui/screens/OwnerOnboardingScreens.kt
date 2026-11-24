package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DomainAdd
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.CreatedInvitationResult
import com.example.data.model.OrgMemberRole
import com.example.data.model.OwnerOnboardingStep
import com.example.ui.OnboardingUiState
import com.example.ui.components.FeedbackMessageCard
import com.example.ui.components.OnboardingStepIndicator
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalCivicStatusPalette

@Composable
fun OwnerOnboardingFlowScreen(
  uiState: OnboardingUiState,
  onBackToEntry: () -> Unit,
  onSwitchToSignIn: () -> Unit,
  onRegisterOwnerAccount: (String, String, String, String, String) -> Unit,
  onCreateOrganization: (String, String, String, String) -> Unit,
  onSaveOrgProfile: (String, String, String, String, Boolean) -> Unit,
  onPickOrgLogo: () -> Unit,
  onCaptureCurrentGps: () -> Unit,
  onAddWorkplace: (String, String, String, String, String) -> Unit,
  onAddDepartment: (String, String) -> Unit,
  onSaveAttendanceSettings: (Boolean, Boolean, Boolean, List<String>, String, String, Int) -> Unit,
  onSendInvitation: (String, OrgMemberRole, String?, String?) -> Unit,
  onGoToDashboard: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (uiState.ownerStep == OwnerOnboardingStep.ACCOUNT_REGISTRATION) {
    BackHandler { onBackToEntry() }
  } else if (uiState.activeOrganization != null && uiState.ownerStep == OwnerOnboardingStep.STRUCTURE_SETUP) {
    BackHandler { onGoToDashboard() }
  }

  val ownerSteps =
    remember {
      listOf(
        1 to "Account",
        2 to "Organisation",
        3 to "Org Profile",
        4 to "Setup & Invites",
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
              containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
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
                text = "Organisation Owner Onboarding",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
              )
              StatusPill(
                label = "OWNER FLOW",
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
              )
            }
            OnboardingStepIndicator(
              steps = ownerSteps,
              currentStepNumber = uiState.ownerStep.stepNumber,
              accentColor = MaterialTheme.colorScheme.primary,
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
            onSignInAfterVerification = onSwitchToSignIn,
          )
        }
      }

      when (uiState.ownerStep) {
        OwnerOnboardingStep.ACCOUNT_REGISTRATION -> {
          item {
            OwnerStep1AccountCard(
              isLoading = uiState.isSubmitting,
              onRegister = onRegisterOwnerAccount,
              onBack = onBackToEntry,
              onSwitchToSignIn = onSwitchToSignIn,
            )
          }
        }
        OwnerOnboardingStep.ORGANISATION_CREATION -> {
          item {
            OwnerStep2OrganizationCard(
              isLoading = uiState.isSubmitting,
              onCreateOrganization = onCreateOrganization,
            )
          }
        }
        OwnerOnboardingStep.ORGANISATION_PROFILE -> {
          item {
            OwnerStep3OrgProfileCard(
              orgName = uiState.activeOrganization?.name.orEmpty(),
              logoUrl = uiState.activeOrganization?.logoUrl,
              existingDesc = uiState.activeOrganization?.description.orEmpty(),
              existingEmail = uiState.activeOrganization?.contactEmail.orEmpty(),
              existingPhone = uiState.activeOrganization?.contactPhone.orEmpty(),
              existingAddress = uiState.activeOrganization?.address.orEmpty(),
              isLoading = uiState.isSubmitting,
              onPickLogo = onPickOrgLogo,
              onSave = { desc, email, phone, addr ->
                onSaveOrgProfile(desc, email, phone, addr, false)
              },
              onSkip = { onSaveOrgProfile("", "", "", "", true) },
            )
          }
        }
        OwnerOnboardingStep.STRUCTURE_SETUP -> {
          item {
            OwnerStep4StructureWizard(
              uiState = uiState,
              onCaptureCurrentGps = onCaptureCurrentGps,
              onAddWorkplace = onAddWorkplace,
              onAddDepartment = onAddDepartment,
              onSaveAttendanceSettings = onSaveAttendanceSettings,
              onSendInvitation = onSendInvitation,
              onGoToDashboard = onGoToDashboard,
            )
          }
        }
        OwnerOnboardingStep.DASHBOARD -> Unit
      }
    }
  }
}

@Composable
private fun OwnerStep1AccountCard(
  isLoading: Boolean,
  onRegister: (String, String, String, String, String) -> Unit,
  onBack: () -> Unit,
  onSwitchToSignIn: () -> Unit,
) {
  var fullName by rememberSaveable { mutableStateOf("") }
  var email by rememberSaveable { mutableStateOf("") }
  var phone by rememberSaveable { mutableStateOf("") }
  var password by rememberSaveable { mutableStateOf("") }
  var confirmPassword by rememberSaveable { mutableStateOf("") }

  Card(
    modifier = Modifier.fillMaxWidth().testTag("owner_step1_card"),
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
      Text(text = "Step 1: Owner Account Registration", style = MaterialTheme.typography.headlineMedium)
      Text(
        text =
          "Create your administrator credentials. After verification, you will register your organisation.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      OutlinedTextField(
        value = fullName,
        onValueChange = { fullName = it },
        label = { Text(stringResource(R.string.full_name_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("owner_full_name_input"),
      )
      OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Official Work Email") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("owner_email_input"),
      )
      OutlinedTextField(
        value = phone,
        onValueChange = { phone = it },
        label = { Text("Phone Number (Optional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("owner_phone_input"),
      )
      OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password (min 8 chars, letter + number)") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth().testTag("owner_password_input"),
      )
      OutlinedTextField(
        value = confirmPassword,
        onValueChange = { confirmPassword = it },
        label = { Text(stringResource(R.string.confirm_password_label)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth().testTag("owner_confirm_password_input"),
      )

      Button(
        onClick = { onRegister(fullName, email, phone, password, confirmPassword) },
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("owner_register_submit_button"),
      ) {
        if (isLoading) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary,
          )
        } else {
          Icon(Icons.Filled.PersonAdd, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Create Owner Account")
        }
      }

      TextButton(
        onClick = onSwitchToSignIn,
        modifier = Modifier.align(Alignment.CenterHorizontally),
      ) {
        Text(stringResource(R.string.already_have_account_sign_in))
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OwnerStep2OrganizationCard(
  isLoading: Boolean,
  onCreateOrganization: (String, String, String, String) -> Unit,
) {
  var orgName by rememberSaveable { mutableStateOf("") }
  var registrationNumber by rememberSaveable { mutableStateOf("") }
  var countryCode by rememberSaveable { mutableStateOf("UG") }
  var currencyCode by rememberSaveable { mutableStateOf("UGX") }

  val countryPresets = listOf("UG" to "Uganda", "KE" to "Kenya", "TZ" to "Tanzania", "RW" to "Rwanda")
  val currencyPresets = listOf("UGX", "KES", "TZS", "RWF", "USD")

  Card(
    modifier = Modifier.fillMaxWidth().testTag("owner_step2_org_card"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "Step 2: Register Your Organisation",
        style = MaterialTheme.typography.headlineMedium,
      )
      Text(
        text =
          "Creates your institutional workspace and assigns your authenticated account as Organisation Owner via CivicTrack's server function.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      OutlinedTextField(
        value = orgName,
        onValueChange = { orgName = it },
        label = { Text("Organisation Name *") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("org_name_input"),
      )
      OutlinedTextField(
        value = registrationNumber,
        onValueChange = { registrationNumber = it },
        label = { Text("Organisation Registration Number (Optional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("org_reg_number_input"),
      )

      Text("Country (Default: Uganda — UG)", style = MaterialTheme.typography.labelLarge)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        countryPresets.forEach { (code, name) ->
          FilterChip(
            selected = countryCode.equals(code, ignoreCase = true),
            onClick = {
              countryCode = code
              currencyCode =
                when (code) {
                  "UG" -> "UGX"
                  "KE" -> "KES"
                  "TZ" -> "TZS"
                  "RW" -> "RWF"
                  else -> currencyCode
                }
            },
            label = { Text("$name ($code)") },
          )
        }
      }
      OutlinedTextField(
        value = countryCode,
        onValueChange = { if (it.length <= 2) countryCode = it.uppercase() },
        label = { Text("Country Code (ISO 2-letter)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("org_country_input"),
      )

      Text("Currency (Default: Ugandan Shilling — UGX)", style = MaterialTheme.typography.labelLarge)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        currencyPresets.forEach { code ->
          FilterChip(
            selected = currencyCode.equals(code, ignoreCase = true),
            onClick = { currencyCode = code },
            label = { Text(code) },
          )
        }
      }
      OutlinedTextField(
        value = currencyCode,
        onValueChange = { if (it.length <= 3) currencyCode = it.uppercase() },
        label = { Text("Currency Code (ISO 3-letter)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("org_currency_input"),
      )

      Button(
        onClick = {
          onCreateOrganization(orgName, registrationNumber, countryCode, currencyCode)
        },
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("create_organization_button"),
      ) {
        if (isLoading) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary,
          )
        } else {
          Icon(Icons.Filled.Apartment, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Create Organisation")
        }
      }
    }
  }
}

@Composable
private fun OwnerStep3OrgProfileCard(
  orgName: String,
  logoUrl: String?,
  existingDesc: String,
  existingEmail: String,
  existingPhone: String,
  existingAddress: String,
  isLoading: Boolean,
  onPickLogo: () -> Unit,
  onSave: (String, String, String, String) -> Unit,
  onSkip: () -> Unit,
) {
  var description by rememberSaveable(orgName) { mutableStateOf(existingDesc) }
  var contactEmail by rememberSaveable(orgName) { mutableStateOf(existingEmail) }
  var contactPhone by rememberSaveable(orgName) { mutableStateOf(existingPhone) }
  var address by rememberSaveable(orgName) { mutableStateOf(existingAddress) }

  Card(
    modifier = Modifier.fillMaxWidth().testTag("owner_step3_profile_card"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "Step 3: Organisation Profile ($orgName)",
        style = MaterialTheme.typography.headlineMedium,
      )
      Text(
        text =
          "Add institutional details for team members and official reports. All fields here are optional.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        if (!logoUrl.isNullOrBlank()) {
          AsyncImage(
            model = logoUrl,
            contentDescription = "Organisation logo",
            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
          )
        }
        OutlinedButton(onClick = onPickLogo, modifier = Modifier.testTag("upload_org_logo_button")) {
          Icon(Icons.Filled.Image, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text(if (logoUrl.isNullOrBlank()) "Upload Organisation Logo" else "Change Logo")
        }
      }

      OutlinedTextField(
        value = description,
        onValueChange = { description = it },
        label = { Text("Organisation Description") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth().testTag("org_description_input"),
      )
      OutlinedTextField(
        value = contactEmail,
        onValueChange = { contactEmail = it },
        label = { Text("Official Contact Email") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("org_contact_email_input"),
      )
      OutlinedTextField(
        value = contactPhone,
        onValueChange = { contactPhone = it },
        label = { Text("Official Contact Phone") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("org_contact_phone_input"),
      )
      OutlinedTextField(
        value = address,
        onValueChange = { address = it },
        label = { Text("Headquarters Address") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("org_address_input"),
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = onSkip,
          enabled = !isLoading,
          modifier = Modifier.weight(1f).height(48.dp).testTag("skip_org_profile_button"),
        ) {
          Text("Skip for Now")
        }
        Button(
          onClick = { onSave(description, contactEmail, contactPhone, address) },
          enabled = !isLoading,
          modifier = Modifier.weight(1f).height(48.dp).testTag("save_org_profile_button"),
        ) {
          Text("Save & Continue")
          Spacer(Modifier.width(6.dp))
          Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OwnerStep4StructureWizard(
  uiState: OnboardingUiState,
  onCaptureCurrentGps: () -> Unit,
  onAddWorkplace: (String, String, String, String, String) -> Unit,
  onAddDepartment: (String, String) -> Unit,
  onSaveAttendanceSettings: (Boolean, Boolean, Boolean, List<String>, String, String, Int) -> Unit,
  onSendInvitation: (String, OrgMemberRole, String?, String?) -> Unit,
  onGoToDashboard: () -> Unit,
) {
  var selectedSection by rememberSaveable { mutableIntStateOf(0) }
  val sections = listOf("Workplaces", "Departments", "Rules", "Invitations")
  val dashboard = uiState.ownerDashboard

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "Step 4: Establish Organisation Structure",
          style = MaterialTheme.typography.headlineMedium,
        )
        Text(
          text =
            "Configure your workplaces, departments, attendance policy, and team invitations progressively. You can open the Owner Dashboard at any time.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
          onClick = onGoToDashboard,
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("wizard_go_to_dashboard_button"),
        ) {
          Icon(Icons.Filled.Dashboard, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Continue to Owner Dashboard")
        }
      }
    }

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
      sections.forEachIndexed { idx, label ->
        SegmentedButton(
          selected = selectedSection == idx,
          onClick = { selectedSection = idx },
          shape = SegmentedButtonDefaults.itemShape(index = idx, count = sections.size),
        ) {
          Text(label, maxLines = 1)
        }
      }
    }

    when (selectedSection) {
      0 ->
        AddWorkplaceSection(
          uiState = uiState,
          existingWorkplacesCount = dashboard?.workplaces?.size ?: 0,
          onCaptureGps = onCaptureCurrentGps,
          onAddWorkplace = onAddWorkplace,
        )
      1 ->
        AddDepartmentSection(
          isLoading = uiState.isSubmitting,
          existingDepartments = dashboard?.departments.orEmpty(),
          onAddDepartment = onAddDepartment,
        )
      2 ->
        AttendanceRulesSection(
          isLoading = uiState.isSubmitting,
          currentSettings = dashboard?.settings,
          onSave = onSaveAttendanceSettings,
        )
      3 ->
        InviteTeamSection(
          uiState = uiState,
          onSendInvitation = onSendInvitation,
        )
    }
  }
}

@Composable
private fun AddWorkplaceSection(
  uiState: OnboardingUiState,
  existingWorkplacesCount: Int,
  onCaptureGps: () -> Unit,
  onAddWorkplace: (String, String, String, String, String) -> Unit,
) {
  var wpName by rememberSaveable { mutableStateOf("") }
  var wpAddress by rememberSaveable { mutableStateOf("") }
  var latText by rememberSaveable { mutableStateOf("0.3476") } // Kampala default editable
  var lngText by rememberSaveable { mutableStateOf("32.5825") }
  var radiusText by rememberSaveable { mutableStateOf("150") }

  LaunchedEffect(uiState.capturedWorkplaceLat, uiState.capturedWorkplaceLng) {
    uiState.capturedWorkplaceLat?.let { latText = "%.6f".format(it) }
    uiState.capturedWorkplaceLng?.let { lngText = "%.6f".format(it) }
  }

  Card(
    modifier = Modifier.fillMaxWidth().testTag("wizard_workplace_card"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Add Workplace & Geofence", style = MaterialTheme.typography.titleLarge)
        StatusPill(
          label = "$existingWorkplacesCount added",
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
      }
      OutlinedTextField(
        value = wpName,
        onValueChange = { wpName = it },
        label = { Text("Workplace Name (e.g. Kampala HQ)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("workplace_name_input"),
      )
      OutlinedTextField(
        value = wpAddress,
        onValueChange = { wpAddress = it },
        label = { Text("Physical Address") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      OutlinedButton(onClick = onCaptureGps, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.GpsFixed, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Use Current Device GPS Coordinates")
      }
      if (!uiState.gpsCaptureStatus.isNullOrBlank()) {
        Text(
          text = uiState.gpsCaptureStatus,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = latText,
          onValueChange = { latText = it },
          label = { Text("Latitude") },
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = lngText,
          onValueChange = { lngText = it },
          label = { Text("Longitude") },
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
      }
      OutlinedTextField(
        value = radiusText,
        onValueChange = { radiusText = it },
        label = { Text("Permitted Geofence Radius (meters)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      Button(
        onClick = { onAddWorkplace(wpName, wpAddress, latText, lngText, radiusText) },
        enabled = !uiState.isSubmitting,
        modifier = Modifier.fillMaxWidth().testTag("add_workplace_submit_button"),
      ) {
        Icon(Icons.Filled.AddBusiness, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Save Workplace")
      }
    }
  }
}

@Composable
private fun AddDepartmentSection(
  isLoading: Boolean,
  existingDepartments: List<com.example.data.model.DepartmentDto>,
  onAddDepartment: (String, String) -> Unit,
) {
  var deptName by rememberSaveable { mutableStateOf("") }
  var deptDesc by rememberSaveable { mutableStateOf("") }

  Card(
    modifier = Modifier.fillMaxWidth().testTag("wizard_department_card"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Add Organisational Department", style = MaterialTheme.typography.titleLarge)
        StatusPill(
          label = "${existingDepartments.size} added",
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
      }
      OutlinedTextField(
        value = deptName,
        onValueChange = { deptName = it },
        label = { Text("Department Name (e.g. Field Operations)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("department_name_input"),
      )
      OutlinedTextField(
        value = deptDesc,
        onValueChange = { deptDesc = it },
        label = { Text("Description (Optional)") },
        modifier = Modifier.fillMaxWidth(),
      )
      Button(
        onClick = {
          onAddDepartment(deptName, deptDesc)
          if (deptName.isNotBlank()) {
            deptName = ""
            deptDesc = ""
          }
        },
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth().testTag("add_department_submit_button"),
      ) {
        Icon(Icons.Filled.DomainAdd, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Add Department")
      }

      if (existingDepartments.isNotEmpty()) {
        HorizontalDivider()
        existingDepartments.forEach { dept ->
          Text("• ${dept.name}", style = MaterialTheme.typography.bodyMedium)
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AttendanceRulesSection(
  isLoading: Boolean,
  currentSettings: com.example.data.model.OrganizationSettingsDto?,
  onSave: (Boolean, Boolean, Boolean, List<String>, String, String, Int) -> Unit,
) {
  var requireBiometric by rememberSaveable {
    mutableStateOf(currentSettings?.requireBiometricForAttendance ?: false)
  }
  var allowOutsideGeofence by rememberSaveable {
    mutableStateOf(currentSettings?.allowCheckInOutsideGeofence ?: false)
  }
  var requireLocation by rememberSaveable {
    mutableStateOf(currentSettings?.requireLocationVerification ?: true)
  }
  var startTime by rememberSaveable { mutableStateOf(currentSettings?.workStartTime ?: "08:30") }
  var endTime by rememberSaveable { mutableStateOf(currentSettings?.workEndTime ?: "17:00") }
  var graceMinutes by rememberSaveable {
    mutableStateOf((currentSettings?.lateGraceMinutes ?: 15).toString())
  }
  val allDays = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
  val selectedDays = remember {
    mutableStateListOf<String>().apply {
      addAll(currentSettings?.workingDays ?: listOf("MON", "TUE", "WED", "THU", "FRI"))
    }
  }

  Card(
    modifier = Modifier.fillMaxWidth().testTag("wizard_rules_card"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text("Attendance Settings & Schedule", style = MaterialTheme.typography.titleLarge)

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Require Location Geofence Verification", modifier = Modifier.weight(1f))
        Switch(checked = requireLocation, onCheckedChange = { requireLocation = it })
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Require Device Biometric Unlock on Check-In", modifier = Modifier.weight(1f))
        Switch(checked = requireBiometric, onCheckedChange = { requireBiometric = it })
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Log Outside-Geofence Attempts as Flagged", modifier = Modifier.weight(1f))
        Switch(checked = allowOutsideGeofence, onCheckedChange = { allowOutsideGeofence = it })
      }

      Text("Working Days", style = MaterialTheme.typography.labelLarge)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        allDays.forEach { day ->
          val active = day in selectedDays
          FilterChip(
            selected = active,
            onClick = {
              if (active) selectedDays.remove(day) else selectedDays.add(day)
            },
            label = { Text(day) },
          )
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = startTime,
          onValueChange = { startTime = it },
          label = { Text("Start (HH:mm)") },
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = endTime,
          onValueChange = { endTime = it },
          label = { Text("End (HH:mm)") },
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = graceMinutes,
          onValueChange = { graceMinutes = it },
          label = { Text("Grace (m)") },
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
      }

      Button(
        onClick = {
          onSave(
            requireBiometric,
            allowOutsideGeofence,
            requireLocation,
            selectedDays.toList(),
            startTime,
            endTime,
            graceMinutes.toIntOrNull() ?: 15,
          )
        },
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth().testTag("save_attendance_settings_button"),
      ) {
        Icon(Icons.Filled.Schedule, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Save Attendance Settings")
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InviteTeamSection(
  uiState: OnboardingUiState,
  onSendInvitation: (String, OrgMemberRole, String?, String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val palette = LocalCivicStatusPalette.current
  var email by rememberSaveable { mutableStateOf("") }
  var inviteRole by rememberSaveable { mutableStateOf(OrgMemberRole.SUPERVISOR) }
  var selectedDeptId by rememberSaveable { mutableStateOf<String?>(null) }
  var selectedWorkplaceId by rememberSaveable { mutableStateOf<String?>(null) }

  val departments = uiState.ownerDashboard?.departments.orEmpty()
  val workplaces = uiState.ownerDashboard?.workplaces.orEmpty()

  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Card(
      modifier = Modifier.fillMaxWidth().testTag("invite_team_card"),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text("Invite Supervisor or Employee", style = MaterialTheme.typography.titleLarge)
        Text(
          text =
            "Generates a secure hashed invitation linked to ${uiState.activeOrganization?.name ?: "your organisation"}. Only token hashes are saved in Supabase.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = inviteRole == OrgMemberRole.SUPERVISOR,
            onClick = { inviteRole = OrgMemberRole.SUPERVISOR },
            label = { Text("Supervisor") },
            modifier = Modifier.testTag("invite_role_supervisor_chip"),
          )
          FilterChip(
            selected = inviteRole == OrgMemberRole.EMPLOYEE,
            onClick = { inviteRole = OrgMemberRole.EMPLOYEE },
            label = { Text("Employee") },
            modifier = Modifier.testTag("invite_role_employee_chip"),
          )
        }

        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Invitee Email Address *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("invite_email_input"),
        )

        if (departments.isNotEmpty()) {
          Text("Assigned Department (Optional)", style = MaterialTheme.typography.labelLarge)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = selectedDeptId == null,
              onClick = { selectedDeptId = null },
              label = { Text("Any") },
            )
            departments.forEach { dept ->
              FilterChip(
                selected = selectedDeptId == dept.id,
                onClick = { selectedDeptId = dept.id },
                label = { Text(dept.name) },
              )
            }
          }
        }

        if (workplaces.isNotEmpty()) {
          Text("Assigned Workplace (Optional)", style = MaterialTheme.typography.labelLarge)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = selectedWorkplaceId == null,
              onClick = { selectedWorkplaceId = null },
              label = { Text("Any") },
            )
            workplaces.forEach { wp ->
              FilterChip(
                selected = selectedWorkplaceId == wp.id,
                onClick = { selectedWorkplaceId = wp.id },
                label = { Text(wp.name) },
              )
            }
          }
        }

        Button(
          onClick = { onSendInvitation(email, inviteRole, selectedDeptId, selectedWorkplaceId) },
          enabled = !uiState.isSubmitting,
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("create_invitation_button"),
        ) {
          Icon(Icons.Filled.GroupAdd, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Issue Secure Invitation")
        }
      }
    }

    uiState.lastCreatedInvitation?.let { created ->
      OneTimeInvitationDeliveryCard(
        created = created,
        verifiedContainer = palette.verifiedContainer,
        verifiedTint = palette.verified,
        onCopy = { text ->
          val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
          clipboard.setPrimaryClip(ClipData.newPlainText("CivicTrack Invite", text))
          Toast.makeText(context, "Invitation copied to clipboard", Toast.LENGTH_SHORT).show()
        },
        onShare = { result ->
          val shareIntent =
            Intent(Intent.ACTION_SEND).apply {
              type = "text/plain"
              putExtra(Intent.EXTRA_EMAIL, arrayOf(result.invitation.email))
              putExtra(
                Intent.EXTRA_SUBJECT,
                "CivicTrack Invitation: ${uiState.activeOrganization?.name ?: "Organisation"}",
              )
              putExtra(
                Intent.EXTRA_TEXT,
                "You have been invited to join CivicTrack as ${result.invitation.role}.\n\nOpen Link: ${result.oneTimeDeepLink}\nOr paste Token: ${result.oneTimeRawToken}",
              )
            }
          context.startActivity(Intent.createChooser(shareIntent, "Share CivicTrack Invitation"))
        },
      )
    }
  }
}

@Composable
private fun OneTimeInvitationDeliveryCard(
  created: CreatedInvitationResult,
  verifiedContainer: androidx.compose.ui.graphics.Color,
  verifiedTint: androidx.compose.ui.graphics.Color,
  onCopy: (String) -> Unit,
  onShare: (CreatedInvitationResult) -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth().testTag("created_invitation_delivery_card"),
    colors = CardDefaults.cardColors(containerColor = verifiedContainer),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = "One-Time Invitation Token Ready",
        style = MaterialTheme.typography.titleMedium,
        color = verifiedTint,
      )
      Text(
        text =
          "Recipient: ${created.invitation.email} (${created.invitation.role}). Copy or share this token now — only its cryptographic hash is stored in Supabase.",
        style = MaterialTheme.typography.bodyMedium,
      )
      Text(
        text = "Token: ${created.oneTimeRawToken}",
        style = MaterialTheme.typography.labelMedium,
      )
      Text(
        text = "Deep Link: ${created.oneTimeDeepLink}",
        style = MaterialTheme.typography.labelSmall,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { onCopy(created.oneTimeRawToken) }, modifier = Modifier.weight(1f)) {
          Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text("Copy Token")
        }
        Button(onClick = { onShare(created) }, modifier = Modifier.weight(1f)) {
          Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text("Send Invite")
        }
      }
    }
  }
}
