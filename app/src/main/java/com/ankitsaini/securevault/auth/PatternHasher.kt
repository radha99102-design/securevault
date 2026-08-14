package com.ankitsaini.securevault.auth

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import java.util.Base64

object PatternHasher {
    
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16
    
    fun hashPattern(pattern: List<Int>, salt: ByteArray? = null): String {
        val patternString = pattern.joinToString(",")
        return hashPatternString(patternString, salt)
    }
    
    fun hashPatternString(pattern: String, salt: ByteArray? = null): String {
        val patternSalt = salt ?: generateSalt()
        val spec = PBEKeySpec(pattern.toCharArray(), patternSalt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        
        return Base64.getEncoder().encodeToString(patternSalt) + ":" +
               Base64.getEncoder().encodeToString(hash)
    }
    
    fun verifyPattern(pattern: List<Int>, storedHash: String): Boolean {
        return try {
            val parts = storedHash.split(":")
            if (parts.size != 2) return false
            
            val salt = Base64.getDecoder().decode(parts[0])
            val computedHash = hashPattern(pattern, salt)
            
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
