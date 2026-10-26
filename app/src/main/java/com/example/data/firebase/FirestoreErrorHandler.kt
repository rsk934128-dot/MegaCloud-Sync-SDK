package com.example.data.firebase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException

enum class OperationType {
    GET,
    LIST,
    CREATE,
    UPDATE,
    DELETE
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String) {
    val errorCode = if (exception is FirebaseFirestoreException) {
        exception.code.name
    } else {
        "UNKNOWN"
    }

    val message = exception.localizedMessage ?: exception.message ?: "No message"

    val logPayload = """
        {
          "error": "FirestoreOperationFailed",
          "operation": "${operationType.name}",
          "path": "$path",
          "code": "$errorCode",
          "message": "$message"
        }
    """.trimIndent()

    Log.e("FirestoreError", logPayload, exception)
}
