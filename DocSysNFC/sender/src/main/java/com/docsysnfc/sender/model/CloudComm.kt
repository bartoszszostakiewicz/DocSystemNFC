package com.docsysnfc.sender.model

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.URL

interface UrlCallback {
    fun onUrlAvailable(url: String)
}

class CloudComm(
    private val firebaseStorage: FirebaseStorage = FirebaseStorage.getInstance()

) {


    fun uploadFile(selectedFile: File?) {

        val auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            Log.d("TAG123", "onCreate: ${auth.currentUser}")
            return
        }

        Log.d("TAG123", "onCreate: ${auth.currentUser}")


        val fileRef =
            firebaseStorage.reference.child("images/${auth.currentUser?.uid}/test/${selectedFile?.name}")

        selectedFile?.byteArray?.let { byteArray ->
            if (byteArray.isNotEmpty()) {
//                Log.d("TAG123", "ByteArray is not empty, uploading file :${selectedFile.byteArray.size} bytes")
                fileRef.putBytes(byteArray).addOnSuccessListener {
                    Log.d("TAG123", "File added to cloud: ${selectedFile.byteArray.size} bytes")
                    Log.d("TAG123", "First 10 bytes: ${selectedFile.byteArray.sliceArray(0..9).contentToString()}")
//                    Log.d("TAG123", "onCreateOK: $it")
//                    Log.d("TAG123", "bytearray: ${selectedFile.byteArray}")
//                    Log.d("TAG123", "size: ${selectedFile.byteArray.size}")
                }.addOnFailureListener {
                    Log.d("TAG123", "onCreateNOTOK: $it")
                }
            } else {
                Log.d("TAG123", "ByteArray is empty, file not uploaded")
            }
        } ?: Log.d("TAG123", "Selected file or byteArray is null")




    }

    fun setURLToFile(selectedFile: File?, callback: UrlCallback) {
        val auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            Log.d("TAG123", "onCreate: ${auth.currentUser}")
            callback.onUrlAvailable("https://www.google.com")
            return
        }

        val uriFile = Uri.parse("${selectedFile?.uri}") // file path
        val fileRef =
            firebaseStorage.reference.child("images/${auth.currentUser?.uid}/test/${selectedFile?.name}")

        fileRef.downloadUrl.addOnSuccessListener { uri ->
            val downloadUrl = uri.toString()
            Log.d("TAG123", "onCreatexx: $downloadUrl")

            if (selectedFile != null) {
                selectedFile.url = URL(downloadUrl)
            }

            // Wywołujemy callback z uzyskanym URL
            callback.onUrlAvailable(downloadUrl)
        }.addOnFailureListener { exception ->
            Log.d("TAG123", "onCreatexd: $exception")
            // Jeśli wystąpi błąd, możesz obsłużyć go tutaj
            val errorUrl = "https://www.google.com"
            callback.onUrlAvailable(errorUrl) // W przypadku błędu, wywołujemy callback z innym URL
        }
    }

    fun deleteFile() {

    }

    fun updateFile() {

    }

    fun getFile() {

    }

    fun getFiles() {

    }


}