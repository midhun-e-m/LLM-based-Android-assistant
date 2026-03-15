package com.example.llmosassistant.pccontrol

import com.google.firebase.firestore.FirebaseFirestore

class PcCommandSender {

    private val db = FirebaseFirestore.getInstance()

    fun sendCommand(deviceId: String, command: PcCommand) {

        db.collection("devices")
            .document(deviceId)
            .collection("commands")
            .add(command)
    }
}