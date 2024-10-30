package com.example.chargercharts2.ui.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.chargercharts2.utils.*

class NotificationsViewModel : ViewModel() {
    val message: LiveData<String> = UdpListener.message
    val messages: LiveData<List<String>> = UdpListener.messages
}