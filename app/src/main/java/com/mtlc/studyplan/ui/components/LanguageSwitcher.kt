package com.mtlc.studyplan.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Language switching component with Turkish and UK flag icons
 * Positioned in top-right corner with hover tooltips
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSwitcher(
    currentLanguage: Language = Language.ENGLISH,
    onLanguageChanged: (Language) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = modifier.wrapContentSize()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Turkish Flag
            LanguageFlag(
                flag = "🇹🇷",
                language = Language.TURKISH,
                isSelected = currentLanguage == Language.TURKISH,
                tooltip = "Türkçe",
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLanguageChanged(Language.TURKISH)
                }
            )

            // UK Flag
            LanguageFlag(
                flag = "🇬🇧",
                language = Language.ENGLISH,
                isSelected = currentLanguage == Language.ENGLISH,
                tooltip = "English",
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLanguageChanged(Language.ENGLISH)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageFlag(
    flag: String,
    language: Language,
    isSelected: Boolean,
    tooltip: String,
    onClick: () -> Unit
) {
    // Animation values
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = tween(200),
        label = "flag_scale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFE3F2FD) else Color.Transparent,
        animationSpec = tween(200),
        label = "flag_background"
    )

    Box {
        Surface(
            modifier = Modifier
                .size(40.dp)
                .scale(scale)
                .clip(CircleShape)
                .clickable { onClick() }
                .semantics {
                    contentDescription = "Switch to $tooltip"
                },
            color = backgroundColor,
            shape = CircleShape,
            shadowElevation = if (isSelected) 2.dp else 0.dp
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = flag,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )

                // Selection indicator
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = Color(0xFF1976D2),
                                shape = CircleShape
                            )
                            .align(Alignment.BottomEnd)
                    )
                }
            }
        }

        // Simplified tooltip - using content description for accessibility
        // The tooltip functionality is handled through semantics for better compatibility
    }
}

/**
 * Compact language switcher for smaller spaces
 */
@Composable
fun CompactLanguageSwitcher(
    currentLanguage: Language = Language.ENGLISH,
    onLanguageChanged: (Language) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    Surface(
        modifier = modifier
            .wrapContentSize()
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                val newLanguage = if (currentLanguage == Language.ENGLISH) {
                    Language.TURKISH
                } else {
                    Language.ENGLISH
                }
                onLanguageChanged(newLanguage)
            },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (currentLanguage == Language.ENGLISH) "🇬🇧" else "🇹🇷",
                fontSize = 16.sp
            )
            Text(
                text = if (currentLanguage == Language.ENGLISH) "EN" else "TR",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Language enumeration
 */
enum class Language(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    TURKISH("tr", "Türkçe")
}

/**
 * Language state management
 */
@Composable
fun rememberLanguageState(
    initialLanguage: Language = Language.ENGLISH
): MutableState<Language> {
    return remember { mutableStateOf(initialLanguage) }
}