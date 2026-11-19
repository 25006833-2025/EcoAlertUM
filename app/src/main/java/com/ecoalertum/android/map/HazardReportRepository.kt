package com.ecoalertum.android.map

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.tasks.await

data class HazardReport(
    @DocumentId val id: String = "",
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
    @ServerTimestamp val createdAt: Timestamp? = null
)

class HazardReportRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
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

    /** Newest 200 reports, live. Call remove() on the result when the screen closes. */
    fun getReportsStream(
        onSuccess: (List<HazardReport>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration =
        reportsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(200)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    onSuccess(snapshot.toObjects(HazardReport::class.java))
                }
            }

    /** Live list of one user's reports. Call remove() on the result when the screen closes. */
    fun listenToMyReports(
        uid: String,
        onSuccess: (List<HazardReport>) -> Unit,
        onError: (Exception) -> Unit,
    ): ListenerRegistration =
        reportsCollection
            .whereEqualTo("reporterUid", uid)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    onSuccess(
                        snapshot.toObjects(HazardReport::class.java)
                            .sortedByDescending { it.createdAt }
                    )
                }
            }

    /** Updates status to "verified", "resolved", or "rejected" with moderator audit fields. */
    suspend fun updateReportStatus(reportId: String, newStatus: String): Result<Unit> {
        if (reportId.isBlank()) {
            return Result.failure(IllegalArgumentException("Report ID cannot be blank"))
        }
        return try {
            val moderatorUid = auth.currentUser?.uid
            val updates = mutableMapOf<String, Any>(
                "status" to newStatus,
                "verifiedAt" to FieldValue.serverTimestamp()
            )
            if (moderatorUid != null) {
                updates["verifiedBy"] = moderatorUid
            }
            reportsCollection
                .document(reportId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}