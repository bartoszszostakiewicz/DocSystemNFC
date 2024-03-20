package com.docsysnfc.sender.model.securityModule

import android.util.Log
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

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
        Log.d("nfc1234","ivSpec: ${ivSpec.iv}")
        Log.d("nfc1234","Len ivSpec iv: ${ivSpec.iv.size}")

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
        Log.d("nfc1234","ivSpec: ${ivSpec.iv}")
        Log.d("nfc1234","Len ivSpec iv: ${ivSpec.iv.size}")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
//        if (encryptedData.size % 16 != 0) {
//            val requiredPadding = 16 - (encryptedData.size % 16)
//            val paddedData = encryptedData + ByteArray(requiredPadding) // Dopełnienie zerami
//            Log.d("nfc1234", "Długość danych zaszyfrowanych nie jest wielokrotnością rozmiaru bloku AES (16 bajtów). Dodano dopełnienie zerami.")
//
//          return cipher.doFinal(paddedData)
//        }
        return cipher.doFinal(encryptedData)
    }
}
