package com.bekacodes.aegispay.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bekacodes.aegispay.ui.theme.*

@Composable
fun RadarRing(delayMs: Int, tint: Color = Emerald) {
    val t = rememberInfiniteTransition(label = "radar")
    val p by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, delayMs, LinearEasing)),
        label = "p"
    )
    Box(
        Modifier
            .fillMaxSize()
            .scale(.92f + .2f * p)
            .alpha(.55f * (1 - p))
            .border(1.dp, tint, RoundedCornerShape(28.dp))
    )
}