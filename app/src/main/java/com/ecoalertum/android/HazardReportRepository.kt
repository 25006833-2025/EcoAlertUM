package com.ecoalertum.android

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

data class HazardReport(
    val id: String = "",
    val reporterUid: String = "",
    val category: String = "",
    val aiCategory: String? = null,
    val aiConfidence: Double? = null,
    val description: String = "",
    val photoPath: String? = null,
    val location: GeoPoint? = null,
    val locationLabel: String = "",
    val status: String = "unverified",
    val confirmations: Int = 0,
    val verifiedBy: String? = null,
    val verifiedAt: Timestamp? = null,
    val createdAt: Timestamp? = null
)

class HazardReportRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val reportsCollection = db.collection("reports")

    suspend fun submitReport(report: HazardReport): Result<Unit> {
        return try {
            reportsCollection.add(report).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getReportsStream(
        onSuccess: (List<HazardReport>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        reportsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val reports = snapshot.toObjects(HazardReport::class.java)
                    onSuccess(reports)
                }
            }
    }

    suspend fun updateReportStatus(reportId: String, newStatus: String): Result<Unit> {
        return try {
            reportsCollection
                .document(reportId)
                .update("status", newStatus)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}