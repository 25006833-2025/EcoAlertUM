package com.ecoalertum.android.home

import com.ecoalertum.android.core.data.TestData
import com.ecoalertum.android.core.data.TestData.MINUTE
import com.ecoalertum.android.core.data.TestData.NOW
import com.ecoalertum.android.core.data.fake.FakeAlertRepository
import com.ecoalertum.android.core.data.fake.FakeHazardRepository
import com.ecoalertum.android.core.data.model.Level
import com.ecoalertum.android.core.data.model.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** The ViewModel on the mock scenario (23 Oct 2026, 18:00, thunderstorm over UM). */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val hazards = FakeHazardRepository(TestData.readings(), nowMillis = { NOW })
    private val alerts = FakeAlertRepository(TestData.alerts(), nowMillis = { NOW })
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = HomeViewModel(hazards, alerts, nowMillis = { NOW }, language = "en")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Collects [HomeViewModel.uiState] (it only runs while collected) and returns the latest value. */
    private fun kotlinx.coroutines.test.TestScope.content(): HomeUiState.Content {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel.uiState.value as HomeUiState.Content
    }

    @Test
    fun mockScenarioFillsHeroAndCards() = runTest {
        val state = content()

        // Three active Advisory candidates; the flood alert is the most recent.
        assertEquals(Level.ADVISORY, state.hero.level)
        assertEquals(HeroUiModel.Kind.ALERT, state.hero.kind)
        assertEquals("River level rising: Sg. Klang", state.hero.title)

        val weather = (state.weather as CardState.Ready).data
        assertEquals(Level.ADVISORY, weather.level)
        assertEquals("Thunderstorms Warning", weather.title)

        val flood = (state.flood as CardState.Ready).data
        assertEquals("Sg. Klang di Bukit Putra (F2)", flood.stationName)
        assertEquals(25.12, flood.waterLevelMetres!!, 0.0)
        assertEquals(Level.ADVISORY, flood.level)

        val air = (state.air as CardState.Ready).data
        assertEquals(AirCard(Level.NORMAL, 64, AirQualityCategory.MODERATE), air)

        assertFalse(state.isOutdated)
        assertEquals(NOW, state.updatedAt)
    }

    @Test
    fun sourceDownShowsSavedDataAsOutdated() = runTest {
        val state = content()
        assertFalse(state.isOutdated)

        hazards.simulateSourceDown(Source.INFOBANJIR, lastSuccessAt = NOW - 90 * MINUTE)

        val outdated = viewModel.uiState.value as HomeUiState.Content
        assertTrue(outdated.isOutdated)
        assertEquals(NOW - 90 * MINUTE, outdated.savedAt)
        val flood = outdated.flood as CardState.Ready
        assertTrue(flood.isOutdated)
        assertEquals("Sg. Klang di Bukit Putra (F2)", flood.data.stationName)
        assertFalse((outdated.air as CardState.Ready).isOutdated)
    }

    @Test
    fun retryKeepsShowingDataWhileReloading() = runTest {
        val states = mutableListOf<HomeUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { states += it } }
        val firstContent = states.indexOfFirst { it is HomeUiState.Content }

        viewModel.retry()

        assertTrue(firstContent >= 0)
        assertTrue(states.drop(firstContent).none { it is HomeUiState.Loading })
        assertTrue(viewModel.uiState.value is HomeUiState.Content)
    }

    @Test
    fun allSourcesFailingWithCacheStillShowsData() = runTest {
        content()

        hazards.simulateError("Permission denied")

        val state = viewModel.uiState.value as HomeUiState.Content
        assertTrue(state.isOutdated)
        assertEquals("Sg. Klang di Bukit Putra (F2)", (state.flood as CardState.Ready).data.stationName)
    }
}
