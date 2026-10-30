package com.ecoalertum.android.core.data.repository

import com.ecoalertum.android.core.data.model.Alert
import com.ecoalertum.android.core.data.model.HazardType
import kotlinx.coroutines.flow.Flow

/** Alerts raised by the backend rules. */
interface AlertRepository {

    /**
     * Alerts newest first (by `issuedAt`), for one hazard or all of them when [type] is null.
     * Includes expired and cancelled alerts; screens filter by `status`.
     */
    fun observeAlerts(type: HazardType? = null): Flow<DataState<List<Alert>>>
}
