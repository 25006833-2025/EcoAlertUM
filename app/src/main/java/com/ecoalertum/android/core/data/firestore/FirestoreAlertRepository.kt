package com.ecoalertum.android.core.data.firestore

import com.ecoalertum.android.core.data.model.Alert
import com.ecoalertum.android.core.data.model.HazardType
import com.ecoalertum.android.core.data.repository.AlertRepository
import com.ecoalertum.android.core.data.repository.DataState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Live alerts from `alerts`, newest first. The hazard filter runs on the phone: one hazard maps to
 * several stored types (rain = rainfall + weather_warning), and this avoids a composite index.
 */
class FirestoreAlertRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val limit: Long = 100,
) : AlertRepository {

    override fun observeAlerts(type: HazardType?): Flow<DataState<List<Alert>>> = flow {
        emit(DataState.Loading)
        val alerts = db.collection("alerts")
            .orderBy("issuedAt", Query.Direction.DESCENDING)
            .limit(limit)
            .listen { FirestoreMappers.toAlert(it.id, it.data.orEmpty()) }
        emitAll(
            alerts.map { listened ->
                val matching = listened.items.orEmpty().filter { type == null || it.hazard == type }
                if (listened.error != null) {
                    DataState.Error(listened.error, matching.ifEmpty { null })
                } else {
                    DataState.Success(matching, isOutdated = false, lastUpdated = nowMillis())
                }
            },
        )
    }
}
