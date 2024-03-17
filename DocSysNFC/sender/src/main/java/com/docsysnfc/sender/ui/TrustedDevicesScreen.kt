package com.docsysnfc.sender.ui

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.docsysnfc.sender.MainViewModel

@Composable
fun TrustedDevicesScreen(navController: NavController, viewModel: MainViewModel, context: Context) {
    TODO("Not yet implemented")
    viewModel.disableNFCReaderMode(context as Activity)
}
