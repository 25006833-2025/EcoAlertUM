package com.ecoalertum.android.home

import com.ecoalertum.android.core.data.model.Alert
import com.ecoalertum.android.core.data.model.AlertStatus
import com.ecoalertum.android.core.data.model.HazardReading
import com.ecoalertum.android.core.data.model.Level
import com.ecoalertum.android.core.data.model.ReadingType
import com.ecoalertum.android.core.data.repository.DataState
import kotlin.math.roundToInt

/** Pure rules that turn repository states into [HomeUiState]. No Android code, so it is unit tested on the JVM. */
object HomeUiStateMapper {

    fun map(
        rain: DataState<List<HazardReading>>,
        flood: DataState<List<HazardReading>>,
        haze: DataState<List<HazardReading>>,
        alerts: DataState<List<Alert>>,
        nowMillis: Long,
        language: String,
    ): HomeUiState {
        val weatherCard = cardState(rain) { weatherCard(it, nowMillis, language) }
        val floodCard = cardState(flood, ::floodCard)
        val airCard = cardState(haze, ::airCard)

        val cards = listOf(weatherCard, floodCard, airCard)
        val ready = cards.filterIsInstance<CardState.Ready<*>>()
        if (ready.isEmpty()) {
            return if (cards.any { it is CardState.Loading }) HomeUiState.Loading else HomeUiState.Error
        }
        val outdated = ready.filter { it.isOutdated }

        return HomeUiState.Content(
            hero = hero(alerts.dataOrCache().orEmpty(), rain.dataOrCache().orEmpty(), nowMillis, language),
            weather = weatherCard,
            flood = floodCard,
            air = airCard,
            updatedAt = ready.mapNotNull { it.lastUpdated }.minOrNull(),
            isOutdated = outdated.isNotEmpty(),
            savedAt = outdated.mapNotNull { it.lastUpdated }.minOrNull(),
        )
    }

    /**
     * The most severe active alert or current MET warning. Ties go to the most recently issued.
     * Alerts are trusted on `status` (the backend expires them); warnings have no status, so their `validTo` is checked.
     */
    fun hero(alerts: List<Alert>, rainReadings: List<HazardReading>, nowMillis: Long, language: String): HeroUiModel {
        val alertCandidates = alerts
            .filter { it.status == AlertStatus.ACTIVE }
            .map {
                Candidate(
                    it.level,
                    it.issuedAt,
                    HeroUiModel(
                        level = it.level,
                        kind = HeroUiModel.Kind.ALERT,
                        title = it.title.get(language),
                        untilMillis = it.expiresAt.takeIf { t -> t > 0 },
                        alertId = it.id
                    )
                )
            }

        val warningCandidates = currentWarnings(rainReadings, nowMillis)
            .map { Candidate(it.level, it.issuedAt(), HeroUiModel(it.level, HeroUiModel.Kind.WEATHER_WARNING, it.stationName, it.validTo)) }

        return (alertCandidates + warningCandidates).maxWithOrNull(CANDIDATE_ORDER)?.hero
            ?: HeroUiModel(Level.NORMAL, HeroUiModel.Kind.NONE, title = null, untilMillis = null)
    }

    /** MET only. No warning in effect means [Level.NORMAL]. */
    fun weatherCard(rainReadings: List<HazardReading>, nowMillis: Long, language: String): WeatherCard {
        val warning = currentWarnings(rainReadings, nowMillis)
            .maxWithOrNull(compareBy<HazardReading> { it.level }.thenBy { it.issuedAt() })
            ?: return WeatherCard(Level.NORMAL, title = null, detail = null)
        return WeatherCard(warning.level, warning.stationName, warning.summary.get(language).ifBlank { null })
    }

    /** The nearest river station; ties go to the more severe one. Null when there are no river readings. */
    fun floodCard(floodReadings: List<HazardReading>): FloodCard? {
        val station = floodReadings
            .filter { it.type == ReadingType.FLOOD_LEVEL }
            .minWithOrNull(
                compareBy<HazardReading> { it.stationDistanceKm ?: Double.MAX_VALUE }.thenByDescending { it.level },
            ) ?: return null
        return FloodCard(station.level, station.stationName, station.value)
    }

    /** Null when there is no air quality reading. */
    fun airCard(hazeReadings: List<HazardReading>): AirCard? {
        val reading = hazeReadings.firstOrNull { it.type == ReadingType.AIR_QUALITY } ?: return null
        val api = reading.value?.roundToInt()
        return AirCard(reading.level, api, AirQualityCategory.fromApi(api))
    }

    private fun <T> cardState(state: DataState<List<HazardReading>>, build: (List<HazardReading>) -> T?): CardState<T> =
        when (state) {
            DataState.Loading -> CardState.Loading
            is DataState.Success -> build(state.data)
                ?.let { CardState.Ready(it, state.lastUpdated, state.isOutdated) }
                ?: CardState.Unavailable
            // Saved data is still shown when the read fails (NFR1), marked as outdated.
            is DataState.Error -> state.cachedData?.let(build)
                ?.let { CardState.Ready(it, state.lastUpdated, isOutdated = true) }
                ?: CardState.Unavailable
        }

    private fun <T> DataState<T>.dataOrCache(): T? = when (this) {
        DataState.Loading -> null
        is DataState.Success -> data
        is DataState.Error -> cachedData
    }

    private fun currentWarnings(readings: List<HazardReading>, nowMillis: Long): List<HazardReading> =
        readings.filter { it.type == ReadingType.WEATHER_WARNING && (it.validTo == null || it.validTo > nowMillis) }

    private fun HazardReading.issuedAt(): Long = validFrom ?: observedAt

    private class Candidate(val level: Level, val issuedAt: Long, val hero: HeroUiModel)

    private val CANDIDATE_ORDER = compareBy<Candidate> { it.level }.thenBy { it.issuedAt }
}
