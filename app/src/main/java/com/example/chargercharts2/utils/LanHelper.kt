package com.example.chargercharts2.utils

import java.net.NetworkInterface

private fun getLocalIpAddress(): String {
    return NetworkInterface.getNetworkInterfaces().asSequence()
        .flatMap { it.inetAddresses.asSequence() }
        .find { !it.isLoopbackAddress && it.hostAddress.contains(".") }
        ?.hostAddress ?: "Unknown"
}