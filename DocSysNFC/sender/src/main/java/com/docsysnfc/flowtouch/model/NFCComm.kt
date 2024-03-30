package com.docsysnfc.flowtouch.model

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.NfcAdapter.FLAG_READER_NFC_A
import android.util.Log
import com.docsysnfc.flowtouch.MainViewModel

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

//    fun readTag(tag: Tag, mainViewModel: MainViewModel): ByteArray? {
//        IsoDep.get(tag)?.let { isoDep ->
//            try {
//                isoDep.connect()
//                // Tutaj należy wysłać odpowiednie polecenie APDU do karty.
//                // Poniżej znajduje się przykładowe polecenie APDU, które może nie działać dla Twojej karty
//                // bez odpowiednich dostosowań:
//                val command = byteArrayOf(
//                    0x00.toByte(), // CLA: klasa polecenia
//                    0xA4.toByte(), // INS: SELECT
//                    0x04.toByte(), // P1: Wybierz według nazwy
//                    0x00.toByte(), // P2
//                    // AID: identyfikator aplikacji (zastąp odpowiednim dla Twojej karty)
//                    // Należy zwrócić uwagę, że AID musi być właściwy dla odczytywanej aplikacji na karcie.
//                )
//                // Polecenie SELECT AID
//                val response = isoDep.transceive(command)
//                isoDep.close()
//
//                // Konwersja odpowiedzi na string
//                return response
//            } catch (e: IOException) {
//                // Obsługa błędów komunikacji
//                e.printStackTrace()
//            } finally {
//                try {
//                    if (isoDep.isConnected) {
//                        isoDep.close()
//                    }
//                } catch (e: IOException) {
//                    e.printStackTrace()
//                }
//            }
//        }
//
////        val rawMessages =
//
//
//    }


}