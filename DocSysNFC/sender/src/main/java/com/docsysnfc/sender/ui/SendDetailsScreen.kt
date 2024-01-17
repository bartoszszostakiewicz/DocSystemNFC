package com.docsysnfc.sender.ui

import android.content.Context
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.docsysnfc.R
import com.docsysnfc.sender.MainViewModel



@Composable
fun SendDetailsScreen(
    navController: NavController,
    viewModel: MainViewModel,
    context: Context,
    index: Int
) {

    val file = remember { viewModel.modelSelectedFiles.value[index] }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0CCFBF)) // Podstawowy kolor tła
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .clip(RoundedCornerShape(12)) // Dla zaokrąglonych rogów można użyć innej formy, np. RoundedCornerShape
                .fillMaxWidth(0.9f) // Ustawienie szerokości na 90% maksymalnej szerokości ekranu
                .fillMaxHeight(0.5f) // Ustawienie wysokości na 70% maksymalnej wysokości ekranu
                .background(Color(0xFFB4E5FF))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                // Użyj odpowiedniej ikony
                painter = painterResource(id = R.drawable.plik),
                contentDescription = "Icon",
                modifier = Modifier
                    .size(40.dp)
                    .padding(8.dp)
            )
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