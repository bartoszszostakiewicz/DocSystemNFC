package com.docsysnfc.sender

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.docsysnfc.sender.model.CloudComm
import com.docsysnfc.sender.model.File
import com.docsysnfc.sender.model.FileManager
import com.docsysnfc.sender.model.UrlCallback
import com.docsysnfc.sender.model.securityModule.RSAEncryption
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URL
import com.docsysnfc.sender.model.securityModule.SecurityManager

class MainViewModel (
    app: Application,
) : AndroidViewModel(app) {


    private val context = getApplication<Application>().applicationContext

    //chage to dependency injection .);'()
    private val fileManager = FileManager()
    private val cloudComm = CloudComm()
    private val securityManager = SecurityManager(RSAEncryption())

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


   fun addFile(uri: Uri) {

       viewModelScope.launch {
        val currentList = _modelSelectedFiles.value.toMutableList()
        currentList.add(fileManager.toFile(uri, context))


            fileManager.addFile(uri, context, securityManager) // Dodaj plik
            val file = fileManager.getFiles().last()

            if(file.byteArray.size == 0){
                Log.d("TAG123", "Plik jest pusty lub nie udało się wczytać danych.")
            }else{
                Log.d("TAG123", "Plik dadasdassd: ${file.byteArray.size} bajtów")


                cloudComm.uploadFile(file) // Prześlij plik

                val lastFile = currentList.last()
                cloudComm.setURLToFile(lastFile, object : UrlCallback {
                    override fun onUrlAvailable(url: String) {
                        // Użyj uzyskanego URL
                        _activeURL.value = url
                        _startServiceEvent.value = url
                        _modelSelectedFiles.value = currentList
                    }
                })


            }

        }


    }

    fun fileIsInCloud(file: File): Boolean {
        return file.url != URL("https://www.google.com")
    }

    fun chooseFile() {
        _modelSelectedFiles.update { fileManager.getFiles() }
    }



}