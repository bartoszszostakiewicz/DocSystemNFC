package com.docsysnfc.sender.ui


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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.NavController
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.FractionalThreshold
import androidx.wear.compose.material.rememberSwipeableState
import androidx.wear.compose.material.swipeable
import com.docsysnfc.R
import com.docsysnfc.sender.MainViewModel
import com.docsysnfc.sender.model.AuthenticationState
import com.docsysnfc.sender.model.File
import com.docsysnfc.sender.model.NFCStatus
import com.docsysnfc.sender.model.NFCSysScreen
import com.docsysnfc.sender.ui.theme.appBarColorTheme
import com.docsysnfc.sender.ui.theme.backgroundColor
import com.docsysnfc.sender.ui.theme.buttonsColor
import com.docsysnfc.sender.ui.theme.tilesColor
import com.docsysnfc.sender.ui.theme.whiteColor
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
fun HomeScreenTopBar(title: String, navController: NavController) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = whiteColor,
                style = MaterialTheme.typography.titleLarge
            )
        },
        colors = TopAppBarDefaults.smallTopAppBarColors(
            containerColor = appBarColorTheme,
            titleContentColor = whiteColor
        ),
        actions = {
            LogoutButton(navController = navController)
        }
    )
}

@Composable
fun LogoutButton(navController: NavController) {
    Button(
        onClick = {
            FirebaseAuth.getInstance().signOut()
            navController.navigate(NFCSysScreen.Login.name)
        },
        modifier = Modifier,

        colors = ButtonDefaults.buttonColors(
            buttonsColor,
            contentColor = Color.White
        )
    ) {
        Icon(
            painter = painterResource(R.drawable.log_out),
            contentDescription = stringResource(id = R.string.log_out),
        )
    }

}

@Composable
fun SwipeableTiles(
    selectedFiles: List<File>,
    homeViewModel: MainViewModel,
    navController: NavController
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
    file: File,
    viewModel: MainViewModel,
    navController: NavController
) {
    val tileWidth = integerResource(id = R.integer.tileWidth).dp
    val tileHeight = integerResource(id = R.integer.tileHeight).dp

    val state = rememberSwipeableState(initialValue = 0)
    val offset = state.offset.value
    val sizePx = with(LocalDensity.current) { tileHeight.toPx() }
    val anchors = mapOf(0f to 0, sizePx to 1)

    val isCipher = remember { mutableStateOf(false) }



    val uploadComplete by viewModel.uploadComplete.collectAsState()

    LaunchedEffect(uploadComplete) {
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



    Column(
        modifier = Modifier
            .padding(16.dp)
            .width(tileWidth)
            .height(tileHeight)
            .background(tilesColor, shape = RoundedCornerShape(16.dp))
//            .offset { IntOffset(0, -offset.roundToInt()) }
            .swipeable(
                state = state,
                anchors = anchors,
                thresholds = { _, _ -> FractionalThreshold(0.3f) },
                orientation = Orientation.Horizontal
            )
    ) {
        if (state.currentValue == 1) {
            // Logika usuwania pliku
            viewModel.deleteSelectedFile(file)
        }

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

                Column(
                    modifier = Modifier
                        .padding(start = 35.dp)
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {


                    Icon(
                        painter = painterResource(id = getIconFile(file)),
                        contentDescription = "plik",
                        modifier = Modifier
                            .graphicsLayer {
                                alpha = 0.8f
                                scaleX = 0.8f
                                scaleY = 0.8f
                            }
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp)
                    ) {

                        Text(
                            text = "Name: ${
                                file.name
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
                    Icon(
                        painter = painterResource(if (isCipher.value) R.drawable.cipher_on else R.drawable.cipher_off),
                        contentDescription = "Icon",
                        modifier = Modifier
                            .size(30.dp)
                            .align(Alignment.CenterHorizontally)
                            .clickable {
                                isCipher.value = !isCipher.value
                                viewModel.handleEncryption(file, isCipher.value)
                            }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp)) // Add vertical spacing


        Button(
            onClick = {

                Log.d("TAG123", "linkztondhuehui: ${file.url.toString()}")
                Log.d("TAG123", "linkztondhuehui: ${viewModel.modelSelectedFiles.value.size}")
                Log.d("TAG123", "linkztondhuehui: ${viewModel.modelSelectedFiles.value[0].url}")

                viewModel.pushFileIntoCloud(file, isCipher.value)

            },
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                buttonsColor,
                contentColor = Color.White
            )

        ) {
            Text(text = stringResource(id = R.string.send_file))
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
        },
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            buttonsColor,
            contentColor = Color.White
        )
    ) {
        Text(text = stringResource(id = R.string.choose_file))
    }
}


@Composable
fun HomeScreen(navController: NavController, viewModel: MainViewModel, context: Context) {

    val authenticationState by viewModel.authenticationState.collectAsState()

    if(authenticationState == AuthenticationState.FAILURE || authenticationState == AuthenticationState.UNKNOWN){
        navController.navigate(NFCSysScreen.Login.name)
    }

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

    Scaffold(
        topBar = { HomeScreenTopBar(title = "flowtouch", navController = navController) },
        floatingActionButton = {
            Box(modifier = Modifier.fillMaxSize()) {
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .background(backgroundColor)
                .padding(innerPadding)
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
    contentDescription: String? = null
) {
    Column {

        Button(
            onClick = onClick,
            modifier = modifier

                .scale(0.65f),
            colors = ButtonDefaults.buttonColors(
                buttonsColor,
                contentColor = whiteColor
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
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun NFCNotSupportedDialog() {

    var showDialog by remember { mutableStateOf(true) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            text = { Text(stringResource(R.string.nfc_not_supported)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                    }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}

@Composable
fun PromptToEnableNFCDialog() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(true) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            text = { Text(stringResource(R.string.nfc_disabled_prompt)) },


            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                    },
                    colors = ButtonDefaults.buttonColors(
                        buttonsColor,
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.enable_nfc))
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        buttonsColor,
                        contentColor = Color.White
                    )
                )
                {
                    Text(stringResource(R.string.cancel))
                }
            }

        )
    }
}


