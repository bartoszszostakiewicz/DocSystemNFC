package com.docsysnfc.sender.ui

import android.app.Activity
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.navigation.NavController
import com.docsysnfc.R
import com.docsysnfc.sender.MainViewModel
import com.docsysnfc.sender.model.File
import com.docsysnfc.sender.ui.theme.backgroundColor
import com.docsysnfc.sender.ui.theme.fileIsNotInCloudColor
import com.docsysnfc.sender.ui.theme.sendingFileColor

fun updateNfcDataCipher(context: Context, isActive: Boolean) {
    context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit {
        putBoolean("isCipher", isActive)
        apply()
    }
}

fun getIconFile(file: File): Int {

    var icon: Int = R.drawable.plik

    when (file.type) {
        "png", "jpeg", "jpg", "gif", "bmp", "tiff", "svg" -> {
            icon = R.drawable.img1
        }

        "mp3", "wav", "ogg", "aac", "flac", "m4a" -> {
            icon = R.drawable.note2
        }

        "mp4", "avi", "mkv", "mov", "wmv", "flv" -> {
            icon = R.drawable.movie2
        }
    }

    return icon
}


@Composable
fun SendScreen(
    navController: NavController,
    viewModel: MainViewModel,
    context: Context,
    index: Int
) {
    viewModel.disableNFCReaderMode(context as Activity)

    /************TODO ADD VIBRATION during sending files***************/

    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    val vibrationEffect = VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)

    /***************************/

    var isCipher by rememberSaveable { mutableStateOf(false) }

    val file = remember { viewModel.modelSelectedFiles.value[index] }

    //if animate is true, the file is ready to transfer and is in the cloud
    val animate by remember { mutableStateOf(/*viewModel.fileIsInCloud(file)*/ true) }

    //data is ready to transfer
    setSenderMode(context, animate)

    //check public key


    val iconSize = LocalConfiguration.current.screenWidthDp.dp * 0.40f

    val scale = if (animate) {
        rememberInfiniteTransition(
            label = ""
        ).animateFloat(
            initialValue = 0.99f,
            targetValue = 1.01f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ), label = ""
        ).value
    } else 1f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (animate) sendingFileColor else fileIsNotInCloudColor)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(RoundedCornerShape(12))
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.5f)
                .background(backgroundColor)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            Spacer(modifier = Modifier.weight(1f, true))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {

                Column {
                    Row(
                        modifier = Modifier,
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            painter = painterResource(id = getIconFile(file)), // Replace with the actual drawable resource ID
                            contentDescription = "Icon",
                            modifier = Modifier
                                .size(iconSize)
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Column {
                            Icon(
                                painter = painterResource(if (isCipher) R.drawable.cipher_on else R.drawable.cipher_off),
                                contentDescription = "Icon",
                                modifier = Modifier
                                    .size(36.dp)
                            )
                            Text(
                                text = stringResource(id = R.string.encryption),
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Switch(
                            modifier = Modifier,
                            checked = isCipher,
                            onCheckedChange = {
                                isCipher = it
                                Log.d("TAG123", "click")
                            }
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.cloud11),
                                contentDescription = "Icon",
                                modifier = Modifier
                                    .size(36.dp)
                            )
                            Text(
                                text = stringResource(id = R.string.cloud),
                            )
                        }

                        Icon(
                            painter = painterResource(R.drawable.ok),
                            contentDescription = "Icon",
                            modifier = Modifier
                                .size(36.dp)
                                .weight(0.55f)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f, false))
            }

            Spacer(modifier = Modifier.weight(1f, false))

            Row {
                Column {
                    Text(
                        text = stringResource(R.string.file_name) + stringResource(R.string.colon) + file.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, start = 8.dp)
                    )

                    Text(
                        text = stringResource(R.string.file_size) + stringResource(R.string.colon) + file.size.toString() + stringResource(
                            R.string.mb
                        ),
                        modifier = Modifier.padding(top = 8.dp, start = 8.dp)
                    )
                    Text(
                        text = stringResource(R.string.file_type) + stringResource( R.string.colon) + file.type,
                        modifier = Modifier.padding(top = 8.dp, start = 8.dp)
                    )
                }
            }
        }
    }
}




