package com.example.chargercharts2.utils

import android.util.Log
import com.example.chargercharts2.models.CsvData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Timer
import kotlin.concurrent.schedule
import kotlin.random.Random

object UdpSender {
    private var random = Random
    private var timer: Timer? = null

    fun closeTimer(){
        Log.i("UdpSender", "closeTimer")
        timer?.cancel()
        timer = null
    }

    fun mockRealData(port: Int, name: String, csvData: CsvData, interval: Long = 500) {
        closeTimer()
        val serverAddress = "127.0.0.1"

        val address = InetAddress.getByName(serverAddress)
        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        CoroutineScope(Dispatchers.Default).launch  {
            val socket1 = DatagramSocket()
            try {
                csvData.values.forEach { csvDataValue ->
                    // Create a message to send
                    val dateTime = dateTimeFormatter.format(csvDataValue.dateTime)
                    val str = "$name,${dateTime},${csvDataValue.voltage},${csvDataValue.relay}"
                    val message1 = str.toByteArray()

                    //Log.i("UdpSender.mockRealData", "$str")

                    // Create a DatagramPacket to send the message
                    val packet1 = DatagramPacket(message1, message1.size, address, port)

                    // Send the packet
                    socket1.send(packet1)

                    delay(interval)
                }

            } catch (e: Exception) {
                Log.e("UdpSender", "mockRealData Error sending UDP packet", e)
            } finally {
                socket1.close()
            }
        }
    }

    fun mockUDPDataSenders(port: Int)
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

                val voltage4 = random.nextInt(0, 10).toFloat()
                val name4 = "ChargerCharts|8.0V|"

                // Create a message to send
                val message1 = "$name1,$dateTime,$voltage1,${random.nextInt(0,2)}".toByteArray()
                val message2 = "$name2,$dateTime,$voltage2,${random.nextInt(0,2)}".toByteArray()
                val message3 = "$name3,$dateTime,$voltage3,${random.nextInt(0,2)}".toByteArray()
                val message4 = "$name4,$dateTime,$voltage4,${random.nextInt(0,2)}".toByteArray()

                // Create a DatagramPacket to send the message
                val packet1 = DatagramPacket(message1, message1.size, address, port)
                val packet2 = DatagramPacket(message2, message2.size, address, port)
                val packet3 = DatagramPacket(message3, message3.size, address, port)
                val packet4 = DatagramPacket(message4, message4.size, address, port)

                // Send the packet
                socket1.send(packet1)
                socket2.send(packet2)

                socket1.send(packet3)
                socket2.send(packet4)

            }catch (e: Exception){
                Log.e("UdpSender", "mockUDPDataSenders Error sending UDP packet", e)
            }
            finally {
                socket1.close()
                socket2.close()
            }
        }
    }
}