package com.example.llmosassistant.pccontrol

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class QRScannerActivity : AppCompatActivity() {

    private val qrLauncher =
        registerForActivityResult(ScanContract()) { result ->

            if (result.contents != null) {

                val intent = Intent()
                intent.putExtra("deviceId", result.contents)

                setResult(Activity.RESULT_OK, intent)
                finish()
            } else {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val options = ScanOptions()
        options.setPrompt("Scan PC Link QR")
        options.setBeepEnabled(true)
        options.setOrientationLocked(true)
        options.setCaptureActivity(PortraitCaptureActivity::class.java)

        qrLauncher.launch(options)
    }
}