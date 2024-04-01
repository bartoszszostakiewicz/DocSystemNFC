package com.docsysnfc.flowtouch

import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.docsysnfc.R
import com.docsysnfc.flowtouch.model.flowtouchStates.AuthenticationState
import com.docsysnfc.flowtouch.model.CloudComm
import com.docsysnfc.flowtouch.model.flowtouchStates.CreateAccountState
import com.docsysnfc.flowtouch.model.File
import com.docsysnfc.flowtouch.model.FileManager
import com.docsysnfc.flowtouch.model.flowtouchStates.InternetConnectionStatus
import com.docsysnfc.flowtouch.model.NFCComm
import com.docsysnfc.flowtouch.model.flowtouchStates.NFCStatus
import com.docsysnfc.flowtouch.model.flowtouchStates.NFCSysScreen
import com.docsysnfc.flowtouch.model.SecurityManager
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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



    private val _publicKey = MutableStateFlow("")
    val publicKey = _publicKey.asStateFlow()



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

    fun filterSelectedFilesTypes(selectedTypes: SnapshotStateList<String>) {
        _filteredSelectedFiles.value = _modelSelectedFiles.value.filter { file ->
            selectedTypes.contains(file.type)
        }
    }

    private val _filteredSelectedFiles = MutableStateFlow<List<File>>(emptyList())
    val filteredSelectedFiles = _filteredSelectedFiles.asStateFlow()


    //its required for nfc status is available or not
    private val _nfcStatus = MutableStateFlow(NFCStatus.Unknown)
    val nfcStatus = _nfcStatus.asStateFlow()

    private val _internetConnStatus = MutableStateFlow(InternetConnectionStatus.DISCONNECTED)
    val internetConnStatus = _internetConnStatus.asStateFlow()


    //probalbly to delete
    private val currentNDEFMessage = MutableStateFlow("google.com")
    val activeURL = currentNDEFMessage.asStateFlow()


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

    private val _additionalEncryption = MutableStateFlow(false)
    val additionalEncryption = _additionalEncryption.asStateFlow()

    fun setAdditionalEncryption(value: Boolean) {
        _additionalEncryption.value = value
    }

    private val _cloudMirroring = MutableStateFlow(false)
    val cloudMirroring = _cloudMirroring.asStateFlow()

    fun setCloudMirroring(value: Boolean) {
        _cloudMirroring.value = value
    }


    init {

        nfcCommm.initNFCAdapter(context)
        initFilesFromFirebaseStorage()
        //every 1 second we are checking the nfc status
        viewModelScope.launch {

            while (true) {

//                Log.d("NFC1235", "Active url =  ${_activeURL.value}")
                kotlinx.coroutines.delay(1000)
                if (auth.currentUser != null) {
//                    Log.d(TAG, "User is authenticated ${auth.currentUser?.email}")
                    _authenticationState.value = AuthenticationState.SUCCESS
                    _navigationDestination.value = NFCSysScreen.Home

                } else {
//                    Log.d(TAG, "User is not authenticated")
                    _authenticationState.value = AuthenticationState.FAILURE
                    _navigationDestination.value = NFCSysScreen.Login

                }
            }
//            _authenticationState.value =
//                if (isUserAuthenticated) AuthenticationState.SUCCESS else AuthenticationState.FAILURE


        }
    }

    fun setActivityVisibility(isVisible: Boolean) {
        _isActivityVisible.value = isVisible
    }

    fun logOff() {
        navigationDestination.value = NFCSysScreen.Login
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
                Log.d(TAG, "Zmiana nazwy pliku na: $uniqueName$fileExtension")
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
                //Log.d("qwertty", "1signInWithEmail:success")
                _authenticationState.value = AuthenticationState.SUCCESS
            } else {
                //Log.w("qwertty", "signInWithEmail:failure", it.exception)
                _authenticationState.value = AuthenticationState.FAILURE
                Toast.makeText(
                    context,
                    context.getString(R.string.invalid_credentials),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }.isSuccessful


    }

    fun createAccount(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.d(TAG, "createUserWithEmail:success")
                    _createAccountState.value = CreateAccountState.SUCCESS
                } else {
                    Log.w(TAG, "createUserWithEmail:failure", it.exception)
                    _createAccountState.value = CreateAccountState.FAILURE
                }
            }.isSuccessful
    }



    private val _navigationDestination = MutableStateFlow(NFCSysScreen.Home)
    val navigationDestination = _navigationDestination

    fun setNavigationDestination(destination: NFCSysScreen) {
        Log.d(TAG, "setNavigationDestination: $destination")
        _navigationDestination.value = destination
    }




    fun enableNFCReaderMode(activity: Activity) {
        nfcCommm.enableNFCReader(activity, this)
    }

    fun disableNFCReaderMode(activity: Activity) {
        nfcCommm.disableNFCReader(activity)
    }

    suspend fun downloadFile(file: File) {

        Log.d(TAG, "Rozpoczęcie pobierania pliku: ${file.downloadLink}")
        _fileIsDownloading.value = true

        cloudComm.downloadFile(
            downloadDirectory = "${Environment.DIRECTORY_DOWNLOADS}/DocSysNfc/Received",
            downloadLink = file.downloadLink,
            context = context
        ) { fileUri, mimeType ->
            if (fileUri == null) {
                Log.e(TAG, "Nie udało się pobrać pliku, fileUri jest null.")
                _fileIsDownloading.value = false
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




            Log.d(
                "NFC123",
                "Tworzenie obiektu File: ${file.name}, Rozmiar: ${file.size} MB"
            )

            updateReceivedFiles(file)

            Log.d(TAG, "Aktualizacja otrzymanych plików: ${file.name}")
        }

    }

    suspend fun downloadFile(downloadLink: String, receivesFiles: Boolean = true) {

        val separator = R.string.separator.toString()
        val parts = downloadLink.split(separator)

        Log.d(TAG, "Parts: $parts")

        Log.d(TAG, "Rozpoczęcie pobierania pliku: $downloadLink")
        _fileIsDownloading.value = true

        cloudComm.downloadFile(
            downloadDirectory = "${Environment.DIRECTORY_DOWNLOADS}/DocSysNfc/Received",
            downloadLink = downloadLink,
            context = context
        ) { fileUri, mimeType ->
            if (fileUri == null) {
                Log.e("NFC123", "Nie udało się pobrać pliku, fileUri jest null.")
                _fileIsDownloading.value = false
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
                updateReceivedFiles(downloadedFile)
            } else {
                updateSelectedFiles(downloadedFile)
            }
            Log.d(TAG, "Aktualizacja otrzymanych plików: ${downloadedFile.name}")
        }
    }

    private fun updateSelectedFiles(newFile: File) {
        _fileIsDownloading.value = false

        _modelSelectedFiles.update { currentFiles ->
            Log.d(TAG, "Nazwa dodawanego pliku: ${newFile.name}")
            Log.d(TAG, "Uri dodawanego pliku: ${newFile.uri}")
            Log.d(TAG, "DLUGSC TABLICY: ${newFile.byteArray.size}")

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
                Log.d(TAG, "Zmieniono nazwę na: $newName")
            }
            val updatedFiles = currentFiles.map { existingFile ->
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

            updatedFiles
        }
    }


    private fun updateReceivedFiles(newFile: File) {
        _fileIsDownloading.value = false

        _receivesFiles.update { currentFiles ->
            Log.d(TAG, "Nazwa dodawanego pliku: ${newFile.name}")
            Log.d(TAG, "Uri dodawanego pliku: ${newFile.uri}")

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
                Log.d(TAG, "Zmieniono nazwę na: $newName")
            }

            // Sprawdzenie, czy plik z takim samym URI lub linkiem do pobrania już istnieje
            if (currentFiles.any {
                    it.downloadLink == newFile.downloadLink
                }
            ) {


                Toast.makeText(
                    context,
                    "Plik ${newFile.name} jest już otrzymany.",
                    Toast.LENGTH_LONG
                ).show()
                //update this file
                val updatedFiles = currentFiles.map { existingFile ->
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
                            // Możesz dodać więcej pól do aktualizacji
                        }
                    } else {
                        // Plik nie wymaga aktualizacji
                        existingFile
                    }
                }
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
                        // Utwórz SecretKeySpec na podstawie odszyfrowanego klucza

//                        Log.d("NFC1234", "Len of decrypted key: ${decryptedKey.size}")
//                        val originalKey = SecretKeySpec(decryptedKey, "AES")
//
////                        val decodedData = file.encryptedByteArray
////                        var originalKey = SecretKeySpec(decodedKey, 0, decodedKey.size, "AES")
//                        val decodedData = file.encryptedByteArray
//                        val decodedIV = Base64.getDecoder().decode(file.iV)
//
//
//
//
//                        Log.d("NFC1234", "Len of encrypted data: ${file.encryptedByteArray.size}")
//                        Log.d(
//                            "NFC1234",
//                            "First ten bytes of encrypted data: ${file.encryptedByteArray.take(10)}"
//                        )
//                        Log.d("NFC1234", "IV LEN ${file.iV.length} ")
//                        Log.d("NFC1234", "IV = ${file.iV}")
//                        Log.d("NFC1234", "${file.iV} ${file.secretKey}  ${file.encryptedByteArray}")
//
//                        //tutaj muszisz poinformować o tym ze plik zostal odszyfrowany moze callback
//                        val decryptedData =
//                            securityManager.decryptDataAES(decodedData, originalKey, decodedIV)
//
//
//                        file.byteArray = decryptedData
//
//
//                        val tmpFile = FileManager.byteArrayToFile(context, file.byteArray, file.name)
//                        FileManager.deleteFile(file, context)
//
//
//                        if (tmpFile.name.isNotEmpty()) {
//                            //zastanow sie co tu masz nadpisaiwac
//                            file.name = tmpFile.name
//                            file.uri = tmpFile.uri
//                            file.type = tmpFile.type
//                            file.size = file.size
//
//                        }
//
//                        file.encryptionState = false
//                        val endTime = System.currentTimeMillis()
//                        val duration = endTime - startTime
//                        Log.d("NFC1234", "Czas operacji deszyfrowania: $duration ms")


                    }
                    // Jeśli długość klucza nie pasuje, odszyfruj klucz algorytmem RSA
//                    securityManager.getPrivateKey(context,auth.currentUser?.email.toString())?.let {
//                        securityManager.decryptDataRSA(
//                            decodedKey,
//                            it
//                        ) { decryptedKey ->
//                            // Utwórz SecretKeySpec na podstawie odszyfrowanego klucza
//                //                        val originalKey = SecretKeySpec(decryptedKey, "AES")
//                            Log.d("NFC1234", "Len of decrypted key: ${decryptedKey.size}")
//
//                        }
//                    }
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


    private val _uploadComplete = MutableStateFlow(false)
    val uploadComplete = _uploadComplete.asStateFlow()

    fun pushFileIntoCloud(file: File, cipher: Boolean) {

        viewModelScope.launch {

            Log.d(TAG, "Plik ${file.name}: ${file.byteArray.size} bajtów")
            cloudComm.uploadFile(file, cipher, onUrlAvailable = { url ->
                if (cipher) {
                    currentNDEFMessage.value =
                        (url + R.string.separator + file.secretKey + R.string.separator + file.iV + R.string.separator)
                } else {
                    currentNDEFMessage.value = url
                }
                _uploadComplete.value = true
                file.isUploading = false
            })

        }
    }

    fun setNdefMessage(ndefMessage: String) {
        currentNDEFMessage.value = R.string.separator.toString() + ndefMessage + R.string.separator.toString()
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
        val currentList = _modelSelectedFiles.value.toMutableList()
        if (currentList.any { it.name == newFile.name && it.url.toString() == newFile.url.toString() }) {
            Log.d(TAG, "Plik już istnieje w liście: ${newFile.name}")
        } else {
            currentList.add(newFile)
            _modelSelectedFiles.value = currentList
        }
    }

    fun filterSelectedFilesSize(value: ClosedFloatingPointRange<Float>) {
        _filteredSelectedFiles.value = _modelSelectedFiles.value.filter { file ->
            file.size in value
        }
    }

    fun checkKey() {
        //sprawdzam czy klucze dla aliasu auth. sa w keystore
        securityManager.checkKey(auth.currentUser?.email.toString())

    }

    fun toggleEncryption() {
        _additionalEncryption.value = !_additionalEncryption.value
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
        _cloudMirroring.value = !_cloudMirroring.value
    }

    fun validatePublicKey(pkey: String): Boolean {
        return securityManager.validatePublicKey(pkey)
    }

    fun updateActiveURL(file: File) {
        currentNDEFMessage.value =
            file.url.toString() + R.string.separator + file.secretKey + R.string.separator + file.iV + R.string.separator
    }

    fun addReceivedFile(downloadedFile: File) {
        viewModelScope.launch {
            _receivesFiles.update { currentFiles ->
                // Sprawdzanie czy lista zawiera już plik o takiej samej nazwie
                if (currentFiles.any { it == downloadedFile }) {
                    // Jeśli tak, zwracamy aktualną listę bez zmian
                    currentFiles
                } else {
                    // Jeśli nie, dodajemy nowy plik do listy
                    currentFiles + downloadedFile
                }
            }
        }
    }

    fun checkInternetStatus(context: Context) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        if (activeNetwork?.isConnectedOrConnecting == true) {
            _internetConnStatus.value = InternetConnectionStatus.CONNECTED
        } else {
            _internetConnStatus.value = InternetConnectionStatus.DISCONNECTED
        }
    }


   fun processNFCData(response: ByteArray?) {

        if (response != null) {
            val payloadString = response.toString(Charsets.UTF_8)
            Log.d(TAG, "Odczytane dane NFC: $payloadString")

            viewModelScope.launch {
                if(payloadString.contains("https")) {
                    downloadFile(payloadString, receivesFiles = true)
                }else{
                    val parts = payloadString.split(R.string.separator.toString())
                    _publicKey.value = parts[1]
                    Log.d(TAG, "Public key: ${parts[1]}")
                }
            }
        }



    }

}










