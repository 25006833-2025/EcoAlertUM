package com.ecoalertum.android.learn.lesson

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecoalertum.android.R
import com.ecoalertum.android.core.data.model.LocalizedText
import com.ecoalertum.android.learn.LearnTopic
import com.ecoalertum.android.ui.theme.EcoAlertUMTheme
import kotlinx.coroutines.launch

/**
 * Short lesson for one hazard topic (Module 5, FR5.2), laid out as in the FR5.2 wireframe.
 * Swipe or tap Next through the cards; the last card leads to the topic's post-quiz.
 */
@Composable
fun LessonScreen(
    onExit: () -> Unit,
    onStartPostQuiz: (quizId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LessonViewModel = viewModel(factory = LessonViewModel.Factory),
) {
    val lesson = viewModel.lesson
    if (lesson == null) {
        LessonNotFound(onExit, modifier)
        return
    }
    LessonContent(
        lesson = lesson,
        lang = viewModel.language,
        hasQuiz = viewModel.quizId != null,
        onReachedEnd = viewModel::markCompleted,
        onStartPostQuiz = { viewModel.quizId?.let(onStartPostQuiz) },
        onExit = onExit,
        modifier = modifier,
    )
}

@Composable
internal fun LessonContent(
    lesson: Lesson,
    lang: String,
    hasQuiz: Boolean,
    onReachedEnd: () -> Unit,
    onStartPostQuiz: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val topic = LearnTopic.from(lesson.topic)
    val count = lesson.cards.size
    val pagerState = rememberPagerState(pageCount = { count })
    val scope = rememberCoroutineScope()
    val page = pagerState.currentPage
    val isLast = page == count - 1

    // Reaching the last card counts as finishing the lesson.
    LaunchedEffect(isLast) { if (isLast) onReachedEnd() }

    Column(modifier.fillMaxSize()) {
        // Header: topic, card counter, minutes, segmented progress
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(50), color = topic.container, contentColor = topic.onContainer) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Icon(topic.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(stringResource(R.string.lesson_card_of, page + 1, count), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    stringResource(R.string.lesson_minutes, lesson.minutes),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(count) { i ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(6.dp)
                            .background(
                                if (i <= page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                CircleShape,
                            )
                    )
                }
            }
        }

        // Cards
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
            LessonCardContent(lesson.cards[index], topic, lang)
        }

        // Bottom buttons
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = { if (page == 0) onExit() else scope.launch { pagerState.animateScrollToPage(page - 1) } },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.height(52.dp),
            ) { Text(stringResource(if (page == 0) R.string.lesson_close else R.string.lesson_back), fontWeight = FontWeight.Bold) }

            Button(
                onClick = {
                    when {
                        !isLast -> scope.launch { pagerState.animateScrollToPage(page + 1) }
                        hasQuiz -> onStartPostQuiz()
                        else -> onExit()
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).height(52.dp),
            ) {
                Text(
                    stringResource(
                        when {
                            !isLast -> R.string.lesson_next
                            hasQuiz -> R.string.lesson_take_post_quiz
                            else -> R.string.lesson_done
                        }
                    ),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun LessonCardContent(card: LessonCard, topic: LearnTopic, lang: String) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Illustration: topic colour + large topic icon (swap for real artwork later)
        Box(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(topic.container, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(topic.icon, contentDescription = null, tint = topic.onContainer, modifier = Modifier.size(84.dp))
        }

        Text(card.title.get(lang), style = MaterialTheme.typography.headlineSmall)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            card.points.forEach { point ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier
                            .padding(top = 8.dp)
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.secondary, CircleShape)
                    )
                    Text(point.get(lang), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                }
            }
        }

        if (card.tip.get(lang).isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(stringResource(R.string.lesson_good_to_know), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                    }
                    Text(card.tip.get(lang), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun LessonNotFound(onExit: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.lesson_not_found), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onExit) { Text(stringResource(R.string.quiz_back_to_learn)) }
    }
}

@Preview(showBackground = true, heightDp = 820)
@Composable
private fun LessonPreview() {
    val card = LessonCard(
        title = LocalizedText("Never walk through flowing water"),
        points = listOf(
            LocalizedText("Moving water can knock you over, even when it looks shallow"),
            LocalizedText("Flood water can hide open drains and broken paths"),
            LocalizedText("Wait in a building or take another route"),
        ),
        tip = LocalizedText("Drain covers can be washed away in heavy rain."),
    )
    val lesson = Lesson("preview", "floods", LocalizedText("Heavy rain"), 3, List(4) { card })
    EcoAlertUMTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            LessonContent(lesson, "en", hasQuiz = true, onReachedEnd = {}, onStartPostQuiz = {}, onExit = {})
        }
    }
}
