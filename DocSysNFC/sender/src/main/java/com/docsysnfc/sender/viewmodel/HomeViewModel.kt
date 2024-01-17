package com.docsysnfc.sender.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.docsysnfc.sender.model.File
import com.docsysnfc.sender.model.FileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class HomeViewModel (
    app: Application,
//    private var fileManager: FileManager = FileManager(), // jak jest tutaj to wywala apke !!!!!!!!!!!
) : AndroidViewModel(app) {


    private val context = getApplication<Application>().applicationContext


    private var fileManager = FileManager()

    //change list uri

    private val _modelSelectedFiles = MutableStateFlow<List<File>>(emptyList())
    val modelSelectedFiles = _modelSelectedFiles.asStateFlow()


    private val _activeURL = MutableStateFlow("google.com")
    val urlStateFlow = _activeURL.asStateFlow()


    private val _startServiceEvent = MutableStateFlow<String?>(null)
    val startServiceEvent = _startServiceEvent.asStateFlow()

    private val _isActivityVisible = MutableStateFlow(true)
    val isActivityVisible = _isActivityVisible.asStateFlow()

    init {
        _modelSelectedFiles.update { fileManager.getFiles() }
    }

    fun setActivityVisibility(isVisible: Boolean) {
        _isActivityVisible.value = isVisible
    }

    fun resetServiceEvent() {
        _startServiceEvent.value = null
    }

//    fun initService() {
//        _startServiceEvent.value = "chat.openai.com"
//    }

    private fun addFile(uri: Uri) {
        val currentList = _modelSelectedFiles.value.toMutableList()
        currentList.add(fileManager.toFile(uri, context))

        fileManager.addFile(uri, context)

        _modelSelectedFiles.value = currentList


        _activeURL.value = uri.toString()
        _startServiceEvent.value = uri.toString()
    }
//
//    fun getFiles(): MutableList<File> {
//        return fileManager.getFiles()
//    }
//
//
//    fun getSizeFile(uri: Uri): Double {
//        return fileManager.getSizeFile(context, uri)
//    }
//
//    fun getNameFile(uri: Uri): String {
//        return fileManager.getNameFile(context, uri, extension = false)
//    }
//
//    fun getTypeFile(uri: Uri): String {
//        return fileManager.getTypeFile(context, uri)
//    }

    fun chooseFile() {
        _modelSelectedFiles.update { fileManager.getFiles() }
    }

    fun onFilePicked(uri: Uri) {
        // Logika obsługi wybranego pliku, na przykład:
        addFile(uri)
    }

}