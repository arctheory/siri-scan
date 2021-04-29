package com.arcworks.cocoscan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {
    // viewFinder value to hold the camera preview
    private val viewFinder: PreviewView by lazy { findViewById(R.id.viewFinder) }

    private val openImage: ImageButton by lazy { findViewById(R.id.openImage) }
    // captureBtn value to hold camera capture button
    private val captureBtn: ImageButton by lazy { findViewById(R.id.capture_btn) }
    // imagePreview value to hold captured image preview
    private val imagePreview: ImageView by lazy { findViewById(R.id.preview) }

    // imageCapture to hold captured image data
    private var imageCapture: ImageCapture? = null
    // outDir variable to hold file of saved image
    private lateinit var outDir: File
    // cameraExecutor to execute camera capture event on seperate thread ** Not important **
    private lateinit var cameraExecutor: ExecutorService
    // convert outDir file to Uri to hold image path in device storage
    private var currentImgFilePath: Uri? = null

    private val pickImage = 100

    /**
     * onCreate function
     * it initiates the app process called activity
     * in this activity we can access camera,
     * and capture image
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // default
        setContentView(R.layout.activity_main) // default
        imagePreview.clipToOutline = true // not important

        // Check for camera permissions on device if not granted asks for permission
        if (allPermissionsGranted()) {
            // if granted permission start camera
            startCamera()
        } else {
            // if not yet granted ask permission
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
        cameraExecutor = Executors.newSingleThreadExecutor() // not important
        outDir = getOutputDirectory() // gets the current app folder

        // Take photo on button(captureBtn) click
        captureBtn.setOnClickListener {
            takePhoto() // takes photo and saves to app folder
        }

        // Shows the preview image on clicking it
        imagePreview.setOnClickListener {
            // Bellow process to show the image preview in PreviewActivity
            currentImgFilePath?.let { uri ->
                gotoPreview(uri)
            }
        }

        openImage.setOnClickListener {
            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            startActivityForResult(gallery, pickImage)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == RESULT_OK && requestCode == pickImage) {
            val imageUri = data?.data
            if ( imageUri != null ) {
                gotoPreview(imageUri)
            } else {
                Toast.makeText(baseContext, "Unable to open image", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun gotoPreview(uri: Uri) {
        Intent(this, PreviewActivity::class.java).run {
            this.data = uri
            startActivity(this)
        }
    }

    /**
     * takePhoto function
     * this functions captures image
     * and save it to outDir
     */
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        // Create file to hold latest capture
        val photoFile = File(outDir,
            "coco-" + SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg")

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        /**
         * Add image capture listener callback,
         * once photo has been taken this callback
         * triggers
         */
        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        currentImgFilePath = Uri.fromFile(photoFile)

                        // show in image preview
                        imagePreview.setImageURI(currentImgFilePath)

                        val msg = "Photo capture succeeded: $currentImgFilePath"
                        Toast.makeText(baseContext, msg, Toast.LENGTH_LONG).show()
                        Log.d(TAG, msg)
                    }
                })
    }

    /**
     * startCamera function
     * this function start camera session
     * camera preview is shown on screen once
     * the camera is active
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(viewFinder.surfaceProvider)
                    }

            imageCapture = ImageCapture.Builder().build()

            // Defaults to rear camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
        this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    // create output directory for storing images
    private fun getOutputDirectory(): File {
        val dir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (dir != null && dir.exists()) dir else filesDir
    }

    // asks for permission
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    // checks the camera permission
    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults:
            IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            // Start camera if all permissions granted
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                /**
                 * If permission denied prompt to user and finish activity
                 */
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    // initial varibales to hold information about this activity
    companion object {
        private const val TAG = "Scanner"
        private const val FILENAME_FORMAT = "dd-MM-yyyy-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 20
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    // Remove the camera executor on the activity is destroyed
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}