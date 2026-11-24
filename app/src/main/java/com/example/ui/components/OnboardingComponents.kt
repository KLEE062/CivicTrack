package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalCivicStatusPalette

@Composable
fun OnboardingStepIndicator(
  steps: List<Pair<Int, String>>,
  currentStepNumber: Int,
  accentColor: Color = MaterialTheme.colorScheme.primary,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    steps.forEach { (number, label) ->
      val completed = number < currentStepNumber
      val active = number == currentStepNumber
      val containerColor =
        when {
          active -> accentColor
          completed -> accentColor.copy(alpha = 0.16f)
          else -> MaterialTheme.colorScheme.surfaceVariant
        }
      val contentColor =
        when {
          active -> MaterialTheme.colorScheme.onPrimary
          completed -> accentColor
          else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

      Surface(
        color = containerColor,
        shape = RoundedCornerShape(20.dp),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Box(
            modifier =
              Modifier.size(20.dp)
                .clip(CircleShape)
                .background(
                  if (active) Color.White.copy(alpha = 0.22f)
                  else accentColor.copy(alpha = 0.15f)
                ),
            contentAlignment = Alignment.Center,
          ) {
            if (completed) {
              Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Completed",
                tint = contentColor,
                modifier = Modifier.size(13.dp),
              )
            } else {
              Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
              )
            }
          }
          Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
          )
        }
      }
    }
  }
}

@Composable
fun FeedbackMessageCard(
  errorMessage: String?,
  infoMessage: String?,
  pendingVerificationEmail: String? = null,
  onSignInAfterVerification: (() -> Unit)? = null,
  onDismiss: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val palette = LocalCivicStatusPalette.current

  if (!errorMessage.isNullOrBlank()) {
    Card(
      modifier = modifier.fillMaxWidth().testTag("onboarding_error_card"),
      colors = CardDefaults.cardColors(containerColor = palette.errorContainer),
    ) {
      Row(
        modifier = Modifier.padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
      ) {
        Icon(Icons.Filled.Error, contentDescription = "Error", tint = palette.error)
        Text(
          text = errorMessage,
          style = MaterialTheme.typography.bodyMedium,
          color = palette.error,
          modifier = Modifier.weight(1f),
        )
        if (onDismiss != null) {
          TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
      }
    }
  }

  if (!pendingVerificationEmail.isNullOrBlank()) {
    Card(
      modifier = modifier.fillMaxWidth().testTag("email_verification_card"),
      colors = CardDefaults.cardColors(containerColor = palette.warningContainer),
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(Icons.Filled.MarkEmailRead, contentDescription = null, tint = palette.warning)
          Text(
            text = "Email Verification Required",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
        Text(
          text =
            "Check $pendingVerificationEmail for your verification link. Once confirmed, sign in to resume onboarding automatically.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (onSignInAfterVerification != null) {
          TextButton(
            onClick = onSignInAfterVerification,
            modifier = Modifier.testTag("proceed_after_email_verify_button"),
          ) {
            Text("I Verified My Email — Sign In")
          }
        }
      }
    }
  } else if (!infoMessage.isNullOrBlank()) {
    Card(
      modifier = modifier.fillMaxWidth().testTag("onboarding_info_card"),
      colors = CardDefaults.cardColors(containerColor = palette.verifiedContainer),
    ) {
      Row(
        modifier = Modifier.padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.Filled.Info, contentDescription = "Info", tint = palette.verified)
        Text(
          text = infoMessage,
          style = MaterialTheme.typography.bodyMedium,
          color = palette.verified,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
fun DashboardMetricCard(
  title: String,
  value: String,
  subtitle: String,
  icon: ImageVector,
  accentColor: Color = MaterialTheme.colorScheme.primary,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
        )
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accentColor,
          modifier = Modifier.size(18.dp),
        )
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
      )
    }
  }
}

@Composable
fun InstitutionalEmptyStateCard(
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
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
      ),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(28.dp),
      )
      Text(text = title, style = MaterialTheme.typography.titleMedium)
      Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      if (actionLabel != null && onAction != null) {
        Spacer(Modifier.width(4.dp))
        TextButton(onClick = onAction) { Text(actionLabel) }
      }
    }
  }
}
