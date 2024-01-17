package com.qifan.readnfcmessage.parser

import android.nfc.NdefRecord
import java.io.UnsupportedEncodingException
import java.util.Arrays

class TextRecord(
    /**
     * ISO/IANA language code
     */
    val languageCode: String,
    /**
     * Returns the ISO/IANA language code associated with this text element.
     */
    val text: String
) : ParsedNdefRecord {

    override fun str(): String {
        return text
    }

    companion object {
        // TODO: deal with text fields which span multiple NdefRecords
        fun parse(record: NdefRecord): TextRecord? {
            return if (record.tnf == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(
                    record.type,
                    NdefRecord.RTD_TEXT
                )
            ) {
                try {
                    val payload = record.payload
                    /*
                          * payload[0] contains the "Status Byte Encodings" field, per the
                          * NFC Forum "Text Record Type Definition" section 3.2.1.
                          *
                          * bit7 is the Text Encoding Field.
                          *
                          * if (Bit_7 == 0): The text is encoded in UTF-8 if (Bit_7 == 1):
                          * The text is encoded in UTF16
                          *
                          * Bit_6 is reserved for future use and must be set to zero.
                          *
                          * Bits 5 to 0 are the length of the IANA language code.
                          */
                    val textEncoding = if (payload[0].toInt() and 128 == 0) "UTF-8" else "UTF-16"
                    val languageCodeLength = payload[0].toInt() and 63
                    val languageCode = String(payload, 1, languageCodeLength, charset("US-ASCII"))
                    val text = String(
                        payload, languageCodeLength + 1,
                        payload.size - languageCodeLength - 1, charset(textEncoding)
                    )
                    TextRecord(languageCode, text)
                } catch (e: UnsupportedEncodingException) {
                    // should never happen unless we get a malformed tag.
                    throw IllegalArgumentException(e)
                }
            } else {
                null
            }
        }

        fun isText(record: NdefRecord): Boolean {
            return try {
                parse(record)
                true
            } catch (e: IllegalArgumentException) {
                false
            }
        }
    }
}