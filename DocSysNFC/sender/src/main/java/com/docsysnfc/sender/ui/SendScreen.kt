package com.docsysnfc.sender.ui

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
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
    Send(title = R.string.send),
    SendDetails(title = R.string.send_details),
    Receive(title = R.string.receive),
}


@Composable
fun AppNavigation(viewModel: MainViewModel, context: Context) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NFCSysScreen.Send.name
    ) {


        composable(NFCSysScreen.Send.name) { SendScreen(navController, viewModel, context) }

        composable(
            route = "${NFCSysScreen.SendDetails.name}/{index}",
            arguments = listOf(
                navArgument("index") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val index = backStackEntry.arguments?.getInt("index")
            if (index != null) {
                SendDetailsScreen(navController, viewModel, context, index)
            } else {
                // Obsługa błędu, jeśli index jest null
            }
        }
        composable(NFCSysScreen.Receive.name) { ReceiveScreen(navController, viewModel, context) }


    }
}


@Composable
fun SendScreen(navController: NavController,viewModel: MainViewModel, context: Context){
    Column(
        modifier = Modifier
            .background(backgroundColor)
    ) {

        val activeUri by viewModel.urlStateFlow.collectAsState()

        val selectedFiles by viewModel.modelSelectedFiles.collectAsState()


        Text(text =activeUri.toString())


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




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTiles(selectedFiles: List<File>, context: Context, homeViewModel: MainViewModel, navController: NavController) {
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

    var sliderValues by remember { mutableStateOf(List(numberOfTiles) { 0f }) }



    var newSliderValues by remember { mutableStateOf(List(numberOfTiles) { 0f }) }


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
            // Pobierz wybrany plik na podstawie indeksu
            //val selectedFile = selectedFiles[index]
//            var sliderValue = sliderValues[index] // ta linnia wywala ten blad

            var sliderValue by remember { mutableStateOf(0f) }

            if (index in sliderValues.indices) {
                sliderValue = sliderValues[index]
            }

            val specialColor = if (specialTileState) Color.Red else tilesColor

            // Calculate the new size based on the slider value
            val newSize = 200.dp
//                (if (sliderValue == 1f) 250.dp else tileWidth).takeIf { !specialTileState }
//                    ?: 300.dp

            // Calculate the new offset based on the specialTileIndex
            val specialOffsetX = if (specialTileIndex == index) {
                ((tileWidth - newSize) / 2).value
            } else {
                0f
            }

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .width(newSize)
                    .height(newSize)
                    .offset { IntOffset(offsetX.roundToInt() + specialOffsetX.roundToInt(), 0) }
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



                Slider(
                    value = sliderValue,
                    onValueChange = {

                        sliderValue = it
                        vibrator.vibrate(vibrationEffect)

                        // Check if slider is at max value and change the special tile index
                        if (sliderValue == 1f) {
                            specialTileIndex = index

                        }
                    },
                    modifier = Modifier
                        .scale(1f, 5f)
                        .fillMaxWidth(),

                    colors = SliderDefaults.colors(
                        activeTrackColor = Color.Green,
                        thumbColor = Color.Transparent
                    ),
                )

                ChoosenFile(selectedFile = selectedFiles[index], viewModel = homeViewModel, navController = navController)

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

            navController.navigate("${NFCSysScreen.SendDetails.name}/${viewModel.modelSelectedFiles.value.indexOf(selectedFile)}")


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


