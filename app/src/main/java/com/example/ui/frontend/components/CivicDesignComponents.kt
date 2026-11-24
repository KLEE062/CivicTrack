package com.example.ui.frontend.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.data.model.SupabaseConnectionReport
import com.example.ui.frontend.data.CivicAttendanceStatus
import com.example.ui.frontend.data.CivicEmployee
import com.example.ui.frontend.data.CivicLeaveStatus
import com.example.ui.frontend.data.CivicNotification
import com.example.ui.theme.LocalCivicStatusPalette

@Composable
fun CivicSummaryCard(
  title: String,
  value: String,
  subtitle: String,
  icon: ImageVector,
  accentColor: Color = MaterialTheme.colorScheme.primary,
  onClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier =
      modifier.then(
        if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
      ),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        Box(
          modifier =
            Modifier.size(28.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(accentColor.copy(alpha = 0.12f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(16.dp),
          )
        }
      }
      Text(
        text = value,
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun CivicEmployeeCard(
  employee: CivicEmployee,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalCivicStatusPalette.current
  val isLeave = employee.status.contains("Leave", ignoreCase = true)
  val isInvited = employee.status.contains("Invited", ignoreCase = true)
  val pillBg =
    when {
      isLeave -> palette.warningContainer
      isInvited -> MaterialTheme.colorScheme.tertiaryContainer
      else -> palette.verifiedContainer
    }
  val pillFg =
    when {
      isLeave -> palette.warning
      isInvited -> MaterialTheme.colorScheme.onTertiaryContainer
      else -> palette.verified
    }

  Card(
    modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        modifier =
          Modifier.size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        val initials =
          employee.fullName
            .split(" ")
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .take(2)
            .joinToString("")
        Text(
          text = initials,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = employee.fullName,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = "${employee.jobTitle} • ${employee.employeeCode}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "${employee.departmentName} • ${employee.workplaceName}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        CivicPill(label = employee.status, containerColor = pillBg, contentColor = pillFg)
        Text(
          text = "${employee.attendanceRatePercent}% att.",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
fun CivicAttendanceStatusBadge(
  status: CivicAttendanceStatus,
  modifier: Modifier = Modifier,
) {
  val palette = LocalCivicStatusPalette.current
  val (bg, fg) =
    when (status) {
      CivicAttendanceStatus.ON_TIME -> palette.verifiedContainer to palette.verified
      CivicAttendanceStatus.LATE -> palette.warningContainer to palette.warning
      CivicAttendanceStatus.INCOMPLETE -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
      CivicAttendanceStatus.EXCEPTION -> palette.errorContainer to palette.error
      CivicAttendanceStatus.ABSENT -> palette.errorContainer to palette.error
    }
  CivicPill(label = status.label, containerColor = bg, contentColor = fg, modifier = modifier)
}

@Composable
fun CivicLeaveStatusBadge(
  status: CivicLeaveStatus,
  modifier: Modifier = Modifier,
) {
  val palette = LocalCivicStatusPalette.current
  val (bg, fg) =
    when (status) {
      CivicLeaveStatus.PENDING -> palette.warningContainer to palette.warning
      CivicLeaveStatus.APPROVED -> palette.verifiedContainer to palette.verified
      CivicLeaveStatus.REJECTED -> palette.errorContainer to palette.error
    }
  CivicPill(label = status.label, containerColor = bg, contentColor = fg, modifier = modifier)
}

@Composable
fun CivicPill(
  label: String,
  containerColor: Color,
  contentColor: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    color = containerColor,
    shape = RoundedCornerShape(16.dp),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = contentColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}

@Composable
fun CivicNotificationCard(
  notification: CivicNotification,
  onMarkRead: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth().clickable(onClick = onMarkRead),
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (notification.isRead) MaterialTheme.colorScheme.surface
          else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
      ),
    border =
      BorderStroke(
        1.dp,
        if (notification.isRead) MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
      ),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.Top,
    ) {
      Box(
        modifier =
          Modifier.size(36.dp)
            .clip(CircleShape)
            .background(
              if (notification.isRead) MaterialTheme.colorScheme.surfaceVariant
              else MaterialTheme.colorScheme.primaryContainer
            ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Filled.Notifications,
          contentDescription = null,
          tint =
            if (notification.isRead) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp),
        )
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = notification.title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
          )
          if (!notification.isRead) {
            Box(
              modifier =
                Modifier.size(8.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primary)
            )
          }
        }
        Text(
          text = notification.message,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "${notification.category} • ${notification.timestampLabel}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          if (!notification.isRead) {
            Text(
              text = "Tap to mark read",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }
    }
  }
}

@Composable
fun CivicFormField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  placeholder: String = "",
  singleLine: Boolean = true,
  readOnly: Boolean = false,
  enabled: Boolean = true,
  isPassword: Boolean = false,
  visualTransformation: VisualTransformation = VisualTransformation.None,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  supportingText: String? = null,
  testTag: String? = null,
  modifier: Modifier = Modifier,
) {
  var passwordVisible by rememberSaveable { mutableStateOf(false) }
  val effectiveVisualTransformation =
    if (isPassword) {
      if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
    } else {
      visualTransformation
    }

  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    placeholder =
      if (placeholder.isNotBlank()) {
        { Text(placeholder) }
      } else null,
    singleLine = singleLine,
    readOnly = readOnly,
    enabled = enabled,
    visualTransformation = effectiveVisualTransformation,
    keyboardOptions = keyboardOptions,
    trailingIcon =
      if (isPassword) {
        {
          val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
          val description = if (passwordVisible) "Hide password" else "Show password"
          IconButton(onClick = { passwordVisible = !passwordVisible }) {
            Icon(imageVector = image, contentDescription = description)
          }
        }
      } else null,
    supportingText =
      if (!supportingText.isNullOrBlank()) {
        { Text(supportingText) }
      } else null,
    modifier =
      modifier.fillMaxWidth().let { m -> if (testTag != null) m.testTag(testTag) else m },
  )
}

@Composable
fun CivicPrimaryButton(
  text: String,
  onClick: () -> Unit,
  icon: ImageVector? = null,
  enabled: Boolean = true,
  testTag: String? = null,
  modifier: Modifier = Modifier,
) {
  Button(
    onClick = onClick,
    enabled = enabled,
    shape = RoundedCornerShape(12.dp),
    modifier =
      modifier.fillMaxWidth().heightIn(min = 48.dp).let { m ->
        if (testTag != null) m.testTag(testTag) else m
      },
  ) {
    if (icon != null) {
      Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(8.dp))
    }
    Text(text, style = MaterialTheme.typography.labelLarge)
  }
}

@Composable
fun CivicSecondaryButton(
  text: String,
  onClick: () -> Unit,
  icon: ImageVector? = null,
  enabled: Boolean = true,
  isDestructive: Boolean = false,
  testTag: String? = null,
  modifier: Modifier = Modifier,
) {
  OutlinedButton(
    onClick = onClick,
    enabled = enabled,
    shape = RoundedCornerShape(12.dp),
    colors =
      if (isDestructive) {
        ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
      } else {
        ButtonDefaults.outlinedButtonColors()
      },
    border =
      if (isDestructive) BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
      else ButtonDefaults.outlinedButtonBorder(enabled),
    modifier =
      modifier.fillMaxWidth().heightIn(min = 48.dp).let { m ->
        if (testTag != null) m.testTag(testTag) else m
      },
  ) {
    if (icon != null) {
      Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(8.dp))
    }
    Text(text, style = MaterialTheme.typography.labelLarge)
  }
}

@Composable
fun CivicSearchBar(
  query: String,
  onQueryChange: (String) -> Unit,
  placeholder: String,
  testTag: String? = null,
  modifier: Modifier = Modifier,
) {
  OutlinedTextField(
    value = query,
    onValueChange = onQueryChange,
    singleLine = true,
    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
    trailingIcon = {
      if (query.isNotEmpty()) {
        IconButton(onClick = { onQueryChange("") }) {
          Icon(Icons.Filled.Close, contentDescription = "Clear search")
        }
      }
    },
    placeholder = { Text(placeholder) },
    shape = RoundedCornerShape(12.dp),
    modifier =
      modifier.fillMaxWidth().let { m -> if (testTag != null) m.testTag(testTag) else m },
  )
}

@Composable
fun CivicFilterChipRow(
  options: List<String>,
  selectedOption: String,
  onOptionSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    options.forEach { option ->
      FilterChip(
        selected = option == selectedOption,
        onClick = { onOptionSelected(option) },
        label = { Text(option) },
        modifier = Modifier.heightIn(min = 48.dp),
      )
    }
  }
}

@Composable
fun CivicEmptyState(
  icon: ImageVector,
  title: String,
  description: String,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
      ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(32.dp),
      )
      Text(text = title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
      Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      if (actionLabel != null && onAction != null) {
        Spacer(Modifier.height(4.dp))
        OutlinedButton(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp)) {
          Text(actionLabel)
        }
      }
    }
  }
}

@Composable
fun CivicLoadingState(
  message: String = "Loading view...",
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth().padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
fun CivicConfirmationDialog(
  title: String,
  message: String,
  confirmLabel: String,
  dismissLabel: String = "Cancel",
  isDestructive: Boolean = false,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title, style = MaterialTheme.typography.titleLarge) },
    text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
    confirmButton = {
      Button(
        onClick = onConfirm,
        colors =
          if (isDestructive) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
          else ButtonDefaults.buttonColors(),
        modifier = Modifier.testTag("dialog_confirm_button"),
      ) {
        Text(confirmLabel)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, modifier = Modifier.testTag("dialog_dismiss_button")) {
        Text(dismissLabel)
      }
    },
  )
}

@Composable
fun CivicProfileHeader(
  fullName: String,
  roleLabel: String,
  subtitle: String,
  badgeText: String,
  avatarUrl: String? = null,
  containerColor: Color = MaterialTheme.colorScheme.secondary,
  contentColor: Color = MaterialTheme.colorScheme.onSecondary,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = containerColor),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Box(
        modifier =
          Modifier.size(56.dp)
            .clip(CircleShape)
            .background(contentColor.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
      ) {
        if (!avatarUrl.isNullOrBlank()) {
          AsyncImage(
            model = avatarUrl,
            contentDescription = "$fullName profile photo",
            modifier = Modifier.size(56.dp).clip(CircleShape),
            contentScale = ContentScale.Crop,
          )
        } else {
          val initials =
            fullName
              .split(" ")
              .mapNotNull { it.firstOrNull()?.uppercaseChar() }
              .take(2)
              .joinToString("")
              .ifBlank { "CT" }
          Text(
            text = initials,
            style = MaterialTheme.typography.titleLarge,
            color = contentColor,
          )
        }
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        CivicPill(
          label = badgeText,
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
        )
        Text(text = fullName, style = MaterialTheme.typography.headlineMedium, color = contentColor)
        Text(
          text = "$roleLabel • $subtitle",
          style = MaterialTheme.typography.bodyMedium,
          color = contentColor.copy(alpha = 0.85f),
        )
      }
    }
  }
}

@Composable
fun CivicNoticeBanner(
  title: String,
  message: String,
  isWarning: Boolean = false,
  onDismiss: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val palette = LocalCivicStatusPalette.current
  val bg = if (isWarning) palette.warningContainer else MaterialTheme.colorScheme.secondaryContainer
  val fg = if (isWarning) palette.warning else MaterialTheme.colorScheme.primary

  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = bg),
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.Top,
    ) {
      Icon(
        imageVector = if (isWarning) Icons.Filled.VerifiedUser else Icons.Filled.Info,
        contentDescription = null,
        tint = fg,
        modifier = Modifier.size(18.dp),
      )
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = message,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (onDismiss != null) {
        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
          Icon(Icons.Filled.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
        }
      }
    }
  }
}

@Composable
fun SupabaseConnectionCard(
  report: SupabaseConnectionReport,
  onRecheck: () -> Unit,
  isChecking: Boolean = false,
  onNavigateToRegistration: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val palette = LocalCivicStatusPalette.current
  val (bg, fg, icon) =
    when {
      !report.isConfigured -> Triple(palette.warningContainer, palette.warning, Icons.Filled.CloudOff)
      report.isConnected -> Triple(palette.verifiedContainer, palette.verified, Icons.Filled.CloudDone)
      else -> Triple(palette.errorContainer, palette.error, Icons.Filled.CloudOff)
    }

  Card(
    modifier = modifier.fillMaxWidth().testTag("supabase_connection_card"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, fg.copy(alpha = 0.45f)),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f),
        ) {
          Icon(imageVector = icon, contentDescription = null, tint = fg)
          Text(
            text = report.statusSummary,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
        CivicPill(
          label =
            when {
              !report.isConfigured -> "Unconfigured"
              report.isConnected -> "Connected"
              else -> "Offline / Error"
            },
          containerColor = bg,
          contentColor = fg,
        )
      }

      Text(
        text = report.guidance,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      if (report.isConfigured) {
        CivicDetailFieldRow(label = "Supabase Host", value = report.projectHost ?: "Configured")
        CivicDetailFieldRow(
          label = "Auth API (/auth/v1/settings)",
          value = if (report.authEndpointReachable) "Reachable" else "Unreachable",
        )
        CivicDetailFieldRow(
          label = "PostgREST API (/rest/v1/)",
          value =
            when {
              report.restApiStatus.isNotBlank() -> report.restApiStatus
              report.restEndpointReachable -> "Reachable"
              else -> "Not yet verified"
            },
          showDivider = false,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
          onClick = onRecheck,
          enabled = !isChecking,
          modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("supabase_recheck_button"),
        ) {
          if (isChecking) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(6.dp))
            Text("Probing...")
          } else {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Verify Connection")
          }
        }
        if (onNavigateToRegistration != null) {
          Button(
            onClick = onNavigateToRegistration,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("supabase_direct_register_button"),
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Go to Registration")
          }
        }
      }
    }
  }
}

@Composable
fun CivicFeedbackBannerRow(
  feedbackMessage: String?,
  errorMessage: String?,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalCivicStatusPalette.current
  if (!errorMessage.isNullOrBlank()) {
    Card(
      modifier = modifier.fillMaxWidth().testTag("frontend_error_banner"),
      colors = CardDefaults.cardColors(containerColor = palette.errorContainer),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.Filled.ErrorOutline, contentDescription = "Error", tint = palette.error)
        Text(
          text = errorMessage,
          style = MaterialTheme.typography.bodyMedium,
          color = palette.error,
          modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onDismiss) { Text("Dismiss") }
      }
    }
  } else if (!feedbackMessage.isNullOrBlank()) {
    Card(
      modifier = modifier.fillMaxWidth().testTag("frontend_info_banner"),
      colors = CardDefaults.cardColors(containerColor = palette.verifiedContainer),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.Filled.Info, contentDescription = "Notice", tint = palette.verified)
        Text(
          text = feedbackMessage,
          style = MaterialTheme.typography.bodyMedium,
          color = palette.verified,
          modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onDismiss) { Text("OK") }
      }
    }
  }
}

@Composable
fun CivicQuickNavRow(
  title: String,
  subtitle: String,
  icon: ImageVector,
  onClick: () -> Unit,
  trailingBadge: String? = null,
  testTag: String? = null,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier =
      modifier
        .fillMaxWidth()
        .let { m -> if (testTag != null) m.testTag(testTag) else m }
        .clickable(onClick = onClick),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        modifier =
          Modifier.size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      if (!trailingBadge.isNullOrBlank()) {
        CivicPill(
          label = trailingBadge,
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
      }
      Icon(
        imageVector = Icons.Filled.ChevronRight,
        contentDescription = "Open $title",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
fun CivicDetailFieldRow(
  label: String,
  value: String,
  showDivider: Boolean = true,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Top,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.weight(0.42f),
      )
      Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.End,
        modifier = Modifier.weight(0.58f),
      )
    }
    if (showDivider) {
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))
    }
  }
}
