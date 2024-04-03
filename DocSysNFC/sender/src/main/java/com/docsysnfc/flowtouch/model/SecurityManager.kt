package com.docsysnfc.flowtouch.model

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

/**
 * The SecurityManager class provides a range of cryptographic operations for secure data handling.
 * This includes methods to manage encryption and decryption of data, validate public keys,
 * and work with the Android KeyStore for secure key management.
 *
 * This class encapsulates various security-related functionalities such as key generation,
 * key retrieval, and cryptographic operations (e.g., RSA and AES encryption/decryption),
 * providing a simplified interface for handling common cryptographic tasks.
 */
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
        encryptedDataBlocks: ByteArray,
        alias: String,
        callback: (ByteArray) -> Unit
    ) {
        try {
            Log.d(TAG, "Starting decryption process")

            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
                load(null)
            }
            Log.d(TAG, "KeyStore loaded")

            val privateKeyEntry = keyStore.getEntry(alias, null) as? KeyStore.PrivateKeyEntry
                ?: throw Exception("No such alias: $alias")

            Log.d(TAG, "PrivateKeyEntry obtained")

            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.DECRYPT_MODE, privateKeyEntry.privateKey)
            Log.d(TAG, "Cipher initialized for decryption")

                try {
                    Log.d(TAG, "Decrypting $encryptedDataBlocks")
                    val decryptedBlock = cipher.doFinal(encryptedDataBlocks)
                    callback(decryptedBlock)
                } catch (e: Exception) {
                    Log.e(TAG, "Error during decryption: ${e.message}")
                    e.printStackTrace()
                }

            Log.d(TAG, "Decryption process completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during decryption: ${e.message}")
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
        keyGenerator.init(256)
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

    /**
     * Checks if an RSA key pair exists in the Android KeyStore under the specified alias.
     * If the key pair doesn't exist, it generates a new one with the RSA algorithm and PKCS1 padding.
     *
     * @param alias The unique alias used to check the presence of the key pair in the KeyStore.
     *              The same alias is used for generating the key pair if it doesn't exist.
     **/
    fun checkKey(alias: String) {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")

        keyStore.load(null)

        if (!keyStore.containsAlias(alias)) {
            Log.d(TAG, "Key does not exist")
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
                Log.d(TAG, "Key generated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error while generating key pair ${e.message}")
            }
        } else {
            Log.d(TAG, "Keys exists")
        }
    }



    /**
     * Retrieves the public key associated with the given alias from the Android KeyStore.
     * The public key is returned as a Base64 encoded String.
     *
     * @param alias The alias name used to retrieve the public key from the KeyStore.
     * @return A Base64 encoded String representation of the public key.
     */
    fun getPublicKey(alias: String): String {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val publicKey = keyStore.getCertificate(alias).publicKey
        return Base64.getEncoder().encodeToString(publicKey.encoded)
    }


    /**
     * Validates the format of the provided public key string.
     *
     * This method attempts to parse the provided Base64 encoded public key string into an
     * RSA public key object to ensure that it is properly formatted and can be used for RSA operations.
     *
     * @param publicKey The Base64 encoded string representation of the RSA public key to validate.
     * @return A boolean value indicating whether the public key is valid (true) or not (false).
     */
    fun validatePublicKey(publicKey: String): Boolean {
        return try {
            val keyFactory = KeyFactory.getInstance("RSA")
            val keySpec = X509EncodedKeySpec(Base64.getDecoder().decode(publicKey))
            keyFactory.generatePublic(keySpec)
            true
        } catch (e: Exception) {
            Log.d(TAG, "Invalid public key: ${e.message}")
            false
        }
    }

}

