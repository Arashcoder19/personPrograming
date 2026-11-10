package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.BackupRestoreDialog
import com.example.ui.screens.*
import com.example.ui.theme.DaftareManTheme
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookApp(
    viewModel: NotebookViewModel = viewModel()
) {
    val currentSection by viewModel.currentSection.collectAsState()
    val isDarkModeOverride by viewModel.isDarkMode.collectAsState()
    val isBackupDialogOpen by viewModel.isBackupDialogOpen.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val systemDark = isSystemInDarkTheme()
    val effectiveDark = isDarkModeOverride ?: systemDark

    DaftareManTheme(darkTheme = effectiveDark) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "دفتر من",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "مدیریت کارها، اهداف و رشد فردی",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        actions = {
                            TextButton(onClick = { viewModel.toggleDarkMode() }) {
                                Text(
                                    text = when (isDarkModeOverride) {
                                        true -> "🌙 تیره"
                                        false -> "☀️ روشن"
                                        null -> "◐ پوسته"
                                    },
                                    fontSize = 12.sp
                                )
                            }
                            Button(
                                onClick = { viewModel.isBackupDialogOpen.value = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "پشتیبان‌گیری",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // Navigation Bar (Scrollable Tabs for all 10 areas)
                    ScrollableTabRow(
                        selectedTabIndex = currentSection.ordinal,
                        edgePadding = 12.dp,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        AppSection.values().forEach { section ->
                            val selected = currentSection == section
                            Tab(
                                selected = selected,
                                onClick = { viewModel.navigateTo(section) },
                                text = {
                                    Text(
                                        text = section.title,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                },
                                icon = {
                                    Icon(
                                        imageVector = getSectionIcon(section, selected),
                                        contentDescription = section.title,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Crossfade(targetState = currentSection, label = "SectionTransition") { section ->
                    when (section) {
                        AppSection.TODAY -> TodayScreen(viewModel)
                        AppSection.SEARCH -> SearchScreen(viewModel)
                        AppSection.TASKS -> TasksScreen(viewModel)
                        AppSection.PROJECTS -> ProjectsScreen(viewModel)
                        AppSection.FINANCE -> FinanceScreen(viewModel)
                        AppSection.BUY -> BuyScreen(viewModel)
                        AppSection.GOALS -> GoalsScreen(viewModel)
                        AppSection.IDEAS -> IdeasScreen(viewModel)
                        AppSection.GROW -> GrowScreen(viewModel)
                        AppSection.SESSIONS -> ThinkingSessionsScreen(viewModel)
                    }
                }
            }

            if (isBackupDialogOpen) {
                BackupRestoreDialog(
                    viewModel = viewModel,
                    onDismiss = { viewModel.isBackupDialogOpen.value = false }
                )
            }
        }
    }
}

fun getSectionIcon(section: AppSection, selected: Boolean): ImageVector {
    return when (section) {
        AppSection.TODAY -> if (selected) Icons.Filled.Today else Icons.Outlined.Today
        AppSection.SEARCH -> Icons.Filled.Search
        AppSection.TASKS -> if (selected) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle
        AppSection.PROJECTS -> if (selected) Icons.Filled.Folder else Icons.Outlined.Folder
        AppSection.FINANCE -> if (selected) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet
        AppSection.BUY -> if (selected) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart
        AppSection.GOALS -> if (selected) Icons.Filled.Flag else Icons.Outlined.Flag
        AppSection.IDEAS -> if (selected) Icons.Filled.Lightbulb else Icons.Outlined.Lightbulb
        AppSection.GROW -> if (selected) Icons.Filled.TrendingUp else Icons.Outlined.TrendingUp
        AppSection.SESSIONS -> if (selected) Icons.Filled.Psychology else Icons.Outlined.Psychology
    }
}
