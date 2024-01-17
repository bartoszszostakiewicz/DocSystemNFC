package com.docsysnfc.sender.view


import android.content.ContentValues.TAG
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.docsysnfc.sender.ui.theme.DocSysNFCTheme
import com.google.firebase.storage.FirebaseStorage
import java.io.File

val storage = FirebaseStorage.getInstance()


class MainActivity : ComponentActivity() {


    private fun createDirecory(
        folderName: String = "NFC_DocSys",
        path: String = Environment.DIRECTORY_DOCUMENTS
    ): File {
        val folder = File(Environment.getExternalStoragePublicDirectory(path), folderName)



        if (!folder.exists()) {
            if (folder.mkdirs()) {
                Log.d(TAG, "Folder created")
            } else {
                Log.d(TAG, "Folder not created")
            }
        }
        return folder
    }


    override fun onCreate(savedInstanceState: Bundle?) {

        /******************************SENDING DATA******************************/
        // Create a storage reference from our app
        //val storageRef = storage.reference


        /*******************************************************************************************/
        /******************************RECEIVING DATA******************************/

        val pathString = "Zrzut ekranu 2023-06-17 155338.png"


        val folder = createDirecory()
        Log.d(TAG, "Folder path: ${folder.absolutePath}")

        val imgRef = storage.getReferenceFromUrl("gs://docsysnfc.appspot.com/").child(pathString)

        //save img into NFC_DocSys folder
        val localFile = File(folder, pathString)
        imgRef.getFile(localFile).addOnFailureListener { exception ->
            Log.e(TAG, "File download failed: $exception")
        }

        /*******************************************************************************************/


        super.onCreate(savedInstanceState)
        setContent {
            DocSysNFCTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android")

                }
            }
        }
    }
}


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Column {

        Text(
            text = "Hello $name!",
            modifier = modifier
        )
        ButtonWithColor()
    }
}


@Composable
fun ButtonWithColor(text: String = "Button") {

    var myText by remember {
        mutableStateOf(text)
    }
    Button(
        onClick = {
            myText = "CLICKED"


        }, elevation = ButtonDefaults.elevatedButtonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp

        ),
        border = BorderStroke(1.dp, Color.Red)
    )

    {
        Text(
            text = myText,
            color = Color.White
        )
    }
}