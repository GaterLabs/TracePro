package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

/**
 * Modern, friendly rounded avatar for TracerPro AI Copilot.
 * Scales dynamically to any Dp size while maintaining crisp vector aesthetics.
 */
@Composable
fun AiRobotAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    containerBackground: Color = Slate900,
    accentColor: Color = EmeraldPrimary,
    eyeColor: Color = Color(0xFF34D399) // Mint / glowing emerald
) {
    val cornerRadius = size * 0.28f

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(containerBackground)
            .border(1.dp, EmeraldBorder, RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.75f)) {
            val w = this.size.width
            val h = this.size.height

            // 1. Antenna Stem & Glowing Orb
            val stemWidth = w * 0.08f
            val stemHeight = h * 0.16f
            val antennaRadius = w * 0.09f

            // Antenna glowing ball
            drawCircle(
                color = accentColor,
                radius = antennaRadius,
                center = Offset(w * 0.5f, h * 0.11f)
            )
            // Antenna stem
            drawRoundRect(
                color = accentColor,
                topLeft = Offset(w * 0.5f - stemWidth / 2f, h * 0.11f),
                size = Size(stemWidth, stemHeight),
                cornerRadius = CornerRadius(stemWidth / 2, stemWidth / 2)
            )

            // 2. Head Parameters
            val headTop = h * 0.24f
            val headWidth = w * 0.82f
            val headHeight = h * 0.58f
            val headLeft = (w - headWidth) / 2f
            val headRadius = w * 0.18f

            // 3. Ear bolts/pads
            val earWidth = w * 0.10f
            val earHeight = h * 0.22f
            val earTop = headTop + (headHeight - earHeight) / 2f

            // Left Ear
            drawRoundRect(
                color = accentColor.copy(alpha = 0.9f),
                topLeft = Offset(headLeft - earWidth * 0.55f, earTop),
                size = Size(earWidth, earHeight),
                cornerRadius = CornerRadius(earWidth * 0.4f, earWidth * 0.4f)
            )
            // Right Ear
            drawRoundRect(
                color = accentColor.copy(alpha = 0.9f),
                topLeft = Offset(headLeft + headWidth - earWidth * 0.45f, earTop),
                size = Size(earWidth, earHeight),
                cornerRadius = CornerRadius(earWidth * 0.4f, earWidth * 0.4f)
            )

            // 4. Robot Head Base
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(headLeft, headTop),
                size = Size(headWidth, headHeight),
                cornerRadius = CornerRadius(headRadius, headRadius)
            )

            // 5. Visor / Dark Digital Screen
            val visorTop = headTop + headHeight * 0.16f
            val visorWidth = headWidth * 0.78f
            val visorHeight = headHeight * 0.46f
            val visorLeft = headLeft + (headWidth - visorWidth) / 2f
            val visorRadius = headRadius * 0.65f

            drawRoundRect(
                color = Slate900,
                topLeft = Offset(visorLeft, visorTop),
                size = Size(visorWidth, visorHeight),
                cornerRadius = CornerRadius(visorRadius, visorRadius)
            )

            // 6. Glowing Expressive Round Eyes
            val eyeRadius = visorHeight * 0.25f
            val eyeCenterY = visorTop + visorHeight * 0.50f
            val eyeOffsetFromCenter = visorWidth * 0.26f

            // Left eye glow & pupil
            drawCircle(
                color = eyeColor,
                radius = eyeRadius,
                center = Offset(w * 0.5f - eyeOffsetFromCenter, eyeCenterY)
            )
            // Right eye glow & pupil
            drawCircle(
                color = eyeColor,
                radius = eyeRadius,
                center = Offset(w * 0.5f + eyeOffsetFromCenter, eyeCenterY)
            )

            // Eye highlights (cute anime/friendly shine)
            val shineRadius = eyeRadius * 0.35f
            drawCircle(
                color = Color.White,
                radius = shineRadius,
                center = Offset(w * 0.5f - eyeOffsetFromCenter - eyeRadius * 0.25f, eyeCenterY - eyeRadius * 0.25f)
            )
            drawCircle(
                color = Color.White,
                radius = shineRadius,
                center = Offset(w * 0.5f + eyeOffsetFromCenter - eyeRadius * 0.25f, eyeCenterY - eyeRadius * 0.25f)
            )

            // 7. Cheerful Mouth / Speaker slit
            val mouthY = headTop + headHeight * 0.77f
            val mouthWidth = headWidth * 0.32f
            val mouthHeight = h * 0.045f
            drawRoundRect(
                color = accentColor,
                topLeft = Offset(w * 0.5f - mouthWidth / 2f, mouthY),
                size = Size(mouthWidth, mouthHeight),
                cornerRadius = CornerRadius(mouthHeight / 2, mouthHeight / 2)
            )
        }
    }
}
