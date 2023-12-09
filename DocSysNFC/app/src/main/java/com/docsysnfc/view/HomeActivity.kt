package com.docsysnfc.view

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NdefRecord.createMime
import android.nfc.NfcAdapter
import android.nfc.NfcEvent
import android.nfc.Tag
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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
import com.docsysnfc.R
import com.docsysnfc.ui.theme.backgroundCollor
import com.docsysnfc.ui.theme.backgroundCollor2
import com.docsysnfc.ui.theme.buttonsColor
import com.docsysnfc.ui.theme.tilesColor
import com.docsysnfc.viewmodel.HomeViewModel
import com.google.firebase.storage.FirebaseStorage
import kotlin.math.roundToInt




//class SendActivity : ComponentActivity(), NfcAdapter.CreateNdefMessageCallback {
//
//
//    private val homeViewModel by viewModels<HomeViewModel>()
//
//    private val nfcAdapter: NfcAdapter? by lazy {
//        NfcAdapter.getDefaultAdapter(this)
//    }
//
//    private val fileSelectionLauncher =
//        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//            if (result.resultCode == Activity.RESULT_OK) {
//                val data: Intent? = result.data
//                val selectedFileUri: Uri? = data?.data
//                if (selectedFileUri != null) {
//                    homeViewModel.addFile(selectedFileUri)
//                }
//            }
//        }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//
//
//
//        setContent {
//            Column(
//                modifier = Modifier
//                    .background(backgroundCollor)
//            ) {
//
//                val selectedFiles by homeViewModel.modelSelectedFiles.collectAsState()
//
//
//                Spacer(modifier = Modifier.weight(1f, true))
//
//                SwipableTiles(
//                    selectedFiles = selectedFiles,
//                    context = this@SendActivity,
//                    homeViewModel = homeViewModel
//                )
//
//                Spacer(modifier = Modifier.weight(1f, true))
//
//                ChooseFileButton(
//                    homeViewModel = homeViewModel,
//                    fileSelectionLauncher,
//                    context = this@SendActivity
//
//                )
//
//            }
//        }
//    }
//
//
//
//    override fun createNdefMessage(p0: NfcEvent?): NdefMessage {
//        val text = "Beam me up, Android!\n\n" +
//                "Beam Time: " + System.currentTimeMillis()
//        return NdefMessage(
//            arrayOf(
//                createMime("application/vnd.com.docnfc", text.toByteArray())
//
//            )
//        )
//    }
//
//}


class SendActivity : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_nfc)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null || !nfcAdapter!!.isEnabled) {
            // Informuj użytkownika o braku wsparcia NFC lub że NFC jest wyłączone
            return
        }

        val uri = Uri.parse("http://www.example.com")
        val uriRecord = NdefRecord.createUri(uri)
        val ndefMessage = NdefMessage(arrayOf(uriRecord))

//        nfcAdapter?.setNdefPushMessage(ndefMessage, this)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipableTiles(selectedFiles: List<Uri>, context: Context, homeViewModel: HomeViewModel) {
    val tileWidth = 200.dp // Width of each tile
    val tileHeight = 200.dp // Height of each tile
    var isSwiping by remember { mutableStateOf(false) }
    var offsetX by remember { mutableFloatStateOf(0f) }

    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    val vibrationEffect = VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)

    // State to track whether a tile is in a special state
    var specialTileState by remember { mutableStateOf(false) }

    var specialTileIndex by remember { mutableStateOf(-1) }


    val numberOfTiles = selectedFiles.size


    Log.d("filesize", "rekompozycja = essa")

    LazyRow(
        modifier = Modifier
            .background(backgroundCollor2)
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

            var sliderValue by remember { mutableStateOf(0f) }

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
                                homeViewModel.getNameFile(
                                  
                                    selectedFiles[index]
                                )
                            }", fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Type: ${
                                homeViewModel.getTypeFile(
                                   
                                    selectedFiles[index]
                                )
                            }", color = Color.Gray
                        )
                        Text(
                            text = "Size: ${
                                homeViewModel.getSizeFile(
                                    
                                    selectedFiles[index]
                                )
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

            }
        }
    }
}


@Composable
fun ChooseFileButton(
    homeViewModel: HomeViewModel,
    fileSelectionLauncher: ActivityResultLauncher<Intent>,
    context: Context
) {
    Button(
        onClick = {
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
    selectedFile: Uri?,
    firebaseStorage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    val fileName = selectedFile?.lastPathSegment ?: "No file selected"

    Button(
        onClick = {


            val uriFile = Uri.parse("$selectedFile") // file path
            val fileRef = firebaseStorage.reference.child("test.jpg")

            fileRef.putFile(uriFile)
                .addOnSuccessListener { taskSnapshot ->
                    // Get a URL to the uploaded content
                    val downloadUrl: Uri = taskSnapshot.uploadSessionUri!!
                    Log.d("TAG123", "onCreate: $downloadUrl")
                }
                .addOnFailureListener {
                    // Handle unsuccessful uploads
                    // ...
                    Log.d("TAG123", "onCreate: $it")
                }

        },
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            buttonsColor,
            contentColor = Color.White
        )

    ) {
        Text(text = fileName)
    }
}





