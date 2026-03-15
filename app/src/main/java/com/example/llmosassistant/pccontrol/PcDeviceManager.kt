package com.example.llmosassistant.pccontrol

import android.content.Context

class PcDeviceManager(context: Context) {

    private val prefs =
        context.getSharedPreferences("pc_device", Context.MODE_PRIVATE)

    fun saveDeviceId(deviceId: String) {
        prefs.edit().putString("device_id", deviceId).apply()
    }

    fun getDeviceId(): String? {
        return prefs.getString("device_id", null)
    }

    fun isLinked(): Boolean {
        return getDeviceId() != null
    }
}