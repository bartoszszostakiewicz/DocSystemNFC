package com.docsysnfc.flowtouch.model.flowtouchStates

import android.nfc.NdefMessage
import android.nfc.Tag
import com.docsysnfc.flowtouch.model.File

data class UiState(
    val ndefMessage: String = "",
    val publicKey: String = "",
    val publicKeyOwner: String = "",
    val fileIsCipher: Boolean = false,
    val fileIsDownloading: Boolean = false,
    val receivesFiles: List<File> = emptyList(),
    val modelSelectedFiles: List<File> = emptyList(),
    val filteredSelectedFiles: List<File> = emptyList(),
    val nfcStatus: NFCStatus = NFCStatus.Unknown,
    val internetConnStatus: InternetConnectionStatus = InternetConnectionStatus.DISCONNECTED,
    val authenticationStatus: AuthenticationStatus = AuthenticationStatus.UNKNOWN,
    val createAccountStatus: CreateAccountStatus = CreateAccountStatus.UNKNOWN,
    val nfcTag: Tag? = null,
    val additionalEncryption: Boolean = false,
    val cloudMirroring: Boolean = false,
    val navigationDestination: NFCSysScreen = NFCSysScreen.Home,
    val uploadComplete: Boolean = false,
)