package com.docsysnfc.model

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.icu.text.DecimalFormat
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts


class FileManager(
    //private val securityManager: SecurityManager
) {


    private val selectedFiles = mutableListOf<Uri>()


    fun addFile(uri: Uri) {
        selectedFiles.add(uri)
    }

    fun getFiles(): MutableList<Uri> {
        return selectedFiles
    }


    /**
     * @param activity
     * @param launcher
     * @return uri file
     */

    fun chooseFile(activity: ActivityResultContracts) {



        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        //launcher.launch(intent)
    }

    /**
     * @param context
     * @param uri
     * @return size file in bytes
     */

    fun getSizeFile(context: Context, uri: Uri): Double {
        val contentResolver = context.contentResolver
        val fileDescriptor = contentResolver.openFileDescriptor(uri, "r")
        val sizeInBytes = fileDescriptor?.use {
            it.statSize.toDouble()
        } ?: 0.0 // Jeśli plik nie istnieje lub nie można go odczytać, zwróć 0.0 MB

        // Przelicz na megabajty (MB) i zaokrąglij do 2 miejsc po przecinku
        val sizeInMB = sizeInBytes / (1000.0 * 1000.0)
        return DecimalFormat("#.##").format(sizeInMB).toDouble()
    }

    /**
     * @param context
     * @param uri
     * @return name file
     */

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

            return name.toString().substringBeforeLast(".").substring(0, minOf(name.length, 10))
        } else {
            return "default"
        }

    }

    /**
     * @param context
     * @param uri
     * @return type file
     */

    fun getTypeFile(context: Context, uri: Uri): String {
        return getNameFile(context, uri, extension = true).substringAfterLast(".")
    }

}
