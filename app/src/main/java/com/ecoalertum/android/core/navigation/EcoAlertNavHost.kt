package com.ecoalertum.android.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import com.ecoalertum.android.alerts.AlertDetailScreen
import com.ecoalertum.android.alerts.AlertHistoryScreen
import com.ecoalertum.android.alerts.AlertPreferencesScreen
import com.ecoalertum.android.alerts.AlertsScreen
import com.ecoalertum.android.assistant.AssistantScreen
import com.ecoalertum.android.auth.AuthViewModel
import com.ecoalertum.android.auth.ForgotPasswordScreen
import com.ecoalertum.android.auth.LoginScreen
import com.ecoalertum.android.auth.RegisterScreen
import com.ecoalertum.android.auth.VerifyEmailScreen
import com.ecoalertum.android.home.HomeScreen
import com.ecoalertum.android.learn.LearnScreen
import com.ecoalertum.android.learn.ProgressScreen
import com.ecoalertum.android.learn.lesson.LessonScreen
import com.ecoalertum.android.learn.lesson.LessonViewModel
import com.ecoalertum.android.learn.quiz.QuizScreen
import com.ecoalertum.android.learn.quiz.QuizViewModel
import com.ecoalertum.android.map.MapScreen
import com.ecoalertum.android.map.ModeratorQueueScreen
import com.ecoalertum.android.map.MyReportsScreen
import com.ecoalertum.android.map.SubmitReportScreen
import com.ecoalertum.android.settings.AboutScreen
import com.ecoalertum.android.settings.EmergencyContactsScreen
import com.ecoalertum.android.assistant.HazardExplainedScreen

/**
 * The app's navigation graph.
 *
 * Login sits in front of [Routes.MAIN_GRAPH]. [startDestination] comes from [AuthViewModel.startRoute]
 * (login, verify email, or straight into the app). Moving between login and the app is done by
 * EcoAlertApp, which listens to the auth events. The top bar, side menu and bottom bar only show
 * inside [Routes.MAIN_GRAPH], so the login screens get a full screen.
 */
@Composable
fun EcoAlertNavHost(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    startDestination: String,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        authGraph(navController, authViewModel)
        mainGraph(navController)
    }
}

/** Module 6: the screens a student sees before they are logged in. */
private fun NavGraphBuilder.authGraph(navController: NavHostController, authViewModel: AuthViewModel) {
    composable(Routes.LOGIN) {
        LoginScreen(
            viewModel = authViewModel,
            onRegister = { navController.navigate(Routes.REGISTER) { launchSingleTop = true } },
            onForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) { launchSingleTop = true } },
        )
    }
    composable(Routes.REGISTER) {
        RegisterScreen(viewModel = authViewModel, onBackToLogin = { navController.popBackStack() })
    }
    composable(Routes.FORGOT_PASSWORD) {
        ForgotPasswordScreen(viewModel = authViewModel, onBackToLogin = { navController.popBackStack() })
    }
    composable(Routes.VERIFY_EMAIL) {
        VerifyEmailScreen(viewModel = authViewModel)
    }
}

/** Everything behind login: the bottom-bar tabs and the side-menu screens. */
private fun NavGraphBuilder.mainGraph(navController: NavHostController) {
    navigation(startDestination = Routes.HOME, route = Routes.MAIN_GRAPH) {
        // Bottom-bar tabs
        composable(Routes.HOME) {
            HomeScreen(
                onWhatToDo = {
                    navController.navigateToTab(Routes.ALERTS)
                },
                onExplain = { alertId ->
                    navController.navigate(Routes.hazardExplained(alertId))
                },
            )
        }
        composable(Routes.ALERTS) {
            AlertsScreen(onAlertClick = { id -> navController.navigate(Routes.alertDetail(id)) })
        }
        composable(Routes.MAP) { MapScreen() }
        composable(Routes.ASSISTANT) { AssistantScreen() }
        composable(Routes.LEARN) {
            LearnScreen(
                onStartQuiz = { quizId, phase -> navController.navigate(Routes.quiz(quizId, phase.value)) },
                onStartLesson = { topic -> navController.navigate(Routes.lesson(topic)) },
                onOpenProgress = { navController.navigate(Routes.PROGRESS) { launchSingleTop = true } },
            )
        }

        // Side-menu screens
        composable(Routes.PROGRESS) { ProgressScreen() }
        composable(Routes.MY_REPORTS) {
            MyReportsScreen(onAddReport = { navController.navigate(Routes.SUBMIT_REPORT) })
        }
        composable(Routes.ALERT_HISTORY) { AlertHistoryScreen() }
        composable(Routes.ALERT_PREFERENCES) { AlertPreferencesScreen() }
        composable(Routes.EMERGENCY_CONTACTS) { EmergencyContactsScreen() }
        composable(Routes.ABOUT) { AboutScreen() }
        composable(Routes.MODERATOR_QUEUE) { ModeratorQueueScreen() }

        // Reached from the + button on My Reports
        composable(Routes.SUBMIT_REPORT) {
            SubmitReportScreen(onSubmitted = { navController.popBackStack() })
        }

        // Reached from the Learn tab (Module 5, FR5.1)
        composable(
            route = Routes.QUIZ,
            arguments = listOf(
                navArgument(QuizViewModel.ARG_QUIZ_ID) { type = NavType.StringType },
                navArgument(QuizViewModel.ARG_PHASE) { type = NavType.StringType },
            ),
        ) {
            QuizScreen(onExit = { navController.popBackStack() })
        }

        // Reached from the Learn tab (Module 5, FR5.2). Finishing it opens the topic's post-quiz.
        composable(
            route = Routes.LESSON,
            arguments = listOf(navArgument(LessonViewModel.ARG_TOPIC) { type = NavType.StringType }),
        ) {
            LessonScreen(
                onExit = { navController.popBackStack() },
                onStartPostQuiz = { quizId ->
                    navController.navigate(Routes.quiz(quizId, "post")) {
                        popUpTo(Routes.LESSON) { inclusive = true }
                    }
                },
            )
        }


        // Module 2: one alert, from the Alerts list or a notification (FR2.1)
        composable(
            route = Routes.ALERT_DETAIL,
            arguments = listOf(navArgument("alertId") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "ecoalertum://alert/{alertId}" }),
        ) {
            AlertDetailScreen(
                onExplainWithAI = { alertId ->
                    navController.navigate(Routes.hazardExplained(alertId))
                },
            )
        }

        // Module 4, FR4.1: AI hazard explanation.
        composable(
            route = Routes.HAZARD_EXPLAINED,
            arguments = listOf(
                navArgument("alertId") { type = NavType.StringType }
            ),
        ) { backStackEntry ->
            val alertId = backStackEntry.arguments
                ?.getString("alertId")
                .orEmpty()

            HazardExplainedScreen(
                alertId = alertId,
            )
        }
    }
}

/** Opens a bottom-bar tab the same way the bottom bar does (see EcoAlertApp). */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
