package com.ankitsaini.securevault.auth

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object BiometricCryptoHelper {
    
    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "SecureVaultBiometricKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    
    fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        
        return if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
            entry.secretKey
        } else {
            createSecretKey()
        }
    }
    
    private fun createSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE
        )
        
        val parameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).apply {
            setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            setKeySize(256)
            setUserAuthenticationRequired(true)
            setInvalidatedByBiometricEnrollment(true)
        }.build()
        
        keyGenerator.init(parameterSpec)
        return keyGenerator.generateKey()
    }
    
    fun getCipherForEncryption(): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        return cipher
    }
    
    fun getCipherForDecryption(iv: ByteArray): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)
        return cipher
    }
    
    fun encryptData(data: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = getCipherForEncryption()
        val encryptedData = cipher.doFinal(data)
        return Pair(encryptedData, cipher.iv)
    }
    
    fun decryptData(encryptedData: ByteArray, iv: ByteArray): ByteArray {
        val cipher = getCipherForDecryption(iv)
        return cipher.doFinal(encryptedData)
    }
    
    fun isBiometricKeyValid(): Boolean {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
            keyStore.containsAlias(KEY_ALIAS)
        } catch (e: Exception) {
            false
        }
    }
    
    fun deleteBiometricKey() {
        try {
            val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
            }
        } catch (e: Exception) {
            // Key deletion failed
        }
    }
}
