package com.docsysnfc.flowtouch.ui


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.NavController
import androidx.wear.compose.material.Checkbox
import androidx.wear.compose.material.CheckboxDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import com.docsysnfc.R
import com.docsysnfc.flowtouch.MainViewModel
import com.docsysnfc.flowtouch.model.AuthenticationState
import com.docsysnfc.flowtouch.model.File
import com.docsysnfc.flowtouch.model.NFCStatus
import com.docsysnfc.flowtouch.model.NFCSysScreen
import com.docsysnfc.flowtouch.ui.theme.appBarColorTheme
import com.docsysnfc.flowtouch.ui.theme.backgroundColor
import com.docsysnfc.flowtouch.ui.theme.buttonsColor
import com.docsysnfc.flowtouch.ui.theme.deleteButtonsColor
import com.docsysnfc.flowtouch.ui.theme.tilesColor
import com.docsysnfc.flowtouch.ui.theme.whiteColor
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.roundToInt


//change location of this function
fun setSenderMode(context: Context, isActive: Boolean) {
    context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit {
        putBoolean("senderMode", isActive)
        apply()
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenTopBar(title: String, navController: NavController,viewModel: MainViewModel) {
    TopAppBar(title = {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            color = whiteColor,
            style = MaterialTheme.typography.titleLarge
        )
    }, colors = TopAppBarDefaults.smallTopAppBarColors(
        containerColor = appBarColorTheme, titleContentColor = whiteColor
    ), actions = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxHeight() // Zapewnia, że Row zajmuje całą dostępną wysokość
        ) {

            TopBarClickableIcon(navController = navController, viewModel)
        }
    })
}

@Composable
fun TopBarClickableIcon(navController: NavController,viewModel: MainViewModel) {
    Row {
        Icon(
            painterResource(id = R.drawable.key2),
            contentDescription = stringResource(id = R.string.share_key),
            modifier = Modifier
                .size(30.dp)
                .clickable {
                    viewModel.setNdefMessage(viewModel.getPublicKey())
                    navController.navigate(NFCSysScreen.ShareKeyScreen.name)
                }
        )
        Spacer(modifier = Modifier.width(16.dp))
        Icon(
            painter = painterResource(R.drawable.log_out),
            contentDescription = stringResource(id = R.string.log_out),
            modifier = Modifier
                .clickable {
                    FirebaseAuth
                        .getInstance()
                        .signOut()
                    navController.navigate(NFCSysScreen.Login.name)
                }
                .padding(end = 30.dp)
                .size(30.dp),
        )

    }
}

@Composable
fun SwipeableTiles(
    selectedFiles: List<File>, homeViewModel: MainViewModel, navController: NavController
) {
    LazyRow {
        items(selectedFiles) { file ->
            SwipeableTile(file, homeViewModel, navController)
        }
    }
}

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun SwipeableTile(
    file: File, viewModel: MainViewModel, navController: NavController
) {
    val tileWidth = integerResource(id = R.integer.tileWidth).dp
    val tileHeight = integerResource(id = R.integer.tileHeight).dp


    val isCipher = remember { mutableStateOf(file.isCipher) }
    val showDeleteDialog = remember { mutableStateOf(false) }
    val showCipherDialog = remember { mutableStateOf(false) }
    val fileIsOnDevice = remember { mutableStateOf(false) }
    val downloadStart = remember { mutableStateOf(false) }
    val isUploading = remember { mutableStateOf(file.isUploading) }

    val cloudMirroring = viewModel.cloudMirroring.collectAsState()

    val additonalEncrytpion = viewModel.additionalEncryption.collectAsState()

    if(cloudMirroring.value){
        if(!fileIsOnDevice.value) {
            viewModel.setDownloadStatus(true)
            downloadStart.value = true
        }
    }
    if(additonalEncrytpion.value){
        isCipher.value = true
        viewModel.handleEncryption(file, isCipher.value)
    }


    val uploadComplete by viewModel.uploadComplete.collectAsState()
    val fileIsDownloading by viewModel.fileIsDownloading.collectAsState()


    LaunchedEffect(fileIsDownloading) {
        if (downloadStart.value) {
            viewModel.downloadFile(file.downloadLink, receivesFiles = false)
            downloadStart.value = false
            fileIsOnDevice.value = true // jak to sie ustawi na true chce wywolac rekompozcyje
        }

    }
    LaunchedEffect(file) {
        fileIsOnDevice.value = file.uri.toString() != file.downloadLink
        if (file.type == "binary") {
            isCipher.value = true
            fileIsOnDevice.value = true
        }

    }


    LaunchedEffect(isUploading) {

        if (uploadComplete) {
            navController.navigate(
                "${NFCSysScreen.Send.name}/${
                    viewModel.modelSelectedFiles.value.indexOf(
                        file
                    )
                }"
            )
            viewModel.resetUploadComplete()
        }
    }

    if (showDeleteDialog.value) {
        AlertDialog(onDismissRequest = {
           /*showDeleteDialog.value = false*/
        }, title = {
            Text(text = stringResource(id = R.string.delete_confirm))
        }, text = {
            Text(text = stringResource(id = R.string.file_delete_confirm_text))
        }, confirmButton = {
            Button(
                onClick = {
                    viewModel.deleteSelectedFile(file)
                    showDeleteDialog.value = false
                }, colors = ButtonDefaults.buttonColors(
                    deleteButtonsColor, contentColor = Color.White
                )
            ) {
                Text(stringResource(id = R.string.delete))
            }
        }, dismissButton = {
            Button(
                onClick = {
                    showDeleteDialog.value = false
                }, colors = ButtonDefaults.buttonColors(
                    buttonsColor, contentColor = Color.White
                )
            ) {
                Text(stringResource(id = R.string.cancel))
            }
        })
    }

    if (showCipherDialog.value) {
        AlertDialog(
            onDismissRequest = {
                showCipherDialog.value = false
            },
            title = {
                Text(text = stringResource(id = R.string.file_cipher))
            },
            text = {
                Text(text = stringResource(id = R.string.file_is_binary_info))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCipherDialog.value = false
                    }, colors = ButtonDefaults.buttonColors(
                        buttonsColor, contentColor = Color.White
                    )
                ) {
                    Text(stringResource(id = R.string.ok))
                }
            },
        )
    }



    Column(
        modifier = Modifier
            .padding(16.dp)
            .width(tileWidth)
            .height(tileHeight)
            .background(tilesColor, shape = RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Column {
                    if (fileIsOnDevice.value) {
                        Icon(painter = painterResource(if (isCipher.value) R.drawable.cipher_on else R.drawable.cipher_off),
                            contentDescription = "Icon",
                            modifier = Modifier
                                .size(30.dp)
                                .align(Alignment.CenterHorizontally)
                                .clickable {
                                    if (file.type != "binary") {
                                        isCipher.value = !isCipher.value
                                    } else {
                                        showCipherDialog.value = true
                                    }
                                    viewModel.handleEncryption(file, isCipher.value)
                                })
                    } else {
                        Icon(painter = painterResource(R.drawable.download),
                            contentDescription = "Icon",
                            modifier = Modifier
                                .size(30.dp)
                                .align(Alignment.CenterHorizontally)
                                .clickable {
                                    viewModel.setDownloadStatus(true)
                                    downloadStart.value = true
                                })
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(start = 0.dp)
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {


                    Icon(painter = painterResource(id = getIconFile(file)),
                        contentDescription = "plik",
                        modifier = Modifier.graphicsLayer {
                            alpha = 0.8f
                            scaleX = 0.8f
                            scaleY = 0.8f
                        })

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp)
                    ) {

                        Text(
                            text = "Name: ${
                                file.name.take(14)
                            }", fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Type: ${
                                file.type
                            }", color = Color.Gray
                        )
                        Text(
                            text = "Size: ${
                                file.size
                            }MB", color = Color.Gray
                        )
                    }
                }



                Spacer(modifier = Modifier.height(32.dp))

                Column {
                    Icon(painter = painterResource(R.drawable.cancelled),
                        contentDescription = stringResource(id = R.string.delete_confirm),
                        modifier = Modifier
                            .size(30.dp)
                            .align(Alignment.CenterHorizontally)
                            .clickable {
                                showDeleteDialog.value = true
                            })

                }


            }
        }

        Spacer(modifier = Modifier.height(8.dp)) // Add vertical spacing

        if (isUploading.value) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                stringResource(id = R.string.file_is_uploading),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
                fontWeight = FontWeight.Bold
            )
        }else {

            Button(
                onClick = {

                    Log.d("TAG123", "linkztondhuehui: ${file.url}")
                    Log.d("TAG123", "linkztondhuehui: ${viewModel.modelSelectedFiles.value.size}")
                    Log.d("TAG123", "linkztondhuehui: ${viewModel.modelSelectedFiles.value[0].url}")

                    viewModel.pushFileIntoCloud(file, isCipher.value)

                },
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    buttonsColor, contentColor = Color.White
                ),
                enabled = (file.type != "binary")

            ) {
                Text(text = stringResource(id = R.string.send_file))
            }
        }
    }
}


@Composable
fun ChooseFileButton(
    homeViewModel: MainViewModel,
) {
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { homeViewModel.addFile(it) }
    }
    Button(
        onClick = {
            filePickerLauncher.launch("*/*")
        }, modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(), colors = ButtonDefaults.buttonColors(
            buttonsColor, contentColor = Color.White
        )
    ) {
        Text(text = stringResource(id = R.string.choose_file))
    }
}


@Composable
fun HomeScreen(navController: NavController, viewModel: MainViewModel, context: Context) {


    val authenticationState by viewModel.authenticationState.collectAsState()

    if (authenticationState == AuthenticationState.FAILURE || authenticationState == AuthenticationState.UNKNOWN) {
        navController.navigate(NFCSysScreen.Login.name)
    }

    //key managmenent
    //sprawdzam czy mam klucze jak nie to generuje
    viewModel.checkKey()
//

    viewModel.disableNFCReaderMode(context as Activity)



    LaunchedEffect(Unit) {
        viewModel.checkNFCStatus()
    }


    // Observe NFC status
    val nfcStatus by viewModel.nfcStatus.collectAsState()


    when (nfcStatus) {
        NFCStatus.Enabled -> {
            Log.d("nfcstatus", "enabled")

        }

        NFCStatus.Disabled -> {

            Log.d("nfcstatus", "disabled")
            PromptToEnableNFCDialog()
        }

        NFCStatus.NotSupported -> {

            Log.d("nfcstatus", "notsupported")
            NFCNotSupportedDialog()
        }

        NFCStatus.Unknown -> {
            Log.d("nfcstatus", "unknown")
        }
    }

    Scaffold(topBar = { HomeScreenTopBar(title = "flowtouch", navController = navController,viewModel) },
        floatingActionButton = {
            Box(modifier = Modifier.fillMaxSize()) {}
        }) { innerPadding ->
        Column(
            modifier = Modifier
                .background(backgroundColor)
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {

            setSenderMode(context, false)


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 1.dp), // Adjust padding as needed
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(
                    onClick = { navController.navigate(NFCSysScreen.Receive.name) },
                    iconId = R.drawable.plik,
                    contentDescription = context.resources.getString(R.string.file_icon),
                    modifier = Modifier.padding(1.dp)
                )
                IconButton(
                    onClick = {
                        navController.navigate(NFCSysScreen.Settings.name)
                    },
                    iconId = R.drawable.settings2,
                    contentDescription = context.resources.getString(R.string.settings),
                    modifier = Modifier.padding(1.dp)
                )

            }

            Spacer(modifier = Modifier.weight(1f, true))

            if(viewModel.modelSelectedFiles.collectAsState().value.isNotEmpty() ){
                    Filters(viewModel)
            }

            SwipeableTiles(
                selectedFiles = viewModel.modelSelectedFiles.collectAsState().value,
                homeViewModel = viewModel,
                navController = navController
            )

            Spacer(modifier = Modifier.weight(1f, true))

            ChooseFileButton(
                homeViewModel = viewModel,
            )

        }
    }
}

@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconId: Int,
    contentDescription: String? = null,
    scale: Float = 0.65f,
) {
    Column {

        Button(
            onClick = onClick, modifier = modifier

                .scale(scale), colors = ButtonDefaults.buttonColors(
                buttonsColor, contentColor = whiteColor
            )
        ) {
            Icon(
                painter = painterResource(id = iconId),
                contentDescription = contentDescription,
                modifier = Modifier

            )
        }
        Text(
            text = contentDescription.toString(),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun NFCNotSupportedDialog() {

    var showDialog by remember { mutableStateOf(true) }

    if (showDialog) {
        AlertDialog(onDismissRequest = { /*showDialog = false*/ },
            text = { Text(stringResource(R.string.nfc_not_supported)) },
            confirmButton = {
                Button(onClick = {
                    showDialog = false
                }) {
                    Text(stringResource(R.string.ok))
                }
            })
    }
}

@Composable
fun PromptToEnableNFCDialog() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(true) }

    if (showDialog) {
        AlertDialog(onDismissRequest = { /*showDialog = false*/ },
            text = { Text(stringResource(R.string.nfc_disabled_prompt)) },


            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                    }, colors = ButtonDefaults.buttonColors(
                        buttonsColor, contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.enable_nfc))
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showDialog = false
                    }, colors = ButtonDefaults.buttonColors(
                        buttonsColor, contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }

        )
    }
}


@Composable
fun Filters(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {

    val isDropdownExpanded = remember { mutableStateOf(false) }
    val sizeRange = remember { mutableStateOf(0f..100f) }
    val selectedTypes = remember { mutableStateListOf<String>() }

    LaunchedEffect(selectedTypes) {
        viewModel.filterSelectedFilesTypes(selectedTypes)
    }

    LaunchedEffect(sizeRange) {
        viewModel.filterSelectedFilesSize(sizeRange.value)

    }


    Column(
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {


        Button(
            onClick = { isDropdownExpanded.value = true }, colors = ButtonDefaults.buttonColors(
                buttonsColor, contentColor = Color.White
            )
        ) {
            Text(stringResource(id = R.string.filter))
        }

        Column(
            modifier = Modifier
                .padding(start = 24.dp, top = 48.dp)
                .fillMaxSize(),
        ) {


            DropdownMenu(
                modifier = Modifier
                    .background(tilesColor)
                    .padding(horizontal = 32.dp, vertical = 32.dp)
                    .fillMaxWidth(0.85f),
                expanded = isDropdownExpanded.value,
                onDismissRequest = { isDropdownExpanded.value = false }) {


                Column {
                    Text(
                        text = "${stringResource(id = R.string.file_size)} (${stringResource(id = R.string.mb)}): ${sizeRange.value.start.roundToInt()} - ${sizeRange.value.endInclusive.roundToInt()}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    RangeSlider(
                        colors = SliderDefaults.colors(
                            thumbColor = buttonsColor,
                            activeTrackColor = buttonsColor,
                        ),
                        value = sizeRange.value,
                        onValueChange = { sizeRange.value = it },
                        valueRange = 0f..100f
                    )
                }

                Text(
                    text = stringResource(id = R.string.choose_file_types),
                    style = MaterialTheme.typography.titleMedium
                )

                val checkboxWidth = 60.dp

                Column {
                    val types = listOf(
                        "png",
                        "jpeg",
                        "jpg",
                        "gif",
                        "bmp",
                        "tiff",
                        "svg",
                        "mp3",
                        "wav",
                        "ogg",
                        "aac",
                        "flac",
                        "m4a",
                        "mp4",
                        "avi",
                        "mkv",
                        "mov",
                        "wmv",
                        "flv",
                        "bin"
                    )
                    val rows = types.chunked(4)

                    rows.forEach { rowTypes ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            rowTypes.forEach { type ->
                                Row(
                                    modifier = Modifier.width(checkboxWidth),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = selectedTypes.contains(type),
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                selectedTypes.add(type)
                                            } else {
                                                selectedTypes.remove(type)
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedBoxColor = buttonsColor,
                                            uncheckedBoxColor = buttonsColor
                                        )
                                    )
                                    Text(type)
                                }
                                Spacer(modifier = Modifier.weight(10f))
                            }

                        }
                    }

                }
            }
        }
    }
}

