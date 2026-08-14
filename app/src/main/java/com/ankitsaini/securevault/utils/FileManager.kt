package com.ankitsaini.securevault.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val SECURITY_PHOTOS_DIR = "security_photos"
        private const val BACKUP_DIR = "backups"
        private const val LOGS_DIR = "logs"
        private const val TEMP_DIR = "temp"
        private const val MAX_LOG_FILE_SIZE = 5 * 1024 * 1024 // 5MB
    }
    
    fun saveBitmapToInternalStorage(
        bitmap: android.graphics.Bitmap,
        directory: String,
        filename: String
    ): String? {
        return try {
            val dir = File(context.filesDir, directory)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            
            val file = File(dir, filename)
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(
                    android.graphics.Bitmap.CompressFormat.JPEG,
                    90,
                    outputStream
                )
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }
    
    fun saveBytesToInternalStorage(
        data: ByteArray,
        directory: String,
        filename: String
    ): String? {
        return try {
            val dir = File(context.filesDir, directory)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            
            val file = File(dir, filename)
            FileOutputStream(file).use { outputStream ->
                outputStream.write(data)
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }
    
    fun readBytesFromInternalStorage(filePath: String): ByteArray? {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                file.readBytes()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    fun deleteFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            file.delete()
        } catch (e: Exception) {
            false
        }
    }
    
    fun deleteDirectory(directory: String): Boolean {
        return try {
            val dir = File(context.filesDir, directory)
            if (dir.exists()) {
                dir.deleteRecursively()
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }
    }
    
    fun getFilesInDirectory(directory: String): List<File> {
        val dir = File(context.filesDir, directory)
        if (!dir.exists()) {
            return emptyList()
        }
        
        return dir.listFiles()?.toList() ?: emptyList()
    }
    
    fun getDirectorySize(directory: String): Long {
        val dir = File(context.filesDir, directory)
        if (!dir.exists()) {
            return 0
        }
        
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
    
    fun clearTempFiles() {
        deleteDirectory(TEMP_DIR)
    }
    
    fun clearOldSecurityPhotos(maxAgeDays: Int) {
        val photoDir = File(context.filesDir, SECURITY_PHOTOS_DIR)
        if (!photoDir.exists()) {
            return
        }
        
        val cutoffTime = System.currentTimeMillis() - (maxAgeDays * 24 * 60 * 60 * 1000L)
        
        photoDir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoffTime) {
                file.delete()
            }
        }
    }
    
    fun createBackupFile(data: String): String? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "backup_$timestamp.json"
            saveBytesToInternalStorage(
                data.toByteArray(),
                BACKUP_DIR,
                filename
            )
        } catch (e: Exception) {
            null
        }
    }
    
    fun readLogFile(): String? {
        return try {
            val logDir = File(context.filesDir, LOGS_DIR)
            if (!logDir.exists()) {
                return null
            }
            
            val logFiles = logDir.listFiles()?.sortedByDescending { it.lastModified() }
            logFiles?.firstOrNull()?.readText()
        } catch (e: Exception) {
            null
        }
    }
    
    fun writeLog(message: String) {
        try {
            val logDir = File(context.filesDir, LOGS_DIR)
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            
            val logFile = File(logDir, "security_log.txt")
            
            // Rotate log file if too large
            if (logFile.exists() && logFile.length() > MAX_LOG_FILE_SIZE) {
                val rotatedFile = File(logDir, "security_log_old.txt")
                if (rotatedFile.exists()) {
                    rotatedFile.delete()
                }
                logFile.renameTo(rotatedFile)
            }
            
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            val logEntry = "$timestamp - $message\n"
            
            FileOutputStream(logFile, true).use { outputStream ->
                outputStream.write(logEntry.toByteArray())
            }
        } catch (e: Exception) {
            // Logging failed
        }
    }
    
    fun getAvailableStorageSpace(): Long {
        return try {
            val stat = android.os.StatFs(context.filesDir.absolutePath)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (e: Exception) {
            0
        }
    }
    
    fun getTotalStorageSpace(): Long {
        return try {
            val stat = android.os.StatFs(context.filesDir.absolutePath)
            stat.blockCountLong * stat.blockSizeLong
        } catch (e: Exception) {
            0
        }
    }
}
