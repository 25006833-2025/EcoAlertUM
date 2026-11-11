package com.ecoalertum.android.core.data.firestore

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Latest documents of a live query. After an error, [items] is the last good list (or null) and [error] is set. */
internal data class Listened<T>(val items: List<T>?, val error: String?)

/**
 * Listens to [this] query while the flow is collected. Firestore serves its offline cache first,
 * so saved data arrives quickly even without a connection (NFR1). A listener stops after an error;
 * collecting again (e.g. Retry on Home) starts a new one.
 */
internal fun <T> Query.listen(map: (DocumentSnapshot) -> T?): Flow<Listened<T>> = callbackFlow {
    var last: List<T>? = null
    val registration = addSnapshotListener { snapshot, error ->
        when {
            error != null -> trySend(Listened(last, error.message ?: "Firestore error"))
            snapshot != null -> {
                val items = snapshot.documents.mapNotNull(map)
                last = items
                trySend(Listened(items, null))
            }
        }
    }
    awaitClose { registration.remove() }
}
