package com.ecoalertum.android.map

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

/** Current position, or null if the phone has no fix. Call only after location permission is granted. */
@SuppressLint("MissingPermission")
suspend fun readCurrentLocation(context: Context): Location? =
    try {
        LocationServices.getFusedLocationProviderClient(context)
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .await()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

/** Best-effort street or area name for a position. Returns "" if none is found (for example offline). */
@Suppress("DEPRECATION")
suspend fun lookUpPlaceName(context: Context, latitude: Double, longitude: Double): String =
    withContext(Dispatchers.IO) {
        try {
            if (!Geocoder.isPresent()) return@withContext ""
            val address = Geocoder(context, Locale.getDefault())
                .getFromLocation(latitude, longitude, 1)
                ?.firstOrNull()
            address?.thoroughfare
                ?: address?.subLocality
                ?: address?.getAddressLine(0)
                ?: ""
        } catch (e: Exception) {
            ""
        }
    }