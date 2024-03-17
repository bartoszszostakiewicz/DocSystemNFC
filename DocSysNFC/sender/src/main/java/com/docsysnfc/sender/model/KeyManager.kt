package com.docsysnfc.sender.model

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey

class KeyManager {

    fun generateKeyPair(alias: String): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore"
        )

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).run {
            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
            build()
        }

        keyPairGenerator.initialize(keyGenParameterSpec)
        return keyPairGenerator.generateKeyPair()
    }

    fun getPublicKey(alias: String): PublicKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val entry = keyStore.getEntry(alias, null) as KeyStore.PrivateKeyEntry
        return entry.certificate.publicKey
    }

    fun getPrivateKey(alias: String): PrivateKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val entry = keyStore.getEntry(alias, null) as KeyStore.PrivateKeyEntry
        return entry.privateKey
    }


    fun removeKey(alias: String) {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        keyStore.deleteEntry(alias)
    }

    fun savePublicKey(alias: String, publicKey: PublicKey) {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null) // Keystore must be loaded before it can be accessed

//        // Opublikowanie klucza publicznego wymaga stworzenia certyfikatu, w tym przypadku użyjemy narzędzi zewnętrznych
//        val cert = ... // Konwersja PublicKey do certyfikatu X.509
//
//        // Zapisz certyfikat w Keystore pod danym aliasem
//        keyStore.setCertificateEntry(alias, cert)
    }
}