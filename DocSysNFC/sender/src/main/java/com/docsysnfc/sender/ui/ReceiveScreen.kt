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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.docsysnfc.sender.MainViewModel
import com.docsysnfc.sender.model.File
import com.docsysnfc.sender.ui.theme.backgroundColor
import kotlinx.coroutines.delay


@Composable
fun ReceiveScreen(navController: NavController, viewModel: MainViewModel, context: Context) {

    updateNfcDataTransferState( context,false)

    val selectedFiles by viewModel.modelSelectedFiles.collectAsState()



    ReceiveFileScreen(fileList = selectedFiles)



}

@Composable
fun ReceiveFileScreen(fileList: List<File>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(fileList) { file ->
            FileCard(file = file)
        }
    }
}

@Composable
fun FileCard(file: File) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text(text = "Otrzymany plik:", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Nazwa pliku: ${file.name}", style = MaterialTheme.typography.headlineSmall)
            Text(text = "Rozmiar: ${String.format("%.2f", file.size)} MB", style = MaterialTheme.typography.headlineSmall)
            Text(text = "Typ: ${file.type}", style = MaterialTheme.typography.headlineSmall)

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = { /* TODO: Implement file download */ }) {
                Text(text = "Pobierz")
            }

//            TextButton(onClick = { /* TODO: Implement opening URL in browser */ }) {
//                Text(text = "Otwórz w przeglądarce")
//            }
        }
    }
}
