package com.example.chargercharts2.utils

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.PowerManager
import androidx.fragment.app.Fragment
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import com.example.chargercharts2.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import android.provider.Settings
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job

enum class Actions {
    START,
    STOP
}

enum class ServiceState {
    STARTED,
    STOPPED,
}

private const val name = "SPYSERVICE_KEY"
private const val key = "SPYSERVICE_STATE"

fun setServiceState(context: Context?, state: ServiceState) {
    val sharedPrefs = getPreferences(context)
    sharedPrefs?.edit().let {
        it?.putString(key, state.name)
        it?.apply()
    }
}

fun getServiceState(context: Context?): ServiceState {
    val sharedPrefs = getPreferences(context)
    val value = sharedPrefs?.getString(key, ServiceState.STOPPED.name)
    return ServiceState.valueOf(value.toString())
}

private fun getPreferences(context: Context?): SharedPreferences? {
    return context?.getSharedPreferences(name, 0)
}

fun Fragment.actionOnEndlessService(intent: Intent?, action: Actions) {
    if (getServiceState(this.context) == ServiceState.STOPPED && action == Actions.STOP) return
    (intent ?: Intent(this.context, EndlessNetworkService::class.java)).also {
        it.action = action.name
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            log("Starting the service in >=26 Mode")
            this.context?.startForegroundService(it)
            return
        }
        log("Starting the service in < 26 Mode")
        this.context?.startService(it)
    }
}

fun Fragment.toggleEndlessService(intent: Intent? = null): Intent? {
    val action = if (getServiceState(this.context) == ServiceState.STOPPED) Actions.START else Actions.STOP
    return (intent ?: Intent(this.context, EndlessNetworkService::class.java)).also {
        it.action = action.name
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            log("Starting the service in >=26 Mode")
            this.context?.startForegroundService(it)
            return it
        }
        log("Starting the service in < 26 Mode")
        this.context?.startService(it)
        return it
    }
}

private fun log(message: String){
    Log.i("EndlessService", message)
}

abstract class EndlessNetworkService(
    val useFakePing: Boolean = true,
    val notificationId: Int = 1,
    val notificationChannelName: String = "Endless Service notifications channel",
    val notificationChannelDesc: String = "Endless Service channel",
    val notificationChannelId: String = "ENDLESS SERVICE CHANNEL",
    var notificationContentTitle: String = "Endless Service",
    var notificationContentText: String = "This is your favorite endless service working") : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    protected var isServiceStarted = false

    abstract fun startWork():Job?
    abstract fun stopWork()
    abstract fun onStart(intent: Intent?)

    override fun onBind(intent: Intent): IBinder? {
        log("Some component want to bind with the service")
        // We don't provide binding, so return null
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        log("onStartCommand executed with startId: $startId")
        onStart(intent)
        if (intent != null) {
            val action = intent.action
            log("using an intent with action $action")
            when (action) {
                Actions.START.name -> startService()
                Actions.STOP.name -> stopService()
                else -> log("This should never happen. No action in the received intent")
            }
        } else {
            log(
                "with a null intent. It has been probably restarted by the system."
            )
        }
        // by returning this we make sure the service is restarted if the system kills the service
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        log("The service has been created")
        var notification = createNotification()
        startForeground(notificationId, notification)
        setServiceState(this, ServiceState.STOPPED)
    }

    override fun onDestroy() {
        super.onDestroy()
        log("The service has been destroyed")
        stopService()
        //Toast.makeText(this, "Service destroyed", Toast.LENGTH_SHORT).show()
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        log("Task removed")
        val restartServiceIntent = Intent(applicationContext, EndlessNetworkService::class.java).also {
            it.setPackage(packageName)
        };
        val restartServicePendingIntent: PendingIntent = PendingIntent.getService(this, 1, restartServiceIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE);
        applicationContext.getSystemService(Context.ALARM_SERVICE);
        val alarmService: AlarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager;
        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, restartServicePendingIntent);
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun startService() {
        if (isServiceStarted) return
        log("Starting the foreground service task")
        //Toast.makeText(this, "Service starting its task", Toast.LENGTH_SHORT).show()
        isServiceStarted = true
        setServiceState(this, ServiceState.STARTED)

        // we need this lock so our service gets not affected by Doze Mode
        wakeLock =
            (getSystemService(POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EndlessService::lock").apply {
                    acquire()
                }
            }

        // we're starting a loop in a coroutine
        GlobalScope.launch(Dispatchers.IO) {
            while (isServiceStarted) {
                if(useFakePing) {
                    launch(Dispatchers.IO) {
                        while (isServiceStarted) {
                            pingFakeServer()
                            delay(1 * 30 * 1000)
                        }
                    }
                }
                startWork()?.join()
            }
            log("End of the loop for the service")
        }
    }

    protected fun stopService() {
        log("Stopping the foreground service")
        //Toast.makeText(this, "Service stopping", Toast.LENGTH_SHORT).show()
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopForeground(Service.STOP_FOREGROUND_REMOVE)
            stopSelf()
        } catch (e: Exception) {
            log("Service stopped without being started: ${e.message}")
        }

        isServiceStarted = false
        stopWork()
        setServiceState(this, ServiceState.STOPPED)
    }

    private fun pingFakeServer() {
        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.mmmZ")
        val gmtTime = df.format(Date())

        val deviceId = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)

        val json =
            """
                {
                    "deviceId": "$deviceId",
                    "createdAt": "$gmtTime"
                }
            """
        try {
            Fuel.post("https://jsonplaceholder.typicode.com/posts")
                .jsonBody(json)
                .response { _, _, result ->
                    val (bytes, error) = result
                    if (bytes != null) {
                        log("[response bytes] ${String(bytes)}")
                    } else {
                        log("[response error] ${error?.message}")
                    }
                }
        } catch (e: Exception) {
            log("Error making the request: ${e.message}")
        }
    }

    open fun createNotification(): Notification {
        // depending on the Android API that we're dealing with we will have
        // to use a specific method to create the notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager;
            val channel = NotificationChannel(
                notificationChannelId,
                notificationChannelName,
                NotificationManager.IMPORTANCE_HIGH
            ).let {
                it.description = notificationChannelDesc
                it.enableLights(true)
                it.lightColor = Color.RED
                it.enableVibration(true)
                it.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
                it
            }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        }

        val builder: Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, notificationChannelId)
        else Notification.Builder(this)

        return builder
            .setContentTitle(notificationContentTitle)
            .setContentText(notificationContentText)
            .setContentIntent(pendingIntent)
            //.setSmallIcon(android.R.drawable.ic_menu_info_details)
            //.setTicker("Ticker text")
            .build()
    }
}