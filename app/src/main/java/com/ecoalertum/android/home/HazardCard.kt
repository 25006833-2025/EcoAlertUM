package com.ecoalertum.android.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ecoalertum.android.R
import com.ecoalertum.android.core.data.model.Level

/** What a [HazardCard] shows once it has data. */
data class HazardCardContent(
    val level: Level,
    val headline: String,
    val detail: String?,
    /** Colour dot before [detail], e.g. the air quality category. Text stays in the normal colour for contrast. */
    val detailDotColor: Color? = null,
)

/**
 * One hazard card: title, level badge, headline value, detail, and a source/time line.
 * Pass null [content] with [isLoading] or without it for the "no data" state.
 */
@Composable
fun HazardCard(
    title: String,
    source: String,
    content: HazardCardContent?,
    timeText: String?,
    isOutdated: Boolean,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (isOutdated) {
                    OutdatedBadge()
                    Spacer(Modifier.width(8.dp))
                }
                content?.let { LevelBadge(it.level) }
            }

            when {
                content != null -> {
                    Text(content.headline, style = MaterialTheme.typography.headlineSmall)
                    content.detail?.let {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            content.detailDotColor?.let { dot ->
                                Box(Modifier.size(10.dp).background(dot, CircleShape))
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                isLoading -> Text(stringResource(R.string.home_card_loading), style = MaterialTheme.typography.bodyMedium)
                else -> Text(stringResource(R.string.home_card_unavailable), style = MaterialTheme.typography.bodyMedium)
            }

            val sourceLine = when {
                timeText == null -> stringResource(R.string.home_card_source_only, source)
                isOutdated -> stringResource(R.string.home_card_saved, source, timeText)
                else -> stringResource(R.string.home_card_updated, source, timeText)
            }
            Text(
                text = sourceLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Neutral "OUTDATED" label for cards showing saved data. */
@Composable
fun OutdatedBadge(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.home_outdated_badge),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
