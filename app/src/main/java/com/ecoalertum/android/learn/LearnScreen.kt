package com.ecoalertum.android.learn

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecoalertum.android.R
import com.ecoalertum.android.core.data.model.LocalizedText
import com.ecoalertum.android.learn.quiz.Quiz
import com.ecoalertum.android.learn.quiz.QuizPhase
import com.ecoalertum.android.learn.quiz.QuizQuestion
import com.ecoalertum.android.ui.theme.AlertAmber
import com.ecoalertum.android.ui.theme.EcoAlertUMTheme
import com.ecoalertum.android.ui.theme.Ink
import com.ecoalertum.android.ui.theme.SeverityWarning
import kotlinx.coroutines.launch

/**
 * Learn tab homepage (Module 5), laid out as in the FR5 wireframe.
 * Working now: the "Right now" lesson cards (swipe), pre-quiz shortcut, My progress link,
 * and the topic tiles, which open that topic's quizzes (FR5.1).
 * Lessons (FR5.2) and badge counts (FR5.3) come in weeks 7 and 8.
 */
@Composable
fun LearnScreen(
    onStartQuiz: (quizId: String, phase: QuizPhase) -> Unit,
    onOpenProgress: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LearnViewModel = viewModel(factory = LearnViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val soonText = stringResource(R.string.learn_lessons_soon)
    LearnContent(
        state = state,
        onStartQuiz = onStartQuiz,
        onOpenProgress = onOpenProgress,
        onStartLesson = { Toast.makeText(context, soonText, Toast.LENGTH_SHORT).show() },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LearnContent(
    state: LearnUiState,
    onStartQuiz: (String, QuizPhase) -> Unit,
    onOpenProgress: () -> Unit,
    onStartLesson: (Quiz) -> Unit,
    modifier: Modifier = Modifier,
) {
    val quizzes = state.quizzes
    val lang = state.language
    var sheetQuizId by rememberSaveable { mutableStateOf<String?>(null) }
    val pagerState = rememberPagerState(pageCount = { quizzes.size })

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Header: subtitle + badges chip
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.learn_subtitle),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Surface(
                onClick = onOpenProgress,
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = AlertAmber, modifier = Modifier.size(18.dp))
                    // TODO(FR5.3): real badge count from the user's profile.
                    Text(stringResource(R.string.learn_badges, 0), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (quizzes.isEmpty()) {
            Text(stringResource(R.string.learn_no_quizzes), style = MaterialTheme.typography.bodyLarge)
            return@Column
        }

        // "Right now" lesson cards: swipe left for other lessons
        HorizontalPager(state = pagerState, pageSpacing = 12.dp) { page ->
            val quiz = quizzes[page]
            LessonHeroCard(
                quiz = quiz,
                lang = lang,
                rightNowLabel = state.activeLabel?.takeIf { quiz.topic == state.activeTopic },
                onStartLesson = { onStartLesson(quiz) },
            )
        }
        PagerDots(count = quizzes.size, current = pagerState.currentPage)

        // Pre-quiz shortcut for the lesson on screen
        val current = quizzes[pagerState.currentPage.coerceIn(quizzes.indices)]
        ShortcutRow(
            title = stringResource(R.string.learn_pre_quiz_title),
            body = stringResource(R.string.learn_pre_quiz_body),
            action = stringResource(R.string.learn_start),
            iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
            iconBg = MaterialTheme.colorScheme.tertiaryContainer,
            onClick = { onStartQuiz(current.id, QuizPhase.PRE) },
        )

        // My progress
        ShortcutRow(
            title = stringResource(R.string.learn_my_progress),
            body = null,
            action = stringResource(R.string.learn_view),
            iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
            iconBg = MaterialTheme.colorScheme.secondaryContainer,
            onClick = onOpenProgress,
        )

        // Topics grid
        Text(stringResource(R.string.learn_topics), style = MaterialTheme.typography.titleLarge)
        quizzes.sortedBy { LearnTopic.from(it.topic).ordinal }.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { quiz ->
                    TopicTile(quiz, lang, onClick = { sheetQuizId = quiz.id }, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(4.dp))
    }

    // Topic sheet: choose pre / post / practice quiz
    val sheetQuiz = quizzes.firstOrNull { it.id == sheetQuizId }
    if (sheetQuiz != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = rememberCoroutineScope()
        val start: (QuizPhase) -> Unit = { phase ->
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                sheetQuizId = null
                onStartQuiz(sheetQuiz.id, phase)
            }
        }
        ModalBottomSheet(
            onDismissRequest = { sheetQuizId = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            TopicSheet(sheetQuiz, lang, onStart = start)
        }
    }
}

@Composable
private fun LessonHeroCard(quiz: Quiz, lang: String, rightNowLabel: String?, onStartLesson: () -> Unit) {
    val topic = LearnTopic.from(quiz.topic)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box {
            // Large faded topic icon as decoration
            Icon(
                topic.icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.10f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(110.dp),
            )
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (rightNowLabel != null) {
                    Surface(shape = RoundedCornerShape(50), color = SeverityWarning, contentColor = Color.White) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Box(Modifier.size(7.dp).background(Color.White, CircleShape))
                            Text(
                                stringResource(R.string.learn_right_now, rightNowLabel.uppercase()),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }
                } else {
                    Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.16f)) {
                        Text(
                            quiz.title.get(lang).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
                Text(
                    stringResource(topic.lessonTitle),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(end = 40.dp),
                )
                Text(
                    stringResource(R.string.learn_lesson_meta, quiz.questions.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
                Button(
                    onClick = onStartLesson,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AlertAmber, contentColor = Ink),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.learn_start_lesson), fontWeight = FontWeight.ExtraBold) }
            }
        }
    }
}

@Composable
private fun PagerDots(count: Int, current: Int) {
    val description = stringResource(R.string.learn_pager_position, current + 1, count)
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
    ) {
        repeat(count) { i ->
            val selected = i == current
            Box(
                Modifier
                    .height(8.dp)
                    .width(if (selected) 22.dp else 8.dp)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape,
                    )
            )
        }
    }
}

@Composable
private fun ShortcutRow(
    title: String,
    body: String?,
    action: String,
    iconTint: Color,
    iconBg: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Box(Modifier.size(40.dp).background(iconBg, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(
                    if (body != null) Icons.Filled.CheckCircle else Icons.Filled.Star,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (body != null) {
                    Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(action, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun TopicTile(quiz: Quiz, lang: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val topic = LearnTopic.from(quiz.topic)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = topic.container,
        contentColor = topic.onContainer,
        modifier = modifier.heightIn(min = 120.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(40.dp).background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(topic.icon, contentDescription = null, modifier = Modifier.size(22.dp)) }
            Text(quiz.title.get(lang), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
            Text(stringResource(R.string.learn_question_count, quiz.questions.size), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TopicSheet(quiz: Quiz, lang: String, onStart: (QuizPhase) -> Unit) {
    val topic = LearnTopic.from(quiz.topic)
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(48.dp).background(topic.container, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(topic.icon, contentDescription = null, tint = topic.onContainer, modifier = Modifier.size(26.dp))
            }
            Column {
                Text(quiz.title.get(lang), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.learn_question_count, quiz.questions.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(stringResource(R.string.learn_topic_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(
            onClick = { onStart(QuizPhase.PRE) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text(stringResource(R.string.learn_take_pre_quiz), fontWeight = FontWeight.Bold) }
        OutlinedButton(
            onClick = { onStart(QuizPhase.POST) },
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text(stringResource(R.string.learn_take_post_quiz), fontWeight = FontWeight.Bold) }
        TextButton(onClick = { onStart(QuizPhase.PRACTICE) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.learn_practice))
        }
    }
}

// --------------------------------------------------------------------------------------------
// Preview
// --------------------------------------------------------------------------------------------

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun LearnPreview() {
    val q = QuizQuestion("q", LocalizedText("?"), listOf(LocalizedText("a"), LocalizedText("b")), 0, LocalizedText(""))
    fun quiz(id: String, topic: String, title: String) = Quiz(id, topic, LocalizedText(title), List(5) { q })
    val state = LearnUiState(
        quizzes = listOf(
            quiz("f", "floods", "Floods on campus"),
            quiz("h", "haze", "Haze and air quality"),
            quiz("s", "storms", "Storms and fallen trees"),
            quiz("t", "heat", "Hot weather and heat"),
        ),
        activeTopic = "floods",
        activeLabel = "Heavy rain",
    )
    EcoAlertUMTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            LearnContent(state, { _, _ -> }, {}, {})
        }
    }
}
