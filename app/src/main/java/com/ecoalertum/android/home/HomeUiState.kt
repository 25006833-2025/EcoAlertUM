package com.ecoalertum.android.home

import com.ecoalertum.android.core.data.model.Level

/** Everything the Home screen shows. Built by [HomeUiStateMapper]. Times are epoch milliseconds. */
sealed interface HomeUiState {

    /** No card has data yet. */
    data object Loading : HomeUiState

    /** Every card failed and there is no saved data to show. */
    data object Error : HomeUiState

    data class Content(
        val hero: HeroUiModel,
        val weather: CardState<WeatherCard>,
        val flood: CardState<FloodCard>,
        val air: CardState<AirCard>,
        /** Oldest update time of the cards that have data: the top bar "Updated" chip. */
        val updatedAt: Long?,
        /** True when any card shows saved data: "Offline" chip, banner and OUTDATED badges. */
        val isOutdated: Boolean,
        /** Oldest update time of the outdated cards: the banner's "saved <time>". */
        val savedAt: Long?,
    ) : HomeUiState
}

/** One hazard card. */
sealed interface CardState<out T> {
    data object Loading : CardState<Nothing>

    /** Could not be read and nothing is saved. */
    data object Unavailable : CardState<Nothing>

    data class Ready<out T>(
        val data: T,
        val lastUpdated: Long?,
        val isOutdated: Boolean,
    ) : CardState<T>
}

/** The "RIGHT NOW" card. */
data class HeroUiModel(
    val level: Level,
    val kind: Kind,
    /** Alert title or MET warning title; null when [kind] is [Kind.NONE]. */
    val title: String?,
    /** When the alert or warning ends, if known. */
    val untilMillis: Long?,
) {
    enum class Kind { ALERT, WEATHER_WARNING, NONE }
}

/** MET Malaysia. [title] and [detail] are null when no warning is in effect. */
data class WeatherCard(
    val level: Level,
    val title: String?,
    val detail: String?,
)

/** Nearest InfoBanjir river station. */
data class FloodCard(
    val level: Level,
    val stationName: String,
    val waterLevelMetres: Double?,
)

/** DOE Air Pollutant Index. */
data class AirCard(
    val level: Level,
    val api: Int?,
    val category: AirQualityCategory?,
)

/** DOE API categories (docs/data-models.md). */
enum class AirQualityCategory(val maxApi: Int) {
    GOOD(50),
    MODERATE(100),
    UNHEALTHY(200),
    VERY_UNHEALTHY(300),
    HAZARDOUS(Int.MAX_VALUE);

    companion object {
        fun fromApi(api: Int?): AirQualityCategory? =
            if (api == null || api < 0) null else entries.first { api <= it.maxApi }
    }
}
