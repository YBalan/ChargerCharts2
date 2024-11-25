package com.example.chargercharts2.utils

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.chargercharts2.BuildConfig.IS_DEBUG_BUILD
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
    const val DEFAULT_PORT = 1985
    const val DEFAULT_DATA_LIMIT = 120

    private var random = Random

    private var _port = DEFAULT_PORT // Default port; can be set by calling `initialize(port)`
    val port: Int get() = _port
    private var _isListening = false
    val isListening: Boolean get() = _isListening
    private var _dataLimit = DEFAULT_DATA_LIMIT
    val dataLimit: Int get() = _dataLimit

    private const val LOGS_LIMIT = 500
    private var showErrorsInMessages : Boolean = true
    private lateinit var socket: DatagramSocket
    private var timer: Timer? = null
    private var listeningJob: Job? = null

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> get() = _message

    private val _messages = MutableLiveData<List<String>>()
    val messages: LiveData<List<String>> get() = _messages

    private val _dataSets = MutableLiveData<Map<String, List<CsvDataValue>>>()
    val dataSets: LiveData<Map<String, List<CsvDataValue>>> get() = _dataSets

    private val dataMap = mutableMapOf<String, MutableList<CsvDataValue>>()

    private val _removedEntry = MutableLiveData<CsvDataValue>()
    val removedEntry: LiveData<CsvDataValue> get() = _removedEntry

    private val _addedEntry = MutableLiveData<CsvDataValue>()
    val addedEntry: LiveData<CsvDataValue> get() = _addedEntry

    private val _lastError = MutableLiveData<String>()
    //val lastError: LiveData<String> get() = _lastError

    val dateTimeFormatter = DateTimeFormatter.ofPattern(CsvData.DATE_TIME_CSV_FORMAT)
        .withResolverStyle(ResolverStyle.STRICT)

    fun initialize(newPort: Int, limit: Int, showErrorsInMessages: Boolean = true) {
        clear()
        stopListening()
        _port = newPort
        _dataLimit = limit
        this.showErrorsInMessages = showErrorsInMessages
        Log.i("UdpListener", "Initialize: port: $_port; dataLimit: $_dataLimit")

        startListening()

        var ip = getLocalIpAddress()
        if(IS_DEBUG_BUILD && ip.startsWith("10.")) {
            mockUDPDataSenders()
        }
    }

    private fun stopListening() {
        Log.i("UdpListener", "stopListening")
        listeningJob?.cancel()  // This will cancel the coroutine if it is active
        listeningJob = null      // Optionally, clear the job reference
        closeTimer()
        _isListening = false
    }

    private fun startListening() {
        Log.i("UdpListener", "startListening")
        listeningJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                socket = DatagramSocket(_port)

                val buffer = ByteArray(1024)
                val packet = DatagramPacket(buffer, buffer.size)
                _isListening = true

                var ip = getLocalIpAddress()
                var port = socket.localPort
                Log.i("UdpListener", "Started")
                postMessage("Started at: $ip:$port...")

                while (_isListening && !socket.isClosed) {
                    socket.receive(packet)
                    Log.i("UdpListener", "received")
                    val message = String(packet.data, 0, packet.length)

                    ip = packet.address.toString()
                    port = packet.port
                    postMessage("$ip:$port: $message")

                    val (setName, timestamp, voltage, relay) = message.split(",").map { it.trim() }
                    val dataList = dataMap.getOrPut(setName) { mutableListOf() }

                    val dateTime = try {
                        LocalDateTime.parse(timestamp, dateTimeFormatter)
                    } catch (e: DateTimeParseException) {
                        Log.e("UdpListener", "text: '$timestamp'", e)
                        postMessage(e.message, isError = true)
                        continue
                    }

                    //val timestampFloat = dateTime.toEpoch().toFloat()
                    val voltageFloat = voltage.toFloat()
                    val relayFloat = relay.toFloat()

                    Log.i("UdpListener", "dataLimit: $_dataLimit; dataList.size: ${dataList.size}")
                    if (_dataLimit > 0 && dataList.size >= _dataLimit) {
                        //dataList.removeAt(0)
                        removeAtForAllDataSets(0)
                    }

                    val addEntry = CsvDataValue(dateTime, voltageFloat, relayFloat)
                    dataList.add(addEntry)

                    _addedEntry.postValue(addEntry)
                    _dataSets.postValue(dataMap)
                }
            } catch (e: Exception) {
                Log.e("UdpListener", "Error receiving UDP packet", e)
                postMessage(e.message, isError = true)
            } finally {
                _isListening = false
                if (!socket.isClosed)
                    socket.close()
                closeTimer()
                postMessage("Stopped")
                Log.i("UdpListener", "Stopped")
            }
        }
    }

    private fun postMessage(message: String?, isError: Boolean = false, addNumber: Boolean = false){
        Log.i("UdpListener", "postMessage")
        if (message.isNullOrEmpty()) return
        var msg: String = if(addNumber) "${(_messages.value?.size ?: 0) + 1}$message"
        else message
        if(!isError || showErrorsInMessages) {
            _message.postValue(msg)
            val updatedList = _messages.value?.toMutableList() ?: mutableListOf()
            if(updatedList.size >= LOGS_LIMIT) updatedList.removeAt(0)
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
                val entry = mapEntry.value.removeAt(idx)
                _removedEntry.postValue(entry)
            }
        }
    }

    fun clear(){
        dataMap.clear()
        //_dataSets.postValue(dataMap)
    }

    private fun closeTimer(){
        Log.i("UdpListener", "closeTimer")
        timer?.cancel()
        timer = null
    }

    private fun mockUDPDataSenders()
    {
        // Schedule a task to run every minute (60000 milliseconds)
        closeTimer()
        timer = Timer()
        timer?.schedule(0, 5000) {

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
                val voltage1 = random.nextInt(23, 28).toFloat()
                val name1 = "ChargerCharts|28V|"

                val voltage2 = random.nextInt(10, 13).toFloat()
                val name2 = "ChargerCharts|12.4V|"

                val voltage3 = random.nextInt(16, 24).toFloat()
                val name3 = "ChargerCharts|16.5V|"

                val voltage4 = random.nextInt(-1, 10).toFloat()
                val name4 = "ChargerCharts|8.0V|"

                // Create a message to send
                val message1 = "$name1,$dateTime,$voltage1,${random.nextInt(0,1)}".toByteArray()
                val message2 = "$name2,$dateTime,$voltage2,${random.nextInt(0,1)}".toByteArray()
                val message3 = "$name3,$dateTime,$voltage3,${random.nextInt(0,1)}".toByteArray()
                val message4 = "$name4,$dateTime,$voltage4,${random.nextInt(0,1)}".toByteArray()

                // Create a DatagramPacket to send the message
                val packet1 = DatagramPacket(message1, message1.size, address, _port)
                val packet2 = DatagramPacket(message2, message2.size, address, _port)
                val packet3 = DatagramPacket(message3, message3.size, address, _port)
                val packet4 = DatagramPacket(message4, message4.size, address, _port)

                // Send the packet
                socket1.send(packet1)
                socket2.send(packet2)

                socket1.send(packet3)
                socket2.send(packet4)

            }catch (e: Exception){
                Log.e("UdpSender", "Error sending UDP packet", e)
            }
            finally {
                socket1.close()
                socket2.close()
            }
        }
    }
}
