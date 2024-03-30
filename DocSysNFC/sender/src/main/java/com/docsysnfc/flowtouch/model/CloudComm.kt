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

    suspend fun downloadFile(
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
            Log.d("nfc123", "Plik już istnieje: ${destinationFile.absolutePath}")
            callback(Uri.fromFile(destinationFile), "Plik już istnieje: ${destinationFile.absolutePath}")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Kod dla Android 10 (API 29) lub nowszego
            downloadFileForQAndAbove(downloadDirectory,downloadLink, context, callback)
        } else {
            // Kod dla starszych wersji Androida
            downloadFileForPreQ(downloadDirectory,downloadLink, context, callback)
        }
    }



    private fun downloadFileForPreQ(
        directory: String,
        downloadLink: String,
        context: Context,
        callback: (Uri?, String) -> Unit
    ) {
        // Logowanie URL


        val link : String
        if(downloadLink.startsWith("https")){
            link = downloadLink
        }else {
            link = downloadLink.drop(3)
        }

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
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                == PackageManager.PERMISSION_GRANTED
            ) {

                // Tworzenie pliku lokalnego
                val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
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
    private fun downloadFileForQAndAbove(
        directory: String,
        downloadLink: String,
        context: Context,
        callback: (Uri?, String) -> Unit
    ) {

        val link : String
        if(downloadLink.startsWith("https")){
            link = downloadLink
        }else if (downloadLink.startsWith("enhttps")) {
            link = downloadLink.drop(1)
        } else {
            link = downloadLink.drop(3)
        }

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



    fun uploadFile(selectedFile: File?, cipher: Boolean = false, onUrlAvailable: (String) -> Unit) {


        if (selectedFile != null) {
            selectedFile.isUploading = true
        }


        if (selectedFile != null) {
            if(selectedFile.url.toString() != "https://www.google.com") {
                onUrlAvailable(selectedFile.url.toString())
                return
            }
        }


        if (auth.currentUser == null) {
            Log.d("TAG123", "User not logged in")
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
                        Log.d("TAG123", "onCreatexd: $exception")
                        onUrlAvailable("Error: $exception")
                    }
                }.addOnFailureListener {
                    Log.d("TAG123", "Upload failure: $it")
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
            selectedFile?.let {file ->
                fileRef.putFile(file.uri).addOnSuccessListener {
                    // Po udanym przesłaniu pliku, pobierz jego URL
                    fileRef.downloadUrl.addOnSuccessListener { uri ->
                        val downloadUrl = uri.toString()
                        selectedFile.url = URL(downloadUrl)
                        onUrlAvailable(downloadUrl)
                    }.addOnFailureListener { exception ->
                        Log.d("TAG123", "onCreatexd: $exception")
                        onUrlAvailable("Error: $exception")
                    }
                }.addOnFailureListener {
                    Log.d("TAG123", "Upload failure: $it")
                    onUrlAvailable("Error: Upload failure: $it")
                }
            } ?: run {
                Log.d("TAG123", "Selected file is null")
                onUrlAvailable("Error: Selected file is null")
            }
            Log.d("TAG123", "Selected file or byteArray is null")
            onUrlAvailable("Error: Selected file or byteArray is null")
        }
    }



    fun deleteFile(file: File) {


        if (auth.currentUser == null) {
            Log.d("nfc123", "onCreate: ${auth.currentUser}")
            return
        }

        val fileRef =
            firebaseStorage.reference.child("files/${auth.currentUser!!.uid}/${file.name}")

        fileRef.delete().addOnSuccessListener {
            Log.d("nfc123", "File deleted")
        }.addOnFailureListener {
            Log.d("nfc123", "File not deleted")
        }
    }

    fun getFilesList(context: Context, onResult: (List<File>) -> Unit, onError: (Exception) -> Unit) {
        if(auth.currentUser == null) {
            Log.d("nfc123", "onCreate: ${auth.currentUser}")
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