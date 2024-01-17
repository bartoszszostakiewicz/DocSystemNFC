package com.docsysnfc.sender

import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.docsysnfc.sender.model.NFCtest
import com.docsysnfc.sender.ui.SendScreen
import com.docsysnfc.sender.viewmodel.HomeViewModel
import kotlinx.coroutines.launch


private const val TAG = "SendActivity123"

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null

    private val homeViewModel by viewModels<HomeViewModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)?.also {
            if (!it.isEnabled) {
                Log.e("NFC123", "NFC is disabled.")
            }
        } ?: run {
            Log.e("NFC123", "NFC is not supported on this device.")
            return
        }
        lifecycleScope.launch {
            homeViewModel.startServiceEvent.collect { ndefMessage ->
                ndefMessage?.let {
                    startNFCService(it)
                    // Resetowanie wartości po uruchomieniu usługi
                    homeViewModel.resetServiceEvent()
                }
            }
        }

        setContent {
          SendScreen(viewModel = homeViewModel, context = this@MainActivity)
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
        homeViewModel.setActivityVisibility(true)
        getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit {
            putBoolean("isActivityVisible", true)
            apply()
        }

    }

    override fun onPause() {
        super.onPause()
        homeViewModel.setActivityVisibility(false)
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





