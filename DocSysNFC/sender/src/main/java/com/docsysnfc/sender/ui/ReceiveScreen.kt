package com.docsysnfc.sender.ui

import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.docsysnfc.sender.MainViewModel
import kotlinx.coroutines.delay


@Composable
fun ReceiveScreen(navController: NavController, viewModel: MainViewModel, context: Context) {

    updateNfcDataTransferState( context,false)

    Column(
        modifier = Modifier
            .background(Color(0xFF0CCFBF))
            .fillMaxSize()
    ){
        Text(text ="ReceiveScreen")

        AutoSizingBox()


    }

}


@Composable
fun AutoSizingBox() {
    // Use an infinite transition to animate the size
    val infiniteTransition = rememberInfiniteTransition()

    // Animate the size between 100.dp and 110.dp
    val size by infiniteTransition.animateValue(
        initialValue = 100.dp,
        targetValue = 110.dp,
        typeConverter = Dp.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Box with the animated size
    Box(
        modifier = Modifier
            .size(size)
            .background(Color.Blue)
    )
}

