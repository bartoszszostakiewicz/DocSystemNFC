package com.docsysnfc.flowtouch.model

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.docsysnfc.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import java.io.IOException
import java.net.URL


val TAG = "NFC123"

class CloudComm(
    private val firebaseStorage: FirebaseStorage = FirebaseStorage.getInstance()
) {


    private val auth = FirebaseAuth.getInstance()


    fun downloadFile(
        downloadDirectory: String = "${Environment.DIRECTORY_DOWNLOADS}/DocSysNfc",
        downloadLink: String,
        context: Context,
        callback: (Uri?, String) -> Unit
    ) {
        val fileName = downloadLink.substringAfterLast('/')
        val destinationFile =
            java.io.File(context.getExternalFilesDir(null), "$downloadDirectory/$fileName")

        if (destinationFile.exists()) {
            Log.d(TAG, "File already exists: ${destinationFile.absolutePath}")
            callback(
                Uri.fromFile(destinationFile),
                "${context.getString(R.string.file_exist)}: ${destinationFile.absolutePath}"
            )
            return
        }


        val httpsIndex = downloadLink.indexOf("https")

        try {
            val link = downloadLink.substring(httpsIndex)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                downloadFileForQAndAbove(downloadDirectory, link, context, callback)
            } else {
                downloadFileForPreQ(downloadDirectory, link, context, callback)
            }
        }catch (e: Exception){
            Log.d(TAG, "Error: $e")
            callback(null, "Error: $e")
        }
    }


    private fun downloadFileForPreQ(
        directory: String,
        link: String,
        context: Context,
        callback: (Uri?, String) -> Unit
    ) {

        // Uzyskanie referencji do pliku w Firebase Storage
        val storageRef = firebaseStorage.getReferenceFromUrl(link)
        Log.d(TAG, "storageRef: $storageRef")


        // Obsługa metadanych
        storageRef.metadata.addOnSuccessListener { metadata ->
            Log.d(TAG, "metadata: $metadata")

            // Tworzenie pliku w katalogu "Downloads"
            val folderName =
                Environment.DIRECTORY_DOWNLOADS // Użycie publicznego katalogu "Downloads"
            val fileName = metadata.name ?: "unknown"
            val mimeType = metadata.contentType ?: "application/octet-stream"

            // Sprawdzenie uprawnień do zapisu
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                == PackageManager.PERMISSION_GRANTED
            ) {

                val downloadsFolder =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val docSysNfcFolder = java.io.File(downloadsFolder, "DocSysNFC")


                if (!docSysNfcFolder.exists()) {
                    docSysNfcFolder.mkdirs()
                }

                var localFile = java.io.File(
                    docSysNfcFolder,
                    fileName + FileManager.getExtensionFromMimeType(mimeType)
                )

                var counter = 1
                val originalName = localFile.nameWithoutExtension
                val extension = localFile.extension

                while (localFile.exists()) {
                    val newName = "$originalName($counter).$extension"
                    localFile = java.io.File(downloadsFolder, newName)
                    counter++
                }



                storageRef.getFile(localFile).addOnSuccessListener {
                    Log.d(TAG, "File successfully downloaded: ${localFile.absolutePath}")
                    // Passing the Uri of the downloaded file
                    callback(Uri.fromFile(localFile), mimeType)
                }.addOnFailureListener { exception ->
                    Log.e(TAG, "Error downloading file: $exception")
                    callback(null, "")
                }

            } else {
                Log.e(TAG, "No write permissions for the Downloads directory")
                callback(null, "")
            }

        }.addOnFailureListener {
            Log.e(TAG, "Error downloading metadata: $it")
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun downloadFileForQAndAbove(
        directory: String,
        link: String,
        context: Context,
        callback: (Uri?, String) -> Unit
    ) {
        val storageRef = firebaseStorage.getReferenceFromUrl(link)
        Log.d(TAG, "StorageRef: $storageRef")


        storageRef.metadata.addOnSuccessListener { metadata ->

            val fileName = metadata.name ?: "unknown"
            val mimeType = metadata.contentType ?: "application/octet-stream"



            var newFileName = fileName
            var fileExists =
                checkIfFileExists(context, Environment.DIRECTORY_DOWNLOADS, newFileName, mimeType)
            var counter = 1
            while (fileExists) {
                newFileName = "${fileName}_${counter++}"
                fileExists = checkIfFileExists(
                    context,
                    Environment.DIRECTORY_DOWNLOADS,
                    newFileName,
                    mimeType
                )
            }


            val contentResolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DOWNLOADS}/DocSysNfc"
                )
            }

            try {
                val fileUri = contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                fileUri?.let { uri ->
                    // Opening output data stream
                    val outputStream = contentResolver.openOutputStream(uri)
                    if (outputStream != null) {
                        // Retrieving stream from StorageReference object
                        storageRef.stream.addOnSuccessListener { stream ->
                            try {
                                stream.stream.copyTo(outputStream)
                                outputStream.close() // Closing the stream after writing is complete
                                Log.d(TAG, "File successfully downloaded: $uri")
                                // Passing the Uri of the downloaded file
                                callback(uri, mimeType)
                            } catch (e: IOException) {
                                Log.e(TAG, "Error while writing data to the stream: $e")
                                callback(null, "")
                            }
                        }.addOnFailureListener { exception ->
                            Log.e(TAG, "Error downloading file: $exception")
                            callback(null, "")
                        }
                    } else {
                        Log.e(TAG, "Cannot open OutputStream")
                        callback(null, "")
                    }
                } ?: run {
                    Log.e(TAG, "Unable to create file in MediaStore")
                    callback(null, "")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error creating file in MediaStore: $e")
                callback(null, "")
            }
        }

    }

    private fun checkIfFileExists(
        context: Context,
        directory: String,
        fileName: String,
        mimeType: String
    ): Boolean {
        val contentResolver = context.contentResolver
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
        val selection =
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.MIME_TYPE} = ?"
        val selectionArgs = arrayOf(fileName, mimeType)
        val queryUri = MediaStore.Files.getContentUri("external")

        contentResolver.query(queryUri, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.count > 0) {
                return true
            }
        }
        return false
    }


    fun uploadFile(
        selectedFile: File?,
        cipher: Boolean = false,
        onUrlAvailable: (String) -> Unit
    ) {

        if (auth.currentUser == null) {
            Log.d(TAG, "User not logged in")
            onUrlAvailable("Error: User not logged in")
            if (selectedFile != null) {
                selectedFile.isUploading = false
            }
            return
        }


        val uniqueID = System.currentTimeMillis().toString()
        val fileName = if (selectedFile?.name?.contains(".") == true) {
            val namePart = selectedFile.name.substringBeforeLast(".")
            val extensionPart = selectedFile.name.substringAfterLast(".")
            "$namePart-$uniqueID.$extensionPart"
        } else {
            "${selectedFile?.name}-$uniqueID"
        }

        val fileRef =
            firebaseStorage.reference.child("files/${auth.currentUser!!.uid}/$fileName")

        val byteArray = if (cipher) selectedFile?.encryptedByteArray else null
        byteArray?.let {
            if (it.isNotEmpty()) {
                fileRef.putBytes(it).addOnSuccessListener {
                    fileRef.downloadUrl.addOnSuccessListener { uri ->
                        val downloadUrl = uri.toString()
                        selectedFile?.url = URL(downloadUrl)
                        onUrlAvailable(downloadUrl)
                    }.addOnFailureListener { exception ->
                        Log.d(TAG, "Upload failure: $exception")
                        onUrlAvailable("Error: $exception")
                    }
                }.addOnFailureListener {
                    Log.d(TAG, "Upload failure: $it")
                    onUrlAvailable("Error: Upload failure: $it")
                    if (selectedFile != null) {
                        selectedFile.isUploading = false
                    }
                }
            } else {
                Log.d(TAG, "ByteArray is empty, file not uploaded")
                onUrlAvailable("Error: ByteArray is empty")
                if (selectedFile != null) {
                    selectedFile.isUploading = false
                } else {
                    Log.d(TAG, "Selected file is null")
                    onUrlAvailable("Error: Selected file is null")
                }
            }
        } ?: run {
            selectedFile?.let { file ->
                fileRef.putFile(file.uri).addOnSuccessListener {
                    fileRef.downloadUrl.addOnSuccessListener { uri ->
                        val downloadUrl = uri.toString()
                        selectedFile.url = URL(downloadUrl)
                        onUrlAvailable(downloadUrl)
                    }.addOnFailureListener { exception ->
                        Log.d(TAG, "Upload failure: $exception")
                        onUrlAvailable("Error: $exception")
                    }
                }.addOnFailureListener {
                    Log.d(TAG, "Upload failure: $it")
                    onUrlAvailable("Error: Upload failure: $it")
                }
            } ?: run {
                Log.d(TAG, "Selected file is null")
                onUrlAvailable("Error: Selected file is null")
            }
            Log.d(TAG, "Selected file or byteArray is null")
            onUrlAvailable("Error: Selected file or byteArray is null")
        }
    }


    fun deleteFile(file: File) {


        if (auth.currentUser == null) {
            Log.d(TAG, "onCreate: ${auth.currentUser}")
            return
        }

        val fileRef =
            firebaseStorage.reference.child("files/${auth.currentUser!!.uid}/${file.name}")

        fileRef.delete().addOnSuccessListener {
            Log.d(TAG, "File deleted")
        }.addOnFailureListener {
            Log.d(TAG, "File not deleted")
        }
    }


    fun getFilesList(onResult: (List<File>) -> Unit, onError: (Exception) -> Unit) {
        if (auth.currentUser == null) {
            Log.d(TAG, "User is null: ${auth.currentUser}")
            return
        }
        val fileRef = firebaseStorage.reference.child("files/${auth.currentUser!!.uid}")
        fileRef.listAll()
            .addOnSuccessListener { listResult ->
                val files = mutableListOf<File>()
                val itemCount = listResult.items.size
                var processedCount = 0

                if (itemCount == 0) {
                    onResult(files)
                } else {
                    listResult.items.forEach { storageReference ->
                        storageReference.metadata.addOnSuccessListener { metadata ->
                            storageReference.downloadUrl.addOnSuccessListener { uri ->
                                val name = metadata.name ?: "unknown"
                                val size = metadata.sizeBytes.toDouble()
                                val type = metadata.contentType ?: "application/octet-stream"
                                val newFile = FileManager.createURLFile(uri, name, size, type)
                                files.add(newFile)
                                processedCount++
                                if (processedCount == itemCount) {
                                    onResult(files)
                                }
                            }.addOnFailureListener {
                                processedCount++
                                if (processedCount == itemCount) {
                                    onResult(files)
                                }
                            }
                        }.addOnFailureListener {
                            processedCount++
                            if (processedCount == itemCount) {
                                onResult(files)
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }


}