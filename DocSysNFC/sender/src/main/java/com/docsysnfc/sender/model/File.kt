package com.docsysnfc.sender.model

import android.net.Uri
import java.net.URL

data class File(
    val name: String,
    val uri: Uri,
    val downloadLink: String,
    val size: Double,
    val type: String,
    var url : URL? = null
){
}