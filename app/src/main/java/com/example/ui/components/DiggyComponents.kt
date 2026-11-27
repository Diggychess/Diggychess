package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.rememberChessFeedback
import com.example.ui.theme.*

enum class DiggyButtonVariant {
    PRIMARY,
    SECONDARY,
    OUTLINED,
    DANGER
}

@Composable
fun DiggyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: DiggyButtonVariant = DiggyButtonVariant.PRIMARY,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    testTag: String? = null
) {
    val feedback = rememberChessFeedback()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !loading) 0.975f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessHigh
        ),
        label = "diggy_button_scale"
    )
    val pressOffsetY by animateFloatAsState(
        targetValue = when {
            pressed && enabled && !loading -> 2.5f
            hovered && enabled && !loading -> -2f
            else -> 0f
        },
        animationSpec = tween(durationMillis = 100),
        label = "diggy_button_press_y"
    )

    val isTactile = enabled && !loading && (variant == DiggyButtonVariant.PRIMARY || variant == DiggyButtonVariant.DANGER)
    val lipHeight = if (isTactile) 3.dp else 0.dp
    val lipColor = when {
        !enabled || loading -> CardSurfaceElevated
        variant == DiggyButtonVariant.PRIMARY -> AccentGreenDark
        variant == DiggyButtonVariant.DANGER -> Color(0xFFB3262A)
        else -> Color.Transparent
    }
    val containerColor = when {
        !enabled || loading -> CardSurfaceElevated
        variant == DiggyButtonVariant.PRIMARY -> if (hovered) AccentGreenHover else AccentGreen
        variant == DiggyButtonVariant.DANGER -> FeedbackDangerRed
        variant == DiggyButtonVariant.SECONDARY -> CardSurfaceElevated
        else -> SurfaceDark
    }
    val contentColor = when (variant) {
        DiggyButtonVariant.PRIMARY -> OnAccentDark
        DiggyButtonVariant.SECONDARY -> TextPrimary
        DiggyButtonVariant.OUTLINED -> TextPrimary
        DiggyButtonVariant.DANGER -> Color.White
    }
    val border = when (variant) {
        DiggyButtonVariant.OUTLINED -> BorderStroke(1.dp, BorderSubtle)
        DiggyButtonVariant.SECONDARY -> BorderStroke(1.dp, BorderSubtle)
        DiggyButtonVariant.PRIMARY -> if (enabled && !loading) BorderStroke(1.dp, AccentGreenHover.copy(alpha = 0.45f)) else null
        else -> null
    }

    val tagMod = if (testTag != null) Modifier.testTag(testTag) else Modifier
    val shape = RoundedCornerShape(DiggySpacing.controlRadius)

    Box(
        propagateMinConstraints = true,
        modifier = modifier
            .heightIn(min = DiggySpacing.primaryButtonHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .then(
                if (isTactile) {
                    Modifier.background(lipColor)
                } else Modifier
            )
            .padding(bottom = lipHeight)
    ) {
        Surface(
            onClick = {
                if (enabled && !loading) {
                    feedback.onTap()
                    onClick()
                }
            },
            enabled = enabled && !loading,
            interactionSource = interactionSource,
            shape = shape,
            color = containerColor,
            border = border,
            modifier = Modifier
                .heightIn(min = DiggySpacing.primaryButtonHeight - lipHeight)
                .graphicsLayer {
                    translationY = if (isTactile) pressOffsetY else 0f
                }
                .then(tagMod)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                val fg = if (enabled && !loading) contentColor else TextDisabled
                if (loading) {
                    CircularProgressIndicator(
                        color = fg,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = fg,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = fg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun diggyInputColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = InputBg,
    unfocusedContainerColor = InputBg,
    disabledContainerColor = InputBg.copy(alpha = 0.72f),
    focusedTextColor = InputText,
    unfocusedTextColor = InputText,
    disabledTextColor = InputHint,
    focusedLabelColor = AccentGreen,
    unfocusedLabelColor = InputHint,
    disabledLabelColor = InputHint,
    focusedPlaceholderColor = InputHint,
    unfocusedPlaceholderColor = InputHint,
    focusedLeadingIconColor = InputHint,
    unfocusedLeadingIconColor = InputHint,
    focusedTrailingIconColor = InputHint,
    unfocusedTrailingIconColor = InputHint,
    focusedBorderColor = AccentGreen,
    unfocusedBorderColor = InputBorder,
    cursorColor = AccentGreen
)

@Composable
fun DiggyCard(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    darkMatte: Boolean = false,
    borderColor: Color? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val bg = when {
        darkMatte && elevated -> MatteDarkSecondary
        darkMatte -> MatteDarkCard
        elevated -> CardSurfaceElevated
        else -> CardDark
    }
    val resolvedBorder = borderColor ?: if (darkMatte) MatteDarkBorder else BorderSubtle
    val border = BorderStroke(1.dp, resolvedBorder)
    val shape = RoundedCornerShape(DiggySpacing.cardRadius)
    val shadow = when {
        darkMatte -> 6.dp
        elevated -> 3.dp
        else -> 2.dp
    }

    if (onClick != null) {
        val feedback = rememberChessFeedback()
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (pressed) 0.985f else 1f,
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessHigh
            ),
            label = "diggy_card_scale"
        )
        Surface(
            onClick = {
                feedback.onTap()
                onClick()
            },
            interactionSource = interactionSource,
            color = bg,
            shape = shape,
            border = border,
            tonalElevation = 0.dp,
            shadowElevation = shadow,
            modifier = modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        ) {
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    } else {
        Surface(
            color = bg,
            shape = shape,
            border = border,
            tonalElevation = 0.dp,
            shadowElevation = shadow,
            modifier = modifier
        ) {
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    }
}

@Composable
fun DiggyChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
    icon: ImageVector? = null,
    testTag: String? = null
) {
    val activeAccent = accentColor ?: AccentGreen
    val bg by animateColorAsState(
        targetValue = if (selected) activeAccent.copy(alpha = 0.18f) else CardSurfaceElevated,
        animationSpec = tween(durationMillis = 180),
        label = "chip_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) activeAccent else BorderSubtle,
        animationSpec = tween(durationMillis = 180),
        label = "chip_border"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) activeAccent else TextMuted,
        animationSpec = tween(durationMillis = 180),
        label = "chip_text"
    )
    val tagMod = if (testTag != null) Modifier.testTag(testTag) else Modifier

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(DiggySpacing.chipRadius),
        color = bg,
        border = BorderStroke(if (selected) 1.25.dp else 1.dp, borderColor),
        modifier = modifier
            .heightIn(min = DiggySpacing.touchTargetMin)
            .then(tagMod)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun <T> DiggySegmentedControl(
    options: List<T>,
    selectedOption: T,
    onSelect: (T) -> Unit,
    labelFor: (T) -> String,
    modifier: Modifier = Modifier,
    tagFor: ((T) -> String)? = null
) {
    val feedback = rememberChessFeedback()
    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(DiggySpacing.controlRadius),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { option ->
                val selected = option == selectedOption
                val pillBg by animateColorAsState(
                    targetValue = if (selected) AccentGreen else Color.Transparent,
                    animationSpec = tween(durationMillis = 180),
                    label = "seg_bg"
                )
                val labelColor by animateColorAsState(
                    targetValue = if (selected) OnAccentDark else TextMuted,
                    animationSpec = tween(durationMillis = 180),
                    label = "seg_fg"
                )
                val tagMod = if (tagFor != null) Modifier.testTag(tagFor(option)) else Modifier

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = DiggySpacing.touchTargetMin)
                        .clip(RoundedCornerShape(9.dp))
                        .background(pillBg)
                        .clickable {
                            feedback.onTap()
                            onSelect(option)
                        }
                        .then(tagMod),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = labelFor(option),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = labelColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun DiggySkeletonBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp
) {
    val transition = rememberInfiniteTransition(label = "skeleton_shimmer")
    val offset by transition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )
    val base = CardSurfaceElevated
    val highlight = BorderSubtle.copy(alpha = 0.7f)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(base, highlight, base),
                    start = Offset(offset, 0f),
                    end = Offset(offset + 280f, 120f)
                )
            )
    )
}

@Composable
fun DiggyEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.SearchOff,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    DiggyCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AccentGreen.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = AccentGreen,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(4.dp))
                DiggyButton(
                    text = actionLabel,
                    onClick = onAction,
                    variant = DiggyButtonVariant.PRIMARY,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun DiggyErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    DiggyCard(
        borderColor = FeedbackDangerRed.copy(alpha = 0.55f),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(FeedbackDangerRed.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = "Error",
                        tint = FeedbackDangerRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
            if (onRetry != null) {
                DiggyButton(
                    text = "Try again",
                    onClick = onRetry,
                    variant = DiggyButtonVariant.OUTLINED,
                    icon = Icons.Rounded.Refresh,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
