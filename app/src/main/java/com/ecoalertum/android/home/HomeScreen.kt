package com.ecoalertum.android.home

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecoalertum.android.R
import com.ecoalertum.android.core.data.model.Level
import java.util.Date
import java.util.Locale

/** Home dashboard (Module 1, FR1.1–1.3). */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onWhatToDo: () -> Unit = {},
    onExplain: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        onRetry = viewModel::retry,
        onWhatToDo = onWhatToDo,
        onExplain = onExplain,
        modifier = modifier,
    )
}

@Composable
fun HomeContent(
    state: HomeUiState,
    onRetry: () -> Unit,
    onWhatToDo: () -> Unit,
    onExplain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        HomeUiState.Loading -> CenteredMessage(modifier) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.home_loading), style = MaterialTheme.typography.bodyMedium)
        }
        HomeUiState.Error -> CenteredMessage(modifier) {
            Text(stringResource(R.string.home_error_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_error_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text(stringResource(R.string.home_retry)) }
        }
        is HomeUiState.Content -> Dashboard(state, onRetry, onWhatToDo, onExplain, modifier)
    }
}

@Composable
private fun Dashboard(
    state: HomeUiState.Content,
    onRetry: () -> Unit,
    onWhatToDo: () -> Unit,
    onExplain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatTime = rememberTimeFormatter()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.isOutdated) {
            OutdatedBanner(savedAt = state.savedAt?.let(formatTime), onRetry = onRetry)
        }
        HeroCard(state.hero, untilText = state.hero.untilMillis?.let(formatTime), onWhatToDo = onWhatToDo)

        HazardCardFor(state.weather, stringResource(R.string.home_card_weather), stringResource(R.string.home_source_met), formatTime) {
            HazardCardContent(
                level = it.level,
                headline = it.title ?: stringResource(R.string.home_weather_no_warning),
                detail = it.detail,
            )
        }
        HazardCardFor(state.flood, stringResource(R.string.home_card_flood), stringResource(R.string.home_source_infobanjir), formatTime) {
            HazardCardContent(
                level = it.level,
                headline = it.waterLevelMetres
                    ?.let { metres -> stringResource(R.string.home_flood_value, String.format(Locale.getDefault(), "%.2f", metres)) }
                    ?: NO_VALUE,
                detail = it.stationName,
            )
        }
        HazardCardFor(state.air, stringResource(R.string.home_card_air), stringResource(R.string.home_source_doe), formatTime) {
            HazardCardContent(
                level = it.level,
                headline = it.api?.let { api -> stringResource(R.string.home_air_value, api) } ?: NO_VALUE,
                detail = it.category?.let { category -> stringResource(category.labelRes) },
                detailDotColor = it.level.color(),
            )
        }

        OutlinedButton(onClick = onExplain, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.home_explain))
        }
    }
}

@Composable
private fun HeroCard(hero: HeroUiModel, untilText: String?, onWhatToDo: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = hero.level.color(), contentColor = hero.level.onColor()),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.home_hero_label, stringResource(hero.level.labelRes).uppercase(Locale.getDefault())),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = hero.title ?: stringResource(R.string.home_hero_none),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = untilText?.let { stringResource(R.string.home_hero_area_until, it) }
                    ?: stringResource(R.string.home_hero_area),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (hero.kind != HeroUiModel.Kind.NONE) {
                Button(
                    onClick = onWhatToDo,
                    colors = ButtonDefaults.buttonColors(containerColor = hero.level.onColor(), contentColor = hero.level.color()),
                ) {
                    Text(stringResource(R.string.home_what_to_do))
                }
            }
        }
    }
}

/** Dashed banner shown while any card shows saved data (NFR1). */
@Composable
private fun OutdatedBanner(savedAt: String?, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .dashedBorder(MaterialTheme.colorScheme.outline, cornerRadius = 12.dp)
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.home_banner_title), style = MaterialTheme.typography.titleSmall)
            Text(
                text = savedAt?.let { stringResource(R.string.home_banner_body, it) }
                    ?: stringResource(R.string.home_banner_body_no_time),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onRetry) { Text(stringResource(R.string.home_retry)) }
    }
}

@Composable
private fun <T> HazardCardFor(
    state: CardState<T>,
    title: String,
    source: String,
    formatTime: (Long) -> String,
    toContent: @Composable (T) -> HazardCardContent,
) {
    when (state) {
        CardState.Loading -> HazardCard(title, source, content = null, timeText = null, isOutdated = false, isLoading = true)
        CardState.Unavailable -> HazardCard(title, source, content = null, timeText = null, isOutdated = false)
        is CardState.Ready -> HazardCard(
            title = title,
            source = source,
            content = toContent(state.data),
            timeText = state.lastUpdated?.let(formatTime),
            isOutdated = state.isOutdated,
        )
    }
}

@Composable
private fun CenteredMessage(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

/** Formats epoch millis as a time of day in the device's 12/24-hour style, e.g. "2:30 pm". */
@Composable
internal fun rememberTimeFormatter(): (Long) -> String {
    val context = LocalContext.current
    return remember(context) {
        val format = DateFormat.getTimeFormat(context)
        val formatter: (Long) -> String = { millis -> format.format(Date(millis)) }
        formatter
    }
}

private fun Modifier.dashedBorder(color: Color, cornerRadius: Dp): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        style = Stroke(
            width = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
        ),
        cornerRadius = CornerRadius(cornerRadius.toPx()),
    )
}

private val AirQualityCategory.labelRes: Int
    get() = when (this) {
        AirQualityCategory.GOOD -> R.string.home_air_good
        AirQualityCategory.MODERATE -> R.string.home_air_moderate
        AirQualityCategory.UNHEALTHY -> R.string.home_air_unhealthy
        AirQualityCategory.VERY_UNHEALTHY -> R.string.home_air_very_unhealthy
        AirQualityCategory.HAZARDOUS -> R.string.home_air_hazardous
    }

private const val NO_VALUE = "—"

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun HomeOutdatedPreview() {
    val now = System.currentTimeMillis()
    MaterialTheme {
        HomeContent(
            state = HomeUiState.Content(
                hero = HeroUiModel(Level.ADVISORY, HeroUiModel.Kind.WEATHER_WARNING, "Thunderstorms Warning", now + 3_600_000),
                weather = CardState.Ready(WeatherCard(Level.ADVISORY, "Thunderstorms Warning", "Thunderstorms for KL until 19:00"), now, false),
                flood = CardState.Ready(FloodCard(Level.ADVISORY, "Sg. Klang di Bukit Putra (F2)", 25.12), now - 5_400_000, true),
                air = CardState.Ready(AirCard(Level.NORMAL, 64, AirQualityCategory.MODERATE), now, false),
                updatedAt = now - 5_400_000,
                isOutdated = true,
                savedAt = now - 5_400_000,
            ),
            onRetry = {},
            onWhatToDo = {},
            onExplain = {},
        )
    }
}
