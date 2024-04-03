package com.docsysnfc.flowtouch.model

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.ByteArrayOutputStream
import java.net.URL

import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.net.URI
import java.nio.charset.Charset
import kotlin.math.round


class FileManager {

    companion object {

        fun toFile(uri: Uri, context: Context): File {

            val byteArray = fileToByteArray(context, uri)
            if (byteArray != null && byteArray.isNotEmpty()) {
                return File(
                    getNameFile(context, uri, false),
                    uri,
                    "",
                    getSizeFile(context, uri),
                    getTypeFile(context, uri),
                    URL("https://www.google.com"),
                    byteArray
                )
            } else {
                return File(
                    getNameFile(context, uri, false),
                    uri,
                    "",
                    getSizeFile(context, uri),
                    getTypeFile(context, uri),
                    URL("https://www.google.com"),
                    ByteArray(0)
                )
            }
        }

        fun deleteFile(file: File, context: Context?) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentResolver = context?.contentResolver
                contentResolver?.delete(file.uri, null, null)
            } else {
                    java.io.File(URI.create(file.uri.toString())).delete()
            }
        }

        fun fileToByteArray(context: Context, uri: Uri, noExtension: Boolean = true): ByteArray? {

            if (uri == Uri.EMPTY) {
                Log.e(TAG, "The Uri is empty.")
                return null
            }

            val inputStream = try {
                context.contentResolver.openInputStream(uri)
            } catch (e: FileNotFoundException) {
                Log.e(TAG, "File not found for the provided Uri.", e)
                return null
            }

            inputStream?.let {
                val byteArrayOutputStream = ByteArrayOutputStream()

                if(noExtension) {
                    val extension = getNameFile(context, uri, true).substringAfterLast(".")
                    val typeBytes = extension.toByteArray(Charset.forName("UTF-8"))
                    byteArrayOutputStream.write(typeBytes.size)
                    byteArrayOutputStream.write(typeBytes)
                }


                val buffer = ByteArray(1024)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead)
                }

                return byteArrayOutputStream.toByteArray()
            }
            return null
        }

        fun byteArrayToFile(
            context: Context,
            byteArray: ByteArray,
            originalFileName: String
        ): File {
            val extensionLength = byteArray[0].toInt()


            if (extensionLength <= 0 || extensionLength >= byteArray.size) {
                throw IllegalArgumentException("Invalid extension length in the file data")
            }

            val extensionBytes = byteArray.copyOfRange(1, 1 + extensionLength)
            val extension = String(extensionBytes, Charset.forName("UTF-8"))
            val fileContent = byteArray.copyOfRange(1 + extensionLength, byteArray.size)


            val savedFileUri = saveFile(fileContent, "$originalFileName.$extension", context)

            return File(
                originalFileName,
                savedFileUri ?: Uri.EMPTY,
                extension,
                fileContent.size.toDouble(),
                getMimeTypeFromExtension(extension) ?: "application/octet-stream",
                URL("https://www.google.com"),
                fileContent
            )
        }

        fun getNameFile(context: Context, uri: Uri, extension: Boolean): String {
            val contentResolver = context.contentResolver
            val cursor = contentResolver.query(uri, null, null, null, null)
            val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor?.moveToFirst()
            val name = cursor?.getString(nameIndex!!)
            cursor?.close()

            return if (name != null) {
                if (extension) {
                    name
                } else {
                    val nameWithoutExtension = name.substringBeforeLast(".")
                    if (nameWithoutExtension.length <= 5) {
                        nameWithoutExtension
                    } else {
                        nameWithoutExtension.substring(0, 5)
                    }
                }
            } else {
                "default"
            }
        }

        fun getSizeFileFromFileUri(uri: Uri): Double {
            val file = uri.path?.let { java.io.File(it) }
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

            Log.d(TAG, "Fetching file size for URI: $uri")

            val cursor = context.contentResolver.query(
                uri, arrayOf(OpenableColumns.SIZE), null, null, null
            )

            if (cursor == null) {
                Log.e(TAG, "Cursor is null for URI: $uri")
                return 0.0
            }

            cursor.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        fileSizeInBytes = it.getLong(sizeIndex)
                        Log.d(TAG, "File size in bytes: $fileSizeInBytes")
                    } else {
                        Log.e(TAG, "Column SIZE not found for URI: $uri")
                    }
                } else {
                    Log.e(TAG, "Cursor cannot move to first row for URI: $uri")
                }
            }

            val fileSizeInMB = fileSizeInBytes.toDouble() / (1024 * 1024)
            Log.d(TAG, "File size in MB: $fileSizeInMB")

            return fileSizeInMB
        }

        fun createURLFile(uri: Uri?, name: String, size: Double, type: String): File {
            val sizeInMb = round(size / (1024 * 1024) * 100) / 100
            return File(
                name,
                uri!!,
                uri.toString(),
                sizeInMb,
                getExtensionFromMimeType(type),
                URL(uri.toString()),
                ByteArray(0)
            )

        }

        fun getExtensionFromMimeType(mimeType: String): String {
            if (mimeType == "application/octet-stream") {
                return "binary"
            }
            return ("." + MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType))
        }

        private fun getMimeTypeFromExtension(extension: String): String? {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }

        private fun saveFile(file: ByteArray, fileName: String, context: Context): Uri? {
            val uriToSavedFile: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/DocSysNfc")
                }

                val resolver = context.contentResolver
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)?.also { uri ->
                    resolver.openOutputStream(uri).use { outputStream ->
                        outputStream?.write(file)
                    }
                }
            } else {
                val downloadsDir =
                    Environment.getExternalStoragePublicDirectory("${Environment.DIRECTORY_DOWNLOADS}/DocSysNfc")
                val newFile = java.io.File(downloadsDir, fileName)
                FileOutputStream(newFile).use { outputStream ->
                    outputStream.write(file)
                }
                Uri.fromFile(newFile)
            }

            return uriToSavedFile
        }

        private fun getSizeFile(context: Context, uri: Uri): Double {
            var fileSize = 0.0

            val cursor = context.contentResolver.query(
                uri, null, null, null, null, null
            )

            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex)
                            .toDouble()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                cursor?.close()
            }

            return round(fileSize / (1024 * 1024) * 100) / 100
        }

        private fun getTypeFile(context: Context, uri: Uri): String {
            return getNameFile(context, uri, extension = true).substringAfterLast(".")
        }

    }
}
