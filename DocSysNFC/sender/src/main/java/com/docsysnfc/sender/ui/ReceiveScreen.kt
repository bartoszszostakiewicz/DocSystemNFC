package com.docsysnfc.sender.ui

import android.app.Activity
import android.content.Context
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
import androidx.compose.material3.ExperimentalMaterial3Api
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


@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun ReceiveScreen(navController: NavController, viewModel: MainViewModel, context: Context) {

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
        nfcTag.value?.let {
            try {
                val ndef = Ndef.get(it)
                ndef?.connect()
                val ndefMessage = ndef?.ndefMessage

                if (ndefMessage != null && ndefMessage.records.isNotEmpty()) {
                    val payload = ndefMessage.records[0].payload
                    val payloadStr = String(payload, Charset.forName("UTF-8"))

                    Log.d("NFC123", "Payload: $payloadStr")

                    viewModel.downloadFile(payloadStr, context)
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

    var isCipher by rememberSaveable { mutableStateOf(false) }


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
                if (file.encryption) {
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
                        ) {
                            Text(text = "Open")
                        }
                    }
                }

                Icon(
                    painter = painterResource(if (isCipher) R.drawable.cipher_on else R.drawable.cipher_off),
                    contentDescription = "Icon",
                    modifier = Modifier
                        .padding(start = 100.dp)
                        .size(50.dp)
                        .align(Alignment.CenterVertically)
                        .clickable {
                            isCipher = !isCipher
                            viewModel.handleEncryption(file, false, true)
                        }
                )
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



