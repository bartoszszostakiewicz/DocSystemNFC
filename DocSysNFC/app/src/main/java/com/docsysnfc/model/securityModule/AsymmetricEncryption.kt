package com.docsysnfc.model.securityModule

abstract class AsymmetricEncryption {

    abstract fun generateKeyPair(): Pair<String, String>

    abstract fun encryptData(data: ByteArray, key: String): ByteArray

    abstract fun decryptData(encryptedData: ByteArray, key: String): ByteArray

}
