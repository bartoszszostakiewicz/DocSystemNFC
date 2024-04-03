package com.docsysnfc.flowtouch

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.nfc.NfcAdapter
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.docsysnfc.R
import com.docsysnfc.flowtouch.model.flowtouchStates.AuthenticationStatus
import com.docsysnfc.flowtouch.model.CloudComm
import com.docsysnfc.flowtouch.model.flowtouchStates.CreateAccountStatus
import com.docsysnfc.flowtouch.model.File
import com.docsysnfc.flowtouch.model.FileManager
import com.docsysnfc.flowtouch.model.flowtouchStates.InternetConnectionStatus
import com.docsysnfc.flowtouch.model.NFCComm
import com.docsysnfc.flowtouch.model.flowtouchStates.NFCStatus
import com.docsysnfc.flowtouch.model.flowtouchStates.NFCSysScreen
import com.docsysnfc.flowtouch.model.SecurityManager
import com.docsysnfc.flowtouch.model.flowtouchStates.UiState
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URL
import java.util.Base64
import javax.crypto.spec.SecretKeySpec

private const val TAG = "NFC1234"

class MainViewModel(
    app: Application,
) : AndroidViewModel(app) {

    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext


    private val fileManager = FileManager()
    private val cloudComm = CloudComm()
    private val securityManager = SecurityManager()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    @SuppressLint("StaticFieldLeak")
    private val nfcComm = NFCComm()


    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()


    init {

        nfcComm.initNFCAdapter(context)
        initFilesFromFirebaseStorage()
        if (auth.currentUser != null) {
            _uiState.update { it.copy(authenticationStatus = AuthenticationStatus.SUCCESS) }
        }

    }

    fun filterSelectedFilesTypes(selectedTypes: SnapshotStateList<String>) {
        _uiState.update { currentState ->
            val filteredFiles = currentState.modelSelectedFiles.filter { file ->
                selectedTypes.contains(file.type)
            }
            currentState.copy(filteredSelectedFiles = filteredFiles)
        }
    }


    fun setAdditionalEncryption(value: Boolean) {
        val newValue = if (value) {
            Toast.makeText(context, context.getString(R.string.additional_encryption_on), Toast.LENGTH_SHORT)
                .show()
            true
        } else {
            Toast.makeText(context, context.getString(R.string.additional_encryption_off), Toast.LENGTH_SHORT)
                .show()
            false
        }
        _uiState.update { it.copy(additionalEncryption = newValue) }
    }


    fun setCloudMirroring(value: Boolean) {
        val newValue = if (value) {
            Toast.makeText(context, context.getString(R.string.cloud_mirroring_on), Toast.LENGTH_SHORT).show()
            true
        } else {
            Toast.makeText(context, context.getString(R.string.cloud_mirroring_off), Toast.LENGTH_SHORT).show()
            false
        }
        _uiState.update { it.copy(cloudMirroring = newValue) }
    }



    private fun markFileAsUploaded(file: File) {
        _uiState.update { currentState ->
            val updatedFiles = currentState.modelSelectedFiles.map {
                if (it == file) it.copy(uploadComplete = true) else it
            }
            currentState.copy(modelSelectedFiles = updatedFiles)
        }
    }


    fun logOff() {
        FirebaseAuth.getInstance().signOut()
        _uiState.update { it.copy(authenticationStatus = AuthenticationStatus.UNKNOWN) }
    }

    fun setFileIsCipher(isCipher: Boolean) {
        val newCipher = if (isCipher) {
            Toast.makeText(context, "Plik jest zaszyfrowany", Toast.LENGTH_SHORT).show()
            true
        } else {
            Toast.makeText(context, "Plik nie jest zaszyfrowany", Toast.LENGTH_SHORT).show()
            false
        }
        _uiState.compareAndSet(
            _uiState.value,
            _uiState.value.copy(fileIsCipher = newCipher)
        )
    }


    fun addFile(uri: Uri) {
        viewModelScope.launch {
            val newFile = fileManager.toFile(uri, context)
            var fileName = newFile.name
            var fileExtension = ""
            val dotIndex = newFile.name.lastIndexOf('.')
            if (dotIndex > 0) {
                fileName = newFile.name.substring(0, dotIndex)
                fileExtension = newFile.name.substring(dotIndex)
            }

            _uiState.update { currentState ->
                val currentList = currentState.modelSelectedFiles.toMutableList()

                var uniqueName = fileName
                var counter = 1
                while (currentList.any { existingFile -> existingFile.name == "$uniqueName$fileExtension" }) {
                    uniqueName = "$fileName${counter++}"
                }

                if (uniqueName != fileName) {
                    newFile.renameTo(uniqueName + fileExtension)
                    Log.d(TAG, "Changing file name to: $uniqueName$fileExtension")
                }

                val isFileAlreadyAdded = currentList.any { existingFile ->
                    existingFile.name == newFile.name
                }

                if (!isFileAlreadyAdded) {
                    currentList.add(newFile)
                }

                if (isFileAlreadyAdded) {
                    Toast.makeText(context, context.getString(R.string.file_already_added), Toast.LENGTH_SHORT).show()
                }

                currentState.copy(modelSelectedFiles = currentList)
            }
        }
    }


    fun fileIsInCloud(file: File): Boolean {
        return file.url != URL("https://www.google.com")
    }


    fun checkNFCStatus() {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
        val newStatus = when {
            nfcAdapter == null -> NFCStatus.NotSupported
            !nfcAdapter.isEnabled -> NFCStatus.Disabled
            else -> NFCStatus.Enabled
        }
        _uiState.update { it.copy(nfcStatus = newStatus) }
    }


    fun signInWithEmailAndPassword(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            _uiState.update { it.copy(authenticationStatus = AuthenticationStatus.FAILURE) }

        } else {
            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                val newAuthStatus = if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    AuthenticationStatus.SUCCESS
                } else {
                    Log.d(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        context,
                        context.getString(R.string.invalid_credentials),
                        Toast.LENGTH_SHORT
                    ).show()
                    AuthenticationStatus.FAILURE
                }

                _uiState.update { it.copy(authenticationStatus = newAuthStatus) }
            }
        }
    }


    fun createAccount(email: String, password: String) {

        val newAuthStatus = if (email.isEmpty() || password.isEmpty()) {
            CreateAccountStatus.FAILURE
        } else {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        Log.d(TAG, "createUserWithEmail:success")
                        CreateAccountStatus.SUCCESS
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", it.exception)
                        CreateAccountStatus.FAILURE
                    }
                }.isSuccessful
            CreateAccountStatus.UNKNOWN
        }
        _uiState.update { it.copy(createAccountStatus = newAuthStatus) }

    }


    fun setNavigationDestination(destination: NFCSysScreen) {
        Log.d(TAG, "setNavigationDestination: $destination")
        _uiState.update { it.copy(navigationDestination = destination) }
    }


    fun enableNFCReaderMode(activity: Activity) {
        nfcComm.enableNFCReader(activity, this)
    }

    fun disableNFCReaderMode(activity: Activity) {
        nfcComm.disableNFCReader(activity)
    }

    fun downloadFile(file: File) {

        Log.d(TAG, "Starting file download: ${file.downloadLink}")

        _uiState.update { it.copy(fileIsDownloading = true) }

        cloudComm.downloadFile(
            downloadDirectory = "${Environment.DIRECTORY_DOWNLOADS}/DocSysNfc/Received",
            downloadLink = file.downloadLink,
            context = context
        ) { fileUri, mimeType ->
            if (fileUri == null) {
                Log.e(TAG, "Failed to download file, fileUri is null.")
                _uiState.update { it.copy(fileIsDownloading = false) }
                return@downloadFile
            }

            Log.d(TAG, "Downloaded file Uri: $fileUri, MIME Type: $mimeType")

            val fileName =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    FileManager.getNameFile(context, fileUri, extension = true).also {
                        Log.d(TAG, "File name (API >= Q): $it")
                    }
                } else {
                    fileUri.lastPathSegment ?: "unknown"
                }


            val fileByteArray =
                FileManager.fileToByteArray(context, fileUri, false) ?: ByteArray(0)

            var fileSize = 0.0

            var cnt = 0


            while (fileSize == 0.0 && cnt < 1000) {
                cnt++
                fileSize =
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        FileManager.getSizeOfFileFromContentUri(context, fileUri).also {
                            Log.d(TAG, "File size (API >= Q): $it MB")
                        }
                    } else {
                        FileManager.getSizeFileFromFileUri(fileUri).also {
                            Log.d(TAG, "File size (API < Q): $it MB")
                        }
                    }
            }


            Log.d(TAG, "Size of downloaded file: $fileSize")

            file.name = fileName
            file.uri = fileUri
            file.byteArray = fileByteArray
            file.size = fileSize
            file.type = mimeType.toString()
            file.url = URL("https://www.google.com")


            val updatedFile = file.copy(
                name = fileName,
                uri = fileUri,
                byteArray = fileByteArray,
                size = fileSize,
                type = mimeType.toString(),
                url = URL("https://www.google.com")
            )



            Log.d(
                "NFC123",
                "Tworzenie obiektu File: ${file.name}, Rozmiar: ${file.size} MB"
            )

            _uiState.update { currentState ->
                currentState.copy(
                    receivesFiles = currentState.receivesFiles + updatedFile,
                    fileIsDownloading = false
                )
            }


            Log.d(TAG, "Aktualizacja otrzymanych plików: ${file.name}")
        }

    }

    suspend fun downloadFile(downloadLink: String, receivesFiles: Boolean = true) {

        val separator = R.string.separator.toString()
        val parts = downloadLink.split(separator)

        Log.d(TAG, "Parts: $parts")
        Log.d(TAG, "Rozpoczęcie pobierania pliku: $downloadLink")

        // Ustawienie stanu 'fileIsDownloading' na true
        _uiState.update { it.copy(fileIsDownloading = true) }

        cloudComm.downloadFile(
            downloadDirectory = "${Environment.DIRECTORY_DOWNLOADS}/DocSysNfc/Received",
            downloadLink = downloadLink,
            context = context
        ) { fileUri, mimeType ->
            if (fileUri == null) {
                Log.e("NFC123", "Nie udało się pobrać pliku, fileUri jest null.")
                _uiState.update { it.copy(fileIsDownloading = false) }
                return@downloadFile
            }

            Log.d(TAG, "Pobrany plik Uri: $fileUri, Typ MIME: $mimeType")

            val fileName =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    FileManager.getNameFile(context, fileUri, extension = true).also {
                        Log.d(TAG, "Nazwa pliku (API >= Q): $it")
                    }
                } else {
                    fileUri.lastPathSegment ?: "unknown"
                }


            val fileByteArray =
                FileManager.fileToByteArray(context, fileUri, false) ?: ByteArray(0)

            var fileSize = 0.0

            var cnt = 0

            Log.d(TAG, "przed: $fileSize")

            Log.d(TAG, "FILE URI: $fileUri")

            while (fileSize == 0.0 && cnt < 1000) {
                cnt++
                fileSize =
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        FileManager.getSizeOfFileFromContentUri(context, fileUri).also {
                            //                    Thread.sleep(10)
                            Log.d(TAG, "Rozmiar pliku (API >= Q): $it MB")
                        }
                    } else {
                        FileManager.getSizeFileFromFileUri(fileUri).also {
                            Log.d(TAG, "Rozmiar pliku (API < Q): $it MB")
                        }
                    }
            }
            Log.d(TAG, "cnt: $cnt")

            Log.d(TAG, "pozyskany rozmiar pliku: $fileSize")


            val downloadedFile = File(
                fileName,
                fileUri,
                downloadLink,
                fileSize,
                mimeType.toString(),
                URL("https://www.google.com"),
                fileByteArray,
                ByteArray(0),
                "",
                "",
                encryptionState = false,

                )

            if (parts.size >= 3) {
                downloadedFile.encryptedByteArray =
                    FileManager.fileToByteArray(context, fileUri, false)!!
                downloadedFile.secretKey = parts[1]
                downloadedFile.iV = parts[2]
                downloadedFile.isCipher = true
            }

            Log.d(
                TAG,
                "Tworzenie obiektu File: ${downloadedFile.name}, Rozmiar: ${downloadedFile.size} MB"
            )
            if (receivesFiles) {
                _uiState.update { currentState ->
                    currentState.copy(
                        receivesFiles = currentState.receivesFiles + downloadedFile,
                        fileIsDownloading = false
                    )
                }
            } else {
                _uiState.update { currentState ->
                    currentState.copy(
                        modelSelectedFiles = currentState.modelSelectedFiles + downloadedFile,
                        fileIsDownloading = false
                    )
                }
            }
            Log.d(TAG, "Aktualizacja otrzymanych plików: ${downloadedFile.name}")
        }
    }

    private fun updateSelectedFiles(newFile: File) {
        _uiState.update { currentState ->
            Log.d(TAG, "Nazwa dodawanego pliku: ${newFile.name}")
            Log.d(TAG, "Uri dodawanego pliku: ${newFile.uri}")
            Log.d(TAG, "DŁUGOŚĆ TABLICY: ${newFile.byteArray.size}")


            val nameWithoutExtension = newFile.name.substringBeforeLast(".")
            val extension = newFile.name.substringAfterLast(".", "")

            var newName = newFile.name
            var counter = 1

            while (currentState.modelSelectedFiles.any { it.name == newName }) {

                newName =
                    "$nameWithoutExtension($counter)${if (extension.isNotEmpty()) ".$extension" else ""}"
                counter++
            }

            if (newName != newFile.name) {
                newFile.name = newName
                Log.d(TAG, "Zmieniono nazwę na: $newName")
            }

            // Aktualizacja plików
            val updatedFiles = currentState.modelSelectedFiles.map { existingFile ->
                if (existingFile.downloadLink == newFile.downloadLink) {

                    Toast.makeText(
                        context,
                        "Informacje o pliku ${newFile.name} zostały zaktualizowane.",
                        Toast.LENGTH_LONG
                    ).show()
                    existingFile.apply {
                        byteArray = newFile.byteArray
                        uri = newFile.uri

                    }
                } else {

                    existingFile
                }
            }

            currentState.copy(modelSelectedFiles = updatedFiles, fileIsDownloading = false)
        }
    }


    private fun updateReceivedFiles(newFile: File) {
        _uiState.update { currentState ->
            Log.d(TAG, "Name of the file being added: ${newFile.name}")
            Log.d(TAG, "URI of the file being added: ${newFile.uri}")

            val nameWithoutExtension = newFile.name.substringBeforeLast(".")
            val extension = newFile.name.substringAfterLast(".", "")

            var newName = newFile.name
            var counter = 1

            while (currentState.receivesFiles.any { it.name == newName }) {
                newName =
                    "$nameWithoutExtension($counter)${if (extension.isNotEmpty()) ".$extension" else ""}"
                counter++
            }


            if (newName != newFile.name) {
                newFile.name = newName
                Log.d(TAG, "Renamed to: $newName")
            }


            val updatedFiles =
                if (currentState.receivesFiles.any { it.downloadLink == newFile.downloadLink }) {
                    Toast.makeText(
                        context,
                        "Plik ${newFile.name} jest już otrzymany.",
                        Toast.LENGTH_LONG
                    ).show()
                    currentState.receivesFiles.map { existingFile ->
                        if (existingFile.downloadLink == newFile.downloadLink) {

                            Toast.makeText(
                                context,
                                "Informacje o pliku ${newFile.name} zostały zaktualizowane.",
                                Toast.LENGTH_LONG
                            ).show()

                            existingFile.apply {
                                name = newFile.name
                                size = newFile.size
                                byteArray = newFile.byteArray
                                uri = newFile.uri
                                type = newFile.type
                            }
                        } else {

                            existingFile
                        }
                    }
                } else {

                    currentState.receivesFiles + newFile
                }

            currentState.copy(receivesFiles = updatedFiles, fileIsDownloading = false)
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
                context.getString(R.string.no_app_info),
                Toast.LENGTH_LONG
            ).show()
        }
    }


    fun setDownloadStatus(b: Boolean) {
        _uiState.update { it.copy(fileIsDownloading = b) }
    }


    fun handleEncryption(file: File, encryption: Boolean, reading: Boolean = false) {
        viewModelScope.launch {

            Log.d(TAG, "Plik ${file.name}: ${file.byteArray.size} bajtów")


            if (encryption && file.encryptedByteArray.isEmpty()) {
                val startTime = System.currentTimeMillis()

                file.encryptionState = true

                val encryptedPack = securityManager.encryptDataAES(file.byteArray)

                file.encryptedByteArray = encryptedPack.first
                file.secretKey = Base64.getEncoder().encodeToString(encryptedPack.second.encoded)
                file.iV = Base64.getEncoder().encodeToString(encryptedPack.third)

                Log.d(TAG, "Len of encrypted data: ${file.encryptedByteArray.size}")
                Log.d(TAG, "First ten bytes of encrypted data: ${file.encryptedByteArray.take(10)}")
                Log.d(TAG, "IV LEN ${file.iV.length} ")
                Log.d(TAG, "Data LEN  = ${file.encryptedByteArray.size}")
                Log.d(TAG, "IV = ${file.iV.toString()}")
                Log.d(TAG, "${file.iV} ${file.secretKey}  ${file.encryptedByteArray}")


                _uiState.update { it.copy(publicKey = "") }

                if (_uiState.value.publicKey.isNotEmpty()) {
                    Log.d(TAG, "Public key: ${_uiState.value.publicKey}")
                } else {
                    Log.d(TAG, "Public key is empty")
                }

                file.encryptionState = false
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime
                Log.d(TAG, "The duration of the encryption operation.: $duration ms")

            } else if (reading && file.encryptedByteArray.isNotEmpty()) {

                val startTime = System.currentTimeMillis()

                file.encryptionState = true


                var decodedKey = Base64.getDecoder().decode(file.secretKey)
                Log.d(TAG, "${file.iV} ${file.secretKey}  ${file.encryptedByteArray}")



                if (decodedKey.size == 256) {
                    securityManager.decryptDataRSA(
                        decodedKey,
                        auth.currentUser?.email.toString()
                    ) { decryptedKey ->
                        Log.d(TAG, "decryptet Key ${decryptedKey}")
                        val base64String = Base64.getEncoder().encodeToString(decryptedKey)
                        Log.d(TAG, "decrypted Key: $base64String")

                        decodedKey = decryptedKey
                    }
                }

                if (decodedKey.size == 32) {

                    val originalKey = SecretKeySpec(decodedKey, 0, decodedKey.size, "AES")
                    val decodedData = file.encryptedByteArray
                    val decodedIV = Base64.getDecoder().decode(file.iV)
                    Log.d(TAG, "Key is not encrypted")

                    Log.d(TAG, "Len of encrypted data: ${file.encryptedByteArray.size}")
                    Log.d(
                        TAG,
                        "First ten bytes of encrypted data: ${file.encryptedByteArray.take(10)}"
                    )
                    Log.d(TAG, "IV LEN ${file.iV.length} ")
                    Log.d(TAG, "IV = ${file.iV}")
                    Log.d(TAG, "${file.iV} ${file.secretKey}  ${file.encryptedByteArray}")


                    val decryptedData =
                        securityManager.decryptDataAES(decodedData, originalKey, decodedIV)


                    file.byteArray = decryptedData


                    val tmpFile = FileManager.byteArrayToFile(context, file.byteArray, file.name)
                    FileManager.deleteFile(file, context)


                    if (tmpFile.name.isNotEmpty()) {
                        file.name = tmpFile.name
                        file.uri = tmpFile.uri
                        file.type = tmpFile.type
                        file.size = file.size

                    }

                    file.encryptionState = false
                    val endTime = System.currentTimeMillis()
                    val duration = endTime - startTime
                    Log.d(TAG, "Czas operacji deszyfrowania: $duration ms")

                } else {
                    Log.d(TAG, "Invalid key $decodedKey")
                }
            }
        }
    }


    fun pushFileIntoCloud(file: File, cipher: Boolean) {

        viewModelScope.launch {

            Log.d(TAG, "File ${file.name}: ${file.byteArray.size} bytes")
            file.isUploading = true
            cloudComm.uploadFile(file, cipher, onUrlAvailable = { url ->
                val newNdefMessage = if (cipher) {
                    "$url${R.string.separator}${file.secretKey}${R.string.separator}${file.iV}${R.string.separator}"
                } else {
                    url
                }

                _uiState.update { currentState ->
                    currentState.copy(
                        ndefMessage = newNdefMessage,
                        uploadComplete = true
                    )
                }
                file.isUploading = false
                markFileAsUploaded(file)
            })

        }
    }

    fun setNdefMessage(ndefMessage: String) {
        _uiState.update { it.copy(ndefMessage = R.string.separator.toString() + ndefMessage + R.string.separator.toString()) }
    }


    fun deleteReceivedFile(file: File) {
        viewModelScope.launch {
            FileManager.deleteFile(file, context)

            _uiState.update { currentState ->
                val updatedFiles = currentState.receivesFiles.filter { it != file }
                currentState.copy(receivesFiles = updatedFiles)
            }
        }
    }


    fun deleteSelectedFile(file: File) {
        viewModelScope.launch {

            if (file.url != URL("https://www.google.com")) {
                cloudComm.deleteFile(file)
            }

            _uiState.update { currentState ->
                val updatedFiles = currentState.modelSelectedFiles.filter { it != file }
                currentState.copy(modelSelectedFiles = updatedFiles)
            }
        }
    }


    fun resetUploadComplete() {
        _uiState.update { it.copy(uploadComplete = false) }
    }

    fun reauthenticateAndDelete(password: String, onResult: (Boolean) -> Unit) {
        val credential =
            EmailAuthProvider.getCredential(auth.currentUser?.email.toString(), password)
        auth.currentUser?.reauthenticate(credential)
            ?.addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    Log.d(TAG, "Re-authentication successful")
                    deleteAccount()
                    onResult(true)
                } else {
                    Log.d(TAG, "Re-authentication failed")
                    onResult(false)
                }
            }
    }


    private fun deleteAccount() {

        auth.currentUser?.delete()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "deleteAccount:success")
            } else if (task.exception is FirebaseAuthRecentLoginRequiredException) {
                Log.d(TAG, "deleteAccount:reauthenticate")
            } else {
                Log.d(TAG, "deleteAccount:failure")

            }
        }
    }


    private fun initFilesFromFirebaseStorage() {
        viewModelScope.launch {
            cloudComm.getFilesList(
                onResult = { files ->
                    files.forEach { newFile ->
                        addFileToList(newFile)
                    }
                },
                onError = { exception ->
                    Log.d(TAG, "Error while fetching files: $exception")
                }
            )
        }
    }

    private fun addFileToList(newFile: File) {
        _uiState.update { currentState ->
            val currentList = currentState.modelSelectedFiles.toMutableList()

            if (currentList.any { it.name == newFile.name && it.url.toString() == newFile.url.toString() }) {
                Log.d(TAG, "File already exists in the list: ${newFile.name}")
                currentState
            } else {
                currentList.add(newFile)
                currentState.copy(modelSelectedFiles = currentList)
            }
        }
    }

    fun filterSelectedFilesSize(value: ClosedFloatingPointRange<Float>) {
        _uiState.update { currentState ->
            val filteredFiles = currentState.modelSelectedFiles.filter { file ->
                file.size in value
            }
            currentState.copy(filteredSelectedFiles = filteredFiles)
        }
    }

    fun checkKey() {
        securityManager.checkKey(auth.currentUser?.email.toString())
    }

    fun toggleEncryption() {
        _uiState.update { currentState ->
            currentState.copy(additionalEncryption = !currentState.additionalEncryption)
        }
    }


    fun getPublicKey(): String {
        return securityManager.getPublicKey(auth.currentUser?.email.toString())

    }


    private fun encryptDataRSA(data: ByteArray, publicKey: String, callback: (ByteArray) -> Unit) {
        securityManager.encryptDataRSA(data, publicKey) { encryptedData ->
            callback(encryptedData)
        }
    }

    fun toggleMirroring() {
        _uiState.update { currentState ->
            currentState.copy(cloudMirroring = !currentState.cloudMirroring)
        }
    }


    private fun validatePublicKey(publicKey: String): Boolean {
        return securityManager.validatePublicKey(publicKey)
    }


    fun checkInternetStatus(context: Context) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        val isConnected = activeNetwork?.isConnectedOrConnecting == true

        _uiState.update { it.copy(internetConnStatus = if (isConnected) InternetConnectionStatus.CONNECTED else InternetConnectionStatus.DISCONNECTED) }

    }


    fun processNFCData(response: ByteArray?) {

        if (response != null) {
            val payloadString = response.toString(Charsets.UTF_8)
            Log.d(TAG, "Read NFC data: $payloadString")

            viewModelScope.launch {
                if (payloadString.contains("https")) {
                    val url = "https" + payloadString.substringAfter("https")
                    downloadFile(url, receivesFiles = true)
                } else {
                    try {
                        val parts = payloadString.split(R.string.separator.toString())
                        val newPublicKey = parts[1]
                        if (validatePublicKey(newPublicKey)) {
                            Log.d(TAG, "Valid public key detected")
                            _uiState.update {
                                it.copy(
                                    publicKey = newPublicKey,
                                    publicKeyOwner = parts[2]
                                )
                            }
                        } else {
                            Log.d(TAG, "Invalid public key")
                        }

                        Log.d(TAG, "Public key: ${parts[1]}")
                        Log.d(TAG, "Public key owner: ${parts[2]}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error while processing NFC data: $e")
                    }
                }
            }
        }

    }

    fun cipherSessionKey(file: File) {

        val sessionKey = file.secretKey
        val publicKey = uiState.value.publicKey


        val sessionKeyByteArray = Base64.getDecoder().decode(sessionKey)


        if (sessionKey.isNotEmpty() && publicKey.isNotEmpty()) {
            viewModelScope.launch {
                encryptDataRSA(sessionKeyByteArray, publicKey) { encryptedData ->
                    val encryptedSessionKey = Base64.getEncoder().encodeToString(encryptedData)
//                    Log.d(TAG, "Encrypted session key: $encryptedSessionKey")
                    file.secretKey = encryptedSessionKey

                    val newMSG =
                        (file.url.toString() + R.string.separator + encryptedSessionKey + R.string.separator + file.iV)
                    Log.d(TAG, newMSG)
                    setNdefMessage(newMSG)
                }
            }
        }

    }

}










