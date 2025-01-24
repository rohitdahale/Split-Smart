package com.rohit.splitsmart.utils

import android.content.Context
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object ReceiptUtils {
    private val storage = FirebaseStorage.getInstance()
    private val receiptsRef = storage.reference.child("receipts")

    suspend fun uploadReceipt(context: Context, imageUri: Uri): String {
        return suspendCoroutine { continuation ->
            val receiptId = UUID.randomUUID().toString()
            val receiptRef = receiptsRef.child("$receiptId.jpg")

            // Open input stream outside of use block
            val inputStream = context.contentResolver.openInputStream(imageUri)

            if (inputStream == null) {
                continuation.resumeWithException(Exception("Failed to open image file"))
                return@suspendCoroutine
            }

            // Create upload task
            val uploadTask = receiptRef.putStream(inputStream)

            // Add completion listener before starting the upload
            uploadTask.addOnCompleteListener { task ->
                // Always close the input stream after upload completes or fails
                try {
                    inputStream.close()
                } catch (e: Exception) {
                    // Log but don't throw as the main operation might have succeeded
                    e.printStackTrace()
                }

                if (!task.isSuccessful) {
                    continuation.resumeWithException(
                        task.exception ?: Exception("Upload failed")
                    )
                    return@addOnCompleteListener
                }

                // Get download URL after successful upload
                receiptRef.downloadUrl
                    .addOnSuccessListener { uri ->
                        continuation.resume(uri.toString())
                    }
                    .addOnFailureListener { exception ->
                        continuation.resumeWithException(exception)
                    }
            }
        }
    }

    fun deleteReceipt(receiptUrl: String) {
        if (receiptUrl.isNotEmpty()) {
            try {
                storage.getReferenceFromUrl(receiptUrl).delete()
            } catch (e: Exception) {
                e.printStackTrace()
                // Consider adding error callback or throwing exception based on your needs
            }
        }
    }
}