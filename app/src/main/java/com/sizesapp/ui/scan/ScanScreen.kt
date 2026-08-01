package com.sizesapp.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.sizesapp.ocr.ParsedLabel
import java.io.File
import java.util.concurrent.Executor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    viewModel: ScanViewModel,
    onScanned: (photoPath: String, parsed: ParsedLabel) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    var pendingPhotoPath by remember { mutableStateOf<String?>(null) }
    val onScannedState = rememberUpdatedState(onScanned)
    LaunchedEffect(uiState.result, pendingPhotoPath) {
        val result = uiState.result
        val photoPath = pendingPhotoPath
        if (result != null && photoPath != null) {
            onScannedState.value(photoPath, result)
            viewModel.consumeResult()
            pendingPhotoPath = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan a label") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (hasCameraPermission) {
                var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
                CameraPreview(onImageCaptureReady = { imageCapture = it })

                FloatingActionButton(
                    onClick = {
                        val capture = imageCapture ?: return@FloatingActionButton
                        val photoFile = File(context.cacheDir, "label_${System.currentTimeMillis()}.jpg")
                        capture.takePicture(
                            photoFile,
                            ContextCompat.getMainExecutor(context),
                            onSuccess = {
                                pendingPhotoPath = photoFile.absolutePath
                                viewModel.processCapturedPhoto(photoFile)
                            },
                            onError = { viewModel.reportCaptureError(it) },
                        )
                    },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp),
                ) {
                    Icon(Icons.Filled.Camera, contentDescription = "Capture")
                }
            } else {
                Text(
                    "Camera permission is needed to scan labels.",
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
            }

            if (uiState.isProcessing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            uiState.error?.let { message ->
                Snackbar(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                    Text(message)
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(onImageCaptureReady: (ImageCapture) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener(
                {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    // These photos only ever feed OCR (or get shrunk into a
                    // thumbnail for storage) -- MINIMIZE_LATENCY over
                    // MAXIMIZE_QUALITY since photography quality isn't the goal.
                    val imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture,
                    )
                    onImageCaptureReady(imageCapture)
                },
                ContextCompat.getMainExecutor(ctx),
            )
            previewView
        },
    )
}

private fun ImageCapture.takePicture(
    file: File,
    executor: Executor,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
) {
    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
    takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) = onSuccess()
            override fun onError(exception: ImageCaptureException) {
                onError(exception.message ?: "Couldn't capture the photo, try again.")
            }
        },
    )
}
