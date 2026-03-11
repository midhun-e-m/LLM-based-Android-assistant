package com.example.llmosassistant.utils

import android.content.Context
import android.provider.ContactsContract

object ContactResolver {

    fun getPhoneNumber(context: Context, name: String): String? {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val cursor = context.contentResolver.query(
            uri,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$name%"),
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                return it.getString(
                    it.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    )
                )
            }
        }
        return null
    }
}
