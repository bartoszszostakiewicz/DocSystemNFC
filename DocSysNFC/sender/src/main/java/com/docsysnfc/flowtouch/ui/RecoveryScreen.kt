package com.docsysnfc.flowtouch.ui

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.docsysnfc.flowtouch.MainViewModel

@Composable
fun RecoveryScreen(navController: NavController, viewModel: MainViewModel, context: Context) {
    viewModel.disableNFCReaderMode(context as Activity)
}
