package com.example.chargercharts2.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.NetworkInterface
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle


class HomeViewModel : ViewModel() {

    private val _voltageData = MutableLiveData<Map<String, List<Pair<Float, Float>>>>()
    val voltageData: LiveData<Map<String, List<Pair<Float, Float>>>> get() = _voltageData

    private val _ipAddress = MutableLiveData<String>()
    val ipAddress: LiveData<String> get() = _ipAddress

    private val udpPort = 1985
    private val dataSets = mutableMapOf<String, MutableList<Pair<Float, Float>>>() // Map of set names to entries

    val dateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
        .withResolverStyle(ResolverStyle.STRICT)

    init {
        _ipAddress.value = getLocalIpAddress()
        startUdpListener()
    }

    private fun startUdpListener() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val socket = DatagramSocket(udpPort)
                val buffer = ByteArray(1024)
                val packet = DatagramPacket(buffer, buffer.size)

                while (true) {
                    socket.receive(packet)
                    val message = String(packet.data, 0, packet.length)

                    val (setName, timestamp, voltage) = message.split(",").map { it.trim() }

                    _ipAddress.postValue(message)

                    val dateTime = try { LocalDateTime.parse(timestamp, dateTimeFormatter)
                    }catch (e: DateTimeParseException) {
                        Log.e("parseCsvFile", "text: '$timestamp'")
                        e.printStackTrace()
                        continue
                    }

                    val timestampFloat = dateTime.toEpoch().toFloat()
                    val voltageFloat = voltage.toFloat()

                    // Add to the corresponding set's entries
                    val entries = dataSets.getOrPut(setName) { mutableListOf() }
                    entries.add(timestampFloat to voltageFloat)

                    _voltageData.postValue(dataSets)
                }
            } catch (e: Exception) {
                Log.e("UDP", "Error receiving UDP packet", e)
                _ipAddress.postValue(e.message)
            }
        }
    }

    private fun getLocalIpAddress(): String {
        return NetworkInterface.getNetworkInterfaces().asSequence()
            .flatMap { it.inetAddresses.asSequence() }
            .find { !it.isLoopbackAddress && it.hostAddress.contains(".") }
            ?.hostAddress ?: "Unknown"
    }
}

private fun getDateTime(seconds: Float) : LocalDateTime{
    return LocalDateTime.ofEpochSecond(seconds.toLong(), 0, ZoneOffset.UTC)
}

// Convert LocalDateTime to epoch milliseconds
private fun LocalDateTime.toEpoch(): Long {
    return this.atZone(ZoneOffset.UTC).toInstant().epochSecond
}

private fun getDefaultZoneOffset(): ZoneOffset {
    return ZonedDateTime.now(ZoneId.systemDefault()).offset
}


