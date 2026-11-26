package com.example.modulos3y4.data.remote

import com.example.modulos3y4.data.IssPositionRecord
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Escribe en Firestore los registros del historial.
 *
 * Se usa set() sobre un documento con identificador propio y no add(): si el
 * Worker sube un registro y muere antes de marcarlo como sincronizado, el
 * reintento vuelve a escribir el mismo documento en lugar de crear un duplicado.
 */
class IssFirestoreSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun upload(record: IssPositionRecord) {
        firestore.collection(COLLECTION)
            .document(record.id.toString())
            .set(
                mapOf(
                    "latitude" to record.latitude,
                    "longitude" to record.longitude,
                    "timestamp" to record.timestamp,
                    "savedAt" to record.savedAt
                )
            )
            .await()
    }

    companion object {
        private const val COLLECTION = "iss_history"
    }
}
