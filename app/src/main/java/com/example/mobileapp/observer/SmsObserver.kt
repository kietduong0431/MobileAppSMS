package com.example.mobileapp.observer

import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.provider.Telephony
import android.util.Log
import com.example.mobileapp.service.MonitoringService
import java.util.concurrent.ConcurrentHashMap

class SmsObserver(private val context: Context, handler: Handler) :
    ContentObserver(handler) {

    // URI for sent messages
    private val SMS_SENT_URI = Uri.parse("content://sms/sent")

    // Lưu trữ các tin nhắn đã xử lý để tránh xử lý trùng lặp
    private val processedMessages = ConcurrentHashMap<String, Long>()
    
    // Biến để ngăn xử lý quá nhiều sự kiện trong thời gian ngắn
    private var lastProcessedTime = 0L
    private val THROTTLE_TIME_MS = 500 // 500ms để lọc sự kiện

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        
        // Kiểm tra thời gian để giảm số lần xử lý
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastProcessedTime < THROTTLE_TIME_MS) {
            // Bỏ qua sự kiện nếu quá gần sự kiện trước đó
            return
        }
        lastProcessedTime = currentTime
        
        Log.d("SmsObserver", "Phát hiện thay đổi SMS đã gửi")

        val cursor = context.contentResolver.query(
            SMS_SENT_URI,
            null,
            null,
            null,
            "${Telephony.Sms._ID} DESC" // Sắp xếp theo ID giảm dần (mới nhất đầu tiên)
        )

        cursor?.use {
            // Chỉ xử lý tối đa 5 tin nhắn gần nhất
            var count = 0
            if (it.moveToFirst()) {
                do {
                    val messageId = it.getString(it.getColumnIndexOrThrow(Telephony.Sms._ID))
                    val address = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
                    val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
                    val date = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
                    
                    // Tạo khóa duy nhất cho tin nhắn này
                    val messageKey = "$messageId-$date"

                    // Chỉ xử lý nếu chưa từng xử lý tin nhắn này
                    if (!processedMessages.containsKey(messageKey)) {
                        // Lưu lại tin nhắn này vào danh sách đã xử lý
                        processedMessages[messageKey] = date
                        
                        // Loại bỏ các tin nhắn cũ khỏi bộ nhớ nếu quá nhiều
                        if (processedMessages.size > 50) {
                            val oldestEntries = processedMessages.entries
                                .sortedBy { it.value }
                                .take(10)
                                
                            oldestEntries.forEach { processedMessages.remove(it.key) }
                        }

                        Log.d("SmsObserver", "Xử lý tin nhắn đã gửi | SĐT: $address | Nội dung: $body")

                        // Gửi dữ liệu đến service để xử lý
                        val serviceIntent = MonitoringService.buildSmsIntent(
                            context, address, body, date
                        )
                        context.startService(serviceIntent)
                    }
                    
                    count++
                    // Chỉ xử lý tối đa 5 bản ghi
                    if (count >= 5) break
                    
                } while (it.moveToNext())
            }
        }
    }
} 