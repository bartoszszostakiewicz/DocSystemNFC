package com.docsysnfc.sender.model

import android.net.Uri
import java.net.URL

data class File(
    val name: String,
    val uri: Uri,
    val downloadLink: String,
    val size: Double,
    val type: String,
    var url: URL = URL("https://www.google.com"),
    var byteArray: ByteArray,
    var encryptedByteArray: ByteArray = ByteArray(0)
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as File

        if (name != other.name) return false
        if (uri != other.uri) return false
        if (downloadLink != other.downloadLink) return false
        if (size != other.size) return false
        if (type != other.type) return false
        if (url != other.url) return false
        if (!byteArray.contentEquals(other.byteArray)) return false
        return encryptedByteArray.contentEquals(other.encryptedByteArray)
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + uri.hashCode()
        result = 31 * result + downloadLink.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + byteArray.contentHashCode()
        result = 31 * result + encryptedByteArray.contentHashCode()
        return result
    }


}