package com.jolabs.looplog.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.jolabs.looplog.habit.R
import com.jolabs.looplog.data.repository.ai.AiFeatureAvailability
import com.jolabs.looplog.habit.navigation.AiHabitCreateRoute
import com.jolabs.looplog.habit.navigation.HabitCreateRoute
import com.jolabs.looplog.habit.navigation.HabitDetailRoute
import com.jolabs.looplog.habit.navigation.habitNavGraph
import com.jolabs.looplog.habit.ui.habitHome.HabitHomeRoute
import com.jolabs.looplog.habit.ui.habitStats.HabitStatsRoute
import com.jolabs.looplog.settings.SettingsRoute
import com.jolabs.looplog.settings.AppearanceRoute
import com.jolabs.looplog.settings.rememberBackupImportAction
import com.jolabs.looplog.settings.settingsNavGraph
import com.jolabs.looplog.ui.components.AdaptiveCenteredContainer
import com.jolabs.looplog.ui.components.ManualHabitCard
import com.jolabs.looplog.ui.components.SmartHabitCard
import kotlinx.coroutines.launch

internal enum class EmptyStateCardState {
    Loading,
    Smart,
    Manual,
}

internal fun AiFeatureAvailability.toEmptyStateCardState(): EmptyStateCardState = when (this) {
    AiFeatureAvailability.Checking -> EmptyStateCardState.Loading
    AiFeatureAvailability.Available -> EmptyStateCardState.Smart
    AiFeatureAvailability.Unavailable -> EmptyStateCardState.Manual
}

/**
 * Main app navigation setup - NavHost as primary container.
 * Conditionally shows either the empty state (when no habits exist)
 * or the main tabs interface (when habits exist).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppNavigationSetup(
    modifier: Modifier = Modifier,
    onShowChangelog: () -> Unit = {},
    appViewModel: AppViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val contentState by appViewModel.contentState.collectAsStateWithLifecycle()
    val aiFeatureAvailability by appViewModel.aiFeatureAvailability.collectAsStateWithLifecycle()
    val emptyStateCardState = aiFeatureAvailability.toEmptyStateCardState()
    val isAiAvailable = emptyStateCardState == EmptyStateCardState.Smart
    val importHabit = rememberBackupImportAction()

    val backStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(backStackEntry) {
        val entry = backStackEntry ?: return@LaunchedEffect
        val screenName = when {
            entry.destination.hasRoute<EmptyStateRoute>() -> AppAnalytics.Screens.EmptyState
            entry.destination.hasRoute<HabitDetailRoute>() -> AppAnalytics.Screens.HabitDetail
            entry.destination.hasRoute<AppearanceRoute>() -> AppAnalytics.Screens.Appearance
            entry.destination.hasRoute<HabitCreateRoute>() -> {
                val habitId = entry.toRoute<HabitCreateRoute>().habitId
                if (habitId == null || habitId == 0L) {
                    AppAnalytics.Screens.HabitCreate
                } else {
                    AppAnalytics.Screens.HabitEdit
                }
            }
            else -> null
        }
        screenName?.let(appViewModel::trackScreenView)
    }

    // Reconcile the stack once a root is current. Root entries also render the stable app mode,
    // so an obsolete root never becomes visible while this effect replaces it.
    LaunchedEffect(contentState, backStackEntry) {
        val currentDest = navController.currentDestination
        when (contentState) {
            AppContentState.Main -> if (currentDest?.hasRoute<EmptyStateRoute>() == true) {
                navController.navigate(MainTabsRoute) {
                    popUpTo(EmptyStateRoute) { inclusive = true }
                }
            }
            AppContentState.Empty -> if (currentDest?.hasRoute<MainTabsRoute>() == true) {
                navController.navigate(EmptyStateRoute) {
                    popUpTo(MainTabsRoute) { inclusive = true }
                }
            }
            is AppContentState.Error, AppContentState.Loading -> Unit
        }
    }

    Scaffold(
        modifier = modifier.windowInsetsPadding(
            WindowInsets.systemBars.union(WindowInsets.displayCutout)
        ),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        when (val state = contentState) {
            AppContentState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator(Modifier.size(48.dp))
                }
            }

            is AppContentState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            AppContentState.Empty, AppContentState.Main -> {
                // Keep the NavHost stable by determining startDestination once and
                // using navigation for transitions thereafter.
                val startDestination = remember {
                    if (state == AppContentState.Main) MainTabsRoute else EmptyStateRoute
                }
                val emptyStateContent = @Composable {
                    EmptyStateScreen(
                        cardState = emptyStateCardState,
                        onCreateFirstHabit = {
                            navController.navigate(
                                HabitCreateRoute(habitId = 0L, fromDetail = false)
                            ) {
                                launchSingleTop = true
                            }
                        },
                        onImportHabit = importHabit,
                        goToAIHabitCreation = {
                            navController.navigate(AiHabitCreateRoute) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                val mainTabsContent = @Composable {
                    MainTabsScreen(
                        navController = navController,
                        isAiAvailable = isAiAvailable,
                        trackScreen = appViewModel::trackScreenView,
                        onShowChangelog = onShowChangelog,
                    )
                }

                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.padding(innerPadding),
                    enterTransition = {
                        slideIntoContainer(
                            towards = navigationSlideDirection(isPop = false),
                            animationSpec = tween(NAVIGATION_ANIMATION_DURATION_MILLIS),
                        )
                    },
                    exitTransition = {
                        slideOutOfContainer(
                            towards = navigationSlideDirection(isPop = false),
                            animationSpec = tween(NAVIGATION_ANIMATION_DURATION_MILLIS),
                        )
                    },
                    popEnterTransition = {
                        slideIntoContainer(
                            towards = navigationSlideDirection(isPop = true),
                            animationSpec = tween(NAVIGATION_ANIMATION_DURATION_MILLIS),
                        )
                    },
                    popExitTransition = {
                        slideOutOfContainer(
                            towards = navigationSlideDirection(isPop = true),
                            animationSpec = tween(NAVIGATION_ANIMATION_DURATION_MILLIS),
                        )
                    },
                    predictivePopEnterTransition = { _ ->
                        slideIntoContainer(
                            towards = navigationSlideDirection(isPop = true),
                            animationSpec = tween(NAVIGATION_ANIMATION_DURATION_MILLIS),
                        )
                    },
                    predictivePopExitTransition = { _ ->
                        slideOutOfContainer(
                            towards = navigationSlideDirection(isPop = true),
                            animationSpec = tween(NAVIGATION_ANIMATION_DURATION_MILLIS),
                        )
                    },
                ) {
                    composable<EmptyStateRoute>(
                        enterTransition = {
                            if (initialState.destination.hasRoute<MainTabsRoute>()) {
                                EnterTransition.None
                            } else {
                                null
                            }
                        },
                        exitTransition = {
                            if (targetState.destination.hasRoute<MainTabsRoute>()) {
                                ExitTransition.None
                            } else {
                                null
                            }
                        }
                    ) {
                        if (state == AppContentState.Main) mainTabsContent() else emptyStateContent()
                    }

                    composable<MainTabsRoute>(
                        enterTransition = {
                            if (initialState.destination.hasRoute<EmptyStateRoute>()) {
                                EnterTransition.None
                            } else {
                                null
                            }
                        },
                        exitTransition = {
                            if (targetState.destination.hasRoute<EmptyStateRoute>()) {
                                ExitTransition.None
                            } else {
                                null
                            }
                        }
                    ) {
                        if (state == AppContentState.Empty) emptyStateContent() else mainTabsContent()
                    }

                    habitNavGraph(
                        onCreatePress = { habitId, fromDetail ->
                            navController.navigate(
                                HabitCreateRoute(habitId = habitId, fromDetail = fromDetail)
                            ) {
                                launchSingleTop = true
                            }
                        },
                        onDetailPress = { habitId ->
                            navController.navigate(HabitDetailRoute(habitId = habitId)) {
                                launchSingleTop = true
                            }
                        },
                        onAiCreatePress = {
                            navController.navigate(AiHabitCreateRoute) {
                                launchSingleTop = true
                            }
                        },
                        onNavigateUp = {
                            navController.popBackStack()
                        }
                    )
                    settingsNavGraph(
                        onNavigateUp = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
private fun MainTabsScreen(
    navController: NavController,
    isAiAvailable: Boolean,
    trackScreen: (String) -> Unit,
    onShowChangelog: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { BottomNavItem.items.size })
    val coroutineScope = rememberCoroutineScope()
    var isFabExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        trackScreen(
            when (pagerState.currentPage) {
                0 -> AppAnalytics.Screens.Home
                1 -> AppAnalytics.Screens.Stats
                else -> AppAnalytics.Screens.Settings
            }
        )
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentPage = pagerState.currentPage,
                onTabClick = { page ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(page)
                    }
                }
            )
        },
        floatingActionButton = {
            if (pagerState.currentPage != 2) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnimatedVisibility(
                        visible = isAiAvailable && isFabExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            ExtendedFloatingActionButton(
                                onClick = {
                                    isFabExpanded = false
                                    navController.navigate(AiHabitCreateRoute) {
                                        launchSingleTop = true
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.wand),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                text = {
                                    Text(
                                        text = "Create with AI",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                },
                                shape = RoundedCornerShape(16.dp)
                            )

                            ExtendedFloatingActionButton(
                                onClick = {
                                    isFabExpanded = false
                                    navController.navigate(
                                        HabitCreateRoute(habitId = 0L, fromDetail = false)
                                    ) {
                                        launchSingleTop = true
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.add),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                text = {
                                    Text(
                                        text = "Add habit manually",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }

                    FloatingActionButton(
                        onClick = {
                            if (isAiAvailable) {
                                isFabExpanded = !isFabExpanded
                            } else {
                                navController.navigate(
                                    HabitCreateRoute(habitId = 0L, fromDetail = false)
                                ) {
                                    launchSingleTop = true
                                }
                            }
                        },
                        containerColor = if (isFabExpanded) {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        contentColor = if (isFabExpanded) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onPrimary
                        },
                        shape = CircleShape
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isFabExpanded) R.drawable.close else R.drawable.add
                            ),
                            contentDescription = stringResource(
                                if (isFabExpanded) R.string.dismiss else R.string.new_habit
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            when (page) {
                0 -> HabitHomeRoute(
                    onCreatePress = { habitId ->
                        navController.navigate(
                            HabitCreateRoute(habitId = habitId, fromDetail = false)
                        ) {
                            launchSingleTop = true
                        }
                    }
                )
                1 -> HabitStatsRoute(
                    onCreatePress = { habitId ->
                        navController.navigate(
                            HabitCreateRoute(habitId = habitId, fromDetail = false)
                        ) {
                            launchSingleTop = true
                        }
                    },
                    onDetailPress = { habitId ->
                        navController.navigate(HabitDetailRoute(habitId = habitId)) {
                            launchSingleTop = true
                        }
                    }
                )
                2 -> SettingsRoute(
                    onWhatsNewClick = onShowChangelog,
                    onAppearanceClick = {
                        navController.navigate(AppearanceRoute) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(
    currentPage: Int,
    onTabClick: (Int) -> Unit
) {
    NavigationBar {
        BottomNavItem.items.forEachIndexed { index, item ->
            val selected = currentPage == index

            NavigationBarItem(
                selected = selected,
                onClick = { onTabClick(index) },
                icon = {
                    Icon(
                        painter = painterResource(id = item.icon),
                        contentDescription = stringResource(item.label)
                    )
                },
                label = {
                    Text(text = stringResource(item.label))
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EmptyStateScreen(
    cardState: EmptyStateCardState,
    onCreateFirstHabit: () -> Unit,
    onImportHabit: () -> Unit,
    goToAIHabitCreation: () -> Unit
) {
    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            AdaptiveCenteredContainer(modifier = Modifier.fillMaxSize()) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (cardState) {
                        EmptyStateCardState.Loading -> LoadingIndicator(Modifier.size(48.dp))
                        EmptyStateCardState.Smart -> {
                            SmartHabitCard(
                                modifier = Modifier.padding(40.dp),
                                wandIcon = R.drawable.wand,
                                privacyIcon = R.drawable.lock,
                                addIcon = R.drawable.add,
                                onCreateFirstHabit = onCreateFirstHabit,
                                onImportHabit = onImportHabit,
                                goToAIHabitCreation = goToAIHabitCreation
                            )
                        }
                        EmptyStateCardState.Manual -> {
                            ManualHabitCard(
                                modifier = Modifier.padding(40.dp),
                                addIcon = R.drawable.add,
                                onCreateFirstHabit = onCreateFirstHabit,
                                onImportHabit = onImportHabit,
                            )
                        }
                    }
                }
            }
        }
    }
}
