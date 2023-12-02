package com.docsysnfc.viewmodel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import com.docsysnfc.model.FileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.concurrent.fixedRateTimer
import kotlin.random.Random

class HomeViewModel @Inject constructor(
    private val context: Context,
    private val activityResultContracts: ActivityResultContracts

) : ViewModel() {

    private val fileManager = FileManager()

    private val _modelSelectedFiles = MutableStateFlow<List<Uri>>(emptyList())

    val modelSelectedFiles = _modelSelectedFiles.asStateFlow()

    private val _modelData = MutableStateFlow(6)

    val modelData = _modelData.asStateFlow()


    init {
        _modelSelectedFiles.update { fileManager.getFiles() }
        makeConnectionRequest()
    }

    fun makeConnectionRequest() {

        fixedRateTimer(period = 1000L) {
            _modelData.update { Random.nextInt(0, 100) }
        }
    }


    fun addFile(uri: Uri) {
        val currentList = _modelSelectedFiles.value.toMutableList()
        currentList.add(uri)
        fileManager.addFile(uri)
        _modelSelectedFiles.value = currentList
    }

    fun getFiles(): MutableList<Uri> {
        return fileManager.getFiles()
    }


    fun getSizeFile(uri: Uri): Double {
        return fileManager.getSizeFile(context, uri)
    }

    fun getNameFile(uri: Uri): String {
        return fileManager.getNameFile(context, uri, extension = false)
    }

    fun getTypeFile(uri: Uri): String {
        return fileManager.getTypeFile(context, uri)
    }

    fun chooseFile() {
        fileManager.chooseFile(activityResultContracts)

        _modelSelectedFiles.update { fileManager.getFiles() }
    }


}