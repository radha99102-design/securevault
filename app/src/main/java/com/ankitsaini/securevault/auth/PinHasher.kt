package com.ankitsaini.securevault.auth

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import java.util.Base64

object PinHasher {
    
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16
    
    fun hashPin(pin: String, salt: ByteArray? = null): String {
        val pinSalt = salt ?: generateSalt()
        val spec = PBEKeySpec(pin.toCharArray(), pinSalt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        
        return Base64.getEncoder().encodeToString(pinSalt) + ":" +
               Base64.getEncoder().encodeToString(hash)
    }
    
    fun verifyPin(pin: String, storedHash: String): Boolean {
        return try {
            val parts = storedHash.split(":")
            if (parts.size != 2) return false
            
            val salt = Base64.getDecoder().decode(parts[0])
            val computedHash = hashPin(pin, salt)
            
            // Use constant-time comparison to prevent timing attacks
            MessageDigest.isEqual(
                computedHash.toByteArray(),
                storedHash.toByteArray()
            )
        } catch (e: Exception) {
            false
        }
    }
    
    private fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return salt
    }
}
