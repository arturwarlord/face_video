package com.facevideo.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.unit.dp

/**
 * Crisp, theme-tinted vector artwork for story reactions.
 * Reaction IDs remain compatible with the existing Supabase values, but no
 * emoji glyphs are rendered in the story viewer.
 */
@Composable
internal fun StoryReactionGlyph(
    reaction: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val strokeWidth = size.minDimension * 0.075f
        val outline = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        when (reaction) {
            "❤️" -> {
                val heart = Path().apply {
                    moveTo(w * 0.50f, h * 0.86f)
                    cubicTo(w * 0.41f, h * 0.78f, w * 0.12f, h * 0.55f, w * 0.12f, h * 0.33f)
                    cubicTo(w * 0.12f, h * 0.12f, w * 0.37f, h * 0.08f, w * 0.50f, h * 0.29f)
                    cubicTo(w * 0.63f, h * 0.08f, w * 0.88f, h * 0.12f, w * 0.88f, h * 0.33f)
                    cubicTo(w * 0.88f, h * 0.55f, w * 0.59f, h * 0.78f, w * 0.50f, h * 0.86f)
                    close()
                }
                drawPath(heart, tint, style = outline)
            }

            "😂" -> {
                drawCircle(tint, radius = w * 0.40f, center = Offset(w * 0.50f, h * 0.49f), style = outline)
                drawCircle(tint, radius = w * 0.035f, center = Offset(w * 0.35f, h * 0.42f))
                drawCircle(tint, radius = w * 0.035f, center = Offset(w * 0.65f, h * 0.42f))
                val smile = Path().apply {
                    moveTo(w * 0.28f, h * 0.57f)
                    cubicTo(w * 0.35f, h * 0.78f, w * 0.65f, h * 0.78f, w * 0.72f, h * 0.57f)
                }
                drawPath(smile, tint, style = outline)
                val tear = Path().apply {
                    moveTo(w * 0.10f, h * 0.34f)
                    cubicTo(w * 0.02f, h * 0.43f, w * 0.05f, h * 0.56f, w * 0.14f, h * 0.57f)
                    cubicTo(w * 0.23f, h * 0.56f, w * 0.24f, h * 0.44f, w * 0.10f, h * 0.34f)
                    close()
                }
                drawPath(tear, tint, style = outline)
                val otherTear = Path().apply {
                    moveTo(w * 0.90f, h * 0.34f)
                    cubicTo(w * 0.76f, h * 0.44f, w * 0.77f, h * 0.56f, w * 0.86f, h * 0.57f)
                    cubicTo(w * 0.95f, h * 0.56f, w * 0.98f, h * 0.43f, w * 0.90f, h * 0.34f)
                    close()
                }
                drawPath(otherTear, tint, style = outline)
            }

            "😮" -> {
                drawCircle(tint, radius = w * 0.40f, center = Offset(w * 0.50f, h * 0.49f), style = outline)
                drawCircle(tint, radius = w * 0.035f, center = Offset(w * 0.36f, h * 0.40f))
                drawCircle(tint, radius = w * 0.035f, center = Offset(w * 0.64f, h * 0.40f))
                drawOval(
                    color = tint,
                    topLeft = Offset(w * 0.42f, h * 0.57f),
                    size = androidx.compose.ui.geometry.Size(w * 0.16f, h * 0.22f),
                    style = outline
                )
            }

            "😢" -> {
                drawCircle(tint, radius = w * 0.40f, center = Offset(w * 0.50f, h * 0.48f), style = outline)
                drawCircle(tint, radius = w * 0.033f, center = Offset(w * 0.36f, h * 0.40f))
                drawCircle(tint, radius = w * 0.033f, center = Offset(w * 0.64f, h * 0.40f))
                val frown = Path().apply {
                    moveTo(w * 0.31f, h * 0.72f)
                    cubicTo(w * 0.39f, h * 0.56f, w * 0.61f, h * 0.56f, w * 0.69f, h * 0.72f)
                }
                drawPath(frown, tint, style = outline)
                val drop = Path().apply {
                    moveTo(w * 0.79f, h * 0.49f)
                    cubicTo(w * 0.70f, h * 0.60f, w * 0.72f, h * 0.72f, w * 0.80f, h * 0.73f)
                    cubicTo(w * 0.89f, h * 0.72f, w * 0.89f, h * 0.60f, w * 0.79f, h * 0.49f)
                    close()
                }
                drawPath(drop, tint, style = outline)
            }

            "🔥" -> {
                val flame = Path().apply {
                    moveTo(w * 0.52f, h * 0.08f)
                    cubicTo(w * 0.57f, h * 0.29f, w * 0.83f, h * 0.36f, w * 0.82f, h * 0.63f)
                    cubicTo(w * 0.81f, h * 0.85f, w * 0.66f, h * 0.94f, w * 0.49f, h * 0.94f)
                    cubicTo(w * 0.26f, h * 0.94f, w * 0.14f, h * 0.78f, w * 0.18f, h * 0.58f)
                    cubicTo(w * 0.21f, h * 0.43f, w * 0.32f, h * 0.34f, w * 0.36f, h * 0.18f)
                    cubicTo(w * 0.42f, h * 0.29f, w * 0.44f, h * 0.38f, w * 0.43f, h * 0.46f)
                    cubicTo(w * 0.56f, h * 0.34f, w * 0.58f, h * 0.22f, w * 0.52f, h * 0.08f)
                    close()
                }
                drawPath(flame, tint, style = outline)
                val inner = Path().apply {
                    moveTo(w * 0.51f, h * 0.57f)
                    cubicTo(w * 0.60f, h * 0.69f, w * 0.60f, h * 0.78f, w * 0.49f, h * 0.84f)
                    cubicTo(w * 0.37f, h * 0.82f, w * 0.37f, h * 0.70f, w * 0.44f, h * 0.62f)
                    cubicTo(w * 0.47f, h * 0.59f, w * 0.50f, h * 0.58f, w * 0.51f, h * 0.57f)
                    close()
                }
                drawPath(inner, tint.copy(alpha = 0.55f))
            }

            "👏" -> {
                val leftPalm = Path().apply {
                    moveTo(w * 0.14f, h * 0.48f)
                    lineTo(w * 0.25f, h * 0.28f)
                    cubicTo(w * 0.29f, h * 0.21f, w * 0.38f, h * 0.23f, w * 0.38f, h * 0.32f)
                    lineTo(w * 0.38f, h * 0.52f)
                    lineTo(w * 0.47f, h * 0.39f)
                    cubicTo(w * 0.54f, h * 0.30f, w * 0.62f, h * 0.38f, w * 0.57f, h * 0.48f)
                    lineTo(w * 0.43f, h * 0.79f)
                    cubicTo(w * 0.38f, h * 0.89f, w * 0.22f, h * 0.86f, w * 0.18f, h * 0.75f)
                    close()
                }
                val rightPalm = Path().apply {
                    moveTo(w * 0.86f, h * 0.48f)
                    lineTo(w * 0.75f, h * 0.28f)
                    cubicTo(w * 0.71f, h * 0.21f, w * 0.62f, h * 0.23f, w * 0.62f, h * 0.32f)
                    lineTo(w * 0.62f, h * 0.52f)
                    lineTo(w * 0.53f, h * 0.39f)
                    cubicTo(w * 0.46f, h * 0.30f, w * 0.38f, h * 0.38f, w * 0.43f, h * 0.48f)
                    lineTo(w * 0.57f, h * 0.79f)
                    cubicTo(w * 0.62f, h * 0.89f, w * 0.78f, h * 0.86f, w * 0.82f, h * 0.75f)
                    close()
                }
                drawPath(leftPalm, tint, style = outline)
                drawPath(rightPalm, tint, style = outline)
                listOf(
                    Offset(w * 0.50f, h * 0.08f) to Offset(w * 0.50f, h * 0.18f),
                    Offset(w * 0.21f, h * 0.12f) to Offset(w * 0.28f, h * 0.20f),
                    Offset(w * 0.79f, h * 0.12f) to Offset(w * 0.72f, h * 0.20f)
                ).forEach { (start, end) ->
                    drawLine(tint, start, end, strokeWidth = strokeWidth, cap = StrokeCap.Round)
                }
            }
        }
    }
}
