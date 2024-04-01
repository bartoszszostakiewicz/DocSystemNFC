package com.docsysnfc.flowtouch.model

import android.app.Activity
import android.content.Context
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.NfcAdapter.FLAG_READER_NFC_A
import android.util.Log
import com.docsysnfc.flowtouch.MainViewModel
import java.math.BigInteger

val APDU_SELECT = byteArrayOf(
    0x00.toByte(), // CLA	- Class - Class of instruction
    0xA4.toByte(), // INS	- Instruction - Instruction code
    0x04.toByte(), // P1	- Parameter 1 - Instruction parameter 1
    0x00.toByte(), // P2	- Parameter 2 - Instruction parameter 2
    0x07.toByte(), // Lc field	- Number of bytes present in the data field of the command
    0xD2.toByte(),
    0x76.toByte(),
    0x00.toByte(),
    0x00.toByte(),
    0x85.toByte(),
    0x01.toByte(),
    0x01.toByte(), // NDEF Tag Application name
    0x00.toByte(), // Le field	- Maximum number of bytes expected in the data field of the response to the command
)

val CAPABILITY_CONTAINER_OK = byteArrayOf(
    0x00.toByte(), // CLA	- Class - Class of instruction
    0xa4.toByte(), // INS	- Instruction - Instruction code
    0x00.toByte(), // P1	- Parameter 1 - Instruction parameter 1
    0x0c.toByte(), // P2	- Parameter 2 - Instruction parameter 2
    0x02.toByte(), // Lc field	- Number of bytes present in the data field of the command
    0xe1.toByte(),
    0x03.toByte(), // file identifier of the CC file
)

val READ_CAPABILITY_CONTAINER = byteArrayOf(
    0x00.toByte(), // CLA	- Class - Class of instruction
    0xb0.toByte(), // INS	- Instruction - Instruction code
    0x00.toByte(), // P1	- Parameter 1 - Instruction parameter 1
    0x00.toByte(), // P2	- Parameter 2 - Instruction parameter 2
    0x0f.toByte(), // Lc field	- Number of bytes present in the data field of the command
)

// In the scenario that we have done a CC read, the same byte[] match
// for ReadBinary would trigger and we don't want that in succession
var READ_CAPABILITY_CONTAINER_CHECK = false

val READ_CAPABILITY_CONTAINER_RESPONSE = byteArrayOf(
    0x00.toByte(), 0x11.toByte(), // CCLEN length of the CC file
    0x20.toByte(), // Mapping Version 2.0
    0xFF.toByte(), 0xFF.toByte(), // MLe maximum
    0xFF.toByte(), 0xFF.toByte(), // MLc maximum
    0x04.toByte(), // T field of the NDEF File Control TLV
    0x06.toByte(), // L field of the NDEF File Control TLV
    0xE1.toByte(), 0x04.toByte(), // File Identifier of NDEF file
    0xFF.toByte(), 0xFE.toByte(), // Maximum NDEF file size of 65534 bytes
    0x00.toByte(), // Read access without any security
    0xFF.toByte(), // Write access without any security
    0x90.toByte(), 0x00.toByte(), // A_OKAY
)

val NDEF_SELECT_OK = byteArrayOf(
    0x00.toByte(), // CLA	- Class - Class of instruction
    0xa4.toByte(), // Instruction byte (INS) for Select command
    0x00.toByte(), // Parameter byte (P1), select by identifier
    0x0c.toByte(), // Parameter byte (P1), select by identifier
    0x02.toByte(), // Lc field	- Number of bytes present in the data field of the command
    0xE1.toByte(),
    0x04.toByte(), // file identifier of the NDEF file retrieved from the CC file
)

val NDEF_READ_BINARY = byteArrayOf(
    0x00.toByte(), // Class byte (CLA)
    0xb0.toByte(), // Instruction byte (INS) for ReadBinary command
)

val NDEF_READ_BINARY_NLEN = byteArrayOf(
    0x00.toByte(), // Class byte (CLA)
    0xb0.toByte(), // Instruction byte (INS) for ReadBinary command
    0x00.toByte(),
    0x00.toByte(), // Parameter byte (P1, P2), offset inside the CC file
    0x02.toByte(), // Le field
)

val A_OKAY = byteArrayOf(
    0x90.toByte(), // SW1	Status byte 1 - Command processing status
    0x00.toByte(), // SW2	Status byte 2 - Command processing qualifier
)

val A_ERROR = byteArrayOf(
    0x6A.toByte(), // SW1	Status byte 1 - Command processing status
    0x82.toByte(), // SW2	Status byte 2 - Command processing qualifier
)

val NDEF_ID = byteArrayOf(0xE1.toByte(), 0x04.toByte())



class NFCComm() {

    private var nfcAdapter: NfcAdapter? = null

    fun initNFCAdapter(context: Context) {
        nfcAdapter = NfcAdapter.getDefaultAdapter(context)

    }

    fun enableNFCReader(activity: Activity,viewModel: MainViewModel) {
        val readerCallback = NfcAdapter.ReaderCallback { tag ->

            // Obsługa odczytu NFC
            if(tag != null){
               //android.nfc.tech.BasicTagTechnology.connect(tag)

                //wyzeruj wczesnieijsza komunikacje i komunikuj sie od nowa
                READ_CAPABILITY_CONTAINER_CHECK = false


                val isoDep = android.nfc.tech.IsoDep.get(tag)
                isoDep?.connect()


                var response = isoDep.transceive(APDU_SELECT)
                Log.d("NFC123","SEND APDU_SELECT")
                Log.d("NFC123", "1: ${response?.contentToString()}")

                response = isoDep.transceive(CAPABILITY_CONTAINER_OK)

                Log.d("NFC123","SEND CAPABILITY_CONTAINER_OK")
                Log.d("NFC123", "2: ${response?.contentToString()}")

                response = isoDep.transceive(READ_CAPABILITY_CONTAINER)

                Log.d("NFC123","SEND READ_CAPABILITY_CONTAINER")
                Log.d("NFC123", "3: ${response?.contentToString()}")




                isoDep.close()



//                viewModel.setNFCTag(tag)
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