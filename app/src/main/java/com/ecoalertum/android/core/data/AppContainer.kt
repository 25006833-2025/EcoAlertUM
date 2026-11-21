package com.ecoalertum.android.core.data

import android.content.Context
import com.ecoalertum.android.BuildConfig
import com.ecoalertum.android.auth.AuthRepository
import com.ecoalertum.android.auth.FirebaseAuthRepository
import com.ecoalertum.android.core.data.fake.FakeAlertRepository
import com.ecoalertum.android.core.data.fake.FakeHazardRepository
import com.ecoalertum.android.core.data.fake.MockJsonParser
import com.ecoalertum.android.core.data.firestore.FirestoreAlertRepository
import com.ecoalertum.android.core.data.firestore.FirestoreHazardRepository
import com.ecoalertum.android.core.data.repository.AlertRepository
import com.ecoalertum.android.core.data.repository.BundledEmergencyContactRepository
import com.ecoalertum.android.core.data.repository.EmergencyContactRepository
import com.ecoalertum.android.core.data.repository.HazardRepository
import com.ecoalertum.android.learn.quiz.BundledQuizRepository
import com.ecoalertum.android.learn.quiz.FirestoreQuizAttemptRepository
import com.ecoalertum.android.learn.quiz.QuizAttemptRepository
import com.ecoalertum.android.learn.quiz.QuizRepository
import com.ecoalertum.android.learn.quiz.QuizScoreStore
import com.ecoalertum.android.learn.quiz.SharedPrefsQuizScoreStore

/**
 * Creates the app's repositories once. Screens get them from here instead of constructing them,
 * so moving from fake to Firestore data changes one line per repository.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val assets = appContext.assets

    // Live Firestore data needs a signed-in UM user (firestore.rules). Set USE_LIVE_DATA in
    // app/build.gradle.kts to true once login exists; until then the app shows the mock scenario.
    private val useLiveData = BuildConfig.USE_LIVE_DATA

    val hazardRepository: HazardRepository by lazy {
        if (useLiveData) {
            FirestoreHazardRepository()
        } else {
            FakeHazardRepository(MockJsonParser.parseReadings(readAsset("mock/hazardReadings.json")))
        }
    }

    val alertRepository: AlertRepository by lazy {
        if (useLiveData) {
            FirestoreAlertRepository()
        } else {
            FakeAlertRepository(MockJsonParser.parseAlerts(readAsset("mock/alerts.json")))
        }
    }

    // Module 6: login. Created on first use, after MainActivity has pointed Firebase at the emulator (debug builds).
    val authRepository: AuthRepository by lazy { FirebaseAuthRepository() }

    // Module 6 (FR6.2): emergency contacts are bundled in the app so they open with no signal.
    val emergencyContactRepository: EmergencyContactRepository by lazy {
        BundledEmergencyContactRepository { readAsset("mock/emergencyContacts.json") }
    }

    // Module 5: quiz content is bundled in the app for v1 (docs/data-models.md).
    val quizRepository: QuizRepository by lazy { BundledQuizRepository { readAsset("quizzes/quizzes.json") } }

    // Module 5: saves finished quiz attempts to Firestore.
    val quizAttemptRepository: QuizAttemptRepository by lazy { FirestoreQuizAttemptRepository() }

    // Module 5: latest pre/post scores on the phone, for the "before vs after" card.
    val quizScoreStore: QuizScoreStore by lazy { SharedPrefsQuizScoreStore(appContext) }

    private fun readAsset(path: String): String = assets.open(path).bufferedReader().use { it.readText() }
}
