package com.ecoalertum.android.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ecoalertum.android.R
import com.ecoalertum.android.core.data.model.Level

/** The level word on its level colour, e.g. "Advisory" on amber. */
@Composable
fun LevelBadge(level: Level, modifier: Modifier = Modifier) {
    Surface(
        color = level.color(),
        contentColor = level.onColor(),
        shape = RoundedCornerShape(percent = 50),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(level.labelRes),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@get:StringRes
val Level.labelRes: Int
    get() = when (this) {
        Level.NORMAL -> R.string.home_level_normal
        Level.ADVISORY -> R.string.home_level_advisory
        Level.WARNING -> R.string.home_level_warning
        Level.DANGER -> R.string.home_level_danger
    }
