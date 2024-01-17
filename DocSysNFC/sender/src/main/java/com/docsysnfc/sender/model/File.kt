package com.docsysnfc.sender.model

import android.net.Uri

data class File(
    val name: String,
    val uri: Uri,
    val downloadLink: String,
    val size: Double,
    val type: String,
){
}