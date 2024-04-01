package com.docsysnfc.flowtouch.model

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class SecurityManager {



    /**
     * Encrypts data with RSA
     * @param data the data to be encrypted
     * @param publicKey the public key to encrypt the data with
     * @return the encrypted data
     */
    fun encryptDataRSA(data: ByteArray, publicKey: String, callback: (ByteArray) -> Unit) {
        try {
            val keyFactory = KeyFactory.getInstance("RSA")
            val keySpec = X509EncodedKeySpec(Base64.getDecoder().decode(publicKey))
            val key = keyFactory.generatePublic(keySpec)

            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, key)

            val keyLength = (key as RSAPublicKey).modulus.bitLength()
            val maxBlockSize = (keyLength / 8) - 11

            // Podziel dane na bloki o maksymalnej długości
            var offset = 0
            while (offset < data.size) {
                val blockSize =
                    if (offset + maxBlockSize < data.size) maxBlockSize else data.size - offset
                val block = data.copyOfRange(offset, offset + blockSize)
                val encryptedBlock = cipher.doFinal(block)
                callback(encryptedBlock)
                offset += maxBlockSize
            }
        } catch (e: Exception) {
            // Obsłuż wyjątek
            e.printStackTrace()
        }
    }


    /**
     * Decrypts RSA encrypted data that is divided into blocks
     * @param encryptedDataBlocks array of encrypted data blocks
     * @param alias the alias for the private key in the AndroidKeyStore
     * @param callback function to process decrypted data
     */
    fun decryptDataRSA(
        encryptedDataBlocks: Array<ByteArray>,
        alias: String,
        callback: (ByteArray) -> Unit
    ) {
        try {
            Log.d("nfc123", "Starting decryption process")

            // Ładowanie AndroidKeyStore
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
                load(null)
            }
            Log.d("nfc123", "KeyStore loaded")

            // Pobieranie klucza prywatnego za pomocą aliasu
            val privateKeyEntry = keyStore.getEntry(alias, null) as? KeyStore.PrivateKeyEntry
                ?: throw Exception("No such alias: $alias")
            Log.d("nfc123", "PrivateKeyEntry obtained")

            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.DECRYPT_MODE, privateKeyEntry.privateKey)
            Log.d("nfc123", "Cipher initialized for decryption")

            // Odszyfrowanie każdego bloku danych
            encryptedDataBlocks.forEach { encryptedBlock ->
                val decryptedBlock = cipher.doFinal(encryptedBlock)
                Log.d("nfc123", "Block decrypted")
                callback(decryptedBlock)
            }
            Log.d("nfc123", "Decryption process completed")
        } catch (e: Exception) {
            // Obsłuż wyjątek
            Log.e("nfc123", "Error during decryption: ${e.message}")
            e.printStackTrace()
        }
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
        Log.d("nfc1234", "ivSpec: ${ivSpec.iv}")
        Log.d("nfc1234", "Len ivSpec iv: ${ivSpec.iv.size}")

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        val encryptedData = cipher.doFinal(data)

        return Triple(encryptedData, secretKey, iv)
    }

    fun splitDataIntoRSABlocks(data: ByteArray, keyLengthInBits: Int): Array<ByteArray> {
        val maxBlockSize =
            (keyLengthInBits / 8) - 11 // Maksymalny rozmiar bloku dla szyfrowania RSA.

        // Lista na przechowywanie bloków
        val blocks = mutableListOf<ByteArray>()

        // Podział danych na bloki
        var offset = 0
        while (offset < data.size) {
            val blockSize = minOf(maxBlockSize, data.size - offset)
            val block = data.copyOfRange(offset, offset + blockSize)
            blocks.add(block)
            offset += blockSize
        }

        // Konwersja listy na tablicę
        return blocks.toTypedArray()
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
        Log.d("nfc1234", "ivSpec: ${ivSpec.iv}")
        Log.d("nfc1234", "Len ivSpec iv: ${ivSpec.iv.size}")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)


        return cipher.doFinal(encryptedData)
    }

    fun checkKey(alias: String) {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")

        keyStore.load(null)

        // Sprawdzenie, czy klucz już istnieje.
        if (!keyStore.containsAlias(alias)) {
            Log.d("nfc1234", "Klucz nie istnieje, generowanie klucza")
            // Klucz nie istnieje, więc generujemy nowy.
            val keyPairGenerator =
                KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
            val parameterSpec = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).run {
                setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                build()
            }
            try {
                keyPairGenerator.initialize(parameterSpec)
                keyPairGenerator.generateKeyPair()
                Log.d("nfc1234", "Klucz został pomyślnie wygenerowany")
            } catch (e: Exception) {
                Log.e("nfc1234", "Błąd podczas generowania klucza: ${e.message}")
            }
        } else {
//            Log.d("nfc1234", "Klucz istnieje")
        }

    }

    fun getPublicKey(alias: String): String {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val publicKey = keyStore.getCertificate(alias).publicKey
        return Base64.getEncoder().encodeToString(publicKey.encoded)
    }

    fun getPrivateKey(context: Context, alias: String): String? {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }

        val entry = ks.getEntry(alias, null) as? KeyStore.PrivateKeyEntry
        if (entry == null) {
            Log.w("nfc123", "Not an instance of a PrivateKeyEntry")
            return null
        }
//        Log.d("nfc123", "Private key: ${entry.privateKey.}")
        return Base64.getEncoder().encodeToString(entry.privateKey.encoded)
    }


    fun validatePublicKey(pkey: String): Boolean {
        return try {
            val keyFactory = KeyFactory.getInstance("RSA")
            val keySpec = X509EncodedKeySpec(Base64.getDecoder().decode(pkey))
            keyFactory.generatePublic(keySpec)
            true
        } catch (e: Exception) {
            false
        }
    }


}

