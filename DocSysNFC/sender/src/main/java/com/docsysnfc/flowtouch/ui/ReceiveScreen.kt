package com.docsysnfc.flowtouch.ui

import android.app.Activity
import android.content.Context
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.docsysnfc.R
import com.docsysnfc.flowtouch.MainViewModel
import com.docsysnfc.flowtouch.model.File
import com.docsysnfc.flowtouch.ui.theme.backgroundColor
import com.docsysnfc.flowtouch.ui.theme.buttonsColor
import com.docsysnfc.flowtouch.ui.theme.deleteButtonsColor
import com.docsysnfc.flowtouch.ui.theme.tilesColor
import com.docsysnfc.flowtouch.ui.theme.whiteColor
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
    val showDeleteDialog = remember { mutableStateOf(false) }


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
                val isoDep = IsoDep.get(tag)
                isoDep?.connect()
                val response = isoDep?.transceive(APDU_SELECT)
                Log.d("NFC123", "Response: $response")
                isoDep?.close()

                // Spróbuj odczytać jako NDEF
                val ndef = Ndef.get(tag)
                ndef?.connect()
                val ndefMessage = ndef?.ndefMessage
                if (ndefMessage != null) {
                    val payload = ndefMessage.records[0].payload
                    val payloadStr = String(payload, Charset.forName("UTF-8"))

                    Log.d("NFC123", "Payload: $payloadStr")

                    viewModel.downloadFile(payloadStr)
                }
                ndef?.close()


            } catch (e: Exception) {
                viewModel.setDownloadStatus(false)
                showDialog.value = true
                Log.e("NFC123", "Błąd przy odczycie NFC: ${e.message}")
            }
        }
    }

    val fileList = viewModel.receiveFiles.collectAsState()

    Log.d("NFC123", "fileList: ${fileList.value.size}")

    ReceiveFileScreen(fileList.value, viewModel, context, showDeleteDialog)

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
            onDismissRequest =  {}/*onDismiss*/,
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
    showDeleteDialog: MutableState<Boolean>,
) {




    val isCipher = rememberSaveable { mutableStateOf(file.isCipher) }
    val isDownloaded = rememberSaveable { mutableStateOf(file.isDownloaded) }

    LaunchedEffect(file) {
        isCipher.value = file.isCipher
        isDownloaded.value = file.isDownloaded
        Log.d("NFC123", "isCipher: ${isCipher.value} for ${file.name} file uri: ${file.uri}")
    }

    if (showDeleteDialog.value) {
        AlertDialog(
            onDismissRequest = { /*showDeleteDialog.value = false*/ },
            title = { Text(stringResource(id = R.string.delete_confirm)) },
            text = { Text(stringResource(id = R.string.delete_confirm_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteReceivedFile(file)
                        showDeleteDialog.value = false
                    },
                    colors = ButtonDefaults.buttonColors(deleteButtonsColor)
                ) { Text(stringResource(id = R.string.delete_confirm)) }
            },
            dismissButton = {
                Button(
                    onClick = { showDeleteDialog.value = false },
                    colors = ButtonDefaults.buttonColors(buttonsColor)
                ) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
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
                        text = "${stringResource(R.string.file_name)}${stringResource(id = R.string.colon)} ${file.name.take(8)}",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Icon(
                        painter = painterResource(id = R.drawable.cancelled),
                        contentDescription = stringResource(id = R.string.file_icon),
                        modifier = Modifier
                            .size(30.dp)
                            .clickable {
                                showDeleteDialog.value = true
                            }
                    )
                }
            }


            Text(
                text = "${stringResource(id = R.string.file_type)}${stringResource(id = R.string.colon)} ${file.type}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${stringResource(id = R.string.file_size)}${stringResource(id = R.string.colon)} ${String.format("%.2f", file.size)} ${stringResource(id = R.string.mb)}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                if (file.encryptionState) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = contentColorFor(backgroundColor),
                        )
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
                            enabled = !isCipher.value && isDownloaded.value
                        ) {
                            Text(text = "Open")
                        }
                    }
                }

                if(!isDownloaded.value) {
                    Icon(
                        painter = painterResource(R.drawable.download),
                        contentDescription = stringResource(id = R.string.file_icon),
                        modifier = Modifier
                            .padding(start = 100.dp)
                            .size(50.dp)
                            .align(Alignment.CenterVertically)
                            .clickable {
                                isCipher.value = !isCipher.value
                                viewModel.handleEncryption(file, false, true)
                            }
                    )
                }else if(isCipher.value) {
                    Icon(
                        painter = painterResource(if (isCipher.value) R.drawable.cipher_on else R.drawable.cipher_off),
                        contentDescription = stringResource(id = R.string.file_icon),
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
fun ReceiveFileScreen(fileList: List<File>, mainViewModel: MainViewModel, context: Context,showDeleteDialog: MutableState<Boolean>) {


    Scaffold(
        topBar = {
            HomeScreenTopBar(title = stringResource(id = R.string.received_file), navController = NavController(context), viewModel = mainViewModel)
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

            if(fileList.isNotEmpty()){
                
                Filters(
                    mainViewModel
                )
            }




            LazyColumn(
                modifier = Modifier
                    .padding(top = 64.dp)
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
                            showDeleteDialog = showDeleteDialog
                        )
                    }
                }
            }
        }
    }
}



