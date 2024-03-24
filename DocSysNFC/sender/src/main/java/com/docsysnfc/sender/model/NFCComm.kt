package com.docsysnfc.sender.model

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.NfcAdapter.FLAG_READER_NFC_A
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import com.docsysnfc.sender.MainActivity
import com.docsysnfc.sender.MainViewModel
import java.io.UnsupportedEncodingException
import java.math.BigInteger

class NFCComm() {

    private var nfcAdapter: NfcAdapter? = null

    fun initNFCAdapter(context: Context) {
        nfcAdapter = NfcAdapter.getDefaultAdapter(context)

    }

    fun enableNFCReader(activity: Activity,viewModel: MainViewModel) {
        val readerCallback = NfcAdapter.ReaderCallback { tag ->

            // Obsługa odczytu NFC
            if(tag != null){
                Log.d("NFC123", "NFC Data: $tag")
                viewModel.setNFCTag(tag)
            }
        }
        val READER_FLAGS = FLAG_READER_NFC_A
        nfcAdapter?.enableReaderMode(activity, readerCallback, READER_FLAGS, null)
        if(nfcAdapter?.isEnabled == true){
            Log.d("NFC123", "NFC enabled")
        }else{
            Log.d("NFC123", "NFC disabled")
        }
    }

    fun disableNFCReader(context: Context) {
//        nfcAdapter?.disableReaderMode(context as Activity)
    }





}