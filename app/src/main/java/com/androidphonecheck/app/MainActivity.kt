package com.androidphonecheck.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.androidphonecheck.app.diagnostic.AutomaticDiagnostics
import com.androidphonecheck.app.ui.AndroidPhoneCheckApp

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val automaticResults = AutomaticDiagnostics(applicationContext).collect()
        setContent {
            AndroidPhoneCheckApp(automaticResults = automaticResults)
        }
    }
}
