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
import kotlinx.coroutines.Job
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.util.Timer
import kotlin.concurrent.schedule
import java.net.InetAddress
import kotlin.random.Random

object UdpListener {
    val random = Random
    var timer: Timer? = null
    private var port = 1985 // Default port; can be set by calling `initialize(port)`
    private var isListening = false
    private var dataLimit = 50
    private val messagesLimit = 500
    private var showErrorsInMessages : Boolean = true
    private lateinit var socket: DatagramSocket

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> get() = _message

    private val _messages = MutableLiveData<List<String>>()
    val messages: LiveData<List<String>> get() = _messages

    private val _dataSets = MutableLiveData<Map<String, List<Pair<Float, Float>>>>()
    val dataSets: LiveData<Map<String, List<Pair<Float, Float>>>> get() = _dataSets

    private val dataMap = mutableMapOf<String, MutableList<Pair<Float, Float>>>()

    val dateTimeFormatter = DateTimeFormatter.ofPattern(CsvData.dateTimeCsvFormat)
        .withResolverStyle(ResolverStyle.STRICT)

    private val _lastError = MutableLiveData<String>()
    val lastError: LiveData<String> get() = _lastError

    private var listeningJob: Job? = null

    fun initialize(newPort: Int, limit: Int, showErrorsInMessages: Boolean = true) {
        stopListening()
        port = newPort
        dataLimit = limit
        this.showErrorsInMessages = showErrorsInMessages
        Log.i("UdpListener", "Initialize")

        startListening()

        var ip = getLocalIpAddress()
        if(ip.startsWith("10.")) {
            mockIfInDebug()
        }
    }

    private fun stopListening() {
        listeningJob?.cancel()  // This will cancel the coroutine if it is active
        listeningJob = null      // Optionally, clear the job reference
        closeTimer()
        isListening = false
    }

    private fun startListening() {
        listeningJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                socket = DatagramSocket(port)
                val buffer = ByteArray(1024)
                val packet = DatagramPacket(buffer, buffer.size)
                isListening = true

                var ip = getLocalIpAddress()
                var port = socket.localPort
                Log.i("UdpListener", "Started")
                postMessage("Started at: $ip:$port...")

                while (isListening && !socket.isClosed) {
                    socket.receive(packet)
                    Log.i("UdpListener", "received")
                    val message = String(packet.data, 0, packet.length)

                    ip = packet.address.toString()
                    port = packet.port
                    postMessage("$ip:$port: $message")

                    val (setName, timestamp, voltage) = message.split(",").map { it.trim() }
                    val dataList = dataMap.getOrPut(setName) { mutableListOf() }

                    val dateTime = try { LocalDateTime.parse(timestamp, dateTimeFormatter)
                    }catch (e: DateTimeParseException) {
                        Log.e("UdpListener", "text: '$timestamp'", e)
                        postMessage(e.message, isError = true)
                        continue
                    }

                    val timestampFloat = dateTime.toEpoch().toFloat()
                    val voltageFloat = voltage.toFloat()

                    Log.i("UdpListener", "dataList.size: ${dataList.size}")
                    if (dataList.size >= dataLimit){
                        dataList.removeAt(0)
                        //removeAtForAllDataSets(0)
                    }
                    dataList.add(timestampFloat to voltageFloat)

                    _dataSets.postValue(dataMap)
                }
            } catch (e: Exception) {
                Log.e("UdpListener", "Error receiving UDP packet", e)
                postMessage(e.message, isError = true)
            } finally {
                isListening = false
                if(!socket.isClosed)
                    socket.close()
                closeTimer()
                postMessage("Stopped")
                Log.i("UdpListener", "Stopped")
            }
        }
    }

    private fun postMessage(message: String?, isError: Boolean = false, addNumber: Boolean = true){
        Log.i("UdpListener", "postMessage")
        if (message.isNullOrEmpty()) return
        var msg: String = if(addNumber) "${(_messages.value?.size ?: 0) + 1}$message"
        else message
        if(!isError || showErrorsInMessages) {
            _message.postValue(msg)
            val updatedList = _messages.value?.toMutableList() ?: mutableListOf()
            if(updatedList.size >= messagesLimit) updatedList.removeAt(0)
            updatedList.add(msg)
            _messages.postValue(updatedList)
        }
        if(isError) {
            _lastError.postValue(msg)
        }
    }

    private fun removeAtForAllDataSets(idx: Int){
        Log.i("UdpListener", "removeAtForAllDataSets")
        for (mapEntry in dataMap){
            if(mapEntry.value.isNotEmpty() && idx < mapEntry.value.size) {
                Log.i("UdpListener", "removeAt: ${mapEntry.key}")
                mapEntry.value.removeAt(idx)
            }
        }
    }

    private fun closeTimer(){
        timer?.cancel()
        timer = null
    }

    private fun mockIfInDebug()
    {
        // Schedule a task to run every minute (60000 milliseconds)
        closeTimer()
        timer = Timer()
        timer?.schedule(0, 1000) {

            val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val localDateTime = LocalDateTime.now()
            val dateTime =  dateTimeFormatter.format(localDateTime)
            Log.d("UdpSender", "Task executed at: $dateTime")
            val serverAddress = "127.0.0.1"
            val socket1 = DatagramSocket()
            val socket2 = DatagramSocket()
            // Get the server's InetAddress
            val address = InetAddress.getByName(serverAddress)

            try {
                val voltage1 = random.nextInt(10, 14).toFloat()
                val name1 = "Test1"

                val voltage2 = random.nextInt(23, 28).toFloat()
                val name2 = "Test2"
                // Create a message to send
                val message1 = "$name1,$dateTime,$voltage1".toByteArray()
                val message2 = "$name2,$dateTime,$voltage2".toByteArray()

                // Create a DatagramPacket to send the message
                val packet1 = DatagramPacket(message1, message1.size, address, port)
                val packet2 = DatagramPacket(message2, message2.size, address, port)

                // Send the packet
                socket1.send(packet1)
                socket2.send(packet2)
            }catch (e: Exception){
                Log.e("UdpSender", "Error sending UDP packet", e)
            }
        }
    }
}
