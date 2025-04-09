package com.enrique_john_wayne_m.cupfe_scanner

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import android.util.Base64


class MainActivity : AppCompatActivity() {
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val expectedBaseUrl = "http://192.168.20.209/expresso-cafe/api/stripePayment/payment_form_order.php"
    private val expectedToken = "ABC123SECRET"

    // ✅ Prevents multiple QR reads
    private var hasScanned = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), 1001
            )
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(findViewById<androidx.camera.view.PreviewView>(R.id.previewView).surfaceProvider)
            }

            val barcodeScannerOptions = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()

            val imageAnalysis = ImageAnalysis.Builder().build().also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImageProxy(imageProxy, barcodeScannerOptions)
                }
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)

        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy, options: BarcodeScannerOptions) {
        if (hasScanned) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient(options)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val encodedUrl = barcode.rawValue
                        if (encodedUrl != null) {
                            // Decode the Base64 encoded QR code content
                            val decodedUrl = String(Base64.decode(encodedUrl, Base64.NO_WRAP))

                            if (decodedUrl.startsWith(expectedBaseUrl)) {
                                val uri = android.net.Uri.parse(decodedUrl)
                                val tokenFromQR = uri.getQueryParameter("token")

                                if (tokenFromQR == expectedToken) {
                                    if (!hasScanned) {
                                        hasScanned = true
                                        Toast.makeText(this, "✅ Valid QR Code!", Toast.LENGTH_SHORT).show()

                                        val intent = Intent(Intent.ACTION_VIEW)
                                        intent.data = android.net.Uri.parse(decodedUrl)
                                        startActivity(intent)
                                    }
                                } else {
                                    Toast.makeText(this, "❌ Invalid or missing token", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(this, "❌ Invalid QR Code URL", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error scanning QR code", Toast.LENGTH_SHORT).show()
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }



    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}