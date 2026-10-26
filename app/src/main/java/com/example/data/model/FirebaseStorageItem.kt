package com.example.data.model

data class FirebaseStorageItem(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val updatedEpochMs: Long,
    val contentType: String? = null,
    val downloadUrl: String? = null,
    val isFolder: Boolean = false,
    val bucketName: String = "",
    val localFilePath: String? = null,
    val isLocalVault: Boolean = false
)
