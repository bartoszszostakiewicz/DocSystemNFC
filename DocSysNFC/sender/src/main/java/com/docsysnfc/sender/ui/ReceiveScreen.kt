package com.docsysnfc.sender.ui

import android.app.Activity
import android.content.Context
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.docsysnfc.R
import com.docsysnfc.sender.MainViewModel
import com.docsysnfc.sender.model.File
import com.docsysnfc.sender.ui.theme.backgroundColor
import com.docsysnfc.sender.ui.theme.buttonsColor
import com.docsysnfc.sender.ui.theme.tilesColor
import com.docsysnfc.sender.ui.theme.whiteColor
import java.nio.charset.Charset


val APDU_SELECT = byteArrayOf(
    0x00.toByte(), // CLA	- Class - Class of instruction
    0xA4.toByte(), // INS	- Instruction - Instruction code
    0x04.toByte(), // P1	- Parameter 1 - Instruction parameter 1
    0x00.toByte(), // P2	- Parameter 2 - Instruction parameter 2
    0x07.toByte(), // Lc field	- Number of bytes present in the data field of the command
    0xD2.toByte(),
    0x76.toByte(),
    0x00.toByte(),
    0x00.toByte(),
    0x85.toByte(),
    0x01.toByte(),
    0x01.toByte(), // NDEF Tag Application name
    0x00.toByte(), // Le field	- Maximum number of bytes expected in the data field of the response to the command
)

val CAPABILITY_CONTAINER_OK = byteArrayOf(
    0x00.toByte(), // CLA	- Class - Class of instruction
    0xa4.toByte(), // INS	- Instruction - Instruction code
    0x00.toByte(), // P1	- Parameter 1 - Instruction parameter 1
    0x0c.toByte(), // P2	- Parameter 2 - Instruction parameter 2
    0x02.toByte(), // Lc field	- Number of bytes present in the data field of the command
    0xe1.toByte(),
    0x03.toByte(), // file identifier of the CC file
)

val READ_CAPABILITY_CONTAINER = byteArrayOf(
    0x00.toByte(), // CLA	- Class - Class of instruction
    0xb0.toByte(), // INS	- Instruction - Instruction code
    0x00.toByte(), // P1	- Parameter 1 - Instruction parameter 1
    0x00.toByte(), // P2	- Parameter 2 - Instruction parameter 2
    0x0f.toByte(), // Lc field	- Number of bytes present in the data field of the command
)

// In the scenario that we have done a CC read, the same byte[] match
// for ReadBinary would trigger and we don't want that in succession
var READ_CAPABILITY_CONTAINER_CHECK = false

val READ_CAPABILITY_CONTAINER_RESPONSE = byteArrayOf(
    0x00.toByte(), 0x11.toByte(), // CCLEN length of the CC file
    0x20.toByte(), // Mapping Version 2.0
    0xFF.toByte(), 0xFF.toByte(), // MLe maximum
    0xFF.toByte(), 0xFF.toByte(), // MLc maximum
    0x04.toByte(), // T field of the NDEF File Control TLV
    0x06.toByte(), // L field of the NDEF File Control TLV
    0xE1.toByte(), 0x04.toByte(), // File Identifier of NDEF file
    0xFF.toByte(), 0xFE.toByte(), // Maximum NDEF file size of 65534 bytes
    0x00.toByte(), // Read access without any security
    0xFF.toByte(), // Write access without any security
    0x90.toByte(), 0x00.toByte(), // A_OKAY
)

val NDEF_SELECT_OK = byteArrayOf(
    0x00.toByte(), // CLA	- Class - Class of instruction
    0xa4.toByte(), // Instruction byte (INS) for Select command
    0x00.toByte(), // Parameter byte (P1), select by identifier
    0x0c.toByte(), // Parameter byte (P1), select by identifier
    0x02.toByte(), // Lc field	- Number of bytes present in the data field of the command
    0xE1.toByte(),
    0x04.toByte(), // file identifier of the NDEF file retrieved from the CC file
)

val NDEF_READ_BINARY = byteArrayOf(
    0x00.toByte(), // Class byte (CLA)
    0xb0.toByte(), // Instruction byte (INS) for ReadBinary command
)

val NDEF_READ_BINARY_NLEN = byteArrayOf(
    0x00.toByte(), // Class byte (CLA)
    0xb0.toByte(), // Instruction byte (INS) for ReadBinary command
    0x00.toByte(),
    0x00.toByte(), // Parameter byte (P1, P2), offset inside the CC file
    0x02.toByte(), // Le field
)

val A_OKAY = byteArrayOf(
    0x90.toByte(), // SW1	Status byte 1 - Command processing status
    0x00.toByte(), // SW2	Status byte 2 - Command processing qualifier
)

val A_ERROR = byteArrayOf(
    0x6A.toByte(), // SW1	Status byte 1 - Command processing status
    0x82.toByte(), // SW2	Status byte 2 - Command processing qualifier
)
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun ReceiveScreen(navController: NavController, viewModel: MainViewModel, context: Context) {

//    val authenticationState by viewModel.authenticationState.collectAsState()
//
//    if(authenticationState == AuthenticationState.FAILURE || authenticationState == AuthenticationState.UNKNOWN){
//        navController.navigate(NFCSysScreen.Login.name)
//    }

    setSenderMode(context, false)
    viewModel.enableNFCReaderMode(context as Activity)

    val showNfcPrompt = remember { mutableStateOf(true) }
    val showDialog = remember { mutableStateOf(false) }


    ErrorAlert(
        title = stringResource(id = R.string.NFCRead),
        text = stringResource(R.string.NFCReadDescription),
        showDialog = showNfcPrompt,
        onDismiss = { showNfcPrompt.value = false })

    ErrorAlert(
        title = stringResource(id = R.string.NFCReadError),
        text = stringResource(id = R.string.NFCReadErrorDesc),
        showDialog = showDialog,
        onDismiss = { showDialog.value = false })

    val nfcTag = viewModel.nfcTag.collectAsState()

    LaunchedEffect(nfcTag.value) {
        nfcTag.value?.let {tag ->
            try {
                // Spróbuj odczytać jako NDEF
                val ndef = Ndef.get(tag)
                ndef?.connect()
                val ndefMessage = ndef?.ndefMessage
                if (ndefMessage != null) {
                    val payload = ndefMessage.records[0].payload
                    val payloadStr = String(payload, Charset.forName("UTF-8"))

                    Log.d("NFC123", "Payload: $payloadStr")

                    viewModel.downloadFile(payloadStr, context)
                }
                ndef?.close()

                var isoDep: IsoDep? = null
                // Jeśli nie udało się jako NDEF, spróbuj jako IsoDep
                if (ndefMessage == null) {
                    isoDep = IsoDep.get(tag)
                    isoDep?.connect()
                    // Wyślij komendę APDU, aby odczytać dane
                    // Na przykład SELECT AID komenda dla aplikacji smartcard
                    val command =READ_CAPABILITY_CONTAINER // konkretna komenda APDU
                    val response = isoDep?.transceive(command)?.clone()
                    Log.d("NFC123", "Response: ${response.toString()}")
                    if (response != null) {
                        val isoDepStr = String(response, Charset.forName("UTF-8"))
                        Log.d("NFC123", "IsoDep Payload: $isoDepStr")
                    }
                    isoDep?.close()
                }

                // Jeśli inne metody zawiodły, spróbuj jako NfcA
                if (ndefMessage == null && isoDep == null) {
                    val nfcA = NfcA.get(tag)
                    nfcA?.connect()
                    // Wyślij surową komendę, aby odczytać dane
                    val command = READ_CAPABILITY_CONTAINER // konkretna komenda dla NfcA
                    val response = nfcA?.transceive(command)?.clone()
                    Log.d("NFC123", "Response: ${response.toString()}")
                    if (response != null) {
                        val nfcAStr = String(response, Charset.forName("UTF-8"))
                        Log.d("NFC123", "NfcA Payload: $nfcAStr")
                    }
                    nfcA?.close()
                }
            } catch (e: Exception) {
                viewModel.setDownloadStatus(false)
                showDialog.value = true
                Log.e("NFC123", "Błąd przy odczycie NFC: ${e.message}")
            }
        }
    }

    val fileList = viewModel.receiveFiles.collectAsState()

    Log.d("NFC123", "fileList: ${fileList.value.size}")

    ReceiveFileScreen(fileList.value, viewModel, context)

}

@Composable
fun ErrorAlert(
    title: String,
    text: String,
    showDialog: MutableState<Boolean>,
    onDismiss: () -> Unit
) {
    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = title) },
            text = { Text(text = text) },
            confirmButton = {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(buttonsColor)
                ) {
                    Text(stringResource(id = R.string.ok))
                }
            }
        )
    }
}


@Composable
fun FileCard(
    file: File,
    viewModel: MainViewModel,
    context: Context,
) {

//    var isCipher = rememberSaveable { mutableStateOf(file.isCipher) }
//    Log.d("NFC123", "isCipher: ${isCipher.value} for ${file.name}")

//    LaunchedEffect(file.isCipher) {
//        isCipher.value = file.isCipher
//    }


    val isCipher = rememberSaveable { mutableStateOf(file.isCipher) }
    LaunchedEffect(file) {
        isCipher.value = file.isCipher
        Log.d("NFC123", "isCipher: ${isCipher.value} for ${file.name} file uri: ${file.uri}")
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = tilesColor),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(8.dp),

        ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {

            Column(
                modifier = Modifier
                    .align(Alignment.End)
                    .fillMaxWidth(),
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "File name: ${file.name.take(8)}",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Icon(
                        painter = painterResource(id = R.drawable.cancelled),
                        contentDescription = "Icon",
                        modifier = Modifier
                            .size(30.dp)
                            .clickable {
                                viewModel.deleteReceivedFile(file)
                            }
                    )
                }
            }



            Text(
                text = "Size: ${String.format("%.2f", file.size)} MB",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                if (file.encryptionState) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    Row {
                        Button(
                            onClick = {
                                viewModel.openFile(context, file.uri, file.type)
                            },
                            colors = ButtonDefaults.buttonColors(
                                buttonsColor,
                                contentColor = whiteColor
                            ),
                            enabled = !isCipher.value
                        ) {
                            Text(text = "Open")
                        }
                    }
                }
                if(isCipher.value) {
                    Icon(
                        painter = painterResource(if (isCipher.value) R.drawable.cipher_on else R.drawable.cipher_off),
                        contentDescription = "Icon",
                        modifier = Modifier
                            .padding(start = 100.dp)
                            .size(50.dp)
                            .align(Alignment.CenterVertically)
                            .clickable {
                                isCipher.value = !isCipher.value
                                viewModel.handleEncryption(file, false, true)
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun ReceiveFileScreen(fileList: List<File>, mainViewModel: MainViewModel, context: Context) {

    Scaffold(
        topBar = {
            HomeScreenTopBar(title = stringResource(id = R.string.received_file), navController = NavController(context))
        }
    ) { innerPadding ->
        val isLoading = mainViewModel.fileIsDownloading.collectAsState().value

        Box(
            Modifier
                .background(backgroundColor)
                .padding(innerPadding)
        ) {

            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(16.dp),
                        color = contentColorFor(backgroundColor)
                    )
                    Text(
                        stringResource(id = R.string.downloading),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                items(fileList) { file ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FileCard(
                            file = file,
                            viewModel = mainViewModel,
                            context = context,
                        )
                    }
                }
            }
        }
    }
}



