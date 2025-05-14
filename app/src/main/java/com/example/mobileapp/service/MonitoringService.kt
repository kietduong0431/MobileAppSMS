package com.example.mobileapp.service

import android.annotation.TargetApi
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import com.example.mobileapp.db.MonitorDatabase
import com.example.mobileapp.model.MonitorLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.StringWriter
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class MonitoringService : Service() {

    companion object {
        // TODO: Thay URL API thật ở đây
        const val API_URL = "https://api.example.com/monitor"
        // Thời gian timeout API (30 giây)
        const val API_TIMEOUT_SECONDS = 30L
        
        fun buildCallIntent(
            context: Context,
            phone: String,
            callType: String,
            timestamp: Long,
            duration: Long
        ): Intent {
            return Intent(context, MonitoringService::class.java).apply {
                putExtra("type", "call")
                putExtra("phone", phone)
                putExtra("callType", callType)
                putExtra("timestamp", timestamp)
                putExtra("duration", duration)
            }
        }
        
        fun buildSmsIntent(
            context: Context,
            phone: String,
            message: String,
            timestamp: Long
        ): Intent {
            return Intent(context, MonitoringService::class.java).apply {
                putExtra("type", "sms")
                putExtra("phone", phone)
                putExtra("message", message)
                putExtra("timestamp", timestamp)
            }
        }
    }
    
    // Tạo một instance của OkHttpClient cho toàn bộ service
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (val type = it.getStringExtra("type")) {
                "sms" -> {
                    val phone = it.getStringExtra("phone") ?: ""
                    val message = it.getStringExtra("message") ?: ""
                    val timestamp = it.getLongExtra("timestamp", 0)

                    sendToApiAndSave(type, phone, message, timestamp)
                }

                "call" -> {
                    val phone = it.getStringExtra("phone") ?: ""
                    val callType = it.getStringExtra("callType") ?: ""
                    val timestamp = it.getLongExtra("timestamp", 0)
                    val duration = it.getLongExtra("duration", 0)

                    val content = "$callType - $duration seconds"
                    sendToApiAndSave(type, phone, content, timestamp)
                }

                else -> Log.w("MonitoringService", "Loại dữ liệu không hỗ trợ: $type")
            }
        }
        return START_NOT_STICKY
    }

    @TargetApi(Build.VERSION_CODES.DONUT)
    private fun sendToApiAndSave(type: String, phone: String, content: String, timestamp: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            var synced = false

            try {
                // Tạo XML document
                val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                val doc = docBuilder.newDocument()
                
                // Tạo element root
                val rootElement = doc.createElement("monitorLog")
                doc.appendChild(rootElement)
                
                // Thêm các thông tin
                addXmlElement(doc, rootElement, "type", type)
                addXmlElement(doc, rootElement, "phone", phone)
                addXmlElement(doc, rootElement, "content", content)
                addXmlElement(doc, rootElement, "timestamp", timestamp.toString())
                addXmlElement(doc, rootElement, "deviceId", getDeviceUniqueId())
                addXmlElement(doc, rootElement, "deviceModel", "${Build.MANUFACTURER} ${Build.MODEL}")
                addXmlElement(doc, rootElement, "androidVersion", Build.VERSION.RELEASE)
                
                // Chuyển đổi XML document thành string
                val transformerFactory = TransformerFactory.newInstance()
                val transformer = transformerFactory.newTransformer()
                val source = DOMSource(doc)
                val writer = StringWriter()
                val result = StreamResult(writer)
                transformer.transform(source, result)
                
                val xmlString = writer.toString()
                
                // Gửi dữ liệu XML lên server
                val body = xmlString.toRequestBody("application/xml".toMediaType())

                val request = Request.Builder()
                    .url(API_URL)
                    .post(body)
                    .header("Content-Type", "application/xml")
                    .header("Device-Id", getDeviceUniqueId())
                    .build()

                val response = client.newCall(request).execute()

                synced = response.isSuccessful
                Log.d("MonitoringService", "API XML response: ${response.code} (${if(synced) "success" else "failed"})")
            } catch (e: Exception) {
                Log.e("MonitoringService", "Gửi API XML lỗi: ${e.message}")
            }

            // Luôn lưu log vào Room để hiển thị trong UI
            saveToRoom(type, phone, content, timestamp, synced)
        }
    }
    
    /**
     * Lấy device ID duy nhất
     */
    private fun getDeviceUniqueId(): String {
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }
    
    /**
     * Thêm phần tử XML vào document
     */
    private fun addXmlElement(doc: org.w3c.dom.Document, parent: org.w3c.dom.Element, name: String, value: String) {
        val element = doc.createElement(name)
        element.appendChild(doc.createTextNode(value))
        parent.appendChild(element)
    }

    private fun saveToRoom(type: String, phone: String, content: String, timestamp: Long, synced: Boolean) {
        val db = MonitorDatabase.getDatabase(applicationContext)
        val log = MonitorLog(
            type = type,
            phone = phone,
            content = content,
            timestamp = timestamp,
            synced = synced
        )

        CoroutineScope(Dispatchers.IO).launch {
            db.monitorLogDao().insertLog(log)
            Log.d("MonitoringService", "Dữ liệu được lưu vào Room. synced=$synced")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
