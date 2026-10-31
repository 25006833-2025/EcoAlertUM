package com.ecoalertum.android.core.data

import android.content.Context
import com.ecoalertum.android.core.data.fake.FakeAlertRepository
import com.ecoalertum.android.core.data.fake.FakeHazardRepository
import com.ecoalertum.android.core.data.fake.MockJsonParser
import com.ecoalertum.android.core.data.repository.AlertRepository
import com.ecoalertum.android.core.data.repository.HazardRepository

/**
 * Creates the app's repositories once. Screens get them from here instead of constructing them,
 * so moving from fake to Firestore data changes one line per repository.
 */
class AppContainer(context: Context) {

    private val assets = context.applicationContext.assets

    // Part 2: replace with the Firestore implementation.
    val hazardRepository: HazardRepository by lazy {
        FakeHazardRepository(MockJsonParser.parseReadings(readAsset("mock/hazardReadings.json")))
    }

    // Part 2: replace with the Firestore implementation.
    val alertRepository: AlertRepository by lazy {
        FakeAlertRepository(MockJsonParser.parseAlerts(readAsset("mock/alerts.json")))
    }

    private fun readAsset(path: String): String = assets.open(path).bufferedReader().use { it.readText() }
}
