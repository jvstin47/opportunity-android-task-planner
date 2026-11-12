package com.opportunity.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.opportunity.app.data.local.entity.OpportunityEntity
import com.opportunity.app.domain.model.OpportunityStatus
import com.opportunity.app.ui.theme.SurfaceContainer
import com.opportunity.app.ui.theme.SurfaceContainerLowest
import com.opportunity.app.ui.theme.toColor
import com.opportunity.app.util.DateFormatting
import com.opportunity.app.util.DeadlineUrgency

/**
 * Shared Warm Smoked Atelier opportunity card used across Today, Growth, and Project Detail.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OpportunityCard(
    opportunity: OpportunityEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleComplete: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val isDone = opportunity.status == OpportunityStatus.COMPLETED
    val priorityColor = opportunity.priorityBucket.toColor()
    val haptics = LocalHapticFeedback.current

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isDone) 0.72f else 1f)
            .clickable(onClick = onClick),
        cornerRadius = 18.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (onToggleComplete != null) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDone) MaterialTheme.colorScheme.tertiary
                            else SurfaceContainerLowest.copy(alpha = 0.7f)
                        )
                        .border(
                            width = 1.5.dp,
                            color = if (isDone) MaterialTheme.colorScheme.tertiary else priorityColor.copy(alpha = 0.85f),
                            shape = CircleShape
                        )
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleComplete()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Completed",
                            tint = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(34.dp)
                        .clip(CircleShape)
                        .background(priorityColor)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = opportunity.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                val hasMetadata = !opportunity.category.isNullOrBlank() ||
                    opportunity.estimatedDurationMinutes != null ||
                    opportunity.deadlineEpochMillis != null

                if (hasMetadata) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        opportunity.category?.takeIf { it.isNotBlank() }?.let { category ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceContainer
                            ) {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        opportunity.estimatedDurationMinutes?.let { mins ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = DateFormatting.formatDuration(mins),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        opportunity.deadlineEpochMillis?.let { deadline ->
                            val (_, urgency) = DateFormatting.formatDeadlineBadge(deadline)
                            val deadlineColor = when (urgency) {
                                DeadlineUrgency.OVERDUE -> MaterialTheme.colorScheme.error
                                DeadlineUrgency.TODAY -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = deadlineColor.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = DateFormatting.formatDeadlineLabel(deadline),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = deadlineColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (trailing != null) {
                trailing()
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
