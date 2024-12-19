package com.example.chargercharts2.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log

class NetworkChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        //val activeNetwork = connectivityManager.activeNetwork
        //val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        //val isConnected = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        //Log.i("NetworkChangeReceiver", "Network is ${if (isConnected) "available" else "unavailable"}")
    }
}