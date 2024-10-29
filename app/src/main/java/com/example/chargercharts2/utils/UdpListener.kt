package com.example.chargercharts2.utils

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import com.example.chargercharts2.models.*
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

object UdpListener {
    private var port = 1985 // Default port; can be set by calling `initialize(port)`
    private var isListening = false
    private lateinit var socket: DatagramSocket

    private val _messages = MutableLiveData<String>()
    val messages: LiveData<String> get() = _messages

    private val _dataSets = MutableLiveData<Map<String, List<Pair<Float, Float>>>>()
    val dataSets: LiveData<Map<String, List<Pair<Float, Float>>>> get() = _dataSets

    private val dataMap = mutableMapOf<String, MutableList<Pair<Float, Float>>>()
    private var dataLimit = 50

    val dateTimeFormatter = DateTimeFormatter.ofPattern(CsvData.dateTimeCsvFormat)
        .withResolverStyle(ResolverStyle.STRICT)

    fun initialize(newPort: Int, limit: Int) {
        port = newPort
        dataLimit = limit
        if (!isListening) {
            startListening()
        }
    }

    private fun startListening() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                socket = DatagramSocket(port)
                val buffer = ByteArray(1024)
                val packet = DatagramPacket(buffer, buffer.size)
                isListening = true

                while (isListening) {
                    socket.receive(packet)
                    val message = String(packet.data, 0, packet.length)
                    _messages.postValue(message)

                    val (setName, timestamp, voltage) = message.split(",").map { it.trim() }
                    val dataList = dataMap.getOrPut(setName) { mutableListOf() }

                    val dateTime = try { LocalDateTime.parse(timestamp, dateTimeFormatter)
                    }catch (e: DateTimeParseException) {
                        Log.e("UdpListener", "text: '$timestamp'")
                        e.printStackTrace()
                        continue
                    }

                    val timestampFloat = dateTime.toEpoch().toFloat()
                    val voltageFloat = voltage.toFloat()

                    if (dataList.size >= dataLimit) dataList.removeAt(0)
                    dataList.add(timestampFloat to voltageFloat)

                    _dataSets.postValue(dataMap)
                }
            } catch (e: Exception) {
                Log.e("UDP", "Error receiving UDP packet", e)
            } finally {
                socket.close()
            }
        }
    }
}
