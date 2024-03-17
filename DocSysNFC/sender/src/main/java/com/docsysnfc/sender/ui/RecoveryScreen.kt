package com.docsysnfc.sender.ui

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.docsysnfc.sender.MainViewModel

@Composable
fun RecoveryScreen(navController: NavController, viewModel: MainViewModel, context: Context) {
    viewModel.disableNFCReaderMode(context as Activity)
}
