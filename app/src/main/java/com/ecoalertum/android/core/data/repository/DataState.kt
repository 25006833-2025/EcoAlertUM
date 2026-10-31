package com.ecoalertum.android.core.data.repository

/** What a screen gets from a repository. Times are epoch milliseconds. */
sealed interface DataState<out T> {

    /** Nothing to show yet, not even saved data. */
    data object Loading : DataState<Nothing>

    /**
     * Data to show. [isOutdated] is true when a source has not been refreshed recently
     * (the OUTDATED badge). The data is still shown; [lastUpdated] is the "saved 2:30 pm" time.
     */
    data class Success<out T>(
        val data: T,
        val isOutdated: Boolean,
        val lastUpdated: Long?,
    ) : DataState<T>

    /** The data could not be read. [cachedData] is the last saved copy, if there is one. */
    data class Error<out T>(
        val message: String,
        val cachedData: T? = null,
        val lastUpdated: Long? = null,
    ) : DataState<T>
}
