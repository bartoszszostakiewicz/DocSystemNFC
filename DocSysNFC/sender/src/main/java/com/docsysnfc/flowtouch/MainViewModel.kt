package com.docsysnfc.flowtouch

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


    private val context = getApplication<Application>().applicationContext


    private val fileManager = FileManager()
    private val cloudComm = CloudComm()
    private val securityManager = SecurityManager()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val nfcCommm = NFCComm()


    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()


    fun filterSelectedFilesTypes(selectedTypes: SnapshotStateList<String>) {
//        _filteredSelectedFiles.value = _modelSelectedFiles.value.filter { file ->
//            selectedTypes.contains(file.type)
//        }
    }


    fun setAdditionalEncryption(value: Boolean) {
        val newValue = if (value) {
            Toast.makeText(context, "Dodatkowe szyfrowanie jest włączone", Toast.LENGTH_SHORT)
                .show()
            true
        } else {
            Toast.makeText(context, "Dodatkowe szyfrowanie jest wyłączone", Toast.LENGTH_SHORT)
                .show()
            false
        }
        _uiState.update { it.copy(additionalEncryption = newValue) }
    }

//    private val _cloudMirroring = MutableStateFlow(false)
//    val cloudMirroring = _cloudMirroring.asStateFlow()

    fun setCloudMirroring(value: Boolean) {
        val newValue = if (value) {
            Toast.makeText(context, "Cloud mirroring is enabled", Toast.LENGTH_SHORT).show()
            true
        } else {
            Toast.makeText(context, "Cloud mirroring is disabled", Toast.LENGTH_SHORT).show()
            false
        }
        _uiState.update { it.copy(cloudMirroring = newValue) }
    }


    init {

        nfcCommm.initNFCAdapter(context)
        initFilesFromFirebaseStorage()
        if(auth.currentUser != null){
            _uiState.update { it.copy(authenticationStatus = AuthenticationStatus.SUCCESS) }
        }

    }

    fun markFileAsUploaded(file: File) {
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
                    Log.d(TAG, "Zmiana nazwy pliku na: $uniqueName$fileExtension")
                }

                val isFileAlreadyAdded = currentList.any { existingFile ->
                    existingFile.name == newFile.name
                }

                if (!isFileAlreadyAdded) {
                    currentList.add(newFile)
                }

                if (isFileAlreadyAdded) {
                    Toast.makeText(context, "Plik już dodany", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(context, context.getString(R.string.invalid_credentials), Toast.LENGTH_SHORT).show()
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
        nfcCommm.enableNFCReader(activity, this)
    }

    fun disableNFCReaderMode(activity: Activity) {
        nfcCommm.disableNFCReader(activity)
    }

    suspend fun downloadFile(file: File) {

        Log.d(TAG, "Rozpoczęcie pobierania pliku: ${file.downloadLink}")

        // Ustawienie stanu 'fileIsDownloading' na true
        _uiState.update { it.copy(fileIsDownloading = true) }

        cloudComm.downloadFile(
            downloadDirectory = "${Environment.DIRECTORY_DOWNLOADS}/DocSysNfc/Received",
            downloadLink = file.downloadLink,
            context = context
        ) { fileUri, mimeType ->
            if (fileUri == null) {
                Log.e(TAG, "Nie udało się pobrać pliku, fileUri jest null.")
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
                "NFC123",
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

            // Oddzielenie nazwy pliku od rozszerzenia
            val nameWithoutExtension = newFile.name.substringBeforeLast(".")
            val extension = newFile.name.substringAfterLast(".", "")

            var newName = newFile.name
            var counter = 1
            // Sprawdzenie kolizji nazw
            while (currentState.modelSelectedFiles.any { it.name == newName }) {
                // Jeżeli istnieje kolizja nazw, dodaj numer do nazwy pliku
                newName =
                    "$nameWithoutExtension($counter)${if (extension.isNotEmpty()) ".$extension" else ""}"
                counter++
            }

            // Sprawdzenie, czy nazwa pliku została zmieniona
            if (newName != newFile.name) {
                newFile.name = newName // Ustawienie nowej nazwy pliku
                Log.d(TAG, "Zmieniono nazwę na: $newName")
            }

            // Aktualizacja plików
            val updatedFiles = currentState.modelSelectedFiles.map { existingFile ->
                if (existingFile.downloadLink == newFile.downloadLink) {
                    // Plik istnieje, więc zaktualizuj jego informacje
                    Toast.makeText(
                        context,
                        "Informacje o pliku ${newFile.name} zostały zaktualizowane.",
                        Toast.LENGTH_LONG
                    ).show()
                    // Zaktualizuj tylko potrzebne pola
                    existingFile.apply {
                        byteArray = newFile.byteArray
                        uri = newFile.uri
                        // Możesz dodać więcej pól do aktualizacji
                    }
                } else {
                    // Plik nie wymaga aktualizacji
                    existingFile
                }
            }

            currentState.copy(modelSelectedFiles = updatedFiles, fileIsDownloading = false)
        }
    }


    private fun updateReceivedFiles(newFile: File) {
        _uiState.update { currentState ->
            Log.d(TAG, "Nazwa dodawanego pliku: ${newFile.name}")
            Log.d(TAG, "Uri dodawanego pliku: ${newFile.uri}")

            // Oddzielenie nazwy pliku od rozszerzenia
            val nameWithoutExtension = newFile.name.substringBeforeLast(".")
            val extension = newFile.name.substringAfterLast(".", "")

            var newName = newFile.name
            var counter = 1
            // Sprawdzenie kolizji nazw
            while (currentState.receivesFiles.any { it.name == newName }) {
                // Jeżeli istnieje kolizja nazw, dodaj numer do nazwy pliku
                newName =
                    "$nameWithoutExtension($counter)${if (extension.isNotEmpty()) ".$extension" else ""}"
                counter++
            }

            // Sprawdzenie, czy nazwa pliku została zmieniona
            if (newName != newFile.name) {
                newFile.name = newName // Ustawienie nowej nazwy pliku
                Log.d(TAG, "Zmieniono nazwę na: $newName")
            }

            // Sprawdzenie, czy plik z takim samym URI lub linkiem do pobrania już istnieje
            val updatedFiles =
                if (currentState.receivesFiles.any { it.downloadLink == newFile.downloadLink }) {
                    Toast.makeText(
                        context,
                        "Plik ${newFile.name} jest już otrzymany.",
                        Toast.LENGTH_LONG
                    ).show()
                    currentState.receivesFiles.map { existingFile ->
                        if (existingFile.downloadLink == newFile.downloadLink) {
                            // Plik istnieje, więc zaktualizuj jego informacje
                            Toast.makeText(
                                context,
                                "Informacje o pliku ${newFile.name} zostały zaktualizowane.",
                                Toast.LENGTH_LONG
                            ).show()
                            // Zaktualizuj tylko potrzebne pola
                            existingFile.apply {
                                name = newFile.name
                                size = newFile.size
                                byteArray = newFile.byteArray
                                uri = newFile.uri
                                type = newFile.type
                            }
                        } else {
                            // Plik nie wymaga aktualizacji
                            existingFile
                        }
                    }
                } else {
                    // Dodanie nowego pliku do listy
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
                "Nie znaleziono aplikacji do otwarcia tego typu pliku.",
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

                if (_uiState.value.publicKey.isNotEmpty()) {
                    Log.d(TAG, "public key: ${_uiState.value.publicKey}")
                }

                file.encryptedByteArray = encryptedPack.first
                file.secretKey = Base64.getEncoder().encodeToString(encryptedPack.second.encoded)
                file.iV = Base64.getEncoder().encodeToString(encryptedPack.third)

                Log.d(TAG, "Len of encrypted data: ${file.encryptedByteArray.size}")
                Log.d(TAG, "First ten bytes of encrypted data: ${file.encryptedByteArray.take(10)}")
                Log.d(TAG, "IV LEN ${file.iV.length} ")
                Log.d(TAG, "Data LEN  = ${file.encryptedByteArray.size}")
                Log.d(TAG, "IV = ${file.iV.toString()}")
                Log.d(TAG, "${file.iV} ${file.secretKey}  ${file.encryptedByteArray}")


                //reset public key
                _uiState.update { it.copy(publicKey = "") }

                if (_uiState.value.publicKey.isNotEmpty()) {
                    Log.d(TAG, "public key: ${_uiState.value.publicKey}")
                } else {
                    Log.d(TAG, "public key is empty")
                }

                file.encryptionState = false
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime
                Log.d(TAG, "Czas operacji szyfrowania: $duration ms")

            } else if (reading && file.encryptedByteArray.isNotEmpty()) {
                val startTime = System.currentTimeMillis()
                file.encryptionState = true


                val decodedKey = Base64.getDecoder().decode(file.secretKey)
                Log.d(TAG, "${file.iV} ${file.secretKey}  ${file.encryptedByteArray}")


                val public = securityManager.getPublicKey(auth.currentUser?.email.toString())
//                val privateKey = securityManager.getPrivateKey(context ,auth.currentUser?.email.toString())

                Log.d(TAG, "public key: $public")
//                Log.d("NFC1234", "private key: $privateKey")
                val arrayDecodedKey: Array<ByteArray> =
                    securityManager.splitDataIntoRSABlocks(decodedKey, 2048)

                if (decodedKey.size != 16 && decodedKey.size != 24 && decodedKey.size != 32) {

                    securityManager.decryptDataRSA(
                        arrayDecodedKey,
                        auth.currentUser?.email.toString()
                    ) { decryptedKey ->

                    }

                }

                var originalKey = SecretKeySpec(decodedKey, 0, decodedKey.size, "AES")
                val decodedData = file.encryptedByteArray
                val decodedIV = Base64.getDecoder().decode(file.iV)




                Log.d(TAG, "Len of encrypted data: ${file.encryptedByteArray.size}")
                Log.d(TAG, "First ten bytes of encrypted data: ${file.encryptedByteArray.take(10)}")
                Log.d(TAG, "IV LEN ${file.iV.length} ")
                Log.d(TAG, "IV = ${file.iV}")
                Log.d(TAG, "${file.iV} ${file.secretKey}  ${file.encryptedByteArray}")

                //tutaj muszisz poinformować o tym ze plik zostal odszyfrowany moze callback
                val decryptedData =
                    securityManager.decryptDataAES(decodedData, originalKey, decodedIV)


                file.byteArray = decryptedData


                val tmpFile = FileManager.byteArrayToFile(context, file.byteArray, file.name)
                FileManager.deleteFile(file, context)


                if (tmpFile.name.isNotEmpty()) {
                    //zastanow sie co tu masz nadpisaiwac
                    file.name = tmpFile.name
                    file.uri = tmpFile.uri
                    file.type = tmpFile.type
                    file.size = file.size

                }

                file.encryptionState = false
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime
                Log.d("NFC1234", "Czas operacji deszyfrowania: $duration ms")


            }
        }
    }


    fun pushFileIntoCloud(file: File, cipher: Boolean) {

        viewModelScope.launch {

            Log.d(TAG, "Plik ${file.name}: ${file.byteArray.size} bajtów")
            file.isUploading = true
            cloudComm.uploadFile(file, cipher, onUrlAvailable = { url ->
                val newNdefMessage = if (cipher) {
                    "$url${R.string.separator}${file.secretKey}${R.string.separator}${file.iV}${R.string.separator}"
                } else {
                    url
                }

                // Aktualizacja ndefMessage i uploadComplete w uiState
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
        _uiState.update { it.copy(ndefMessage = ndefMessage) }
    }


    fun deleteReceivedFile(file: File) {
        viewModelScope.launch {
            FileManager.deleteFile(file, context)

            _uiState.update { currentState ->
                // Filtrujemy listę `receivesFiles`, usuwając wybrany plik
                val updatedFiles = currentState.receivesFiles.filter { it != file }
                currentState.copy(receivesFiles = updatedFiles)
            }
        }
    }


    fun deleteSelectedFile(file: File) {
        viewModelScope.launch {
            // Usunięcie pliku z chmury, jeśli ma nietypowy URL
            if (file.url != URL("https://www.google.com")) {
                cloudComm.deleteFile(file)
            }

            // Usunięcie pliku lokalnie (jeśli potrzebne)
            // FileManager.deleteFile(file, context)

            _uiState.update { currentState ->
                // Filtrujemy listę `modelSelectedFiles`, usuwając wybrany plik
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
                    Log.e(TAG, "Błąd pobierania listy plików: $exception")
                }
            )
        }
    }

    private fun addFileToList(newFile: File) {
        _uiState.update { currentState ->
            val currentList = currentState.modelSelectedFiles.toMutableList()

            // Sprawdzanie, czy plik już istnieje w liście
            if (currentList.any { it.name == newFile.name && it.url.toString() == newFile.url.toString() }) {
                Log.d(TAG, "Plik już istnieje w liście: ${newFile.name}")
                currentState // Zwracamy aktualny stan bez zmian
            } else {
                currentList.add(newFile) // Dodanie nowego pliku do listy
                currentState.copy(modelSelectedFiles = currentList) // Zwracanie zaktualizowanego stanu
            }
        }
    }

    fun filterSelectedFilesSize(value: ClosedFloatingPointRange<Float>) {
        _uiState.update { currentState ->
            // Filtrujemy listę `modelSelectedFiles` w zależności od rozmiaru pliku
            val filteredFiles = currentState.modelSelectedFiles.filter { file ->
                file.size in value
            }
            currentState.copy(filteredSelectedFiles = filteredFiles)
        }
    }

    fun checkKey() {
        //sprawdzam czy klucze dla aliasu auth. sa w keystore
        securityManager.checkKey(auth.currentUser?.email.toString())

    }

    fun toggleEncryption() {
        _uiState.update { currentState ->
            // Zmiana wartości 'additionalEncryption' na przeciwną
            currentState.copy(additionalEncryption = !currentState.additionalEncryption)
        }
    }


    fun getPublicKey(): String {
        return securityManager.getPublicKey(auth.currentUser?.email.toString())

    }

    fun encryptDataRSA(data: ByteArray, publicKey: String, callback: (ByteArray) -> Unit) {
        securityManager.encryptDataRSA(data, publicKey) { encryptedData ->
            // Wywołaj funkcję zwrotną z zaszyfrowanymi danymi
            callback(encryptedData)
        }
    }

    fun toggleMirroring() {
        _uiState.update { currentState ->
            // Zmiana wartości 'cloudMirroring' na przeciwną
            currentState.copy(cloudMirroring = !currentState.cloudMirroring)
        }
    }

    fun validatePublicKey(pkey: String): Boolean {
        return securityManager.validatePublicKey(pkey)
    }

    fun updateActiveURL(file: File) {
        val ndefMessage =
            file.url.toString() + R.string.separator + file.secretKey + R.string.separator + file.iV + R.string.separator

        _uiState.update { it.copy(ndefMessage = ndefMessage) }


    }

    fun addReceivedFile(downloadedFile: File) {
        viewModelScope.launch {
            _uiState.update { currentState ->
                // Sprawdzanie, czy lista `receivesFiles` zawiera już plik o takiej samej nazwie
                if (currentState.receivesFiles.any { it == downloadedFile }) {

                    currentState
                } else {

                    currentState.copy(receivesFiles = currentState.receivesFiles + downloadedFile)
                }
            }
        }
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
            Log.d(TAG, "Odczytane dane NFC: $payloadString")

            viewModelScope.launch {
                if (payloadString.contains("https")) {
                    downloadFile(payloadString, receivesFiles = true)
                } else {
                    try {
                        val parts = payloadString.split(R.string.separator.toString())
                        val newPublicKey = parts[1]
                        _uiState.update { it.copy(publicKey = newPublicKey) }
                        Log.d(TAG, "Public key: ${parts[1]}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Błąd podczas przetwarzania danych NFC: $e")
                    }
                }
            }
        }

    }

}










