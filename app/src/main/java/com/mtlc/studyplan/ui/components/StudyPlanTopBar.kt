package com.mtlc.studyplan.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtlc.studyplan.localization.rememberLanguageManager
import kotlinx.coroutines.launch

/**
 * Common top bar for all screens with language switcher
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyPlanTopBar(
    title: String,
    navigationIcon: ImageVector? = null,
    onNavigationClick: (() -> Unit)? = null,
    showLanguageSwitcher: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val context = LocalContext.current
    val languageManager = rememberLanguageManager(context)
    val coroutineScope = rememberCoroutineScope()

    TopAppBar(
        title = {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        navigationIcon = {
            if (navigationIcon != null && onNavigationClick != null) {
                IconButton(onClick = onNavigationClick) {
                    Icon(
                        imageVector = navigationIcon,
                        contentDescription = "Navigation",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        actions = {
            // Custom actions
            actions()

            // Language switcher
            if (showLanguageSwitcher) {
                Box(
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    LanguageSwitcher(
                        currentLanguage = languageManager.currentLanguage,
                        onLanguageChanged = { newLanguage ->
                            coroutineScope.launch {
                                languageManager.changeLanguage(newLanguage)
                            }
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

/**
 * Floating language switcher for pages without top bars (like home)
 */
@Composable
fun FloatingLanguageSwitcher(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val languageManager = rememberLanguageManager(context)
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .wrapContentSize()
            .padding(top = 8.dp, end = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            shadowElevation = 2.dp
        ) {
            Box(modifier = Modifier.padding(6.dp)) {
                LanguageSwitcher(
                    currentLanguage = languageManager.currentLanguage,
                    onLanguageChanged = { newLanguage ->
                        coroutineScope.launch {
                            languageManager.changeLanguage(newLanguage)
                        }
                    }
                )
            }
        }
    }
}

/**
 * Compact floating language switcher for overlay use
 */
@Composable
fun CompactFloatingLanguageSwitcher(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val languageManager = rememberLanguageManager(context)
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CompactLanguageSwitcher(
            currentLanguage = languageManager.currentLanguage,
            onLanguageChanged = { newLanguage ->
                coroutineScope.launch {
                    languageManager.changeLanguage(newLanguage)
                }
            }
        )
    }
}