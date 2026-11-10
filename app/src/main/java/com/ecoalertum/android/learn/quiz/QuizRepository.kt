package com.ecoalertum.android.learn.quiz

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/** Quiz content. Bundled in the app for v1; can move to Firestore later without changing screens. */
interface QuizRepository {
    fun getQuizzes(): List<Quiz>
    fun getQuiz(id: String): Quiz? = getQuizzes().firstOrNull { it.id == id }
}

/** Reads the quizzes once from the bundled JSON. */
class BundledQuizRepository(private val readJson: () -> String) : QuizRepository {
    private val loaded: List<Quiz> by lazy { QuizContentParser.parse(readJson()) }
    override fun getQuizzes(): List<Quiz> = loaded
}

/** Where finished attempts are stored (pre/post scores are the evidence for SDG 13.3). */
interface QuizAttemptRepository {
    suspend fun save(attempt: QuizAttempt): Result<Unit>
}

/**
 * Saves one document per attempt to `quizAttempts`, the collection firestore.rules allows.
 * Fails with [NotSignedInException] until the student is logged in (FR6.1).
 */
class FirestoreQuizAttemptRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) : QuizAttemptRepository {

    override suspend fun save(attempt: QuizAttempt): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(NotSignedInException())
        return try {
            // Firestore keeps the write and syncs it later if offline; the timeout only stops
            // the screen showing "Saving…" forever.
            withTimeout(SAVE_TIMEOUT_MS) {
                db.collection(COLLECTION).add(
                    mapOf(
                        "uid" to uid,
                        "quizId" to attempt.quizId,
                        "phase" to attempt.phase.value,
                        "score" to attempt.score,
                        "maxScore" to attempt.maxScore,
                        "language" to attempt.language,
                        "durationSeconds" to attempt.durationSeconds,
                        "completedAt" to Timestamp.now(),
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val COLLECTION = "quizAttempts"
        private const val SAVE_TIMEOUT_MS = 10_000L
    }
}

class NotSignedInException : Exception("Not signed in")
