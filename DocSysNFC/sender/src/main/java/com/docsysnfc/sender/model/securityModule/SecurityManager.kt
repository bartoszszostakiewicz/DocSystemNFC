package com.docsysnfc.sender.model.securityModule

class SecurityManager(
    private val encryption: AsymmetricEncryption
) {

    fun generateKeys(): Pair<String, String> {
        return encryption.generateKeyPair()
    }

    fun encrypt(data: ByteArray, key: String): ByteArray {
        return encryption.encryptData(data, key)
    }

    fun decrypt(encryptedData: ByteArray, key: String): ByteArray {
        return encryption.decryptData(encryptedData, key)
    }
}
