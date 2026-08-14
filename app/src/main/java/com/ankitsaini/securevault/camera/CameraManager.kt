package com.ankitsaini.securevault.camera

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
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
                    
                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                        .build()
                    
                    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                    val preview = Preview.Builder().build()
                    
                    cameraProvider?.unbindAll()
                    
                    val lifecycleOwner = object : LifecycleOwner {
                        private val lifecycleRegistry = LifecycleRegistry(this)
                        
                        init {
                            lifecycleRegistry.currentState = Lifecycle.State.STARTED
                        }
                        
                        override val lifecycle: Lifecycle
                            get() = lifecycleRegistry
                    }
                    
                    cameraProvider?.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    
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
