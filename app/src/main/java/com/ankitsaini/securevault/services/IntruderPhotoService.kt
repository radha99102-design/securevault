package com.ankitsaini.securevault.services

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
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
class IntruderPhotoService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securityRepository: SecurityRepository
) {
    
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    
    companion object {
        private const val PHOTO_DIRECTORY = "security_photos"
        private const val PHOTO_FILENAME_FORMAT = "yyyyMMdd_HHmmss_SSS"
    }
    
    suspend fun captureIntruderPhoto(
        packageName: String,
        onPhotoCaptured: (String?) -> Unit
    ) {
        if (!hasCameraPermission()) {
            onPhotoCaptured(null)
            return
        }
        
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    cameraProvider = cameraProviderFuture.get()
                    
                    // Set up image capture
                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                        .build()
                    
                    // Get front camera
                    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                    
                    // Bind to lifecycle
                    val preview = Preview.Builder().build()
                    
                    cameraProvider?.unbindAll()
                    cameraProvider?.bindToLifecycle(
                        androidx.lifecycle.LifecycleOwner { androidx.lifecycle.Lifecycle() },
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    
                    // Capture photo
                    capturePhoto(packageName, onPhotoCaptured)
                    
                } catch (e: Exception) {
                    onPhotoCaptured(null)
                }
            }, ContextCompat.getMainExecutor(context))
            
        } catch (e: Exception) {
            onPhotoCaptured(null)
        }
    }
    
    private fun capturePhoto(
        packageName: String,
        onPhotoCaptured: (String?) -> Unit
    ) {
        val photoFile = createPhotoFile()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        
        imageCapture?.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    CoroutineScope(Dispatchers.Main).launch {
                        // Save failed attempt record with photo
                        securityRepository.logFailedUnlock(packageName, "INTRUDER_PHOTO")
                        
                        securityRepository.logEvent(
                            SecurityEvent(
                                packageName = packageName,
                                eventType = EventType.INTRUDER_PHOTO_CAPTURED,
                                eventDetails = "Intruder photo captured",
                                photoPath = photoFile.absolutePath,
                                wasSuccessful = true
                            )
                        )
                        
                        onPhotoCaptured(photoFile.absolutePath)
                    }
                }
                
                override fun onError(exception: ImageCaptureException) {
                    CoroutineScope(Dispatchers.Main).launch {
                        onPhotoCaptured(null)
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
        
        return File(photoDirectory, "INTRUDER_$timestamp.jpg")
    }
    
    fun getIntruderPhotos(): List<File> {
        val photoDirectory = File(context.filesDir, PHOTO_DIRECTORY)
        if (!photoDirectory.exists()) {
            return emptyList()
        }
        
        return photoDirectory.listFiles { file ->
            file.isFile && file.extension == "jpg"
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
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
        cameraExecutor.shutdown()
        releaseCamera()
    }
}
