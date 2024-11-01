package com.example.chargercharts2.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import java.net.NetworkInterface

fun getLocalIpAddress(): String {
    return NetworkInterface.getNetworkInterfaces().asSequence()
        .flatMap { it.inetAddresses.asSequence() }
        .find { !it.isLoopbackAddress && it?.hostAddress?.contains(".") == true }
        ?.hostAddress ?: "Unknown"
}

fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var fileName: String? = null
    if (uri.scheme == "content") {
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && it.moveToFirst()) {
                fileName = it.getString(nameIndex)
            }
        }
    } else if (uri.scheme == "file") {
        fileName = uri.lastPathSegment
    }
    return fileName
}