package com.ecoalertum.android.learn.lesson

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Lesson content. Bundled in the app for v1; can move to Firestore later without changing screens. */
interface LessonRepository {
    fun getLessons(): List<Lesson>
    fun forTopic(topic: String): Lesson? = getLessons().firstOrNull { it.topic == topic }
}

/** Reads the lessons once from the bundled JSON. */
class BundledLessonRepository(private val readJson: () -> String) : LessonRepository {
    private val loaded: List<Lesson> by lazy { LessonContentParser.parse(readJson()) }
    override fun getLessons(): List<Lesson> = loaded
}

/** Which lessons this student has finished, kept on the phone. */
interface LessonProgressStore {
    /** IDs of finished lessons. Updates as soon as a lesson is marked done. */
    val completed: StateFlow<Set<String>>
    fun markCompleted(lessonId: String)
}

class SharedPrefsLessonProgressStore(context: Context) : LessonProgressStore {
    private val prefs = context.applicationContext.getSharedPreferences("lesson_progress", Context.MODE_PRIVATE)
    private val _completed = MutableStateFlow(prefs.getStringSet(KEY, emptySet()).orEmpty().toSet())
    override val completed: StateFlow<Set<String>> = _completed.asStateFlow()

    override fun markCompleted(lessonId: String) {
        if (lessonId in _completed.value) return
        val updated = _completed.value + lessonId
        prefs.edit().putStringSet(KEY, updated).apply()
        _completed.value = updated
    }

    private companion object {
        const val KEY = "completed"
    }
}
