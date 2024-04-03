package com.docsysnfc.flowtouch.ui

import android.app.Activity
import android.content.Context
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.docsysnfc.R
import com.docsysnfc.flowtouch.MainViewModel
import com.docsysnfc.flowtouch.model.File
import com.docsysnfc.flowtouch.ui.theme.backgroundColor
import com.docsysnfc.flowtouch.ui.theme.buttonsColor
import com.docsysnfc.flowtouch.ui.theme.deleteButtonsColor
import com.docsysnfc.flowtouch.ui.theme.tilesColor


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

    val uiState by viewModel.uiState.collectAsState()


    var showEncryptionDialog by remember { mutableStateOf(false) }

    val file = remember { uiState.modelSelectedFiles[index] }


    LaunchedEffect(uiState.additionalEncryption) {
        showEncryptionDialog = uiState.additionalEncryption
    }



    if (showEncryptionDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(stringResource(id = R.string.key_required))
            },
            text = {
                Text(stringResource(id = R.string.key_required_desc))
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cipherSessionKey(file)
                        showEncryptionDialog = false
                    },
                    enabled = uiState.publicKey.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        buttonsColor, contentColor = Color.White
                    ),
                ) {
                    Text(stringResource(id = R.string.submit_pkey) + stringResource(R.string.colon)+"\n" + uiState.publicKeyOwner)
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        viewModel.setAdditionalEncryption(false)
                        showEncryptionDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        deleteButtonsColor, contentColor = Color.White
                    ),
                ) {
                    Text(stringResource(id = R.string.disable_aencryption))
                }
            }


        )
    }
    if ((uiState.additionalEncryption && uiState.publicKey.isNotEmpty()) || !uiState.additionalEncryption) {

        viewModel.disableNFCReaderMode(context as Activity)

        val animate by remember { mutableStateOf(/*viewModel.fileIsInCloud(file)*/ false) }


        setSenderMode(context, true)


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
                .background(backgroundColor)
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
                    .background(tilesColor)
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
                    horizontalArrangement = Arrangement.Center
                ) {

                    Column {
                        Row(
                            modifier = Modifier,
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(
                                painter = painterResource(id = getIconFile(file)),
                                contentDescription = stringResource(id = R.string.file_icon),
                                modifier = Modifier
                                    .size(iconSize)
                                    .align(Alignment.CenterVertically)

                            )
                        }
                    }
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
                            text = stringResource(R.string.file_type) + stringResource(R.string.colon) + file.type,
                            modifier = Modifier.padding(top = 8.dp, start = 8.dp)
                        )
                    }
                }
            }
        }
    } else {
        viewModel.enableNFCReaderMode(context as Activity)
    }
}




