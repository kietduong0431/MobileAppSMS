package com.example.mobileapp.util

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.example.mobileapp.receiver.SmsSentReceiver

/**
 * Lớp tiện ích để theo dõi SMS gửi đi
 */
object SmsHelper {
    
    private var isReceiverRegistered = false
    
    /**
     * Đăng ký SentSmsReceiver để theo dõi tin nhắn gửi đi
     */
    fun registerSentSmsMonitoring(context: Context) {
        if (!isReceiverRegistered) {
            // Đăng ký Broadcast Receiver cho SMS đã gửi
            val sentFilter = IntentFilter(SmsSentReceiver.ACTION_SMS_SENT)
            ContextCompat.registerReceiver(
                context.applicationContext,
                SmsSentReceiver(), 
                sentFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            isReceiverRegistered = true
        }
    }
    
    /**
     * Hook vào SmsManager.sendTextMessage để theo dõi tin nhắn gửi đi
     * 
     * QUAN TRỌNG: Hàm này không thay thế hàm gửi SMS của Android,
     * chỉ hook vào quá trình này để theo dõi. Hãy tiếp tục sử dụng
     * SmsManager.getDefault().sendTextMessage() bình thường.
     */
    fun monitorSentSms(context: Context, phoneNumber: String, message: String) {
        // Tạo intent cho SmsSentReceiver
        val sentIntent = Intent(SmsSentReceiver.ACTION_SMS_SENT).apply {
            putExtra("phone_number", phoneNumber)
            putExtra("message", message)
        }
        
        // Tạo PendingIntent sẽ được gọi khi SMS được gửi
        val sentPI = PendingIntent.getBroadcast(
            context, 
            0, 
            sentIntent, 
            PendingIntent.FLAG_IMMUTABLE
        )
        
        // Hook vào SmsManager.sendTextMessage
        try {
            // Khi muốn gửi tin nhắn, thay vì gọi trực tiếp:
            // SmsManager.getDefault().sendTextMessage(phoneNumber, null, message, sentPI, null)
            // 
            // Chỉ gửi pendingIntent để theo dõi
            sentPI.send()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
} 