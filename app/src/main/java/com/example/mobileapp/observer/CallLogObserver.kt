package com.example.mobileapp.observer

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.provider.CallLog
import android.util.Log
import com.example.mobileapp.service.MonitoringService
import java.util.concurrent.ConcurrentHashMap

class CallLogObserver(private val context: Context, handler: Handler) :
    ContentObserver(handler) {

    // Lưu trữ các cuộc gọi đã xử lý để tránh xử lý trùng lặp
    private val processedCalls = ConcurrentHashMap<String, Long>()
    
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
        
        Log.d("CallLogObserver", "Phát hiện thay đổi CallLog")

        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            null,
            null,
            null,
            "${CallLog.Calls.DATE} DESC" // Sắp xếp theo thời gian giảm dần
        )

        cursor?.use {
            // Chỉ xử lý tối đa 5 cuộc gọi gần nhất
            var count = 0
            if (it.moveToFirst()) {
                do {
                    val number = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER))
                    val type = it.getInt(it.getColumnIndexOrThrow(CallLog.Calls.TYPE))
                    val date = it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DATE))
                    val duration = it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DURATION))
                    
                    // Tạo khóa duy nhất cho cuộc gọi này
                    val callKey = "$number-$date-$type"

                    // Chỉ xử lý nếu chưa từng xử lý cuộc gọi này
                    if (!processedCalls.containsKey(callKey)) {
                        // Lưu lại cuộc gọi này vào danh sách đã xử lý
                        processedCalls[callKey] = date
                        
                        // Loại bỏ các cuộc gọi cũ khỏi bộ nhớ nếu quá nhiều
                        if (processedCalls.size > 50) {
                            val oldestEntries = processedCalls.entries
                                .sortedBy { it.value }
                                .take(10)
                                
                            oldestEntries.forEach { processedCalls.remove(it.key) }
                        }

                        val callType = when (type) {
                            CallLog.Calls.INCOMING_TYPE -> {
                                if (duration > 0) "incoming_answered"
                                else "incoming_missed"
                            }

                            CallLog.Calls.OUTGOING_TYPE -> {
                                if (duration > 0) "outgoing_answered"
                                else "outgoing_not_answered"
                            }

                            CallLog.Calls.MISSED_TYPE -> "missed"
                            CallLog.Calls.REJECTED_TYPE -> "rejected"
                            else -> "other"
                        }

                        Log.d("CallLogObserver", "Xử lý cuộc gọi $callType | SĐT: $number | Thời lượng: $duration")

                        val intent = MonitoringService.buildCallIntent(
                            context, number, callType, date, duration
                        )
                        context.startService(intent)
                    }
                    
                    count++
                    // Chỉ xử lý tối đa 5 bản ghi
                    if (count >= 5) break
                    
                } while (it.moveToNext())
            }
        }
    }
}
