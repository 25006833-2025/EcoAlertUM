package com.ecoalertum.android.core.data.firestore

import com.ecoalertum.android.core.data.freshness.FreshnessPolicy
import com.ecoalertum.android.core.data.freshness.HazardState
import com.ecoalertum.android.core.data.model.HazardReading
import com.ecoalertum.android.core.data.model.HazardType
import com.ecoalertum.android.core.data.repository.DataState
import com.ecoalertum.android.core.data.repository.HazardRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * Live readings from `hazardReadings`, with `sourceStatus` for freshness (docs/data-models.md).
 * Both collections are small (latest-only), so each is one listener. Freshness is re-checked every
 * [recheckMillis] because data becomes outdated with time even when nothing in Firestore changes.
 */
class FirestoreHazardRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val freshnessPolicy: FreshnessPolicy = FreshnessPolicy(),
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val recheckMillis: Long = 60_000L,
) : HazardRepository {

    override fun observeReadings(type: HazardType): Flow<DataState<List<HazardReading>>> = flow {
        emit(DataState.Loading)
        val readings = db.collection(READINGS).listen { FirestoreMappers.toReading(it.id, it.data.orEmpty()) }
        val statuses = db.collection(SOURCE_STATUS).listen { FirestoreMappers.toSourceStatus(it.id, it.data.orEmpty()) }
        emitAll(
            combine(readings, statuses, ticker()) { r, s, _ ->
                // Source status only refines freshness; if it can't be read, readings fall back to fetchedAt.
                val bySource = s.items.orEmpty().associateBy { it.source }
                HazardState.of(type, r.items.orEmpty(), bySource::get, freshnessPolicy, nowMillis(), r.error)
            },
        )
    }

    private fun ticker(): Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(recheckMillis)
        }
    }

    private companion object {
        const val READINGS = "hazardReadings"
        const val SOURCE_STATUS = "sourceStatus"
    }
}
