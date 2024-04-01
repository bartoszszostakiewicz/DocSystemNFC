package com.docsysnfc.flowtouch.model

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.docsysnfc.flowtouch.MainViewModel
import java.io.UnsupportedEncodingException
import java.math.BigInteger


class NFCComm(
) : HostApduService() {


    private val vibrationEffect: VibrationEffect = VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)

    private val TAG = "NFC123"

    private var nfcAdapter: NfcAdapter? = null
    private var vibrator: Vibrator? = null

    private val APDU_SELECT = byteArrayOf(
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

    private val CAPABILITY_CONTAINER_OK = byteArrayOf(
        0x00.toByte(), // CLA	- Class - Class of instruction
        0xa4.toByte(), // INS	- Instruction - Instruction code
        0x00.toByte(), // P1	- Parameter 1 - Instruction parameter 1
        0x0c.toByte(), // P2	- Parameter 2 - Instruction parameter 2
        0x02.toByte(), // Lc field	- Number of bytes present in the data field of the command
        0xe1.toByte(),
        0x03.toByte(), // file identifier of the CC file
    )

    private val READ_CAPABILITY_CONTAINER = byteArrayOf(
        0x00.toByte(), // CLA	- Class - Class of instruction
        0xb0.toByte(), // INS	- Instruction - Instruction code
        0x00.toByte(), // P1	- Parameter 1 - Instruction parameter 1
        0x00.toByte(), // P2	- Parameter 2 - Instruction parameter 2
        0x0f.toByte(), // Lc field	- Number of bytes present in the data field of the command
    )

    // In the scenario that we have done a CC read, the same byte[] match?
    // for ReadBinary would trigger and we don't want that in succession
    private var READ_CAPABILITY_CONTAINER_CHECK = false

    private val READ_CAPABILITY_CONTAINER_RESPONSE = byteArrayOf(
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

    private val NDEF_SELECT_OK = byteArrayOf(
        0x00.toByte(), // CLA	- Class - Class of instruction
        0xa4.toByte(), // Instruction byte (INS) for Select command
        0x00.toByte(), // Parameter byte (P1), select by identifier
        0x0c.toByte(), // Parameter byte (P1), select by identifier
        0x02.toByte(), // Lc field	- Number of bytes present in the data field of the command
        0xE1.toByte(),
        0x04.toByte(), // file identifier of the NDEF file retrieved from the CC file
    )

    private val NDEF_READ_BINARY = byteArrayOf(
        0x00.toByte(), // Class byte (CLA)
        0xb0.toByte(), // Instruction byte (INS) for ReadBinary command
    )

    private val NDEF_READ_BINARY_NLEN = byteArrayOf(
        0x00.toByte(), // Class byte (CLA)
        0xb0.toByte(), // Instruction byte (INS) for ReadBinary command
        0x00.toByte(),
        0x00.toByte(), // Parameter byte (P1, P2), offset inside the CC file
        0x02.toByte(), // Le field
    )

    private val A_OKAY = byteArrayOf(
        0x90.toByte(), // SW1	Status byte 1 - Command processing status
        0x00.toByte(), // SW2	Status byte 2 - Command processing qualifier
    )

    private val A_ERROR = byteArrayOf(
        0x6A.toByte(), // SW1	Status byte 1 - Command processing status
        0x82.toByte(), // SW2	Status byte 2 - Command processing qualifier
    )

    private val NDEF_ID = byteArrayOf(0xE1.toByte(), 0x04.toByte())

    private var NDEF_URI = NdefMessage(createNdefRecord("init message", NDEF_ID))
    private var NDEF_URI_BYTES = NDEF_URI.toByteArray()
    private var NDEF_URI_LEN = fillByteArrayToFixedDimension(
        BigInteger.valueOf(NDEF_URI_BYTES.size.toLong()).toByteArray(),
        2,
    )


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        //activity visible porownoac z gita

        if (intent?.hasExtra("ndefMessage")!!) {
            NDEF_URI = NdefMessage(createNdefRecord(intent.getStringExtra("ndefMessage")!!, NDEF_ID))

            NDEF_URI_BYTES = NDEF_URI.toByteArray()
            NDEF_URI_LEN = fillByteArrayToFixedDimension(
                BigInteger.valueOf(NDEF_URI_BYTES.size.toLong()).toByteArray(),
                2,
            )
        }

        Log.i(TAG, "onStartCommand() | NDEF$NDEF_URI")

        return Service.START_STICKY
    }


    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {

//        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
//        val senderMode = prefs.getBoolean("senderMode", true)
//
//        if (!senderMode) {
//            Log.d(TAG,"Turn on application in reader mode")
//            val intent = Intent(this, MainActivity::class.java).apply {
//                flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                Log.d(TAG, "put Receive as destinationId")
//                putExtra("destinationId", NFCSysScreen.Receive.name)
//            }
//
//            startActivity(intent)
//        }

        val response = when {
            APDU_SELECT.contentEquals(commandApdu) -> {

                Log.i(TAG, "APDU_SELECT triggered. Our Response: $A_OKAY")
                A_OKAY
            }

            CAPABILITY_CONTAINER_OK.contentEquals(commandApdu) -> {
//                Log.i(TAG, "CAPABILITY_CONTAINER_OK triggered. Our Response: " +  $A_OKAY.toString())
                A_OKAY
            }

            READ_CAPABILITY_CONTAINER.contentEquals(commandApdu) && !READ_CAPABILITY_CONTAINER_CHECK -> {
                Log.i(
                    TAG,
                    "READ_CAPABILITY_CONTAINER triggered. Our Response: " + READ_CAPABILITY_CONTAINER_RESPONSE.toHex()
                )
                READ_CAPABILITY_CONTAINER_CHECK = true
                READ_CAPABILITY_CONTAINER_RESPONSE
            }

            NDEF_SELECT_OK.contentEquals(commandApdu) -> {
                Log.i(TAG, "NDEF_SELECT_OK triggered. Our Response: " + A_OKAY.toHex())
                A_OKAY
            }

            NDEF_READ_BINARY_NLEN.contentEquals(commandApdu) -> {
                val response = ByteArray(NDEF_URI_LEN.size + A_OKAY.size)
                System.arraycopy(NDEF_URI_LEN, 0, response, 0, NDEF_URI_LEN.size)
                System.arraycopy(A_OKAY, 0, response, NDEF_URI_LEN.size, A_OKAY.size)

                Log.i(TAG, "NDEF_READ_BINARY_NLEN triggered. Our Response: " + response.toHex())

                READ_CAPABILITY_CONTAINER_CHECK = false
                response
            }

            commandApdu.sliceArray(0..1).contentEquals(NDEF_READ_BINARY) -> {
                var offset = 0
                var length = 1000
                try {
                    offset = commandApdu.sliceArray(2..3).toHex().toInt(16)
                    length = commandApdu.sliceArray(4..4).toHex().toInt(16)
                } catch (e: Exception) {
                    offset = 0
                    length = 1000
                }
                val fullResponse = ByteArray(NDEF_URI_LEN.size + NDEF_URI_BYTES.size)
                System.arraycopy(NDEF_URI_LEN, 0, fullResponse, 0, NDEF_URI_LEN.size)
                System.arraycopy(
                    NDEF_URI_BYTES,
                    0,
                    fullResponse,
                    NDEF_URI_LEN.size,
                    NDEF_URI_BYTES.size,
                )



                val slicedResponse = fullResponse.sliceArray(offset until fullResponse.size)

                val realLength = if (slicedResponse.size <= length) slicedResponse.size else length
                val response = ByteArray(realLength + A_OKAY.size)

                System.arraycopy(slicedResponse, 0, response, 0, realLength)
                System.arraycopy(A_OKAY, 0, response, realLength, A_OKAY.size)

                Log.i(TAG, "NDEF_READ_BINARY triggered. Our Response: fullResponse.contentToString()")

                READ_CAPABILITY_CONTAINER_CHECK = false

                if (vibrator == null) {
                    vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    vibrator?.vibrate(vibrationEffect)
                    Log.d(TAG, "vibrator on")
                } else {
                    vibrator?.vibrate(vibrationEffect)
                    Log.d(TAG, "vibrator on")
                }

                response
            }

            else -> {
                Log.d(TAG, "unknown commandApdu")
                A_ERROR
            }

        }

        return response
    }


    private fun ByteArray.toHex(): String {
        val result = StringBuffer()
        val HEX_CHARS = "0123456789ABCDEF".toCharArray()

        forEach {
            val octet = it.toInt()
            val firstIndex = (octet and 0xF0).ushr(4)
            val secondIndex = octet and 0x0F
            result.append(HEX_CHARS[firstIndex])
            result.append(HEX_CHARS[secondIndex])
        }

        return result.toString()
    }

    private fun createNdefRecord(message: String, id: ByteArray): NdefRecord {

        val textBytes: ByteArray
        try {
            textBytes = message.toByteArray(charset("UTF-8"))
        } catch (e: UnsupportedEncodingException) {
            throw AssertionError(e)
        }

        val recordPayload = ByteArray(textBytes.size)

        System.arraycopy(textBytes, 0, recordPayload, 0, textBytes.size)

        return NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, id, recordPayload)
    }

    private fun fillByteArrayToFixedDimension(array: ByteArray, fixedSize: Int): ByteArray {
        if (array.size == fixedSize) {
            return array
        }
        val start = byteArrayOf(0x00.toByte())
        val filledArray = ByteArray(start.size + array.size)
        System.arraycopy(start, 0, filledArray, 0, start.size)
        System.arraycopy(array, 0, filledArray, start.size, array.size)
        return fillByteArrayToFixedDimension(filledArray, fixedSize)
    }


    override fun onDeactivated(reason: Int) {
        if(reason == 1){
            Log.d("nfc123", "DEACTIVATION_LINK_LOSS")
        }else{
            Log.d("nfc123", "DEACTIVATION_DESELECTED")
        }
    }

    fun initNFCAdapter(context: Context) {
        nfcAdapter = NfcAdapter.getDefaultAdapter(context)
        vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun enableNFCReader(activity: Activity, viewModel: MainViewModel) {
        val readerCallback = NfcAdapter.ReaderCallback { tag ->


            if (tag != null) {

                READ_CAPABILITY_CONTAINER_CHECK = false


                val isoDep = android.nfc.tech.IsoDep.get(tag)
                isoDep?.connect()


                var response = isoDep.transceive(APDU_SELECT)

                Log.i("NFC123", "SEND APDU_SELECT")
                Log.i("NFC123", "Response: ${response?.contentToString()}")

                response = isoDep.transceive(CAPABILITY_CONTAINER_OK)

                Log.i("NFC123", "SEND CAPABILITY_CONTAINER_OK")
                Log.i("NFC123", "Response: ${response?.contentToString()}")

                response = isoDep.transceive(READ_CAPABILITY_CONTAINER)

                Log.i("NFC123", "SEND READ_CAPABILITY_CONTAINER")
                Log.i("NFC123", "Response: ${response?.contentToString()}")

                response = isoDep.transceive(NDEF_SELECT_OK)

                Log.i("NFC123", "NDEF_SELECT_OK")
                Log.i("NFC123", "Response: ${response?.contentToString()}")

                response = isoDep.transceive(NDEF_READ_BINARY_NLEN)

                Log.i("NFC123", "NDEF_READ_BINARY_NLEN")
                Log.i("NFC123", "Response: ${response?.contentToString()}")

                response = isoDep.transceive(NDEF_READ_BINARY)

                Log.i("NFC123", "NDEF_READ_BINARY")
                Log.i("NFC123", "Response: ${response?.contentToString()}")

                isoDep.close()

                vibrator?.vibrate(vibrationEffect)

                viewModel.processNFCData(response)
            }
        }
        val readerFlags = NfcAdapter.FLAG_READER_NFC_A
        nfcAdapter?.enableReaderMode(activity, readerCallback, readerFlags, null)
        if (nfcAdapter?.isEnabled == true) {
            Log.d(com.docsysnfc.flowtouch.model.TAG, "NFC enabled")
        } else {
            Log.d(com.docsysnfc.flowtouch.model.TAG, "NFC disabled")
        }
    }

    fun disableNFCReader(context: Context) {
        nfcAdapter?.disableReaderMode(context as Activity)
    }

}