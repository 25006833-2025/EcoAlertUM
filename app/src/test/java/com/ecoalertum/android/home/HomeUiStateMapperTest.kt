package com.ecoalertum.android.home

import com.ecoalertum.android.core.data.TestData.MINUTE
import com.ecoalertum.android.core.data.TestData.NOW
import com.ecoalertum.android.core.data.model.Alert
import com.ecoalertum.android.core.data.model.AlertStatus
import com.ecoalertum.android.core.data.model.HazardReading
import com.ecoalertum.android.core.data.model.Level
import com.ecoalertum.android.core.data.model.LocalizedText
import com.ecoalertum.android.core.data.model.ReadingType
import com.ecoalertum.android.core.data.model.Source
import com.ecoalertum.android.core.data.repository.DataState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeUiStateMapperTest {

    // --- Test data -------------------------------------------------------------------------

    private fun alert(id: String, level: Level, issuedAt: Long, status: AlertStatus = AlertStatus.ACTIVE) = Alert(
        id = id,
        hazardType = ReadingType.RAINFALL,
        level = level,
        title = LocalizedText(en = "Alert $id", ms = "Amaran $id"),
        status = status,
        issuedAt = issuedAt,
        expiresAt = NOW + 60 * MINUTE,
    )

    private fun warning(level: Level, validFrom: Long, validTo: Long?) = HazardReading(
        id = "met_$validFrom",
        type = ReadingType.WEATHER_WARNING,
        source = Source.MET,
        stationName = "Thunderstorms Warning",
        level = level,
        summary = LocalizedText(en = "Thunderstorms for KL", ms = "Ribut petir bagi KL"),
        validFrom = validFrom,
        validTo = validTo,
    )

    private fun river(name: String, distanceKm: Double?, level: Level, value: Double = 20.0) = HazardReading(
        id = name,
        type = ReadingType.FLOOD_LEVEL,
        source = Source.INFOBANJIR,
        stationName = name,
        stationDistanceKm = distanceKm,
        value = value,
        level = level,
    )

    private fun air(api: Double, level: Level = Level.NORMAL) =
        HazardReading(type = ReadingType.AIR_QUALITY, source = Source.DOE, value = api, level = level)

    private fun <T> fresh(data: T) = DataState.Success(data, isOutdated = false, lastUpdated = NOW)

    private fun map(
        rain: DataState<List<HazardReading>> = fresh(emptyList()),
        flood: DataState<List<HazardReading>> = fresh(listOf(river("A", 1.0, Level.NORMAL))),
        haze: DataState<List<HazardReading>> = fresh(listOf(air(40.0))),
        alerts: DataState<List<Alert>> = fresh(emptyList()),
    ) = HomeUiStateMapper.map(rain, flood, haze, alerts, NOW, language = "en")

    // --- Hero ------------------------------------------------------------------------------

    @Test
    fun heroIsTheMostSevereActiveAlert() {
        val hero = HomeUiStateMapper.hero(
            listOf(alert("advisory", Level.ADVISORY, NOW), alert("danger", Level.DANGER, NOW - 30 * MINUTE)),
            emptyList(), NOW, "en",
        )

        assertEquals(Level.DANGER, hero.level)
        assertEquals(HeroUiModel.Kind.ALERT, hero.kind)
        assertEquals("Alert danger", hero.title)
        assertEquals(NOW + 60 * MINUTE, hero.untilMillis)
    }

    @Test
    fun heroCanBeAMoreSevereWeatherWarning() {
        val hero = HomeUiStateMapper.hero(
            listOf(alert("advisory", Level.ADVISORY, NOW)),
            listOf(warning(Level.WARNING, validFrom = NOW - 60 * MINUTE, validTo = NOW + 30 * MINUTE)),
            NOW, "en",
        )

        assertEquals(Level.WARNING, hero.level)
        assertEquals(HeroUiModel.Kind.WEATHER_WARNING, hero.kind)
        assertEquals("Thunderstorms Warning", hero.title)
        assertEquals(NOW + 30 * MINUTE, hero.untilMillis)
    }

    @Test
    fun heroTieGoesToTheMostRecentlyIssued() {
        val hero = HomeUiStateMapper.hero(
            listOf(alert("older", Level.ADVISORY, NOW - 10 * MINUTE), alert("newer", Level.ADVISORY, NOW - 5 * MINUTE)),
            listOf(warning(Level.ADVISORY, validFrom = NOW - 60 * MINUTE, validTo = null)),
            NOW, "en",
        )

        assertEquals("Alert newer", hero.title)
    }

    @Test
    fun heroIgnoresExpiredAndCancelledAlertsAndEndedWarnings() {
        val hero = HomeUiStateMapper.hero(
            listOf(
                alert("expired", Level.DANGER, NOW, AlertStatus.EXPIRED),
                alert("cancelled", Level.DANGER, NOW, AlertStatus.CANCELLED),
            ),
            listOf(warning(Level.WARNING, validFrom = NOW - 120 * MINUTE, validTo = NOW - 1)),
            NOW, "en",
        )

        assertEquals(HeroUiModel(Level.NORMAL, HeroUiModel.Kind.NONE, title = null, untilMillis = null), hero)
    }

    @Test
    fun heroUsesTheAppLanguage() {
        val hero = HomeUiStateMapper.hero(listOf(alert("a", Level.ADVISORY, NOW)), emptyList(), NOW, "ms")

        assertEquals("Amaran a", hero.title)
    }

    @Test
    fun heroUsesCachedAlertsWhenAlertsFail() {
        val state = map(alerts = DataState.Error("offline", cachedData = listOf(alert("cached", Level.WARNING, NOW))))

        assertEquals("Alert cached", (state as HomeUiState.Content).hero.title)
    }

    // --- Cards -----------------------------------------------------------------------------

    @Test
    fun weatherCardShowsTheMostSevereCurrentWarning() {
        val card = HomeUiStateMapper.weatherCard(
            listOf(
                warning(Level.ADVISORY, validFrom = NOW - 10 * MINUTE, validTo = null),
                warning(Level.WARNING, validFrom = NOW - 20 * MINUTE, validTo = NOW + MINUTE),
                HazardReading(type = ReadingType.RAINFALL, level = Level.DANGER),
            ),
            NOW, "en",
        )

        assertEquals(WeatherCard(Level.WARNING, "Thunderstorms Warning", "Thunderstorms for KL"), card)
    }

    @Test
    fun weatherCardIsNormalWithoutAWarning() {
        // A rain gauge reading is InfoBanjir data, not a MET warning, so it does not change this card.
        val card = HomeUiStateMapper.weatherCard(
            listOf(HazardReading(type = ReadingType.RAINFALL, level = Level.ADVISORY)), NOW, "en",
        )

        assertEquals(WeatherCard(Level.NORMAL, title = null, detail = null), card)
    }

    @Test
    fun floodCardShowsTheNearestStation() {
        val card = HomeUiStateMapper.floodCard(
            listOf(river("far", 8.0, Level.DANGER), river("near", 2.5, Level.NORMAL, value = 21.4), river("unknown", null, Level.WARNING)),
        )

        assertEquals(FloodCard(Level.NORMAL, "near", 21.4), card)
    }

    @Test
    fun floodCardTieGoesToTheMoreSevereStation() {
        val card = HomeUiStateMapper.floodCard(listOf(river("calm", 3.2, Level.NORMAL), river("rising", 3.2, Level.ADVISORY)))

        assertEquals("rising", card!!.stationName)
        assertEquals(Level.ADVISORY, card.level)
    }

    @Test
    fun floodCardIsNullWithoutReadings() {
        assertNull(HomeUiStateMapper.floodCard(emptyList()))
    }

    @Test
    fun airCardRoundsApiAndKeepsTheReadingLevel() {
        assertEquals(AirCard(Level.ADVISORY, 118, AirQualityCategory.UNHEALTHY), HomeUiStateMapper.airCard(listOf(air(117.6, Level.ADVISORY))))
    }

    @Test
    fun airQualityCategoriesFollowDoeBands() {
        val expected = mapOf(
            0 to AirQualityCategory.GOOD,
            50 to AirQualityCategory.GOOD,
            51 to AirQualityCategory.MODERATE,
            100 to AirQualityCategory.MODERATE,
            101 to AirQualityCategory.UNHEALTHY,
            200 to AirQualityCategory.UNHEALTHY,
            201 to AirQualityCategory.VERY_UNHEALTHY,
            300 to AirQualityCategory.VERY_UNHEALTHY,
            301 to AirQualityCategory.HAZARDOUS,
        )
        expected.forEach { (api, category) -> assertEquals("API $api", category, AirQualityCategory.fromApi(api)) }
        assertNull(AirQualityCategory.fromApi(null))
        assertNull(AirQualityCategory.fromApi(-1))
    }

    // --- Outdated, loading and error -------------------------------------------------------

    @Test
    fun freshDataIsNotOutdated() {
        val state = map() as HomeUiState.Content

        assertFalse(state.isOutdated)
        assertNull(state.savedAt)
        assertEquals(NOW, state.updatedAt)
    }

    @Test
    fun outdatedSourceMarksOnlyItsCardAndKeepsTheData() {
        val savedAt = NOW - 90 * MINUTE
        val state = map(flood = DataState.Success(listOf(river("A", 1.0, Level.ADVISORY)), isOutdated = true, lastUpdated = savedAt))
            as HomeUiState.Content

        assertTrue(state.isOutdated)
        assertEquals(savedAt, state.savedAt)
        assertEquals(savedAt, state.updatedAt)
        val flood = state.flood as CardState.Ready
        assertTrue(flood.isOutdated)
        assertEquals("A", flood.data.stationName)
        assertFalse((state.air as CardState.Ready).isOutdated)
    }

    @Test
    fun bannerTimeIsTheOldestOutdatedCard() {
        val state = map(
            flood = DataState.Success(listOf(river("A", 1.0, Level.NORMAL)), isOutdated = true, lastUpdated = NOW - 60 * MINUTE),
            haze = DataState.Success(listOf(air(40.0)), isOutdated = true, lastUpdated = NOW - 120 * MINUTE),
        ) as HomeUiState.Content

        assertEquals(NOW - 120 * MINUTE, state.savedAt)
    }

    @Test
    fun errorWithCachedDataShowsSavedDataAsOutdated() {
        val state = map(haze = DataState.Error("denied", cachedData = listOf(air(64.0)), lastUpdated = NOW - 50 * MINUTE))
            as HomeUiState.Content

        val air = state.air as CardState.Ready
        assertEquals(64, air.data.api)
        assertTrue(air.isOutdated)
        assertEquals(NOW - 50 * MINUTE, air.lastUpdated)
        assertTrue(state.isOutdated)
    }

    @Test
    fun errorWithoutCacheMakesOnlyThatCardUnavailable() {
        val state = map(flood = DataState.Error("denied")) as HomeUiState.Content

        assertEquals(CardState.Unavailable, state.flood)
        assertTrue(state.air is CardState.Ready)
    }

    @Test
    fun screenIsLoadingUntilAnyCardHasData() {
        assertEquals(HomeUiState.Loading, map(rain = DataState.Loading, flood = DataState.Loading, haze = DataState.Loading))
    }

    @Test
    fun screenIsErrorWhenEveryCardFailsWithoutCache() {
        val failed = DataState.Error<List<HazardReading>>("offline")

        assertEquals(HomeUiState.Error, map(rain = failed, flood = failed, haze = failed))
    }

    @Test
    fun oneLoadedCardIsEnoughToShowContent() {
        val state = map(rain = DataState.Loading, flood = DataState.Error("offline"))

        assertTrue(state is HomeUiState.Content)
        assertEquals(CardState.Loading, (state as HomeUiState.Content).weather)
    }

    @Test
    fun activeAlertPassesItsIdToHero() {
        val hero = HomeUiStateMapper.hero(
            listOf(alert("alert-123", Level.ADVISORY, NOW)),
            emptyList(),
            NOW,
            "en"
        )

        assertEquals(HeroUiModel.Kind.ALERT, hero.kind)
        assertEquals("alert-123", hero.alertId)
    }

    @Test
    fun metWarningHasNoAlertId() {
        val hero = HomeUiStateMapper.hero(
            emptyList(),
            listOf(
                warning(
                    Level.WARNING,
                    validFrom = NOW - 10 * MINUTE,
                    validTo = NOW + 30 * MINUTE
                )
            ),
            NOW,
            "en"
        )

        assertEquals(HeroUiModel.Kind.WEATHER_WARNING, hero.kind)
        assertNull(hero.alertId)
    }

    @Test
    fun mostSevereAlertPassesCorrectId() {
        val hero = HomeUiStateMapper.hero(
            listOf(
                alert("advisory", Level.ADVISORY, NOW),
                alert("danger", Level.DANGER, NOW - 30 * MINUTE)
            ),
            emptyList(),
            NOW,
            "en"
        )

        assertEquals(Level.DANGER, hero.level)
        assertEquals("danger", hero.alertId)
    }

}
