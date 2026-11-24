package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.LocationVerificationOutcome
import com.example.ui.OnboardingUiState
import com.example.ui.components.DashboardMetricCard
import com.example.ui.components.FeedbackMessageCard
import com.example.ui.components.InstitutionalEmptyStateCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalCivicStatusPalette

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SupervisorDashboardScreen(
  uiState: OnboardingUiState,
  onRefreshDashboard: () -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val data = uiState.supervisorDashboard
  val org = data?.organization ?: uiState.activeOrganization
  val membership = data?.membership ?: uiState.activeMembership
  val supervisorName =
    data?.supervisorProfile?.fullName
      ?: uiState.userProfile?.fullName
      ?: uiState.session?.email.orEmpty()
  val palette = LocalCivicStatusPalette.current

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

      // Supervisor Authority Header
      item {
        Card(
          modifier = Modifier.fillMaxWidth().testTag("supervisor_dashboard_header"),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary),
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.SupervisorAccount,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiary,
                modifier = Modifier.size(36.dp),
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = org?.name ?: "Supervisor Dashboard",
                  style = MaterialTheme.typography.headlineMedium,
                  color = MaterialTheme.colorScheme.onTertiary,
                )
                Text(
                  text = "Supervisor: $supervisorName",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.88f),
                )
              }
              IconButton(
                onClick = onRefreshDashboard,
                modifier = Modifier.testTag("supervisor_refresh_button"),
              ) {
                Icon(
                  Icons.Filled.Refresh,
                  contentDescription = "Refresh supervisor dashboard",
                  tint = MaterialTheme.colorScheme.onTertiary,
                )
              }
              IconButton(
                onClick = onSignOut,
                modifier = Modifier.testTag("supervisor_sign_out_button"),
              ) {
                Icon(
                  Icons.AutoMirrored.Filled.Logout,
                  contentDescription = "Sign out",
                  tint = MaterialTheme.colorScheme.onTertiary,
                )
              }
            }

            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              StatusPill(
                label = membership?.parsedRole?.displayLabel ?: "Supervisor",
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.tertiary,
              )
              membership?.departments?.let {
                StatusPill(
                  label = "Dept: ${it.name}",
                  containerColor = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.2f),
                  contentColor = MaterialTheme.colorScheme.onTertiary,
                )
              }
              membership?.workplaces?.let {
                StatusPill(
                  label = "Site: ${it.name}",
                  containerColor = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.2f),
                  contentColor = MaterialTheme.colorScheme.onTertiary,
                )
              }
            }
          }
        }
      }

      // Scoped Workforce & Attendance KPIs
      item {
        val assignedCount = data?.assignedEmployees?.size ?: 0
        val presentCount = data?.presentEmployeesCount ?: 0
        val lateCount = data?.lateArrivalsCount ?: 0
        val incompleteCount = data?.incompleteRecordsCount ?: 0
        val pendingReviews = data?.pendingReviewsCount ?: 0
        val pendingLeaves = data?.pendingLeaveRequests?.size ?: 0

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            DashboardMetricCard(
              title = "Assigned Staff",
              value = assignedCount.toString(),
              subtitle = "Within your scope",
              icon = Icons.Filled.People,
              modifier = Modifier.weight(1f),
            )
            DashboardMetricCard(
              title = "Present Today",
              value = presentCount.toString(),
              subtitle = "${data?.todayAttendanceRecords?.size ?: 0} logs today",
              icon = Icons.Filled.CheckCircle,
              accentColor = palette.verified,
              modifier = Modifier.weight(1f),
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            DashboardMetricCard(
              title = "Late Arrivals",
              value = lateCount.toString(),
              subtitle = "After grace window",
              icon = Icons.Filled.AccessTime,
              accentColor = palette.warning,
              modifier = Modifier.weight(1f),
            )
            DashboardMetricCard(
              title = "Incomplete Logs",
              value = incompleteCount.toString(),
              subtitle = "Missing check-out",
              icon = Icons.Filled.HourglassTop,
              accentColor = palette.warning,
              modifier = Modifier.weight(1f),
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            DashboardMetricCard(
              title = "Pending Reviews",
              value = pendingReviews.toString(),
              subtitle = "Flagged or awaiting review",
              icon = Icons.AutoMirrored.Filled.FactCheck,
              accentColor = palette.error,
              modifier = Modifier.weight(1f),
            )
            DashboardMetricCard(
              title = "Leave Requests",
              value = if (data?.leaveFeatureAvailable != false) pendingLeaves.toString() else "0",
              subtitle =
                if (data?.leaveFeatureAvailable == false) "Leave module not configured"
                else "Pending approval",
              icon = Icons.Filled.EventBusy,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      // Today's Scoped Attendance
      item {
        Text(
          text = "Today's Attendance (${data?.todayAttendanceRecords?.size ?: 0})",
          style = MaterialTheme.typography.titleLarge,
        )
      }
      val todayRecords = data?.todayAttendanceRecords.orEmpty()
      if (todayRecords.isEmpty()) {
        item {
          InstitutionalEmptyStateCard(
            icon = Icons.Filled.CheckCircle,
            title = "No attendance records logged today",
            description = "Scoped check-ins and check-outs from employees in your assigned unit appear here in real time.",
          )
        }
      } else {
        items(todayRecords, key = { it.id }) { record ->
          val verified = record.geofenceStatus == LocationVerificationOutcome.VERIFIED.apiValue
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                  text = "${record.checkType.replace('_', ' ')} • Employee ${record.employeeId.take(8)}",
                  style = MaterialTheme.typography.titleMedium,
                )
                Text(
                  text = record.recordedAtIso,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              StatusPill(
                label = record.geofenceStatus,
                containerColor =
                  if (verified) palette.verifiedContainer else palette.warningContainer,
                contentColor = if (verified) palette.verified else palette.warning,
              )
            }
          }
        }
      }

      // Recent Departmental Activities
      item {
        Text(
          text = "Recent Departmental Activities",
          style = MaterialTheme.typography.titleLarge,
        )
      }
      val activities = data?.recentActivities.orEmpty()
      if (activities.isEmpty()) {
        item {
          InstitutionalEmptyStateCard(
            icon = Icons.Filled.History,
            title = "No recent departmental activity",
            description = "Unit-scoped operational and membership audit events appear here from Supabase.",
          )
        }
      } else {
        items(activities, key = { it.id }) { log ->
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(log.action, style = MaterialTheme.typography.titleMedium)
              if (!log.summary.isNullOrBlank()) {
                Text(
                  text = log.summary,
                  style = MaterialTheme.typography.bodyMedium,
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
