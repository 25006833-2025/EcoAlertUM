package com.ecoalertum.android.learn.quiz

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecoalertum.android.R
import com.ecoalertum.android.core.data.model.LocalizedText
import com.ecoalertum.android.learn.LearnTopic
import com.ecoalertum.android.ui.theme.EcoAlertUMTheme

/** Quiz question and result screens (Module 5, FR5.1). */
@Composable
fun QuizScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuizViewModel = viewModel(factory = QuizViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    when (val s = state) {
        QuizUiState.NotFound -> QuizNotFound(onExit, modifier)
        is QuizUiState.InProgress -> QuizQuestionContent(
            state = s,
            onSelect = viewModel::select,
            onCheck = viewModel::check,
            onNext = viewModel::next,
            onExit = onExit,
            modifier = modifier,
        )
        is QuizUiState.Finished -> {
            val shareText = stringResource(
                R.string.quiz_share_text,
                s.session.correctCount,
                s.session.totalQuestions,
                s.session.quiz.title.get(s.language),
            )
            val shareTitle = stringResource(R.string.quiz_share)
            QuizResultContent(
                state = s,
                onShare = {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText)
                    context.startActivity(Intent.createChooser(send, shareTitle))
                },
                onRetry = viewModel::restart,
                onRetrySave = viewModel::save,
                onExit = onExit,
                modifier = modifier,
            )
        }
    }
}

// --------------------------------------------------------------------------------------------
// Question
// --------------------------------------------------------------------------------------------

@Composable
internal fun QuizQuestionContent(
    state: QuizUiState.InProgress,
    onSelect: (Int) -> Unit,
    onCheck: () -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val session = state.session
    val question = session.currentQuestion
    val lang = state.language

    Column(modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Phase chip, question counter, quit
            Row(verticalAlignment = Alignment.CenterVertically) {
                PhaseChip(state.phase)
                Spacer(Modifier.weight(1f))
                Text(
                    stringResource(R.string.quiz_question_of, session.currentIndex + 1, session.totalQuestions),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IconButton(onClick = onExit) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.quiz_close))
                }
            }

            LinearProgressIndicator(
                progress = {
                    (session.currentIndex + if (session.isChecked) 1 else 0).toFloat() / session.totalQuestions
                },
                modifier = Modifier.fillMaxWidth(),
            )

            // Topic + right/wrong counter
            Row(verticalAlignment = Alignment.CenterVertically) {
                TopicChip(session.quiz, lang)
                Spacer(Modifier.weight(1f))
                ScoreCounter(session.correctCount, session.wrongCount)
            }

            Text(
                question.prompt.get(lang),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                question.options.forEachIndexed { index, option ->
                    AnswerOption(
                        letter = letterFor(index),
                        text = option.get(lang),
                        look = optionLook(session, index),
                        selected = session.selectedIndex == index,
                        enabled = !session.isChecked,
                        onClick = { onSelect(index) },
                    )
                }
            }

            if (session.isChecked) {
                val correct = session.isCurrentCorrect
                val answerText = "${letterFor(question.correctIndex)}. ${question.options[question.correctIndex].get(lang)}"
                Feedback(
                    title = if (correct) stringResource(R.string.quiz_feedback_correct)
                    else stringResource(R.string.quiz_feedback_wrong, answerText),
                    body = question.explanation.get(lang),
                    correct = correct,
                )
            }
        }

        // Bottom action
        Box(Modifier.padding(16.dp)) {
            if (!session.isChecked) {
                Button(
                    onClick = onCheck,
                    enabled = session.selectedIndex != null,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text(stringResource(R.string.quiz_check), fontWeight = FontWeight.Bold) }
            } else {
                Button(onClick = onNext, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(
                        stringResource(if (session.isLastQuestion) R.string.quiz_see_results else R.string.quiz_next)
                    )
                }
            }
        }
    }
}

@Composable
private fun TopicChip(quiz: Quiz, lang: String) {
    val topic = LearnTopic.from(quiz.topic)
    Surface(shape = RoundedCornerShape(50), color = topic.container, contentColor = topic.onContainer) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Icon(topic.icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(quiz.title.get(lang), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PhaseChip(phase: QuizPhase) {
    val label = when (phase) {
        QuizPhase.PRE -> R.string.quiz_phase_pre
        QuizPhase.POST -> R.string.quiz_phase_post
        QuizPhase.PRACTICE -> R.string.quiz_phase_practice
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Text(
            stringResource(label),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/** ✓ n  ✕ n pills: how many answers so far were right and wrong. */
@Composable
private fun ScoreCounter(correct: Int, wrong: Int) {
    val description = stringResource(R.string.quiz_correct_count, correct) + ", " +
        stringResource(R.string.quiz_wrong_count, wrong)
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        CounterPill(Icons.Filled.Check, correct, QuizCorrectContainer, QuizOnCorrectContainer)
        CounterPill(Icons.Filled.Close, wrong, QuizWrongContainer, QuizOnWrongContainer)
    }
}

@Composable
private fun CounterPill(icon: androidx.compose.ui.graphics.vector.ImageVector, count: Int, bg: Color, fg: Color) {
    Surface(shape = RoundedCornerShape(50), color = bg, contentColor = fg) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(count.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

private enum class OptionLook { NORMAL, SELECTED, CORRECT, WRONG, DIMMED }

private fun optionLook(session: QuizSession, index: Int): OptionLook {
    val q = session.currentQuestion
    return when {
        !session.isChecked -> if (session.selectedIndex == index) OptionLook.SELECTED else OptionLook.NORMAL
        index == q.correctIndex -> OptionLook.CORRECT
        index == session.selectedIndex -> OptionLook.WRONG
        else -> OptionLook.DIMMED
    }
}

@Composable
private fun AnswerOption(
    letter: String,
    text: String,
    look: OptionLook,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val (border, container, badgeBg, badgeFg) = when (look) {
        OptionLook.NORMAL -> listOf(scheme.outlineVariant, scheme.surfaceContainerLowest, scheme.surfaceVariant, scheme.onSurface)
        OptionLook.SELECTED -> listOf(scheme.primary, scheme.primaryContainer, scheme.primary, scheme.onPrimary)
        OptionLook.CORRECT -> listOf(QuizCorrect, QuizCorrectContainer, QuizCorrect, Color.White)
        OptionLook.WRONG -> listOf(QuizWrong, QuizWrongContainer, QuizWrong, Color.White)
        OptionLook.DIMMED -> listOf(scheme.outlineVariant, scheme.surfaceContainerLowest, scheme.surfaceVariant, scheme.onSurfaceVariant)
    }
    val strong = look != OptionLook.NORMAL && look != OptionLook.DIMMED

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = container,
        border = BorderStroke(if (strong) 2.dp else 1.dp, border),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Surface(shape = CircleShape, color = badgeBg, contentColor = badgeFg, modifier = Modifier.size(30.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    when (look) {
                        OptionLook.CORRECT -> Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        OptionLook.WRONG -> Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        else -> Text(letter, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun Feedback(title: String, body: String, correct: Boolean) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (correct) QuizCorrectContainer else QuizWrongContainer,
        contentColor = if (correct) QuizOnCorrectContainer else QuizOnWrongContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// --------------------------------------------------------------------------------------------
// Result
// --------------------------------------------------------------------------------------------

@Composable
internal fun QuizResultContent(
    state: QuizUiState.Finished,
    onShare: () -> Unit,
    onRetry: () -> Unit,
    onRetrySave: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val session = state.session
    val lang = state.language
    val score = session.correctCount
    val total = session.totalQuestions
    val ratio = if (total == 0) 0f else score.toFloat() / total

    Column(modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Phase + topic, share on the right
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                PhaseChip(state.phase)
                Spacer(Modifier.width(8.dp))
                TopicChip(session.quiz, lang)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onShare) {
                    Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.quiz_share), tint = MaterialTheme.colorScheme.primary)
                }
            }

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
                CircularProgressIndicator(
                    progress = { ratio },
                    strokeWidth = 12.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxSize(),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.quiz_result_score, score, total),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(stringResource(R.string.quiz_result_correct), style = MaterialTheme.typography.labelMedium)
                }
            }

            Text(
                stringResource(
                    when {
                        ratio >= 0.8f -> R.string.quiz_result_great
                        ratio >= 0.5f -> R.string.quiz_result_good
                        else -> R.string.quiz_result_keep_going
                    }
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            BeforeAfterCard(state.preScore, state.postScore, total, state.phase)

            SaveStatusRow(state.saveStatus, onRetrySave)

            // Review
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.quiz_result_review), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    session.quiz.questions.forEachIndexed { i, q ->
                        val picked = session.answers.getOrNull(i)
                        ReviewRow(q, picked, lang)
                    }
                }
            }
        }

        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onRetry,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.height(52.dp),
            ) {
                Text(stringResource(R.string.quiz_retry))
            }
            Button(onClick = onExit, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).height(52.dp)) {
                Text(stringResource(R.string.quiz_back_to_learn))
            }
        }
    }
}

/** "Before vs after the lessons": latest pre-quiz and post-quiz scores side by side (FR5.3). */
@Composable
private fun BeforeAfterCard(pre: Int?, post: Int?, total: Int, phase: QuizPhase) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.quiz_before_after), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            ScoreBar(stringResource(R.string.quiz_pre_label), pre, total, MaterialTheme.colorScheme.outline)
            ScoreBar(stringResource(R.string.quiz_post_label), post, total, QuizCorrect)
            val note = when {
                pre != null && post != null && post > pre -> stringResource(R.string.quiz_gain_up, post - pre)
                pre != null && post != null && post == pre -> stringResource(R.string.quiz_gain_same)
                pre != null && post != null -> stringResource(R.string.quiz_gain_down)
                pre == null -> stringResource(R.string.quiz_need_pre)
                else -> stringResource(R.string.quiz_need_post)
            }
            val good = pre != null && post != null && post > pre
            Text(
                note,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (good) FontWeight.Bold else FontWeight.Medium,
                color = if (good) QuizCorrect else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (phase == QuizPhase.PRACTICE) {
                Text(stringResource(R.string.quiz_practice_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ScoreBar(label: String, score: Int?, total: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(72.dp))
        LinearProgressIndicator(
            progress = { if (score == null || total == 0) 0f else score.toFloat() / total },
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.weight(1f).height(10.dp),
        )
        Text(
            if (score == null) "–" else stringResource(R.string.quiz_result_score, score, total),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(36.dp),
        )
    }
}

@Composable
private fun SaveStatusRow(status: SaveStatus, onRetrySave: () -> Unit) {
    val text = when (status) {
        SaveStatus.IDLE, SaveStatus.SAVING -> stringResource(R.string.quiz_saving)
        SaveStatus.SAVED -> stringResource(R.string.quiz_saved)
        SaveStatus.NOT_SIGNED_IN -> stringResource(R.string.quiz_not_signed_in)
        SaveStatus.FAILED -> stringResource(R.string.quiz_save_failed)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (status == SaveStatus.FAILED) {
            TextButton(onClick = onRetrySave) { Text(stringResource(R.string.quiz_save_retry)) }
        }
    }
}

@Composable
private fun ReviewRow(question: QuizQuestion, picked: Int?, lang: String) {
    val right = picked == question.correctIndex
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            shape = CircleShape,
            color = if (right) QuizCorrect else QuizWrong,
            contentColor = Color.White,
            modifier = Modifier.size(24.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(if (right) Icons.Filled.Check else Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(question.prompt.get(lang), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            if (!right) {
                picked?.let {
                    Text(
                        stringResource(R.string.quiz_result_your_answer, question.options[it].get(lang)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    stringResource(R.string.quiz_result_right_answer, question.options[question.correctIndex].get(lang)),
                    style = MaterialTheme.typography.bodySmall,
                    color = QuizCorrect,
                )
            }
        }
    }
}

@Composable
private fun QuizNotFound(onExit: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.quiz_not_found), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onExit) { Text(stringResource(R.string.quiz_back_to_learn)) }
    }
}

private fun letterFor(index: Int): String = ('A' + index).toString()

// --------------------------------------------------------------------------------------------
// Previews
// --------------------------------------------------------------------------------------------

private val previewQuiz = Quiz(
    id = "preview",
    topic = "floods",
    title = LocalizedText("Floods on campus"),
    questions = List(5) {
        QuizQuestion(
            id = "q$it",
            prompt = LocalizedText("A walkway on your route is covered by fast-moving water. What should you do?"),
            options = listOf(
                LocalizedText("Walk through quickly while it is still shallow"),
                LocalizedText("Wait indoors or take a higher, covered route"),
                LocalizedText("Hold an umbrella and cross carefully"),
                LocalizedText("Follow other students who are crossing"),
            ),
            correctIndex = 1,
            explanation = LocalizedText("Moving water can knock you over even when it looks shallow."),
        )
    },
)

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun QuizQuestionPreview() {
    val session = QuizSession(previewQuiz).select(1).check().next().select(0).check().next().select(1)
    EcoAlertUMTheme(dynamicColor = false) {
        QuizQuestionContent(QuizUiState.InProgress(session, QuizPhase.PRE, "en"), {}, {}, {}, {})
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun QuizCheckedPreview() {
    val session = QuizSession(previewQuiz).select(0).check()
    EcoAlertUMTheme(dynamicColor = false) {
        QuizQuestionContent(QuizUiState.InProgress(session, QuizPhase.PRE, "en"), {}, {}, {}, {})
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun QuizResultPreview() {
    var session = QuizSession(previewQuiz)
    repeat(5) { i -> session = session.select(if (i == 2) 0 else 1).check().next() }
    EcoAlertUMTheme(dynamicColor = false) {
        QuizResultContent(QuizUiState.Finished(session, QuizPhase.POST, "en", SaveStatus.SAVED, preScore = 2, postScore = 4), {}, {}, {}, {})
    }
}
