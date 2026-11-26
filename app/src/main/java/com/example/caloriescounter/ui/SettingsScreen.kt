package com.example.caloriescounter.ui

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.caloriescounter.theme.ThemePalette
import com.example.caloriescounter.theme.ThemeState

@Composable
fun SettingsScreen(
    chatPromptCopied: Boolean = false,
    showDeveloperControls: Boolean = false,
    onConnectNutrition: () -> Unit,
    onChatPromptCopied: () -> Unit = {},
    onResetProfileCreation: () -> Unit = {},
    onResetTutorial: () -> Unit = {},
    bottomContentPadding: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val highlightTransition = rememberInfiniteTransition(label = "chatPromptHighlight")
    val chatPromptHighlightAlpha by highlightTransition.animateFloat(
        initialValue = 0.24f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chatPromptHighlightAlpha"
    )

    val chatGptPrompt = """
You are helping me add meals to IZUN.

If I have not shared a meal, food photo, menu, receipt, or nutrition label yet, briefly tell me what to do next:
- Send a clear food photo, menu, receipt, or nutrition label.
- Include portion size, item count, or package serving size when I know it.
- You can also add meals manually in IZUN without using a chatbot.

When I share a meal or image, respond in two sections:

    Meal review
    - Explain what food, portion size, and item count you identified.
    - Mention any uncertainty or assumptions.
    - Ask me to correct anything that looks wrong before I copy the IZUN Report.

    IZUN Report
    Provide a separate copyable code block that contains only this object. Use null for nutrients you cannot estimate. Use ISO-8601 local date-times for start_time and end_time. If the exact end time is unknown, make end_time one minute after start_time. Use meal_type as breakfast, lunch, dinner, or snack only.

{
  "name": "[Food or meal name]",
  "meal_type": "[breakfast/lunch/dinner/snack]",
  "start_time": "[yyyy-MM-ddTHH:mm:ss]",
  "end_time": "[yyyy-MM-ddTHH:mm:ss]",
  "energy_kcal": [number greater than 0],
  "nutrients": {
    "protein_g": [number or null],
    "total_carbohydrate_g": [number or null],
    "total_fat_g": [number or null],
    "dietary_fiber_g": [number or null],
    "sugar_g": [number or null],
    "saturated_fat_g": [number or null],
    "unsaturated_fat_g": [number or null],
    "trans_fat_g": [number or null],
    "sodium_mg": [number or null],
    "potassium_mg": [number or null],
    "cholesterol_mg": [number or null],
    "calcium_mg": [number or null],
    "iron_mg": [number or null],
    "magnesium_mg": [number or null],
    "zinc_mg": [number or null],
    "vitamin_a_mcg": [number or null],
    "vitamin_c_mg": [number or null],
    "vitamin_d_mcg": [number or null],
    "vitamin_e_mg": [number or null],
    "caffeine_mg": [number or null]
  }
}
    Tell the user to copy the IZUN Report, open Meal > Add in IZUN, tap Paste, and review the values before confirming. Mention that they can also enter meals manually.

""".trimIndent()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = "Settings",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION: THEME CUSTOMIZATION
        SettingsCard(title = "App Customization", icon = Icons.Default.Palette) {
            // Dark Mode Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (ThemeState.isDarkTheme.value) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = "Dark Theme",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Dark Mode", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                Switch(
                    checked = ThemeState.isDarkTheme.value,
                    onCheckedChange = { ThemeState.setDarkTheme(context.applicationContext, it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Color Palette", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val paletteOptions = listOf(ThemePalette.CYAN, ThemePalette.EMERALD, ThemePalette.VIOLET, ThemePalette.CORAL)
                Text("Main color", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f))
                PaletteRow(
                    palettes = paletteOptions,
                    selectedPalette = ThemeState.currentPalette.value,
                    onPaletteSelected = { selected ->
                        ThemeState.setPalette(
                            context.applicationContext,
                            selected.takeUnless { it == ThemeState.currentPalette.value }
                        )
                    }
                )
                Text("Soft background color", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f))
                PaletteRow(
                    palettes = paletteOptions,
                    selectedPalette = ThemeState.surfacePalette.value,
                    onPaletteSelected = { selected ->
                        ThemeState.setSurfacePalette(
                            context.applicationContext,
                            selected.takeUnless { it == ThemeState.surfacePalette.value }
                        )
                    }
                )
                PalettePreview()
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION: CHATGPT PROMPT COPIER
        SettingsCard(
            title = "Chat Prompt",
            icon = Icons.Default.Info,
            modifier = if (!chatPromptCopied) {
                Modifier.border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = chatPromptHighlightAlpha),
                    shape = RoundedCornerShape(20.dp)
                )
            } else {
                Modifier
            }
        ) {
            Text(
                text = "Copy the prompt into a chatbot to get a meal estimate. In IZUN, open Meal > Add, tap Paste, and review the report before confirming. The chatbot can receive whatever you share with it, including photos. Only share details you are comfortable sending to that service.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Chat Prompt", chatGptPrompt)
                    clipboard.setPrimaryClip(clip)
                    onChatPromptCopied()
                    Toast.makeText(context, "Chat Prompt copied to clipboard!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy Chat Prompt")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        SettingsCard(title = "Health Connect", icon = Icons.Default.Info) {
            Text(
                text = "Optionally save nutrition records to Health Connect when you confirm a meal. Manage or revoke access in Android settings.",
                fontSize = 13.sp
            )
            Button(onClick = onConnectNutrition, modifier = Modifier.fillMaxWidth()) {
                Text("Connect nutrition")
            }
        }

        if (showDeveloperControls) {
            Spacer(modifier = Modifier.height(16.dp))

            SettingsCard(title = "Developer", icon = Icons.Default.Info) {
                Text(
                    text = "Replay profile creation or the tutorial without deleting meal history",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Button(
                    onClick = onResetProfileCreation,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Replay Profile Creation")
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onResetTutorial,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Replay Tutorial")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(bottomContentPadding))
    }
}

@Composable
fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .then(modifier),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}

@Composable
fun PaletteRow(
    palettes: List<ThemePalette>,
    selectedPalette: ThemePalette?,
    onPaletteSelected: (ThemePalette) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        palettes.forEach { palette ->
            PaletteCircle(
                color = palette.previewColor(),
                isSelected = selectedPalette == palette,
                onClick = { onPaletteSelected(palette) }
            )
        }
    }
}

@Composable
fun PalettePreview() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.82f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
                )
            }
        }
    }
}

@Composable
fun PaletteCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private fun ThemePalette.previewColor(): Color {
    return when (this) {
        ThemePalette.CYAN -> Color(0xFF06B6D4)
        ThemePalette.EMERALD -> Color(0xFF10B981)
        ThemePalette.TEAL -> Color(0xFF14B8A6)
        ThemePalette.VIOLET -> Color(0xFF8B5CF6)
        ThemePalette.INDIGO -> Color(0xFF6366F1)
        ThemePalette.ROSE -> Color(0xFFFB7185)
        ThemePalette.CORAL -> Color(0xFFF97316)
        ThemePalette.AMBER -> Color(0xFFF59E0B)
        ThemePalette.SLATE -> Color(0xFF64748B)
    }
}
