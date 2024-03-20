package com.docsysnfc.sender.model.securityModule

import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class RSAEncryption {

    companion object {
        private const val RSA_ALGORITHM = "RSA"
    }

    fun encryptDataUsingHybridEncryption(data: ByteArray, publicKey: String): Pair<ByteArray, ByteArray> {
        val (encryptedData, aesKey) = encryptDataAES(data)
        val encryptedAESKey = encryptAESKeyWithRSA(aesKey, publicKey)
        return Pair(encryptedData, encryptedAESKey)
    }


    private fun encryptAESKeyWithRSA(aesKey: SecretKey, publicKeyStr: String): ByteArray {
        val publicKeyBytes = Base64.getDecoder().decode(publicKeyStr)
        val keySpec = X509EncodedKeySpec(publicKeyBytes)
        //TO DO !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        val publicKey = KeyFactory.getInstance(RSA_ALGORITHM).generatePublic(keySpec)

        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.WRAP_MODE, publicKey)

        return cipher.wrap(aesKey)
    }


    /**
     * Generates a pair of keys
     * @return a pair of keys
     */
    fun generateKeyPair(): Pair<String, String> {
        val keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM)
        keyPairGenerator.initialize(2048) // Możesz zmienić rozmiar klucza
        val keyPair = keyPairGenerator.generateKeyPair()

        val publicKey = Base64.getEncoder().encodeToString(keyPair.public.encoded)
        val privateKey = Base64.getEncoder().encodeToString(keyPair.private.encoded)

        return Pair(publicKey, privateKey)
    }


    /**
     * Encrypts data with AES
     * @param data the data to be encrypted
     * @return a Triple of encrypted data, the AES key used to encrypt the data, and the IV
     */
    fun encryptDataAES(data: ByteArray): Triple<ByteArray, SecretKey, ByteArray> {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256) // Używamy klucza 256-bitowego dla AES
        val secretKey = keyGenerator.generateKey()

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        val ivSpec = IvParameterSpec(iv)

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        val encryptedData = cipher.doFinal(data)

        return Triple(encryptedData, secretKey, iv)
    }

    /**
     * Decrypts data with AES
     * @param encryptedData the data to be decrypted
     * @param secretKey the secret key to decrypt the data with
     * @param iv the initialization vector
     * @return the decrypted data
     */

    fun decryptDataAES(encryptedData: ByteArray, secretKey: SecretKey, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        return cipher.doFinal(encryptedData)
    }



    fun encryptData(data: ByteArray, publicKeyStr: String): ByteArray {
        val publicKeyBytes = Base64.getDecoder().decode(publicKeyStr)
        val keySpec = X509EncodedKeySpec(publicKeyBytes)
        val publicKey = KeyFactory.getInstance(RSA_ALGORITHM).generatePublic(keySpec)

        val cipher = Cipher.getInstance(RSA_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)

        return cipher.doFinal(data)
    }


    fun decryptData(encryptedData: ByteArray, privateKeyStr: String): ByteArray {
        val privateKeyBytes = Base64.getDecoder().decode(privateKeyStr)
        val keySpec = PKCS8EncodedKeySpec(privateKeyBytes)
        val privateKey = KeyFactory.getInstance(RSA_ALGORITHM).generatePrivate(keySpec)

        val cipher = Cipher.getInstance(RSA_ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, privateKey)

        return cipher.doFinal(encryptedData)
    }



}
