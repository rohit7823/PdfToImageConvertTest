package com.rohit.pdftoimageconverttest

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun CircularProgressBar(
    percentage: Float,
    number: Int = 100,
    radius: Dp = 100.dp,
    isAnimating: Boolean,
    colors: List<Color> = listOf(Color.White, Color.White, Color(0xFF707070)),
    textColor: Color = Color.White
) {
    val curProgress = animateFloatAsState(
        targetValue = if (isAnimating) percentage else 0f,
        animationSpec = tween(durationMillis = 2000),
        label = "progress"
    )
    if ((curProgress.value * number).toInt() != 0 && (curProgress.value * number).toInt() < 100) {
        Box(
            modifier = Modifier
                .size(radius * 2)
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = colors,
                        center = Offset.Infinite
                    ),
                    useCenter = false,
                    startAngle = 90f,
                    sweepAngle = 360 * curProgress.value,
                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            Text(
                text = "${(curProgress.value * number).toInt()}%",
                style = TextStyle(
                    fontSize = 30.sp, color = textColor
                )
            )
        }
    }
}