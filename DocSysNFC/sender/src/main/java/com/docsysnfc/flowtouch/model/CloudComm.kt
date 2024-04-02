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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import java.io.IOException
import java.net.URL

interface UrlCallback {
    fun onUrlAvailable(url: String)
}

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

        // Sprawdzenie istnienia pliku
        if (destinationFile.exists()) {
            Log.d(TAG, "Plik już istnieje: ${destinationFile.absolutePath}")
            callback(
                Uri.fromFile(destinationFile),
                "Plik już istnieje: ${destinationFile.absolutePath}"
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

                // Tworzenie pliku lokalnego
                val downloadsFolder =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val docSysNfcFolder = java.io.File(downloadsFolder, "DocSysNFC")

                // Sprawdzenie czy katalog istnieje, jeśli nie - próba utworzenia
                if (!docSysNfcFolder.exists()) {
                    docSysNfcFolder.mkdirs()
                }

                var localFile = java.io.File(
                    docSysNfcFolder,
                    fileName + FileManager.getExtensionFromMimeType(mimeType)
                )

                // Dodanie logiki zapobiegającej nadpisywaniu istniejących plików
                var counter = 1
                val originalName = localFile.nameWithoutExtension
                val extension = localFile.extension

                while (localFile.exists()) {
                    // Modyfikacja nazwy pliku, dodając sufiks z numerem
                    val newName = "$originalName($counter).$extension"
                    localFile = java.io.File(downloadsFolder, newName)
                    counter++
                }


                // Pobieranie pliku
                storageRef.getFile(localFile).addOnSuccessListener {
                    Log.d(TAG, "Plik został pomyślnie pobrany: ${localFile.absolutePath}")
                    // Przekazanie Uri do pobranego pliku
                    callback(Uri.fromFile(localFile), mimeType)
                }.addOnFailureListener { exception ->
                    Log.e(TAG, "Błąd pobierania pliku: $exception")
                    callback(null, "")
                }
            } else {
                // Obsługa przypadku, gdy nie ma uprawnień
                Log.e(TAG, "Brak uprawnień do zapisu w katalogu Downloads")
                callback(null, "")
            }

        }.addOnFailureListener {
            Log.e(TAG, "Błąd pobierania metadanych: $it")
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun downloadFileForQAndAbove(
        directory: String,
        link: String,
        context: Context,
        callback: (Uri?, String) -> Unit
    ) {


        // Logowanie URL


        // Uzyskanie referencji do pliku w Firebase Storage
        val storageRef = firebaseStorage.getReferenceFromUrl(link)
        Log.d(TAG, "storageRef: $storageRef")

        // Obsługa metadanych
        storageRef.metadata.addOnSuccessListener { metadata ->
            Log.d(TAG, "metadata: $metadata")

            val fileName = metadata.name ?: "unknown"
            val mimeType = metadata.contentType ?: "application/octet-stream"


            // Sprawdzenie, czy plik o takiej nazwie już istnieje i modyfikacja nazwy
            var newFileName = fileName
            var fileExists =
                checkIfFileExists(context, Environment.DIRECTORY_DOWNLOADS, newFileName, mimeType)
            var counter = 1
            while (fileExists) {
                // Dodanie numeru do nazwy pliku
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
                // Utworzenie Uri wewnętrznego z użyciem MediaStore
                val fileUri = contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                fileUri?.let { uri ->
                    // Otworzenie strumienia danych wyjściowych
                    val outputStream = contentResolver.openOutputStream(uri)
                    if (outputStream != null) {
                        // Pobieranie strumienia z obiektu StorageReference
                        storageRef.stream.addOnSuccessListener { stream ->
                            try {
                                stream.stream.copyTo(outputStream)
                                outputStream.close() // Zamknięcie strumienia po zakończeniu zapisu
                                Log.d(TAG, "Plik został pomyślnie pobrany: ${uri}")
                                // Przekazanie Uri do pobranego pliku
                                callback(uri, mimeType)
                            } catch (e: IOException) {
                                Log.e(TAG, "Błąd przy zapisie danych do strumienia: $e")
                                callback(null, "")
                            }
                        }.addOnFailureListener { exception ->
                            Log.e(TAG, "Błąd pobierania pliku: $exception")
                            callback(null, "")
                        }
                    } else {
                        Log.e(TAG, "Nie można otworzyć OutputStream")
                        callback(null, "")
                    }
                } ?: run {
                    Log.e(TAG, "Nie można utworzyć pliku w MediaStore")
                    callback(null, "")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Błąd przy tworzeniu pliku w MediaStore: $e")
                callback(null, "")
            }
        }.addOnFailureListener {
            Log.e(TAG, "Błąd pobierania metadanych: $it")
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

        // Dodanie unikalnego identyfikatora do nazwy pliku
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
                    // Po udanym przesłaniu pliku, pobierz jego URL
                    fileRef.downloadUrl.addOnSuccessListener { uri ->
                        val downloadUrl = uri.toString()
                        selectedFile?.url = URL(downloadUrl)
                        onUrlAvailable(downloadUrl)
                    }.addOnFailureListener { exception ->
                        Log.d(TAG, "onCreatexd: $exception")
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
                Log.d("TAG123", "ByteArray is empty, file not uploaded")
                onUrlAvailable("Error: ByteArray is empty")
                if (selectedFile != null) {
                    selectedFile.isUploading = false
                } else {

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
            Log.d(TAG, "onCreate: ${auth.currentUser}")
            return
        }
        val fileRef = firebaseStorage.reference.child("files/${auth.currentUser!!.uid}")
        fileRef.listAll()
            .addOnSuccessListener { listResult ->
                val files = mutableListOf<File>()
                val itemCount = listResult.items.size
                var processedCount = 0

                if (itemCount == 0) {
                    onResult(files) // Jeśli nie ma plików, zwróć pustą listę.
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
                            }.addOnFailureListener { exception ->
                                processedCount++
                                if (processedCount == itemCount) {
                                    onResult(files) // Zwraca listę nawet jeśli niektóre pliki nie powiodły się.
                                }
                            }
                        }.addOnFailureListener { exception ->
                            processedCount++
                            if (processedCount == itemCount) {
                                onResult(files) // Zwraca listę nawet jeśli niektóre pliki nie powiodły się.
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