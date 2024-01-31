package com.docsysnfc.sender.ui

import android.content.ClipDescription
import android.content.Context
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box


import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.materialIcon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.docsysnfc.R
import com.docsysnfc.sender.MainViewModel
import com.docsysnfc.sender.model.File
import com.docsysnfc.sender.ui.theme.backgroundColor
import com.docsysnfc.sender.ui.theme.backgroundColor2
import com.docsysnfc.sender.ui.theme.buttonsColor
import com.docsysnfc.sender.ui.theme.tilesColor
import kotlin.math.roundToInt


enum class NFCSysScreen(@StringRes val title: Int) {
    Start(title = R.string.app_name),
    Home(title = R.string.send),
    Send(title = R.string.send_details),
    Receive(title = R.string.receive),
}


//change location of this function
fun updateNfcDataTransferState(context: Context, isActive: Boolean) {
    context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit {
        putBoolean("isNfcDataTransferActive", isActive)
        apply()
    }
}

@Composable
fun AppNavigation(viewModel: MainViewModel, context: Context) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NFCSysScreen.Home.name
    ) {


        composable(NFCSysScreen.Home.name) { HomeScreen(navController, viewModel, context) }

        composable(
            route = "${NFCSysScreen.Send.name}/{index}",
            arguments = listOf(
                navArgument("index") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val index = backStackEntry.arguments?.getInt("index")
            if (index != null) {
                SendScreen(navController, viewModel, context, index)
            } else {
                // Obsługa błędu, jeśli index jest null
            }
        }
        composable(NFCSysScreen.Receive.name) { ReceiveScreen(navController, viewModel, context) }


    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenTopBar(title: String) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = Color.White, // Ustaw biały lub inny kolor tekstu
                style = MaterialTheme.typography.titleLarge // Dostosuj według potrzeb
            )
        },
        colors = TopAppBarDefaults.smallTopAppBarColors(
            containerColor = Color(0xA153ABF0), // Ustaw kolor tła AppBar
            titleContentColor = Color.White // Ustaw kolor tekstu tytułu, jeśli potrzebujesz
        ),
        actions = {







        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTiles(
    selectedFiles: List<File>,
    context: Context,
    homeViewModel: MainViewModel,
    navController: NavController
) {

    val tileWidth = 200.dp // Width of each tile
    val tileHeight = 200.dp // Height of each tile


    var isSwiping by remember { mutableStateOf(false) }
    var offsetX by remember { mutableFloatStateOf(0f) }

    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    val vibrationEffect = VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)

    val numberOfTiles = selectedFiles.size

    // State to track whether a tile is in a special state
    var specialTileState by remember { mutableStateOf(false) }

    var specialTileIndex by remember { mutableIntStateOf(-1) }




    LazyRow(
        modifier = Modifier
            .background(backgroundColor2)
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTransformGestures { _, _, _, offset ->
                    if (isSwiping) {
                        offsetX += offset
                    } else {
                        isSwiping = true
                    }
                }
            }
    ) {
        items(numberOfTiles) { index ->


            val specialColor = if (specialTileState) Color.Red else tilesColor


            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .width(tileWidth)
                    .height(tileHeight)
                    .offset {
                        IntOffset(offsetX.roundToInt(), 0)
                    }
                    .background(specialColor, shape = RoundedCornerShape(16.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.plik),
                        contentDescription = "plik",
                        modifier = Modifier
                            .graphicsLayer {
                                alpha = 0.5f
                                scaleX = 0.5f
                                scaleY = 0.5f
                            }
                    )

                    Spacer(modifier = Modifier.width(8.dp)) // Add spacing between Icon and Text

                    Column {

                        Text(
                            text = "Name: ${
                                selectedFiles[index].name
                            }", fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Type: ${
                                selectedFiles[index].type
                            }", color = Color.Gray
                        )
                        Text(
                            text = "Size: ${
                                selectedFiles[index].size
                            }MB", color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp)) // Add vertical spacing


                ChoosenFile(
                    selectedFile = selectedFiles[index],
                    viewModel = homeViewModel,
                    navController = navController
                )

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
        uri?.let { homeViewModel.onFilePicked(it) }
    }
    Button(
        onClick = {
            filePickerLauncher.launch("image/*")
            homeViewModel.chooseFile()
        },
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            buttonsColor,
            contentColor = Color.White
        )
    ) {
        Text(text = "Choose File")
    }
}


@Composable
fun ChoosenFile(
    selectedFile: File?,
    viewModel: MainViewModel,
    navController: NavController
) {


    Button(
        onClick = {

            Log.d("TAG123", "linkztondhuehui: ${selectedFile?.url.toString()}")

            navController.navigate(
                "${NFCSysScreen.Send.name}/${
                    viewModel.modelSelectedFiles.value.indexOf(
                        selectedFile
                    )
                }"
            )

        },
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            buttonsColor,
            contentColor = Color.White
        )

    ) {
        Text(text = "Details")
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: MainViewModel, context: Context) {
    Scaffold(
        topBar = { HomeScreenTopBar(title = "flowtouch") },
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


            updateNfcDataTransferState(context, false)

            val activeUri by viewModel.urlStateFlow.collectAsState()

            val selectedFiles by viewModel.modelSelectedFiles.collectAsState()


            //Text(text = activeUri.toString())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 1.dp), // Adjust padding as needed
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(
                    onClick = { /*TODO*/ },
                    iconId = R.drawable.plik,
                    contentDescription = context.resources.getString(R.string.file_icon),
                    modifier = Modifier.padding(1.dp)
                )
                IconButton(
                    onClick = { /*TODO*/ },
                    iconId = R.drawable.plik,
                    contentDescription = context.resources.getString(R.string.file_icon),
                    modifier = Modifier.padding(1.dp)
                )

            }

            // Adding second row of buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp), // Adjust padding as needed
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {

                IconButton(
                    onClick = { /*TODO*/ },
                    iconId = R.drawable.latest,
                    contentDescription = context.resources.getString(R.string.latest_icon)
                )


                IconButton(
                    onClick = { /*TODO*/ },
                    iconId = R.drawable.settings2,
                    contentDescription = context.resources.getString(R.string.settings_icon)
                )
            }





            Spacer(modifier = Modifier.weight(1f, true))

            SwipeableTiles(
                selectedFiles = selectedFiles,
                context = context,
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
                contentColor = Color.White
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
