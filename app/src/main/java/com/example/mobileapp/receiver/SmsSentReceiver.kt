package com.example.mobileapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import com.example.mobileapp.service.MonitoringService

class SmsSentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Khi BroadcastReceiver nhận được intent SMS_SENT
        if (intent.action == "SMS_SENT") {
            // Lấy dữ liệu tin nhắn từ intent
            val phoneNumber = intent.getStringExtra("phone_number") ?: ""
            val message = intent.getStringExtra("message") ?: ""
            val timestamp = System.currentTimeMillis()

            Log.d("SmsSentReceiver", "SMS gửi đi | SĐT: $phoneNumber | Nội dung: $message")

            // Gửi dữ liệu đến service để xử lý
            val serviceIntent = MonitoringService.buildSmsIntent(
                context, phoneNumber, message, timestamp
            )
            context.startService(serviceIntent)
        }
    }

    companion object {
        const val ACTION_SMS_SENT = "SMS_SENT"
    }
} 