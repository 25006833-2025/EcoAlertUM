package com.ecoalertum.android.learn

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import com.ecoalertum.android.R

/**
 * How each hazard topic looks on the Learn screens: tile colours from the design system,
 * an outline icon, and the featured lesson shown in the "Right now" card.
 * The quiz's `topic` field in quizzes.json picks the entry.
 */
enum class LearnTopic(
    val id: String,
    val container: Color,
    val onContainer: Color,
    val icon: ImageVector,
    @param:StringRes val lessonTitle: Int,
) {
    FLOODS("floods", Color(0xFFE3F0EC), Color(0xFF0F4C47), lineIcon("floods",
        "M3 15c3 0 3-2 6-2s3 2 6 2 3-2 6-2M3 20c3 0 3-2 6-2s3 2 6 2 3-2 6-2M12 3v7M9 7l3 3 3-3"),
        R.string.learn_lesson_floods),
    HAZE("haze", Color(0xFFE7EAEE), Color(0xFF3F4A55), lineIcon("haze",
        "M4 9h12a3 3 0 1 0-3-3M4 14h15a3 3 0 1 1-3 3M4 19h7"),
        R.string.learn_lesson_haze),
    STORMS("storms", Color(0xFFFDEBD3), Color(0xFF7A4A0B), lineIcon("storms",
        "M13 2L5 14h6l-1 8 8-12h-6z"),
        R.string.learn_lesson_storms),
    HEAT("heat", Color(0xFFFBE4DF), Color(0xFF9F1D14), lineIcon("heat",
        "M12 3v10M9 16a3 3 0 1 0 6 0a3 3 0 0 0-1.5-2.6V5a1.5 1.5 0 0 0-3 0v8.4A3 3 0 0 0 9 16z"),
        R.string.learn_lesson_heat);

    companion object {
        fun from(id: String): LearnTopic = entries.firstOrNull { it.id == id } ?: FLOODS
    }
}

/** A 24 dp outline icon drawn from an SVG path, in the same style as the wireframe icons. */
private fun lineIcon(name: String, path: String): ImageVector =
    ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .addPath(
            pathData = PathParser().parsePathString(path).toNodes(),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        .build()
