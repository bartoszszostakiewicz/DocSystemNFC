package com.docsysnfc.sender.model

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.IOException
import java.net.URL

interface UrlCallback {
    fun onUrlAvailable(url: String)
}

val TAG = "NFC123"

class CloudComm(
    private val firebaseStorage: FirebaseStorage = FirebaseStorage.getInstance()

) {


    private fun createFileInInternalStorage(
        context: Context,
        folderName: String = "NFC_DocSys",
        fileName: String
    ): java.io.File {
        val folder = java.io.File(context.filesDir, folderName)
        if (!folder.exists()) {
            folder.mkdir()
        }

        return java.io.File(folder, fileName)
    }

    suspend fun downloadFile(downloadLink: String, context: Context, callback: (Uri?, String) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Kod dla Android 10 (API 29) lub nowszego
            downloadFileForQAndAbove(downloadLink, context, callback)
        } else {
            // Kod dla starszych wersji Androida
            downloadFileForPreQ(downloadLink, context, callback)
        }
    }

   private fun downloadFileForPreQ(downloadLink: String, context: Context, callback: (Uri?,String) -> Unit) {
        // Logowanie URL


        val link = downloadLink.drop(3)

        Log.d("NFC123", "URL: $link")
        Log.d("NFC123", "URL len: ${link.length}")
        // Uzyskanie referencji do pliku w Firebase Storage
        val storageRef = firebaseStorage.getReferenceFromUrl(link)
        Log.d("NFC123", "storageRef: $storageRef")



        // Obsługa metadanych
        storageRef.metadata.addOnSuccessListener { metadata ->
            Log.d("NFC123", "metadata: $metadata")

            // Tworzenie pliku w katalogu "Downloads"
            val folderName = Environment.DIRECTORY_DOWNLOADS // Użycie publicznego katalogu "Downloads"
            val fileName = metadata.name ?: "unknown"
            val mimeType = metadata.contentType ?: "application/octet-stream"

            // Sprawdzenie uprawnień do zapisu
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {

                // Tworzenie pliku lokalnego
                val downloadsFolder = Environment.getExternalStoragePublicDirectory(folderName)
                var localFile = java.io.File(downloadsFolder, fileName + FileManager.getExtensionFromMimeType(mimeType))

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
                    Log.d("NFC123", "Plik został pomyślnie pobrany: ${localFile.absolutePath}")
                    // Przekazanie Uri do pobranego pliku
                    callback(Uri.fromFile(localFile), mimeType)
                }.addOnFailureListener { exception ->
                    Log.e("NFC123", "Błąd pobierania pliku: $exception")
                    callback(null, "")
                }
            } else {
                // Obsługa przypadku, gdy nie ma uprawnień
                Log.e("NFC123", "Brak uprawnień do zapisu w katalogu Downloads")
                callback(null, "")
            }

        }.addOnFailureListener {
            Log.e("NFC123", "Błąd pobierania metadanych: $it")
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun downloadFileForQAndAbove(downloadLink: String, context: Context, callback: (Uri?, String) -> Unit) {

//        CoroutineScope(Dispatchers.IO).launch {

        if (Looper.myLooper() == Looper.getMainLooper()) {
            // Kod jest wykonywany na głównym wątku
            Log.d("NFC123", "Kod jest wykonywany na głównym wątku")
        } else {
            Log.d("NFC123", "Kod jest wykonywany na wątku tła")
            // Kod jest wykonywany na wątku tła
        }

        val link = downloadLink.drop(3)

        // Logowanie URL
        Log.d("NFC123", "URL: $link")
        Log.d("NFC123", "URL len: ${link.length}")

        // Uzyskanie referencji do pliku w Firebase Storage
        val storageRef = firebaseStorage.getReferenceFromUrl(link)
        Log.d("NFC123", "storageRef: $storageRef")

        // Obsługa metadanych
        storageRef.metadata.addOnSuccessListener { metadata ->
            Log.d("NFC123", "metadata: $metadata")

            val fileName = metadata.name ?: "unknown"
            val mimeType = metadata.contentType ?: "application/octet-stream"


            // Sprawdzenie, czy plik o takiej nazwie już istnieje i modyfikacja nazwy
            var newFileName = fileName
            var fileExists = checkIfFileExists(context, Environment.DIRECTORY_DOWNLOADS, newFileName, mimeType)
            var counter = 1
            while (fileExists) {
                // Dodanie numeru do nazwy pliku
                newFileName = "${fileName}_${counter++}"
                fileExists = checkIfFileExists(context, Environment.DIRECTORY_DOWNLOADS, newFileName, mimeType)
            }



            val contentResolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
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
                                Log.d("NFC123", "Plik został pomyślnie pobrany: ${uri}")
                                // Przekazanie Uri do pobranego pliku
                                callback(uri, mimeType)
                            } catch (e: IOException) {
                                Log.e("NFC123", "Błąd przy zapisie danych do strumienia: $e")
                                callback(null, "")
                            }
                        }.addOnFailureListener { exception ->
                            Log.e("NFC123", "Błąd pobierania pliku: $exception")
                            callback(null, "")
                        }
                    } else {
                        Log.e("NFC123", "Nie można otworzyć OutputStream")
                        callback(null, "")
                    }
                } ?: run {
                    Log.e("NFC123", "Nie można utworzyć pliku w MediaStore")
                    callback(null, "")
                }

            } catch (e: Exception) {
                Log.e("NFC123", "Błąd przy tworzeniu pliku w MediaStore: $e")
                callback(null, "")
            }
        }.addOnFailureListener {
            Log.e("NFC123", "Błąd pobierania metadanych: $it")
        }
    }

    private fun checkIfFileExists(context: Context, directory: String, fileName: String, mimeType: String): Boolean {
        val contentResolver = context.contentResolver
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.MIME_TYPE} = ?"
        val selectionArgs = arrayOf(fileName, mimeType)
        val queryUri = MediaStore.Files.getContentUri("external")

        contentResolver.query(queryUri, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.count > 0) {
                return true
            }
        }
        return false
    }



    private fun createFileInExternalStorage(context: Context, folderName: String, fileName: String): java.io.File {
        val fileDirectory = context.getExternalFilesDir(folderName)
        if (!fileDirectory?.exists()!!) {
            fileDirectory.mkdir()
        }
        return java.io.File(fileDirectory, fileName)
    }

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
                    Log.d(
                        "TAG123",
                        "First 10 bytes: ${
                            selectedFile.byteArray.sliceArray(0..9).contentToString()
                        }"
                    )
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