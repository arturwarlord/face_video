package com.facevideo.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.unit.dp

internal enum class ArturChatIconType {
    VIDEO_NOTE,
    GROUP,
    CHANNEL
}

/**
 * Branded circular artwork for non-person chat entities. Personal profile
 * photos and initials continue to use the regular Avatar composable.
 */
@Composable
internal fun ArturChatTypeAvatar(
    type: ArturChatIconType,
    modifier: Modifier = Modifier
) {
    val palette = when (type) {
        ArturChatIconType.VIDEO_NOTE -> Triple(Color(0xFFE8E8FF), Color(0xFFF2E9FF), Color(0xFF635FD2))
        ArturChatIconType.GROUP -> Triple(Color(0xFFE3EAFF), Color(0xFFEAF0FF), Color(0xFF5369D8))
        ArturChatIconType.CHANNEL -> Triple(Color(0xFFDFF0F0), Color(0xFFE8F1FF), Color(0xFF438B96))
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(palette.first, palette.second))),
        contentAlignment = Alignment.Center
    ) {
        ArturChatTypeGlyph(
            type = type,
            tint = palette.third,
            modifier = Modifier.fillMaxSize().padding(10.dp)
        )
    }
}

/** A crisp line icon that also fits the compact message-composer button. */
@Composable
internal fun ArturChatTypeGlyph(
    type: ArturChatIconType,
    tint: Color,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier) {
        when (type) {
            ArturChatIconType.VIDEO_NOTE -> drawVideoNoteGlyph(tint)
            ArturChatIconType.GROUP -> drawGroupGlyph(tint)
            ArturChatIconType.CHANNEL -> drawChannelGlyph(tint)
        }
    }
}

private fun DrawScope.drawVideoNoteGlyph(color: Color) {
    val d = size.minDimension
    val line = Stroke(width = d * 0.075f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * 0.13f, size.height * 0.29f),
        size = Size(size.width * 0.53f, size.height * 0.42f),
        cornerRadius = CornerRadius(d * 0.09f),
        style = line
    )
    val lens = Path().apply {
        moveTo(size.width * 0.70f, size.height * 0.40f)
        lineTo(size.width * 0.90f, size.height * 0.28f)
        lineTo(size.width * 0.90f, size.height * 0.72f)
        lineTo(size.width * 0.70f, size.height * 0.60f)
        close()
    }
    drawPath(lens, color, style = line)
    val play = Path().apply {
        moveTo(size.width * 0.34f, size.height * 0.39f)
        lineTo(size.width * 0.34f, size.height * 0.61f)
        lineTo(size.width * 0.51f, size.height * 0.50f)
        close()
    }
    drawPath(play, color)
}

private fun DrawScope.drawGroupGlyph(color: Color) {
    val d = size.minDimension
    val line = Stroke(width = d * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawCircle(color, radius = d * 0.105f, center = Offset(size.width * 0.50f, size.height * 0.27f))
    drawCircle(color, radius = d * 0.078f, center = Offset(size.width * 0.23f, size.height * 0.40f))
    drawCircle(color, radius = d * 0.078f, center = Offset(size.width * 0.77f, size.height * 0.40f))
    drawArc(
        color = color,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(size.width * 0.27f, size.height * 0.47f),
        size = Size(size.width * 0.46f, size.height * 0.42f),
        style = line
    )
    drawArc(
        color = color,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(size.width * 0.035f, size.height * 0.60f),
        size = Size(size.width * 0.34f, size.height * 0.29f),
        style = line
    )
    drawArc(
        color = color,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(size.width * 0.625f, size.height * 0.60f),
        size = Size(size.width * 0.34f, size.height * 0.29f),
        style = line
    )
}

private fun DrawScope.drawChannelGlyph(color: Color) {
    val d = size.minDimension
    val line = Stroke(width = d * 0.075f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val horn = Path().apply {
        moveTo(size.width * 0.12f, size.height * 0.40f)
        lineTo(size.width * 0.60f, size.height * 0.25f)
        lineTo(size.width * 0.60f, size.height * 0.75f)
        lineTo(size.width * 0.12f, size.height * 0.60f)
        close()
    }
    drawPath(horn, color, style = line)
    drawLine(
        color = color,
        start = Offset(size.width * 0.28f, size.height * 0.62f),
        end = Offset(size.width * 0.37f, size.height * 0.88f),
        strokeWidth = d * 0.09f,
        cap = StrokeCap.Round
    )
    drawArc(
        color = color,
        startAngle = -52f,
        sweepAngle = 104f,
        useCenter = false,
        topLeft = Offset(size.width * 0.52f, size.height * 0.15f),
        size = Size(size.width * 0.39f, size.height * 0.70f),
        style = line
    )
}
