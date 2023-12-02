package com.docsysnfc.model.securityModule

import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.spec.X509EncodedKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.Cipher
import java.util.Base64

class RSAEncryption : AsymmetricEncryption() {

    companion object {
        private const val RSA_ALGORITHM = "RSA"
    }


    override fun generateKeyPair(): Pair<String, String> {
        val keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM)
        keyPairGenerator.initialize(2048) // Możesz zmienić rozmiar klucza
        val keyPair = keyPairGenerator.generateKeyPair()

        val publicKey = Base64.getEncoder().encodeToString(keyPair.public.encoded)
        val privateKey = Base64.getEncoder().encodeToString(keyPair.private.encoded)

        return Pair(publicKey, privateKey)
    }


    override fun encryptData(data: ByteArray, publicKeyStr: String): ByteArray {
        val publicKeyBytes = Base64.getDecoder().decode(publicKeyStr)
        val keySpec = X509EncodedKeySpec(publicKeyBytes)
        val publicKey = KeyFactory.getInstance(RSA_ALGORITHM).generatePublic(keySpec)

        val cipher = Cipher.getInstance(RSA_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)

        return cipher.doFinal(data)
    }


    override fun decryptData(encryptedData: ByteArray, privateKeyStr: String): ByteArray {
        val privateKeyBytes = Base64.getDecoder().decode(privateKeyStr)
        val keySpec = PKCS8EncodedKeySpec(privateKeyBytes)
        val privateKey = KeyFactory.getInstance(RSA_ALGORITHM).generatePrivate(keySpec)

        val cipher = Cipher.getInstance(RSA_ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, privateKey)

        return cipher.doFinal(encryptedData)
    }
}
