package com.docsysnfc.sender.model.securityModule

class SecurityManager(
    private val encryption: RSAEncryption
) {

    /**
     * Generates a pair of keys
     * @return a pair of keys
     */
    fun generateKeys(): Pair<String, String> {
        return encryption.generateKeyPair()
    }


    /**
     * Encrypts data with AES and RSA
     * @param data the data to be encrypted
     * @param publicKey the public key to encrypt the data with
     * @return a pair of encrypted data and the AES key used to encrypt the data
     */
    fun encryptDataWithAESAndRSA(data: ByteArray, publicKey: String): Pair<ByteArray, ByteArray> {
        return encryption.encryptDataUsingHybridEncryption(data, publicKey)
    }

    fun encrypt(data: ByteArray, key: String): ByteArray {
        return encryption.encryptData(data, key)
    }

    fun decrypt(encryptedData: ByteArray, key: String): ByteArray {
        return encryption.decryptData(encryptedData, key)
    }
}
