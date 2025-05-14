package com.example.mobileapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log
import com.example.mobileapp.service.MonitoringService

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
            val bundle: Bundle? = intent.extras
            if (bundle != null) {
                try {
                    val pdus = bundle.get("pdus") as Array<*>
                    for (pdu in pdus) {
                        val format = bundle.getString("format")
                        val sms = SmsMessage.createFromPdu(pdu as ByteArray, format)
                        val phoneNumber = sms.originatingAddress
                        val message = sms.messageBody
                        val timestamp = sms.timestampMillis

                        Log.d("SmsReceiver", "SMS từ: $phoneNumber - Nội dung: $message")

                        // Gửi dữ liệu đến service để xử lý
                        val serviceIntent = Intent(context, MonitoringService::class.java).apply {
                            putExtra("type", "sms")
                            putExtra("phone", phoneNumber)
                            putExtra("message", message)
                            putExtra("timestamp", timestamp)
                        }

                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "Lỗi đọc SMS", e)
                }
            }
        }
    }
}
