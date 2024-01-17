package com.docsysnfc.sender

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.docsysnfc.sender.model.CloudComm
import com.docsysnfc.sender.model.File
import com.docsysnfc.sender.model.FileManager
import com.docsysnfc.sender.model.UrlCallback
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.net.URL

class MainViewModel (
    app: Application,
//    private var fileManager: FileManager = FileManager(), // jak jest tutaj to wywala apke !!!!!!!!!!!
) : AndroidViewModel(app) {


    private val context = getApplication<Application>().applicationContext


    private val fileManager = FileManager()
    private val cloudComm = CloudComm()

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



    private fun addFile(uri: Uri) {
        val currentList = _modelSelectedFiles.value.toMutableList()
        currentList.add(fileManager.toFile(uri, context))

        fileManager.addFile(uri, context)
        cloudComm.uploadFile(fileManager.toFile(uri, context))
//        cloudComm.setURLToFile(currentList.last())

        cloudComm.setURLToFile(currentList.last(), object : UrlCallback {
            override fun onUrlAvailable(url: String) {
                // Tutaj możesz używać uzyskanego URL, na przykład przypisując go do _activeURL
                _activeURL.value = url
                _startServiceEvent.value = url
            }
        })

        _modelSelectedFiles.value = currentList


        //try to set in different way
//        _activeURL.value = uri.toString()

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