package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.FirestoreAuditReport
import com.example.data.model.FirestoreBackedUpApk
import com.example.data.model.FirestoreUserProfile
import com.example.model.AppAuditItem
import com.example.model.ThreatAuditReport
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirebaseRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    // CRITICAL: Always initialize with R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun requireUserId(): String {
        return auth.currentUser?.uid ?: "demo_operator_uid"
    }

    suspend fun syncUserProfile(user: FirebaseUser) {
        val uid = user.uid
        val docRef = db.collection("users").document(uid)
        try {
            val profile = FirestoreUserProfile(
                userId = uid,
                email = user.email ?: "",
                displayName = user.displayName ?: ""
            )
            docRef.set(profile).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        }
    }

    fun observeAuditReports(): Flow<List<FirestoreAuditReport>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/auditReports"
        emitAll(
            db.collection("users").document(uid).collection("auditReports")
                .snapshots()
                .map { snapshot -> snapshot.toObjects<FirestoreAuditReport>() }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveAuditReport(report: ThreatAuditReport) {
        val uid = requireUserId()
        val collectionRef = db.collection("users").document(uid).collection("auditReports")
        val docRef = collectionRef.document(report.reportId)
        val payload = mapOf(
            "userId" to uid,
            "reportId" to report.reportId,
            "scanDateEpochMs" to report.scanDateEpochMs,
            "totalAppsScanned" to report.totalAppsScanned,
            "safeCount" to report.safeCount,
            "lowRiskCount" to report.lowRiskCount,
            "mediumRiskCount" to report.mediumRiskCount,
            "criticalRiskCount" to report.criticalRiskCount,
            "overallThreatScore" to report.overallThreatScore,
            "keyFindings" to report.keyFindings,
            "isSyncedToCloud" to report.isSyncedToCloud,
            "cloudSyncUrl" to (report.cloudSyncUrl ?: ""),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    fun observeBackedUpApks(): Flow<List<FirestoreBackedUpApk>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/backedUpApks"
        emitAll(
            db.collection("users").document(uid).collection("backedUpApks")
                .snapshots()
                .map { snapshot -> snapshot.toObjects<FirestoreBackedUpApk>() }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveBackedUpApk(app: AppAuditItem, cloudUrl: String) {
        val uid = requireUserId()
        val docId = app.packageName.replace(".", "_")
        val docRef = db.collection("users").document(uid).collection("backedUpApks").document(docId)
        val payload = mapOf(
            "userId" to uid,
            "packageName" to app.packageName,
            "appName" to app.appName,
            "versionName" to app.versionName,
            "apkSizeBytes" to app.apkSizeBytes,
            "threatScore" to app.threatScore,
            "threatLevel" to app.threatLevel.name,
            "cloudStorageUrl" to cloudUrl,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }
}
