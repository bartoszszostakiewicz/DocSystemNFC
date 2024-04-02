package com.docsysnfc.flowtouch.model

import android.net.Uri
import java.net.URL




//data class File(
//    val name: String,
//    val size: Double,
//    val type: String,
//    val uri: Uri,
//    val url: URL = URL("https://www.google.com"),
//    val byteArray: ByteArray,
//    val encryptedByteArray: ByteArray = ByteArray(0),
//    val sessionKey: String = "",
//    val isDownloading: Boolean = false,
//    val isUploading: Boolean = false,
//    val isEncrypting: Boolean = false,
//) {
//    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//        if (javaClass != other?.javaClass) return false
//
//        other as File
//
//        if (name != other.name) return false
//        if (size != other.size) return false
//        if (type != other.type) return false
//        if (uri != other.uri) return false
//        if (url != other.url) return false
//        if (!byteArray.contentEquals(other.byteArray)) return false
//        if (!encryptedByteArray.contentEquals(other.encryptedByteArray)) return false
//        if (sessionKey != other.sessionKey) return false
//        if (isDownloading != other.isDownloading) return false
//        if (isUploading != other.isUploading) return false
//        if (isEncrypting != other.isEncrypting) return false
//
//        return true
//    }
//
//    override fun hashCode(): Int {
//        var result = name.hashCode()
//        result = 31 * result + size.hashCode()
//        result = 31 * result + type.hashCode()
//        result = 31 * result + uri.hashCode()
//        result = 31 * result + url.hashCode()
//        result = 31 * result + byteArray.contentHashCode()
//        result = 31 * result + encryptedByteArray.contentHashCode()
//        result = 31 * result + sessionKey.hashCode()
//        result = 31 * result + isDownloading.hashCode()
//        result = 31 * result + isUploading.hashCode()
//        result = 31 * result + isEncrypting.hashCode()
//        return result
//    }
//
//}

data class File(
    var name: String,
    var uri: Uri,
    var downloadLink: String,
    var size: Double,
    var type: String,
    var url: URL = URL("https://www.google.com"),
    var byteArray: ByteArray,
    var encryptedByteArray: ByteArray = ByteArray(0),
    var secretKey: String = "",
    var iV: String = "",
    var encryptionState:Boolean = false,
    var publicKey : String = "",
    var isCipher: Boolean = false,
    var isDownloading: Boolean = false,
    var isUploading: Boolean = false,
    var isDownloaded: Boolean = false,
    var uploadComplete: Boolean = false,
) {
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

    fun renameTo(newName: String) {
        this.name = newName
    }


}