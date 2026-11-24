package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.OrgMemberRole
import com.example.ui.OnboardingUiState
import com.example.ui.components.DashboardMetricCard
import com.example.ui.components.FeedbackMessageCard
import com.example.ui.components.InstitutionalEmptyStateCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalCivicStatusPalette

@Composable
fun OwnerDashboardScreen(
  uiState: OnboardingUiState,
  onRefreshDashboard: () -> Unit,
  onOpenStructureWizard: () -> Unit,
  onOpenOrgProfile: () -> Unit,
  onSendInvitation: (String, OrgMemberRole, String?, String?) -> Unit,
  onRevokeInvitation: (String) -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val dashboard = uiState.ownerDashboard
  val org = dashboard?.organization ?: uiState.activeOrganization
  val ownerName = dashboard?.ownerName ?: uiState.userProfile?.fullName ?: uiState.session?.email.orEmpty()
  val palette = LocalCivicStatusPalette.current
  var showInviteComposer by rememberSaveable { mutableStateOf(false) }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      if (uiState.errorMessage != null || uiState.infoMessage != null) {
        item {
          FeedbackMessageCard(
            errorMessage = uiState.errorMessage,
            infoMessage = uiState.infoMessage,
          )
        }
      }

      // Header Card
      item {
        Card(
          modifier = Modifier.fillMaxWidth().testTag("owner_dashboard_header"),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            if (!org?.logoUrl.isNullOrBlank()) {
              AsyncImage(
                model = org?.logoUrl,
                contentDescription = "Organisation logo",
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
              )
            } else {
              Icon(
                imageVector = Icons.Filled.Apartment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.size(40.dp),
              )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              StatusPill(
                label = "OWNER • ${org?.countryCode ?: "UG"} (${org?.currencyCode ?: "UGX"})",
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
              )
              Text(
                text = org?.name ?: "Organisation Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSecondary,
              )
              Text(
                text = "Owner: $ownerName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.85f),
              )
            }
            IconButton(onClick = onRefreshDashboard, modifier = Modifier.testTag("owner_refresh_button")) {
              Icon(
                Icons.Filled.Refresh,
                contentDescription = "Refresh dashboard",
                tint = MaterialTheme.colorScheme.onSecondary,
              )
            }
            IconButton(onClick = onSignOut, modifier = Modifier.testTag("owner_sign_out_button")) {
              Icon(
                Icons.AutoMirrored.Filled.Logout,
                contentDescription = "Sign out",
                tint = MaterialTheme.colorScheme.onSecondary,
              )
            }
          }
        }
      }

      // Setup Progress Card
      item {
        val completed = dashboard?.setupCompletedSteps ?: 1
        val total = dashboard?.setupTotalSteps ?: 6
        Card(
          modifier = Modifier.fillMaxWidth().testTag("owner_setup_progress_card"),
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
              Text("Organisation Setup Progress", style = MaterialTheme.typography.titleLarge)
              Text(
                text = "$completed / $total",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            LinearProgressIndicator(
              progress = { completed.toFloat() / total.toFloat() },
              modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Button(
                onClick = onOpenStructureWizard,
                modifier = Modifier.weight(1f).testTag("open_structure_wizard_button"),
              ) {
                Icon(Icons.Filled.SettingsSuggest, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Setup Wizard")
              }
              OutlinedButton(
                onClick = { showInviteComposer = !showInviteComposer },
                modifier = Modifier.weight(1f).testTag("toggle_quick_invite_button"),
              ) {
                Icon(Icons.Filled.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (showInviteComposer) "Hide Invite" else "Invite Staff")
              }
            }
            TextButton(onClick = onOpenOrgProfile) {
              Text("Edit Organisation Profile & Logo")
            }
          }
        }
      }

      if (showInviteComposer) {
        item {
          InviteTeamSection(
            uiState = uiState,
            onSendInvitation = onSendInvitation,
          )
        }
      }

      // Real Backend KPIs
      item {
        val empCount = dashboard?.employees?.size ?: 0
        val deptCount = dashboard?.departments?.size ?: 0
        val wpCount = dashboard?.workplaces?.size ?: 0
        val todayCount = dashboard?.todayAttendanceCount ?: 0
        val todayVerified = dashboard?.todayVerifiedCount ?: 0

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            DashboardMetricCard(
              title = "Total Employees",
              value = empCount.toString(),
              subtitle = if (empCount == 0) "No employees enrolled yet" else "Active workforce records",
              icon = Icons.Filled.People,
              modifier = Modifier.weight(1f),
            )
            DashboardMetricCard(
              title = "Departments",
              value = deptCount.toString(),
              subtitle = if (deptCount == 0) "Add departments in setup" else "Configured units",
              icon = Icons.Filled.Business,
              modifier = Modifier.weight(1f),
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            DashboardMetricCard(
              title = "Workplaces",
              value = wpCount.toString(),
              subtitle = if (wpCount == 0) "Configure geofence sites" else "Registered sites",
              icon = Icons.Filled.LocationCity,
              modifier = Modifier.weight(1f),
            )
            DashboardMetricCard(
              title = "Today's Attendance",
              value = todayCount.toString(),
              subtitle = "$todayVerified geofence verified today",
              icon = Icons.Filled.CheckCircle,
              accentColor = palette.verified,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      // Pending Invitations
      item {
        Text(
          text = "Pending Invitations (${dashboard?.pendingInvitations?.size ?: 0})",
          style = MaterialTheme.typography.titleLarge,
        )
      }
      val pendingInvites = dashboard?.pendingInvitations.orEmpty()
      if (pendingInvites.isEmpty()) {
        item {
          InstitutionalEmptyStateCard(
            icon = Icons.Filled.MailOutline,
            title = "No pending invitations",
            description = "Invite supervisors or employees to join ${org?.name ?: "your organisation"} with scoped roles.",
            actionLabel = "Invite Supervisor",
            onAction = { showInviteComposer = true },
          )
        }
      } else {
        items(pendingInvites, key = { it.id }) { invite ->
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(invite.email, style = MaterialTheme.typography.titleMedium)
                Text(
                  text = "Role: ${invite.role.uppercase()} • Expires: ${invite.expiresAt?.take(10) ?: "N/A"}",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              TextButton(
                onClick = { onRevokeInvitation(invite.id) },
                modifier = Modifier.testTag("revoke_invite_${invite.id}"),
              ) {
                Text("Revoke", color = MaterialTheme.colorScheme.error)
              }
            }
          }
        }
      }

      // Recent Organisational Activity
      item {
        Text(
          text = "Recent Organisational Activity",
          style = MaterialTheme.typography.titleLarge,
        )
      }
      val logs = dashboard?.recentActivities.orEmpty()
      if (logs.isEmpty()) {
        item {
          InstitutionalEmptyStateCard(
            icon = Icons.Filled.History,
            title = "No recorded organisational activity yet",
            description = "Workplace creations, invitations, and membership changes appear here from audit_logs.",
          )
        }
      } else {
        items(logs, key = { it.id }) { log ->
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(log.action, style = MaterialTheme.typography.titleMedium)
                StatusPill(
                  label = log.entityType ?: "AUDIT",
                  containerColor = MaterialTheme.colorScheme.secondaryContainer,
                  contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
              }
              if (!log.summary.isNullOrBlank()) {
                Text(
                  text = log.summary,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              if (!log.createdAt.isNullOrBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                Text(
                  text = log.createdAt,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    }
  }
}
