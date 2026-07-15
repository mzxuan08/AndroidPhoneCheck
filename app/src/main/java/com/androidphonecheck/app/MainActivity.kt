package com.androidphonecheck.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.androidphonecheck.app.diagnostic.DeviceInfoCollector
import com.androidphonecheck.app.ui.AndroidPhoneCheckApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val deviceInfo = DeviceInfoCollector(applicationContext).collect()
        setContent {
            AndroidPhoneCheckApp(deviceInfo = deviceInfo)
        }
    }
}

