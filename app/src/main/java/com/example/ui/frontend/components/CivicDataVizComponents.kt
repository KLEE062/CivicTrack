package com.example.ui.frontend.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.frontend.data.CivicChartBar
import com.example.ui.theme.LocalCivicStatusPalette

@Composable
fun CivicResponsiveScreenContainer(
  feedbackMessage: String?,
  errorMessage: String?,
  onDismissBanner: () -> Unit,
  modifier: Modifier = Modifier,
  content: LazyListScope.() -> Unit,
) {
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(modifier = Modifier.widthIn(max = 840.dp).fillMaxSize()) {
      if (!feedbackMessage.isNullOrBlank() || !errorMessage.isNullOrBlank()) {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
          CivicFeedbackBannerRow(
            feedbackMessage = feedbackMessage,
            errorMessage = errorMessage,
            onDismiss = onDismissBanner,
          )
        }
      }
      LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("civic_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
      )
    }
  }
}

@Composable
fun CivicSectionHeader(
  title: String,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(text = title, style = MaterialTheme.typography.titleLarge)
    if (actionLabel != null && onAction != null) {
      Text(
        text = actionLabel,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable(onClick = onAction).padding(4.dp),
      )
    }
  }
}

@Composable
fun CivicAttendanceBarChart(
  title: String,
  subtitle: String,
  bars: List<CivicChartBar>,
  primaryLabel: String = "On-Time",
  secondaryLabel: String? = "Exceptions / Late",
  modifier: Modifier = Modifier,
) {
  val primaryColor = MaterialTheme.colorScheme.primary
  val secondaryColor = LocalCivicStatusPalette.current.warning
  val trackColor = MaterialTheme.colorScheme.surfaceVariant

  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (bars.isEmpty()) {
        Text(
          text = "No recorded attendance telemetry available yet to compute chart metrics.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(vertical = 12.dp),
        )
      } else {
        Canvas(modifier = Modifier.fillMaxWidth().height(132.dp)) {
          val barSlot = size.width / bars.size
          val barWidth = (barSlot * 0.44f).coerceAtMost(44f)
          val maxHeight = size.height - 16f

          bars.forEachIndexed { index, bar ->
            val x = index * barSlot + (barSlot - barWidth) / 2f
            drawRoundRect(
              color = trackColor,
              topLeft = Offset(x, 8f),
              size = Size(barWidth, maxHeight),
              cornerRadius = CornerRadius(8f, 8f),
            )
            val totalFraction = (bar.primaryValue + bar.secondaryValue).coerceIn(0f, 1f)
            val primaryHeight = maxHeight * bar.primaryValue.coerceIn(0f, 1f)
            val secondaryHeight = maxHeight * (totalFraction - bar.primaryValue.coerceIn(0f, 1f))

            if (secondaryHeight > 0f) {
              drawRoundRect(
                color = secondaryColor,
                topLeft = Offset(x, 8f + maxHeight - primaryHeight - secondaryHeight),
                size = Size(barWidth, secondaryHeight + primaryHeight),
                cornerRadius = CornerRadius(8f, 8f),
              )
            }
            if (primaryHeight > 0f) {
              drawRoundRect(
                color = primaryColor,
                topLeft = Offset(x, 8f + maxHeight - primaryHeight),
                size = Size(barWidth, primaryHeight),
                cornerRadius = CornerRadius(8f, 8f),
              )
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceAround,
        ) {
          bars.forEach { bar ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(bar.label, style = MaterialTheme.typography.labelMedium)
              Text(
                text = "${(bar.primaryValue * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          LegendDot(color = primaryColor, label = primaryLabel)
          if (secondaryLabel != null) {
            LegendDot(color = secondaryColor, label = secondaryLabel)
          }
        }
      }
    }
  }
}

@Composable
private fun LegendDot(color: Color, label: String) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Box(
      modifier =
        Modifier.height(10.dp)
          .widthIn(min = 10.dp)
          .background(color, RoundedCornerShape(3.dp))
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
fun CivicGeofenceBoundaryCard(
  workplaceName: String,
  radiusMeters: Int,
  latitude: Double,
  longitude: Double,
  isAttentionFlagged: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val palette = LocalCivicStatusPalette.current
  val ringColor = if (isAttentionFlagged) palette.warning else MaterialTheme.colorScheme.primary
  val siteColor = MaterialTheme.colorScheme.secondary

  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(text = "Registered Workplace Geofence", style = MaterialTheme.typography.titleMedium)
      Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerR = size.minDimension * 0.42f
        val innerR = size.minDimension * 0.24f

        drawCircle(
          color = ringColor.copy(alpha = 0.12f),
          radius = outerR,
          center = center,
        )
        drawCircle(
          color = ringColor,
          radius = outerR,
          center = center,
          style = Stroke(width = 3f),
        )
        drawCircle(
          color = ringColor.copy(alpha = 0.35f),
          radius = innerR,
          center = center,
          style = Stroke(width = 2f),
        )
        drawCircle(
          color = siteColor,
          radius = 12f,
          center = center,
        )
      }
      Text(
        text = "$workplaceName • Configured radius ${radiusMeters}m",
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
      )
      Text(
        text = "Registered coordinates: %.5f, %.5f".format(latitude, longitude),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
fun CivicDataTableCard(
  title: String,
  headers: List<String>,
  rows: List<List<String>>,
  onRowClick: ((Int) -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(text = title, style = MaterialTheme.typography.titleMedium)
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        headers.forEach { header ->
          Text(
            text = header,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      rows.forEachIndexed { index, row ->
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .then(if (onRowClick != null) Modifier.clickable { onRowClick(index) } else Modifier)
              .padding(horizontal = 10.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          row.forEach { cell ->
            Text(
              text = cell,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.weight(1f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        if (index < rows.lastIndex) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        }
      }
    }
  }
}
