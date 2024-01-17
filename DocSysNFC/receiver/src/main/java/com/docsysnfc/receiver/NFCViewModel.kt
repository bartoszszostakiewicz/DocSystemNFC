package com.docsysnfc.receiver

import android.nfc.NdefMessage
import androidx.lifecycle.ViewModel
import com.qifan.readnfcmessage.parser.NdefMessageParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NfcViewModel : ViewModel() {
    // MutableStateFlow to hold and update the NFC message
    private val _nfcMessages = MutableStateFlow("Initial NFC Message")

    // Expose an immutable StateFlow
    val nfcMessages = _nfcMessages.asStateFlow()

    fun processNfcMessages(messages: List<NdefMessage>) {
        // Parse messages and update the StateFlow
        val parsedMessage = parserNDEFMessage(messages)
        _nfcMessages.value = parsedMessage
    }
    private fun parserNDEFMessage(messages: List<NdefMessage>): String {
        val builder = StringBuilder()
        val records = NdefMessageParser.parse(messages[0])
        val size = records.size

        for (i in 0 until size) {
            val record = records[i]
            val str = record.str()
            builder.append(str).append("\n")
        }
        return builder.toString()
    }
}