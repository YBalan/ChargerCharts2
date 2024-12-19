package com.example.chargercharts2.utils
import android.content.Intent
import androidx.fragment.app.Fragment
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.Job

fun Fragment.toggleUdpService(port: Int, limit: Int, showErrors: Boolean) : Intent? {
    return toggleEndlessService(Intent(this.context, UdpListenerService::class.java).apply {
        putExtra(UdpListenerService.EXTRA_PORT, port)
        putExtra(UdpListenerService.EXTRA_DATA_LIMIT, limit)
        putExtra(UdpListenerService.EXTRA_SHOW_ERRORS, showErrors)
    })
}

fun Fragment.stopUdpListener(intent: Intent?){
    actionOnEndlessService(intent ?: Intent(this.context, UdpListenerService::class.java), Actions.STOP)
}

class UdpListenerService : EndlessNetworkService(
    useFakePing = true,
    notificationId = 1,
    notificationChannelId = "ChargerChartsUdpListenerID",
    notificationChannelName = "Notification Chanel Name",
    notificationChannelDesc = "Notification Chanel Desc",
    notificationContentTitle = "UDP Listener is running",
    notificationContentText = "Press to stop",
    ) {

    private val isWorking get() = isServiceStarted

    private var _port = UdpListener.DEFAULT_PORT
    private var _limit = UdpListener.DEFAULT_DATA_LIMIT
    private var _showErrors = true

    private var wifiLock: WifiManager.WifiLock? = null

    fun initialize(port: Int, limit: Int, showErrors: Boolean){
        _port = port
        _limit = limit
        _showErrors = showErrors
    }

    companion object {
        const val EXTRA_PORT = "EXTRA_PORT"
        const val EXTRA_DATA_LIMIT = "EXTRA_DATA_LIMIT"
        const val EXTRA_SHOW_ERRORS = "EXTRA_SHOW_ERRORS"
    }

    override fun onStart(intent: Intent?) {
        Log.i("UdpListenerService", "onStartCommand - Start")
        val port = intent?.getIntExtra(EXTRA_PORT, _port) ?: _port
        val limit = intent?.getIntExtra(EXTRA_DATA_LIMIT, _limit) ?: _limit
        val showErrors = intent?.getBooleanExtra(EXTRA_SHOW_ERRORS, true) == true

        initialize(port, limit, showErrors)
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        stopService()
    }

    override fun startWork(): Job? {
        Log.i("UdpListenerService", "startWork")
        acquireWifiLock()
        return UdpListener.run(_port, _limit, _showErrors)
    }

    override fun stopWork() {
        Log.i("UdpListenerService", "stopWork")
        releaseWifiLock()
        UdpListener.stopListening()
    }

    private fun acquireWifiLock() {
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "UdpListenerService::WifiLock")
        wifiLock?.acquire()
    }

    private fun releaseWifiLock() {
        wifiLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wifiLock = null
    }
}
