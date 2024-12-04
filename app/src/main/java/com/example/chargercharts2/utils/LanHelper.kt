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

fun Uri?.getFileName(context: Context?): String? {
    var fileName: String? = null
    if (this?.scheme == "content") {
        val cursor: Cursor? = context?.contentResolver?.query(this, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && it.moveToFirst()) {
                fileName = it.getString(nameIndex)
            }
        }
    } else if (this?.scheme == "file") {
        fileName = this.lastPathSegment
    }
    return fileName
}

fun Uri?.getFileExtension(context: Context?): String? {
    val fileName = this.getFileName(context)
    return fileName?.substringAfterLast(".", "")
}