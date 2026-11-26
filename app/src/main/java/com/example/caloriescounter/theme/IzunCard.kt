package com.example.caloriescounter.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

@Composable
internal fun IzunCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = lerp(colors.surface, colors.surfaceVariant, 0.55f),
            contentColor = colors.onSurface
        ),
        border = BorderStroke(1.dp, colors.onSurface.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        content = content
    )
}
