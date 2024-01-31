package com.docsysnfc.sender.ui

import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateValue
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.navigation.NavController
import com.docsysnfc.R
import com.docsysnfc.sender.MainViewModel
import com.docsysnfc.sender.model.File


@Composable
fun SendScreen(
    navController: NavController,
    viewModel: MainViewModel,
    context: Context,
    index: Int
) {

    var isCipher by rememberSaveable { mutableStateOf(false) }

    val file = remember { viewModel.modelSelectedFiles.value[index] }
    val animate by remember { mutableStateOf(/*viewModel.fileIsInCloud(file)*/ true) }
    updateNfcDataTransferState(context, animate)

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val iconSize = screenWidth * 0.25f

    val scale = if (animate) {
        rememberInfiniteTransition(
            label = ""
        ).animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ), label = ""
        ).value
    } else 1f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (animate) Color(0xFF69EE85) else Color(0xFF532B2B))
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
                .background(Color(0xFFB4E5FF))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

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
                            painter = painterResource(id = R.drawable.plik), // Replace with the actual drawable resource ID
                            contentDescription = "Icon",
                            modifier = Modifier
                                .size(iconSize)
                                .padding(top = 8.dp, start = 8.dp)
                        )


                    }
                }

                Column {


                    Column(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.End


                    ) {

                        Text(
                            text = file.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.Black
                        )
                        Text(
                            text = file.type,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = file.size.toString(),
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }


                }
            }

            Spacer(modifier = Modifier.weight(1f, false))


            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                    .fillMaxWidth() // Zapewnia, że `Row` wypełni dostępną szerokość
                    .padding(8.dp)  // Dostosuj według potrzeb
            ) {
                Column {
                    Icon(
                        painter = painterResource(if (isCipher) R.drawable.cipher_on else R.drawable.cipher_off),
                        contentDescription = "Icon",
                        modifier = Modifier
                            .size(36.dp)
//                            .padding(end = 8.dp) // Dostosuj margines, jeśli jest potrzebny
                    )
                    Text(
                        text = "Encryption",
                        modifier = Modifier.padding(end = 8.dp) // Dostosuj margines, jeśli jest potrzebny
                    )
                }

                Switch(
                    checked = isCipher,
                    onCheckedChange = {
                        isCipher = it
                        updateNfcDataCipher(context, it)
                    }
                    // Nie potrzebujesz tu modyfikatora, jeśli już masz padding w Row
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                    .fillMaxWidth() // Zapewnia, że `Row` wypełni dostępną szerokość
                    .padding(8.dp)  // Dostosuj według potrzeb
            ) {
                Column {
                    Icon(
                        painter = painterResource(R.drawable.cloud11),
                        contentDescription = "Icon",
                        modifier = Modifier
                            .size(36.dp)
//                            .padding(end = 8.dp) // Dostosuj margines, jeśli jest potrzebny
                    )
                    Text(
                        text = "Cloud",
                        modifier = Modifier.padding(end = 8.dp) // Dostosuj margines, jeśli jest potrzebny
                    )
                }

                Icon(
                    painter = painterResource(R.drawable.ok),
                    contentDescription = "Icon",
                    modifier = Modifier
                        .size(36.dp)

                )
            }

        }
    }
}

fun updateNfcDataCipher(context: Context, isActive: Boolean) {
    context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit {
        putBoolean("isCipher", isActive)
        apply()
    }
}


fun getIconFile(file: File) : Int{

    var icon: Int = R.drawable.plik

    if(file.type == "png" || file.type == "jpeg" || file.type == "jpg" || file.type == "gif"){
        icon  = R.drawable.img1
    }
    else if(file.type == "mp3" || file.type == "wav" || file.type == "ogg"){
        icon  = R.drawable.img1
    }
    else if(file.type == "mp4" || file.type == "avi" || file.type == "mkv"){
        icon  = R.drawable.movie2
    }

    return icon
}
