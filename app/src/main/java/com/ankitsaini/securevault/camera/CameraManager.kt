package com.ankitsaini.securevault.camera

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.ankitsaini.securevault.data.model.FailedAttemptRecord
import com.ankitsaini.securevault.data.model.SecurityEvent
import com.ankitsaini.securevault.data.model.EventType
import com.ankitsaini.securevault.data.repository.SecurityRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securityRepository: SecurityRepository
) {
    
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var isCameraActive = false
    
    companion object {
        private const val PHOTO_DIRECTORY = "security_photos"
        private const val PHOTO_FILENAME_PREFIX = "INTRUDER"
        private const val PHOTO_FILENAME_FORMAT = "yyyyMMdd_HHmmss_SSS"
        private const val IMAGE_QUALITY = 90
        private const val MAX_IMAGE_WIDTH = 1080
        private const val MAX_IMAGE_HEIGHT = 1920
    }
    
    data class CameraResult(
        val success: Boolean,
        val photoPath: String? = null,
        val errorMessage: String? = null
    )
    
    suspend fun captureIntruderPhoto(
        packageName: String,
        onResult: (CameraResult) -> Unit
    ) {
        if (!hasCameraPermission()) {
            onResult(CameraResult(false, errorMessage = "Camera permission not granted"))
            return
        }
        
        if (isCameraActive) {
            onResult(CameraResult(false, errorMessage = "Camera already in use"))
            return
        }
        
        try {
            isCameraActive = true
            
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            
            cameraProviderFuture.addListener({
                try {
                    cameraProvider = cameraProviderFuture.get()
                    
                    // Configure image capture
                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                        .setTargetRotation(Surface.ROTATION_0)
                        .build()
                    
                    // Select front camera for intruder photo
                    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                    
                    // Create preview (not displayed, but needed for camera to work)
                    val preview = Preview.Builder().build()
                    
                    // Unbind any existing use cases
                    cameraProvider?.unbindAll()
                    
                    // Bind to lifecycle
                    cameraProvider?.bindToLifecycle(
                        createLifecycleOwner(),
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    
                    // Capture photo
                    capturePhoto(packageName, onResult)
                    
                } catch (e: Exception) {
                    isCameraActive = false
                    onResult(CameraResult(false, errorMessage = "Camera initialization failed: ${e.message}"))
                }
            }, ContextCompat.getMainExecutor(context))
            
        } catch (e: Exception) {
            isCameraActive = false
            onResult(CameraResult(false, errorMessage = "Camera error: ${e.message}"))
        }
    }
    
    private fun capturePhoto(
        packageName: String,
        onResult: (CameraResult) -> Unit
    ) {
        val photoFile = createPhotoFile()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        
        imageCapture?.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileOptions) {
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            // Compress the image
                            compressImage(photoFile)
                            
                            // Log the security event
                            securityRepository.logFailedUnlock(packageName, "INTRUDER_PHOTO")
                            
                            securityRepository.logEvent(
                                SecurityEvent(
                                    packageName = packageName,
                                    eventType = EventType.INTRUDER_PHOTO_CAPTURED,
                                    eventDetails = "Intruder photo captured successfully",
                                    photoPath = photoFile.absolutePath,
                                    wasSuccessful = true
                                )
                            )
                            
                            isCameraActive = false
                            releaseCamera()
                            
                            onResult(CameraResult(true, photoPath = photoFile.absolutePath))
                        } catch (e: Exception) {
                            isCameraActive = false
                            releaseCamera()
                            onResult(CameraResult(false, errorMessage = "Failed to process photo"))
                        }
                    }
                }
                
                override fun onError(exception: ImageCaptureException) {
                    CoroutineScope(Dispatchers.Main).launch {
                        isCameraActive = false
                        releaseCamera()
                        onResult(CameraResult(false, errorMessage = "Photo capture failed: ${exception.message}"))
                    }
                }
            }
        )
    }
    
    private fun createPhotoFile(): File {
        val photoDirectory = File(context.filesDir, PHOTO_DIRECTORY)
        if (!photoDirectory.exists()) {
            photoDirectory.mkdirs()
        }
        
        val timestamp = SimpleDateFormat(
            PHOTO_FILENAME_FORMAT,
            Locale.getDefault()
        ).format(Date())
        
        return File(photoDirectory, "${PHOTO_FILENAME_PREFIX}_$timestamp.jpg")
    }
    
    private fun compressImage(photoFile: File) {
        try {
            val originalBitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
            
            // Calculate scaling
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scaleFactor = minOf(
                MAX_IMAGE_WIDTH.toFloat() / width,
                MAX_IMAGE_HEIGHT.toFloat() / height,
                1.0f
            )
            
            if (scaleFactor < 1.0f) {
                val newWidth = (width * scaleFactor).toInt()
                val newHeight = (height * scaleFactor).toInt()
                
                val scaledBitmap = Bitmap.createScaledBitmap(
                    originalBitmap,
                    newWidth,
                    newHeight,
                    true
                )
                
                // Save compressed image
                FileOutputStream(photoFile).use { outputStream ->
                    scaledBitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        IMAGE_QUALITY,
                        outputStream
                    )
                }
                
                // Clean up
                if (scaledBitmap != originalBitmap) {
                    originalBitmap.recycle()
                }
                scaledBitmap.recycle()
            }
        } catch (e: Exception) {
            // Compression failed, use original image
        }
    }
    
    fun getIntruderPhotos(): List<File> {
        val photoDirectory = File(context.filesDir, PHOTO_DIRECTORY)
        if (!photoDirectory.exists()) {
            return emptyList()
        }
        
        return photoDirectory.listFiles { file ->
            file.isFile && file.extension.equals("jpg", ignoreCase = true)
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
    
    fun getIntruderPhotoBitmap(photoPath: String): Bitmap? {
        return try {
            BitmapFactory.decodeFile(photoPath)
        } catch (e: Exception) {
            null
        }
    }
    
    fun deletePhoto(photoPath: String): Boolean {
        return try {
            val photoFile = File(photoPath)
            photoFile.delete()
        } catch (e: Exception) {
            false
        }
    }
    
    fun deleteAllPhotos() {
        val photoDirectory = File(context.filesDir, PHOTO_DIRECTORY)
        if (photoDirectory.exists()) {
            photoDirectory.listFiles()?.forEach { file ->
                file.delete()
            }
        }
    }
    
    fun getPhotoDirectorySize(): Long {
        val photoDirectory = File(context.filesDir, PHOTO_DIRECTORY)
        if (!photoDirectory.exists()) {
            return 0
        }
        
        return photoDirectory.listFiles()?.sumOf { it.length() } ?: 0
    }
    
    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun createLifecycleOwner(): LifecycleOwner {
        return object : LifecycleOwner {
            private val lifecycleRegistry = androidx.lifecycle.LifecycleRegistry(this)
            
            init {
                lifecycleRegistry.currentState = androidx.lifecycle.Lifecycle.State.STARTED
            }
            
            override val lifecycle: androidx.lifecycle.Lifecycle
                get() = lifecycleRegistry
        }
    }
    
    fun releaseCamera() {
        try {
            cameraProvider?.unbindAll()
            imageCapture = null
            cameraProvider = null
        } catch (e: Exception) {
            // Camera release failed
        }
    }
    
    fun shutdown() {
        releaseCamera()
        cameraExecutor.shutdown()
    }
}
