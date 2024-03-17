package com.docsysnfc.sender.model

import android.content.ContentValues
import android.content.Context
import android.icu.text.DecimalFormat
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.ByteArrayOutputStream
import java.net.URL

import com.docsysnfc.sender.model.securityModule.SecurityManager
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.round


class FileManager(
    //private val securityManager: SecurityManager
) {

    private val selectedFiles = mutableListOf<File>()


    fun toFile(uri: Uri, context: Context): File {
        return File(
            getNameFile(context, uri, false),
            uri,
            "",
            getSizeFile(context, uri),
            getTypeFile(context, uri),
            URL("https://www.google.com"),
            fileToByteArray(context, uri)!!
        )
    }


    fun addFile(uri: Uri, context: Context, securityManager: SecurityManager) {
//        Log.d("TAG123", "Before encryption ")

        val keys = securityManager.generateKeys()


        val byteArray = fileToByteArray(context, uri)

        if (byteArray != null && byteArray.isNotEmpty()) {
            // Szyfrowanie danych i klucza AES
//            val (encryptedData, encryptedAESKey) = securityManager.encryptDataWithAESAndRSA(byteArray, keys.first)
            //commented out because decoding is not implemented yet :)
            val encryptedData = byteArray
//            val encryptedAESKey = securityManager.encrypt(ByteArray(0), keys.first)


//            Log.d("TAG123", "Encrypted file: ${encryptedData.size} bytes")

            // Tworzenie nowego obiektu pliku z zaszyfrowanymi danymi
            val newFile = File(
                getNameFile(context, uri, false),
                uri,
                "",
                getSizeFile(context, uri),
                getTypeFile(context, uri),
                URL("https://www.google.com"),
                encryptedData // Używamy zaszyfrowanych danych
            )
            selectedFiles.add(newFile)
            Log.d("TAG123", "File added to selected files: ${newFile.byteArray.size} bytes")
            Log.d(
                "TAG123",
                "First 10 bytes: ${newFile.byteArray.sliceArray(0..9).contentToString()}"
            )
        } else {
//            Log.d("TAG123", "File is empty or failed to load data.")
        }
    }


    fun getFiles(): MutableList<File> {
        return selectedFiles
    }


    private fun fileToByteArray(context: Context, uri: Uri): ByteArray? {

        val inputStream = context.contentResolver.openInputStream(uri)

        inputStream?.let {
            val byteArrayOutputStream = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead)
            }
            return byteArrayOutputStream.toByteArray()
        }
        return null
    }


    fun byteArrayToFile(context: Context, byteArray: ByteArray, fileName: String) {

        // Pobierz typ MIME na podstawie rozszerzenia pliku
        val fileExtension = MimeTypeMap.getFileExtensionFromUrl(fileName)
        val mimeType =
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.lowercase(Locale.ROOT))

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            mimeType?.let { put(MediaStore.MediaColumns.MIME_TYPE, it) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values)
        uri?.let {
            resolver.openOutputStream(it).use { outputStream ->
                outputStream?.write(byteArray)
            }
        }
        Log.d("File123", "File saved to: $uri")
    }

    /**
     * @param context
     * @param uri
     * @return size file in bytes
     */




    /**
     * @param context
     * @param uri
     * @return name file
     */




    /**
     * @param context
     * @param uri
     * @return type file
     */

    private fun getTypeFile(context: Context, uri: Uri): String {
        return getNameFile(context, uri, extension = true).substringAfterLast(".")
    }

    companion object {

       fun getNameFile(context: Context, uri: Uri, extension: Boolean): String {
            val contentResolver = context.contentResolver
            val cursor = contentResolver.query(uri, null, null, null, null)
            val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor?.moveToFirst()
            val name = cursor?.getString(nameIndex!!)
            cursor?.close()
            if (name != null) {

                if (extension) {
                    return name.toString()
                }

                return name.toString().substringBeforeLast(".").substring(0, minOf(name.length, 5))
            } else {
                return "default"
            }

       }

        fun getExtensionFromMimeType(mimeType: String): String {
            return ("." + MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType))
        }

        fun getMimeTypeFromExtension(extension: String): String? {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }

        fun getSizeFile(context: Context, uri: Uri): Double {
            var fileSize: Double = 0.0

            val cursor = context.contentResolver.query(
                uri, null, null, null, null, null
            )

            try {
                if (cursor != null && cursor.moveToFirst()) {
                    // W kolumnie "_size" znajduje się rozmiar pliku w bajtach
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex).toDouble() // Pobiera rozmiar w bajtach jako Long, konwertuje na Double
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                cursor?.close()
            }

            return round(fileSize / (1024 * 1024) * 100) / 100
        }

        fun getSizeFileFromFileUri(uri: Uri): Double {
            val file = uri.path?.let { java.io.File(it) } // Tworzy obiekt File z podanej ścieżki
            if (file != null) {
                return if (file.exists()) {
                    val fileSizeInBytes = file.length()
                    round(fileSizeInBytes.toDouble() / (1024 * 1024) * 100) / 100
                } else {
                    0.0
                }
            }
            return 0.0
        }

        fun getSizeOfFileFromContentUri(context: Context, uri: Uri): Double {
            var fileSizeInBytes: Long = 0

            Log.d("NFC123", "Pobieranie rozmiaru pliku dla URI: $uri")

            val cursor = context.contentResolver.query(
                uri, arrayOf(OpenableColumns.SIZE), null, null, null)

            if (cursor == null) {
                Log.e("NFC123", "Cursor jest null dla URI: $uri")
                return 0.0
            }

            cursor.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        fileSizeInBytes = it.getLong(sizeIndex)
                        Log.d("NFC123", "Rozmiar pliku w bajtach: $fileSizeInBytes")
                    } else {
                        Log.e("NFC123", "Nie znaleziono kolumny SIZE dla URI: $uri")
                    }
                } else {
                    Log.e("NFC123", "Cursor nie może przejść do pierwszego wiersza dla URI: $uri")
                }
            }

            val fileSizeInMB = fileSizeInBytes.toDouble() / (1024 * 1024)
            Log.d("NFC123", "Rozmiar pliku w MB: $fileSizeInMB")

            return fileSizeInMB
        }

    }








}
