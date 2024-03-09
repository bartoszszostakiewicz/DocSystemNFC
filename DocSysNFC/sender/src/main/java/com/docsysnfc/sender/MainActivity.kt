package com.docsysnfc.sender

import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.docsysnfc.R
import com.docsysnfc.sender.model.NFCStatus
import com.docsysnfc.sender.model.NFCtest
import com.docsysnfc.sender.ui.AppNavigation
import com.docsysnfc.sender.ui.SendScreen
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch


private const val TAG = "SendActivity123"

class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        lifecycleScope.launch {
            viewModel.startServiceEvent.collect { ndefMessage ->
                ndefMessage?.let {
                    startNFCService(it)
                    viewModel.resetServiceEvent()
                }
            }
        }

        setContent {
            AppNavigation(viewModel = viewModel , context = this@MainActivity)
        }
    }

    private fun startNFCService(ndefMessage: String) {
        val intent = Intent(this, NFCtest::class.java).apply {
            putExtra("ndefMessage", ndefMessage)
        }
        startService(intent)
    }


    override fun onDestroy() {
        super.onDestroy()
        stopNFCService()
    }

    override fun onResume() {
        super.onResume()
        viewModel.setActivityVisibility(true)
        getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit {
            putBoolean("isActivityVisible", true)
            apply()
        }

    }

    override fun onPause() {
        super.onPause()
        viewModel.setActivityVisibility(false)
        getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit {
            putBoolean("isActivityVisible", false)
            apply()
        }

    }

    private fun stopNFCService() {
        val intent = Intent(this, NFCtest::class.java)
        stopService(intent)
    }

}




