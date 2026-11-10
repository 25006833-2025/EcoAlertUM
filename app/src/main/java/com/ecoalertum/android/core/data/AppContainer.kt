package com.ecoalertum.android.core.data

import android.content.Context
import com.ecoalertum.android.core.data.fake.FakeAlertRepository
import com.ecoalertum.android.core.data.fake.FakeHazardRepository
import com.ecoalertum.android.core.data.fake.MockJsonParser
import com.ecoalertum.android.core.data.repository.AlertRepository
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

    // Part 2: replace with the Firestore implementation.
    val hazardRepository: HazardRepository by lazy {
        FakeHazardRepository(MockJsonParser.parseReadings(readAsset("mock/hazardReadings.json")))
    }

    // Part 2: replace with the Firestore implementation.
    val alertRepository: AlertRepository by lazy {
        FakeAlertRepository(MockJsonParser.parseAlerts(readAsset("mock/alerts.json")))
    }

    // Module 5: quiz content is bundled in the app for v1 (docs/data-models.md).
    val quizRepository: QuizRepository by lazy { BundledQuizRepository { readAsset("quizzes/quizzes.json") } }

    // Module 5: saves finished quiz attempts to Firestore.
    val quizAttemptRepository: QuizAttemptRepository by lazy { FirestoreQuizAttemptRepository() }

    // Module 5: latest pre/post scores on the phone, for the "before vs after" card.
    val quizScoreStore: QuizScoreStore by lazy { SharedPrefsQuizScoreStore(appContext) }

    private fun readAsset(path: String): String = assets.open(path).bufferedReader().use { it.readText() }
}
