package com.ecoalertum.android.map

import androidx.annotation.StringRes
import com.ecoalertum.android.R

/** What a student can report. [key] is what we save in Firestore's `category` field. */
enum class ReportCategory(
    val key: String,
    @param:StringRes val labelRes: Int,
) {
    FLOODED_WALKWAY("flooded_walkway", R.string.reports_category_flooded_walkway),
    FALLEN_TREE("fallen_tree", R.string.reports_category_fallen_tree),
    BLOCKED_DRAIN("blocked_drain", R.string.reports_category_blocked_drain),
}