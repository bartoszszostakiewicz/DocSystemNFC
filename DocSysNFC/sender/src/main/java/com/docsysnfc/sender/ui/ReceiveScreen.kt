package com.docsysnfc.sender.ui

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.docsysnfc.sender.MainViewModel


@Composable
fun ReceiveScreen(navController: NavController, viewModel: MainViewModel, context: Context) {

    Column {
        Text(text ="ReceiveScreen")
    }

}