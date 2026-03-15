package com.example.llmosassistant.pccontrol

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.llmosassistant.R

class PcLinkFragment : Fragment() {

    private lateinit var deviceManager: PcDeviceManager
    private lateinit var statusText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_pc_link, container, false)

        deviceManager = PcDeviceManager(requireContext())

        statusText = view.findViewById(R.id.pcStatusText)
        val linkButton = view.findViewById<Button>(R.id.linkPcButton)

        updateStatus()

        linkButton.setOnClickListener {
            openQRScanner()
        }

        return view
    }

    private fun updateStatus() {

        val deviceId = deviceManager.getDeviceId()

        if (deviceId != null) {
            statusText.text = "Linked to PC: $deviceId"
        } else {
            statusText.text = "No PC linked"
        }
    }

    private fun openQRScanner() {

        val intent = Intent(requireContext(), QRScannerActivity::class.java)

        startActivityForResult(intent, 1001)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {

            val deviceId = data?.getStringExtra("deviceId") ?: return

            deviceManager.saveDeviceId(deviceId)

            updateStatus()
        }
    }
}