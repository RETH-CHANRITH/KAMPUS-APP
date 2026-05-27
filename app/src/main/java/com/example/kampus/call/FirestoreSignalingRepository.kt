package com.example.kampus.call

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import android.util.Log

/**
 * Encapsulates Firestore operations for call signalling: offer/answer, candidates, status.
 */
class FirestoreSignalingRepository(private val firestore: FirebaseFirestore) {
    private val TAG = "SignalingRepo"

    fun getCallDoc(chatId: String, callId: String): DocumentReference {
        return firestore.collection("chats").document(chatId).collection("calls").document(callId)
    }

    suspend fun writeOffer(callRef: DocumentReference, offer: String, offerType: String) {
        try {
            callRef.update(
                mapOf(
                    "offerSdp" to offer,
                    "offerType" to offerType,
                    "status" to "RINGING",
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                ),
            ).await()
            Log.d(TAG, "Offer written to ${callRef.id}")
        } catch (t: Throwable) {
            Log.w(TAG, "writeOffer failed: ${t.message}")
        }
    }

    suspend fun writeAnswer(callRef: DocumentReference, answer: String) {
        try {
            callRef.update(
                mapOf(
                    "answerSdp" to answer,
                    "status" to "ACCEPTED",
                    "acceptedAt" to System.currentTimeMillis(),
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                ),
            ).await()
            Log.d(TAG, "Answer written to ${callRef.id}")
        } catch (t: Throwable) {
            Log.w(TAG, "writeAnswer failed: ${t.message}")
        }
    }

    suspend fun addCandidate(callRef: DocumentReference, collectionName: String, candidateMap: Map<String, Any>) {
        try {
            callRef.collection(collectionName).add(candidateMap).await()
            Log.d(TAG, "Candidate added to ${callRef.id}/$collectionName")
        } catch (t: Throwable) {
            Log.w(TAG, "addCandidate failed: ${t.message}")
        }
    }

    fun listenCallDoc(callRef: DocumentReference, listener: (snapshot: com.google.firebase.firestore.DocumentSnapshot?, error: com.google.firebase.firestore.FirebaseFirestoreException?) -> Unit): ListenerRegistration {
        return callRef.addSnapshotListener { snapshot, error -> listener(snapshot, error) }
    }

    fun listenCandidates(callRef: DocumentReference, collectionName: String, listener: (changes: com.google.firebase.firestore.QuerySnapshot?, error: com.google.firebase.firestore.FirebaseFirestoreException?) -> Unit): ListenerRegistration {
        return callRef.collection(collectionName).addSnapshotListener { snapshot, error -> listener(snapshot, error) }
    }

    suspend fun updateStatus(callRef: DocumentReference, status: String) {
        try {
            callRef.update(mapOf("status" to status, "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp())).await()
        } catch (_: Throwable) {
        }
    }

    suspend fun cleanupCall(callRef: DocumentReference) {
        try {
            val caller = callRef.collection("callerCandidates").get().await()
            caller.documents.forEach { it.reference.delete() }
        } catch (_: Throwable) {}

        try {
            val callee = callRef.collection("calleeCandidates").get().await()
            callee.documents.forEach { it.reference.delete() }
        } catch (_: Throwable) {}

        try {
            callRef.delete().await()
        } catch (_: Throwable) {}
    }
}
