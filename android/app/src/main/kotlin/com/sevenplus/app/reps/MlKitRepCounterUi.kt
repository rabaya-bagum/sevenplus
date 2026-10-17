package com.sevenplus.app.reps

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import com.sevenplus.app.contracts.RepCounterUi
import com.sevenplus.core.RepCounter
import com.sevenplus.core.RepProfile
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Front camera + ML Kit pose (base model) -> joint angle -> core RepCounter. */
class MlKitRepCounterUi : RepCounterUi {
    @Composable
    override fun Panel(exerciseId: String, active: Boolean, modifier: Modifier) {
        val spec = RepProfile.BY_EXERCISE[exerciseId] ?: return
        val context = LocalContext.current
        var granted by remember {
            mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
        }
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
        val hasCamera = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }

        Box(modifier.clip(RoundedCornerShape(16.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
            when {
                !hasCamera -> Text("No camera available", color = Color.White, modifier = Modifier.padding(16.dp))
                !granted -> Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Allow the camera to count reps. Video stays on this device.", color = Color.White)
                    Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text("Allow camera") }
                }
                !active -> Text("Paused", color = Color.White)
                else -> CameraCounter(exerciseId, spec)
            }
        }
    }

    @OptIn(ExperimentalGetImage::class)
    @Composable
    private fun CameraCounter(exerciseId: String, spec: RepProfile.Spec) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        var reps by remember(exerciseId) { mutableIntStateOf(0) }
        var seesBody by remember { mutableStateOf(false) }
        var failed by remember { mutableStateOf(false) }
        val counter = remember(exerciseId) { RepCounter(spec.down, spec.up) }
        val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }

        DisposableEffect(exerciseId, lifecycleOwner) {
            val executor = Executors.newSingleThreadExecutor()
            val detector = PoseDetection.getClient(
                PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.STREAM_MODE).build())
            val busy = AtomicBoolean(false)
            val disposed = AtomicBoolean(false)
            val main = ContextCompat.getMainExecutor(context)
            var provider: ProcessCameraProvider? = null
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                if (disposed.get()) return@addListener
                try {
                    val p = future.get().also { provider = it }
                    val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                    analysis.setAnalyzer(executor) { proxy ->
                        val media = proxy.image
                        if (media == null || disposed.get() || !busy.compareAndSet(false, true)) { proxy.close(); return@setAnalyzer }
                        detector.process(InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees))
                            .addOnSuccessListener(executor) { pose ->
                                val points = pose.allPoseLandmarks.associate {
                                    it.landmarkType to Point2(it.position.x.toDouble(), it.position.y.toDouble(), it.inFrameLikelihood.toDouble())
                                }
                                val angle = PoseMapping.jointAngle(points, spec.joint)
                                val didRep = angle != null && counter.update(angle)
                                val count = counter.count
                                main.execute { seesBody = angle != null; if (didRep) reps = count }
                            }
                            .addOnCompleteListener(executor) { busy.set(false); proxy.close() }
                    }
                    p.unbindAll()
                    if (!p.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) { failed = true; return@addListener }
                    p.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis)
                } catch (_: Exception) {
                    failed = true
                }
            }, main)
            onDispose {
                disposed.set(true)
                runCatching { provider?.unbindAll() }
                runCatching { detector.close() }
                executor.shutdown()
            }
        }

        Box(Modifier.fillMaxSize()) {
            if (failed) {
                Text("Camera unavailable", color = Color.White, modifier = Modifier.align(Alignment.Center))
            } else {
                AndroidView({ previewView }, Modifier.fillMaxSize())
                Column(Modifier.align(Alignment.BottomCenter).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$reps", color = Color.White, style = MaterialTheme.typography.displayMedium)
                    Text(if (seesBody) "reps" else "Step back so your whole body is visible", color = Color.White)
                }
            }
        }
    }
}
