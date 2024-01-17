package com.docsysnfc.receiver

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelProvider
import com.docsysnfc.receiver.ui.theme.DocSysNFCTheme
import com.qifan.readnfcmessage.parser.NdefMessageParser

class MainActivity : ComponentActivity() {

    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var pendingIntent: PendingIntent
    private val nfcViewModel by lazy { ViewModelProvider(this)[NfcViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        nfcAdapter = NfcAdapter.getDefaultAdapter(this)?.also {
            if (!it.isEnabled) {
                Log.e("NFC1234", "NFC is disabled.")
            }
        } ?: run {
            Log.e("NFC1234", "NFC is not supported on this device.")
            return
        }

        if (checkNFCEnable()) {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            Log.e("NFC1234", "NFC is not enabled.")
        }

        setContent {
            DocSysNFCTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android",nfcViewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter.disableForegroundDispatch(this)
    }

    private fun checkNFCEnable(): Boolean {
        return nfcAdapter.isEnabled
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)?.also { rawMessages ->
                val messages: List<NdefMessage> = rawMessages.map { it as NdefMessage }
                nfcViewModel.processNfcMessages(messages)
            }
        }
    }


}

@Composable
fun Greeting(name: String,viewModel: NfcViewModel, modifier: Modifier = Modifier) {
    val text by viewModel.nfcMessages.collectAsState()
    Text(
        text = text,
        modifier = modifier
    )
}
