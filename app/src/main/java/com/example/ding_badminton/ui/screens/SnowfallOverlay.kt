package com.example.ding_badminton.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.isActive
import kotlin.random.Random
import kotlin.math.PI
import kotlin.math.sin

private data class Snowflake(
    val baseXRatio: Float,
    val startYRatio: Float,
    val radiusDp: Float,
    val speedDpPerSec: Float,
    val swingAmpDp: Float,
    val swingFreq: Float,
    val phase: Float,
    val alpha: Float,
    val sparkle: Float,
)

@Composable
fun SnowfallOverlay() {
    val random = remember { Random(System.currentTimeMillis()) }
    val flakes = remember {
        List(150) {
            // 近景雪花略大，远景更细，控制整体观感不夸张
            val nearLayer = random.nextFloat() < 0.45f
            Snowflake(
                baseXRatio = random.nextFloat(),
                startYRatio = random.nextFloat(),
                radiusDp = if (nearLayer) 1.4f + random.nextFloat() * 1.5f else 0.8f + random.nextFloat() * 1.1f,
                speedDpPerSec = if (nearLayer) 16f + random.nextFloat() * 22f else 8f + random.nextFloat() * 12f,
                swingAmpDp = if (nearLayer) 3.5f + random.nextFloat() * 9f else 1.8f + random.nextFloat() * 5.8f,
                swingFreq = if (nearLayer) 0.32f + random.nextFloat() * 0.44f else 0.2f + random.nextFloat() * 0.33f,
                phase = random.nextFloat() * (2f * PI.toFloat()),
                alpha = if (nearLayer) 0.33f + random.nextFloat() * 0.34f else 0.15f + random.nextFloat() * 0.24f,
                sparkle = random.nextFloat(),
            )
        }
    }
    var frameTick by remember { mutableStateOf(0) }
    var elapsedSec by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        var lastNanos = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (lastNanos == 0L) lastNanos = now
                val dt = ((now - lastNanos).coerceAtMost(40_000_000L)).toFloat() / 1_000_000_000f
                lastNanos = now
                elapsedSec += dt
            }
            frameTick++
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        // 读取 tick 以建立重组依赖
        val    frameDependency = frameTick
        if (frameDependency < 0) return@Canvas

        val marginPx = 36.dp.toPx()
        val travelHeight = size.height + marginPx * 2f
        val globalShimmer = 0.9f + 0.1f * sin(elapsedSec * 0.8f)

        flakes.forEach { f ->
            val radiusPx = f.radiusDp.dp.toPx()
            val speedPx = f.speedDpPerSec.dp.toPx()
            val swingAmpPx = f.swingAmpDp.dp.toPx()

            // 基于时间+取模的无缝循环，避免“重置跳变”
            val yRaw = f.startYRatio * travelHeight + elapsedSec * speedPx
            val y = (yRaw % travelHeight) - marginPx

            // 低频风场控制摆动强度：有时几乎直落，有时轻微飘动，避免持续左右摇摆感
            val windFactor = ((sin(elapsedSec * 0.12f + f.phase * 0.31f) + 1f) * 0.5f).coerceIn(0f, 1f)
            val swingStrength = 0.22f + windFactor * 0.78f
            val xSwing = sin(elapsedSec * f.swingFreq * 2f * PI.toFloat() + f.phase) * swingAmpPx * swingStrength
            val x = f.baseXRatio * size.width + xSwing

            val sparkle = 0.84f + 0.16f * sin(elapsedSec * (0.9f + f.sparkle) + f.phase * 0.7f)
            val alpha = (f.alpha * sparkle * globalShimmer).coerceIn(0.08f, 0.78f)

            drawSnowflakeDot(
                center = Offset(x, y),
                radiusPx = radiusPx,
                alpha = alpha,
            )
        }
    }
}

private fun DrawScope.drawSnowflakeDot(
    center: Offset,
    radiusPx: Float,
    alpha: Float,
) {
    val haloRadius = radiusPx * 1.55f
    val haloAlpha = alpha * 0.22f
    drawCircle(
        color = Color.White.copy(alpha = haloAlpha),
        radius = haloRadius,
        center = center,
    )
    drawCircle(
        color = Color.White.copy(alpha = alpha),
        radius = radiusPx,
        center = center,
    )
    // 细描边让近景雪花更“亮”，但保持克制
    drawCircle(
        color = Color.White.copy(alpha = alpha * 0.18f),
        radius = radiusPx + lerp(0.2f, 0.55f, alpha),
        center = center,
        style = Stroke(width = lerp(0.45f, 1.0f, alpha)),
    )
}

