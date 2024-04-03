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


class NFCComm
    : HostApduService() {


    private val vibrationEffect: VibrationEffect =
        VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)

    private val TAG = "NFC123"

    private var nfcAdapter: NfcAdapter? = null
    private var vibrator: Vibrator? = null

    //move this command to other file

    private val applicationSelect = listOf(
        0x00, 0xA4, 0x04, 0x00, 0x07, 0xD2, 0x76, 0x00, 0x00, 0x85, 0x01, 0x01, 0x00
    ).map { it.toByte() }.toByteArray()

    private val capabilityContainerSelect = listOf(
        0x00, 0xa4, 0x00, 0x0c, 0x02, 0xe1, 0x03
    ).map { it.toByte() }.toByteArray()

    private val readBinaryFromCC = listOf(
        0x00, 0xb0, 0x00, 0x00, 0x0f
    ).map { it.toByte() }.toByteArray()

    private val responseReadBinaryFromCC = listOf(
        0x00, 0x11, 0x20, 0xFF, 0xFF, 0xFF, 0xFF, 0x04, 0x06, 0xE1, 0x04, 0xFF, 0xFE, 0x00, 0xFF, 0x90, 0x00,
    ).map { it.toByte() }.toByteArray()

    private val ndefSelect = listOf(
        0x00, 0xa4, 0x00, 0x0c, 0x02, 0xE1, 0x04,
    ).map { it.toByte() }.toByteArray()

    private val ndefReadBinary = listOf(
        0x00, 0xb0,
    ).map { it.toByte() }.toByteArray()

    private val ndefReadBinaryLen = listOf(
        0x00, 0xb0, 0x00, 0x00, 0x02,
    ).map { it.toByte() }.toByteArray()

    private val okay = listOf(
        0x90, 0x00,
    ).map { it.toByte() }.toByteArray()

    private val error = listOf(
        0x6A, 0x82,
    ).map { it.toByte() }.toByteArray()

    private var ccContainerChecked = false

    private val NDEF_ID = byteArrayOf(0xE1.toByte(), 0x04.toByte())

    private var NDEF_URI = NdefMessage(createNdefRecord("Init message", NDEF_ID))
    private var NDEF_URI_BYTES = NDEF_URI.toByteArray()
    private var NDEF_URI_LEN = fillByteArrayToFixedDimension(
        BigInteger.valueOf(NDEF_URI_BYTES.size.toLong()).toByteArray(),
        2,
    )


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.hasExtra("ndefMessage")!!) {
            NDEF_URI =
                NdefMessage(createNdefRecord(intent.getStringExtra("ndefMessage")!!, NDEF_ID))

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

            applicationSelect.contentEquals(commandApdu) -> {
                Log.i(
                    TAG,
                    "receive command: ${commandApdu.contentToString()}\n response: ${okay.contentToString()}."
                )
                okay
            }

            capabilityContainerSelect.contentEquals(commandApdu) -> {
                Log.i(
                    TAG,
                    "receive command: ${commandApdu.contentToString()}\n response: ${okay.contentToString()}"
                )
                okay
            }

            readBinaryFromCC.contentEquals(commandApdu) && !ccContainerChecked -> {
                Log.i(
                    TAG,
                    "receive command: ${commandApdu.contentToString()}\n response: ${responseReadBinaryFromCC.contentToString()}"
                )
                ccContainerChecked = true
                responseReadBinaryFromCC
            }

            ndefSelect.contentEquals(commandApdu) -> {
                Log.i(
                    TAG,
                    "receive command: ${commandApdu.contentToString()}\n response: ${okay.contentToString()}"
                )
                okay
            }

            ndefReadBinaryLen.contentEquals(commandApdu) -> {
                val response = ByteArray(NDEF_URI_LEN.size + okay.size)
                System.arraycopy(NDEF_URI_LEN, 0, response, 0, NDEF_URI_LEN.size)
                System.arraycopy(okay, 0, response, NDEF_URI_LEN.size, okay.size)

                Log.i(
                    TAG, "receive command: ${commandApdu.contentToString()}\n" +
                            " response:  ${response.contentToString()}}"
                )

                ccContainerChecked = false
                response
            }

            commandApdu.sliceArray(0..1).contentEquals(ndefReadBinary) -> {
                var offset: Int
                var length: Int
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
                val response = ByteArray(realLength + okay.size)

                System.arraycopy(slicedResponse, 0, response, 0, realLength)
                System.arraycopy(okay, 0, response, realLength, okay.size)

                Log.i(
                    TAG,
                    "NDEF_READ_BINARY triggered. Our Response: fullResponse.contentToString()"
                )

                ccContainerChecked = false

                if (vibrator == null) {
                    vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    vibrator?.vibrate(vibrationEffect)
                    Log.d(TAG, "Vibrator on")
                } else {
                    vibrator?.vibrate(vibrationEffect)
                    Log.d(TAG, "Vibrator on")
                }

                response
            }else -> {
                Log.d(TAG, "unknown commandApdu")
                error
            }
        }

        return response
    }


    private fun ByteArray.toHex(): String {
        val result = StringBuffer()
        val hexChars = "0123456789ABCDEF".toCharArray()

        forEach {
            val octet = it.toInt()
            val firstIndex = (octet and 0xF0).ushr(4)
            val secondIndex = octet and 0x0F
            result.append(hexChars[firstIndex])
            result.append(hexChars[secondIndex])
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
        if (reason == 1) {
            Log.i(TAG, "DEACTIVATION_LINK_LOSS")
        } else {
            Log.i(TAG, "DEACTIVATION_DESELECTED")
        }
    }

    fun initNFCAdapter(context: Context) {
        nfcAdapter = NfcAdapter.getDefaultAdapter(context)
        vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun enableNFCReader(activity: Activity, viewModel: MainViewModel) {
        val readerCallback = NfcAdapter.ReaderCallback { tag ->


            if (tag != null) {
                val ndef = android.nfc.tech.Ndef.get(tag)

                if (ndef != null) {
                    Log.i(TAG, "NDEF detected")



                    ndef.connect()

                    val response = ndef.ndefMessage.toByteArray()



                    viewModel.processNFCData(response)
                    return@ReaderCallback

                }

                ccContainerChecked = false


                val isoDep = android.nfc.tech.IsoDep.get(tag)
                isoDep?.connect()


                var response = isoDep.transceive(applicationSelect)

                Log.i(TAG, "send: application select")
                Log.i(TAG, "Response: ${response?.contentToString()}")

                response = isoDep.transceive(capabilityContainerSelect)

                Log.i(TAG, "send: capability container select")
                Log.i(TAG, "Response: ${response?.contentToString()}")

                response = isoDep.transceive(readBinaryFromCC)

                Log.i(TAG, "send: read binary from CC")
                Log.i(TAG, "Response: ${response?.contentToString()}")

                response = isoDep.transceive(ndefSelect)

                Log.i(TAG, "send: ndef select")
                Log.i(TAG, "Response: ${response?.contentToString()}")

                response = isoDep.transceive(ndefReadBinaryLen)

                Log.i(TAG, "send: ndef read binary len")
                Log.i(TAG, "Response: ${response?.contentToString()}")

                response = isoDep.transceive(ndefReadBinary)

                Log.i(TAG, "send: ndef read binary")
                Log.i(TAG, "Response: ${response?.contentToString()}")

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