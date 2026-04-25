package com.example.kampus.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object ActivityLogger {

    private const val TAG = "ActivityLogger"

    fun logAction(type: String, text: String, metadata: Map<String, Any> = emptyMap()) {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        val payload = mutableMapOf<String, Any>(
            "type" to type,
            "text" to text,
            "createdAt" to FieldValue.serverTimestamp(),
        )
        payload.putAll(metadata)

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .collection("activities")
            .add(payload)
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to log action: ${e.message}", e)
            }
    }
}
