package com.docsysnfc.sender

import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.docsysnfc.sender.model.AuthenticationState
import com.docsysnfc.R
import com.docsysnfc.sender.model.CloudComm
import com.docsysnfc.sender.model.CreateAccountState
import com.docsysnfc.sender.model.File
import com.docsysnfc.sender.model.FileManager
import com.docsysnfc.sender.model.NFCComm
import com.docsysnfc.sender.model.NFCStatus
import com.docsysnfc.sender.model.NFCSysScreen
import com.docsysnfc.sender.model.UrlCallback
import com.docsysnfc.sender.model.securityModule.RSAEncryption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URL
import com.docsysnfc.sender.model.securityModule.SecurityManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.StateFlow
import java.util.Base64
import javax.crypto.spec.SecretKeySpec

class MainViewModel(
    app: Application,
) : AndroidViewModel(app) {


    private val context = getApplication<Application>().applicationContext

    //change to dependency injection .);'()
    private val fileManager = FileManager()
    private val cloudComm = CloudComm()
    private val securityManager = SecurityManager(RSAEncryption())
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val nfcComm = NFCComm()


    private val _fileIsCipher = MutableStateFlow(false)
    val fileIsCipher = _fileIsCipher.asStateFlow()


    private val _fileIsDownloading = MutableStateFlow(false)
    val fileIsDownloading = _fileIsDownloading.asStateFlow()


    //required for having the latest selected files
    private val _receivesFiles = MutableStateFlow<List<File>>(emptyList())
    val receiveFiles = _receivesFiles.asStateFlow()


    //its required for having the latest selected files but i think its duplicated with fileManager.getFiles() it will be checked
    private val _modelSelectedFiles = MutableStateFlow<List<File>>(emptyList())
    val modelSelectedFiles = _modelSelectedFiles.asStateFlow()


    //its required for nfc status is available or not
    private val _nfcStatus = MutableStateFlow(NFCStatus.Unknown)
    val nfcStatus = _nfcStatus.asStateFlow()


    //probalbly to delete
    private val _activeURL = MutableStateFlow("google.com")
    val activeURL = _activeURL.asStateFlow()




    //probalby to delete
    private val _isActivityVisible = MutableStateFlow(true)
    val isActivityVisible = _isActivityVisible.asStateFlow()


    //it is required
    private val _authenticationState = MutableStateFlow(AuthenticationState.UNKNOWN)
    val authenticationState = _authenticationState.asStateFlow()


    //required for firebase
    private val _createAccountState = MutableStateFlow(CreateAccountState.UNKNOWN)
    val createAccountState = _createAccountState.asStateFlow()

    //required for having the latest nfc tag
    private var _nfcTag = MutableStateFlow<Tag?>(null)
    val nfcTag: StateFlow<Tag?> = _nfcTag.asStateFlow()


    val isUserAuthenticated: Boolean
        get() = auth.currentUser != null

    init {
        //every 1 second we are checking the nfc status
        viewModelScope.launch {
            while (true) {
                Log.d("NFC1235", "Active url =  ${_activeURL.value}")
                kotlinx.coroutines.delay(1000)
            }
        }
        _authenticationState.value =
            if (isUserAuthenticated) AuthenticationState.SUCCESS else AuthenticationState.FAILURE

        nfcComm.initNFCAdapter(context)
    }

    fun setActivityVisibility(isVisible: Boolean) {
        _isActivityVisible.value = isVisible
    }



    fun setFileIsCipher(isCipher: Boolean) {
        _fileIsCipher.value = isCipher
    }


    fun addFile(uri: Uri) {
        viewModelScope.launch {
            val currentList = _modelSelectedFiles.value.toMutableList()
            val newFile = fileManager.toFile(uri, context)

            var fileName = newFile.name
            var fileExtension = ""
            val dotIndex = newFile.name.lastIndexOf('.')
            if (dotIndex > 0) {
                fileName = newFile.name.substring(0, dotIndex)
                fileExtension = newFile.name.substring(dotIndex)
            }

            var uniqueName = fileName
            var counter = 1
            while (currentList.any { existingFile -> existingFile.name == "$uniqueName$fileExtension" }) {
                uniqueName = "$fileName${counter++}"
            }

            if (uniqueName != fileName) {
                newFile.renameTo(uniqueName + fileExtension)
                Log.d("TAG123", "Zmiana nazwy pliku na: $uniqueName$fileExtension")
            }

            val isFileAlreadyAdded = currentList.any { existingFile ->
                existingFile.name == newFile.name
            }

            if (isFileAlreadyAdded) {
                Toast.makeText(context, "Plik już dodany", Toast.LENGTH_SHORT).show()
            } else {
                currentList.add(newFile)
                _modelSelectedFiles.value = currentList

            }
        }
    }


    fun fileIsInCloud(file: File): Boolean {
        return file.url != URL("https://www.google.com")
    }


    fun checkNFCStatus() {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(this.context)
        _nfcStatus.value = when {
            nfcAdapter == null -> NFCStatus.NotSupported
            !nfcAdapter.isEnabled -> NFCStatus.Disabled
            else -> NFCStatus.Enabled
        }
    }


    fun signInWithEmailAndPassword(email: String, password: String) {

        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d("qwertty", "1signInWithEmail:success")
                _authenticationState.value = AuthenticationState.SUCCESS
            } else {
                Log.w("qwertty", "signInWithEmail:failure", it.exception)
                _authenticationState.value = AuthenticationState.FAILURE
            }
        }.isSuccessful


    }

    fun createAccount(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.d("qwertty", "createUserWithEmail:success")
                    _createAccountState.value = CreateAccountState.SUCCESS
                } else {
                    Log.w("qwertty", "createUserWithEmail:failure", it.exception)
                    _createAccountState.value = CreateAccountState.FAILURE
                }
            }.isSuccessful
    }

    private val _nfcData = MutableStateFlow<String>("")
    val nfcData: StateFlow<String> = _nfcData

    fun updateNfcData(data: String?) {
        if (data != null) {
            _nfcData.value = data
        }
    }

    private val _navigationDestination = MutableStateFlow(NFCSysScreen.Home)
    val navigationDestination = _navigationDestination

    fun setNavigationDestination(destination: NFCSysScreen) {
        Log.d("NFC123", "setNavigationDestination: $destination")
        _navigationDestination.value = destination
    }


    fun setNFCTag(tag: Tag) {
        _nfcTag.value = tag
    }

    fun clearNFCTag() {
        _nfcTag.value = null
    }


    fun enableNFCReaderMode(activity: Activity) {
        nfcComm.enableNFCReader(activity, this)
    }

    fun disableNFCReaderMode(activity: Activity) {
        nfcComm.disableNFCReader(activity)
    }


    suspend fun downloadFile(downloadLink: String, context: Context) {

        val separator = R.string.separator.toString()
        val parts = downloadLink.split(separator)

        Log.d("NFC123", "Parts: $parts")

        Log.d("NFC123", "Rozpoczęcie pobierania pliku: $downloadLink")
        _fileIsDownloading.value = true

        cloudComm.downloadFile(downloadLink, context) { fileUri, mimeType ->
            if (fileUri == null) {
                Log.e("NFC123", "Nie udało się pobrać pliku, fileUri jest null.")
                _fileIsDownloading.value = false
                return@downloadFile
            }

            //po pobraniu pliku wez odczytuaj z jego pierwszych bajtow rozszerzenie
            //jesli rozszerzenie jest puste(czyli nie do odczytania)O to znaczy ze plik jest zaszyfrowany
//
//            val extension = FileManager.getExtensionFromByteArray(context, fileUri)
//
//            val mimeType_ = if (extension.isNotEmpty()) {
//                FileManager.getMimeTypeFromExtension(extension)
//            } else {
//                "application/octet-stream"
//            }

            Log.d("NFC123", "Pobrany plik Uri: $fileUri, Typ MIME: $mimeType")

            val fileName =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    FileManager.getNameFile(context, fileUri, extension = true).also {
                        Log.d("NFC123", "Nazwa pliku (API >= Q): $it")
                    }
                } else {
                    fileUri.lastPathSegment ?: "unknown"
                }


            val fileByteArray = FileManager.fileToByteArray(context, fileUri,false) ?: ByteArray(0)

            var fileSize = 0.0

            var cnt = 0

            Log.d("NFC123", "przed: $fileSize")

            Log.d("NFC123", "FILE URI: $fileUri")

            while (fileSize == 0.0 && cnt < 1000) {
                cnt++
                fileSize =
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        FileManager.getSizeOfFileFromContentUri(context, fileUri).also {
                            //                    Thread.sleep(10)
                            Log.d("NFC123", "Rozmiar pliku (API >= Q): $it MB")
                        }
                    } else {
                        FileManager.getSizeFileFromFileUri(fileUri).also {
                            Log.d("NFC123", "Rozmiar pliku (API < Q): $it MB")
                        }
                    }
            }
            Log.d("NFC123", "cnt: $cnt")

            Log.d("NFC123", "pozyskany rozmiar pliku: $fileSize")


            var downloadedFile = File(
                fileName,
                fileUri,
                downloadLink,
                fileSize,
                mimeType.toString(),
                URL("https://www.google.com"),
                fileByteArray,//change t byte array in filemanager is a function to convert file to byte array
            )

            if(parts.size == 3){
                downloadedFile.encryptedByteArray = FileManager.fileToByteArray(context, fileUri,false)!!
                downloadedFile.secretKey = parts[1]
                downloadedFile.iV = parts[2]
            }

            Log.d(
                "NFC123",
                "Tworzenie obiektu File: ${downloadedFile.name}, Rozmiar: ${downloadedFile.size} MB"
            )

            updateReceivedFiles(downloadedFile)
            Log.d("NFC123", "Aktualizacja otrzymanych plików: ${downloadedFile.name}")
        }
    }


    private fun updateReceivedFiles(newFile: File) {
        _fileIsDownloading.value = false

        _receivesFiles.update { currentFiles ->
            Log.d("NFC123", "Nazwa dodawanego pliku: ${newFile.name}")
            Log.d("NFC123", "Uri dodawanego pliku: ${newFile.uri}")

            // Oddzielenie nazwy pliku od rozszerzenia
            val nameWithoutExtension = newFile.name.substringBeforeLast(".")
            val extension = newFile.name.substringAfterLast(".", "")

            var newName = newFile.name
            var counter = 1
            // Sprawdzenie kolizji nazw
            while (currentFiles.any { it.name == newName }) {
                // Jeżeli istnieje kolizja nazw, dodaj numer do nazwy pliku
                newName =
                    "$nameWithoutExtension($counter)${if (extension.isNotEmpty()) ".$extension" else ""}"
                counter++
            }

            // Sprawdzenie, czy nazwa pliku została zmieniona
            if (newName != newFile.name) {
                newFile.name = newName // Ustawienie nowej nazwy pliku
                Log.d("NFC123", "Zmieniono nazwę na: $newName")
            }

            // Sprawdzenie, czy plik z takim samym URI lub linkiem do pobrania już istnieje
            if (currentFiles.any { it.downloadLink == newFile.downloadLink }) {

                Toast.makeText(
                    context,
                    "Plik ${newFile.name} jest już otrzymany.",
                    Toast.LENGTH_LONG
                ).show()
                currentFiles
            } else {
                // Dodanie nowego pliku do listy
                currentFiles + newFile
            }
        }
    }


    fun openFile(context: Context, fileUri: Uri, mimeType: String) {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            openFileForQAndAbove(context, fileUri, mimeType)
        } else {
            openFileForPreQ(context, fileUri, mimeType)
        }

    }

    private fun openFileForQAndAbove(context: Context, fileUri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(fileUri, mimeType)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    }


    private fun openFileForPreQ(context: Context, fileUri: Uri, mimeType: String) {

        val newUri = FileProvider.getUriForFile(
            context,
            "com.docsysnfc.provider",
            java.io.File(fileUri.path!!)
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(newUri, mimeType)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "Nie znaleziono aplikacji do otwarcia tego typu pliku.",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    fun setDownloadStatus(b: Boolean) {
        _fileIsDownloading.value = b
    }

    //zakladamy ze bytearray jest wypelnione
    fun handleEncryption(file: File, encryption: Boolean,reading:Boolean = false) {
        viewModelScope.launch {
            Log.d("TAG123", "Plik ${file.name}: ${file.byteArray.size} bajtów")

            if (encryption && file.encryptedByteArray.isEmpty()) {
                val startTime = System.currentTimeMillis()
                file.encryption = true

                val encryptedPack = securityManager.encryptDataAES(file.byteArray)
                file.encryptedByteArray = encryptedPack.first
                file.secretKey = Base64.getEncoder().encodeToString(encryptedPack.second.encoded)
                file.iV = Base64.getEncoder().encodeToString(encryptedPack.third)

                Log.d("NFC1234", "Len of encrypted data: ${file.encryptedByteArray.size}")
                Log.d("NFC1234", "First ten bytes of encrypted data: ${file.encryptedByteArray.take(10)}")
                Log.d("NFC1234", "IV LEN ${file.iV.length} ")
                Log.d("NFC1234","Data LEN  = ${file.encryptedByteArray.size}")
                Log.d("NFC1234","IV = ${file.iV.toString()}")
                Log.d("NFC1234","${file.iV} ${file.secretKey}  ${file.encryptedByteArray}")

                file.encryption = false
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime
                Log.d("NFC1234", "Czas operacji szyfrowania: $duration ms")

            } else if (reading && file.encryptedByteArray.isNotEmpty()) {
                val startTime = System.currentTimeMillis()
                file.encryption = true
                val decodedKey = Base64.getDecoder().decode(file.secretKey)
                val originalKey = SecretKeySpec(decodedKey,0, decodedKey.size, "AES")
                val decodedData = file.encryptedByteArray
                val decodedIV = Base64.getDecoder().decode(file.iV)


                Log.d("NFC1234", "Len of encrypted data: ${file.encryptedByteArray.size}")
                Log.d("NFC1234", "First ten bytes of encrypted data: ${file.encryptedByteArray.take(10)}")
                Log.d("NFC1234", "IV LEN ${file.iV.length} ")
                Log.d("NFC1234","IV = ${file.iV}")
                Log.d("NFC1234","${file.iV} ${file.secretKey}  ${file.encryptedByteArray}")

                //tutaj muszisz poinformować o tym ze plik zostal odszyfrowany moze callback
                val decryptedData = securityManager.decryptDataAES(decodedData, originalKey, decodedIV)


                file.byteArray = decryptedData

                //now we need override file in downloads loacation on phone
                val tmpFile = FileManager.byteArrayToFile(context, file.byteArray, file.name)

                if(tmpFile.name.isNotEmpty()){
                    //zastanow sie co tu masz nadpisaiwac
                    file.name = tmpFile.name
                    file.uri = tmpFile.uri
                    file.type = tmpFile.type
                    file.size = file.size

                }


                file.encryption = false
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime
                Log.d("NFC1234", "Czas operacji deszyfrowania: $duration ms")
            }

        }

    }

    private val _uploadComplete = MutableStateFlow(false)
    val uploadComplete = _uploadComplete.asStateFlow()
    fun pushFileIntoCloud(file: File, cipher: Boolean) {
        viewModelScope.launch {

            Log.d("TAG123", "Plik ${file.name}: ${file.byteArray.size} bajtów")
            cloudComm.uploadFile(file, cipher,  onUrlAvailable = { url ->
                if (cipher) {
                    _activeURL.value = (url + R.string.separator + file.secretKey + R.string.separator + file.iV)
                } else {
                    _activeURL.value = url
                }
                _uploadComplete.value = true
            })

        }
    }



    fun deleteReceivedFile(file: File) {
        viewModelScope.launch {
            FileManager.deleteFile(file, context)
            _receivesFiles.update { currentFiles ->
                currentFiles.filter { it != file }
            }
        }
    }

    fun deleteSelectedFile(file: File) {
        viewModelScope.launch {
            //delete from cloud
            if (file.url != URL("https://www.google.com")) {
                cloudComm.deleteFile(file)
            }
           // FileManager.deleteFile(file, context)
            _modelSelectedFiles.update { currentFiles ->
                currentFiles.filter { it != file }
            }
        }
    }

    fun resetUploadComplete() {
        _uploadComplete.value = false
    }


}




