package com.example.mobileapp.worker

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.work.*
import com.example.mobileapp.db.MonitorDatabase
import com.example.mobileapp.model.MonitorLog
import com.example.mobileapp.service.MonitoringService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.StringWriter
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    // OkHttpClient cho Worker
    private val client = OkHttpClient.Builder()
        .connectTimeout(MonitoringService.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(MonitoringService.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(MonitoringService.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = MonitorDatabase.getDatabase(applicationContext)
            val unsyncedLogs = db.monitorLogDao().getUnsyncedLogs()
            
            if (unsyncedLogs.isEmpty()) {
                Log.d("SyncWorker", "Không có log nào cần đồng bộ")
                return@withContext Result.success()
            }
            
            Log.d("SyncWorker", "Đang đồng bộ ${unsyncedLogs.size} logs")

            var successCount = 0
            var failCount = 0

            for (log in unsyncedLogs) {
                // Tạo XML document
                val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                val doc = docBuilder.newDocument()
                
                // Tạo element root
                val rootElement = doc.createElement("monitorLog")
                doc.appendChild(rootElement)
                
                // Thêm các thông tin
                addXmlElement(doc, rootElement, "type", log.type)
                addXmlElement(doc, rootElement, "phone", log.phone)
                addXmlElement(doc, rootElement, "content", log.content)
                addXmlElement(doc, rootElement, "timestamp", log.timestamp.toString())
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
                    .url(MonitoringService.API_URL)
                    .post(body)
                    .header("Content-Type", "application/xml")
                    .header("Device-Id", getDeviceUniqueId())
                    .build()

                try {
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        // cập nhật trạng thái synced
                        db.monitorLogDao().updateLog(log.copy(synced = true))
                        Log.d("SyncWorker", "Đồng bộ XML thành công log ID: ${log.id}")
                        successCount++
                    } else {
                        failCount++
                        Log.w("SyncWorker", "Gửi XML không thành công cho log ID: ${log.id}, lỗi: ${response.code}")
                    }
                } catch (e: Exception) {
                    failCount++
                    Log.e("SyncWorker", "Lỗi khi gửi log ID: ${log.id}: ${e.message}")
                }
            }

            Log.d("SyncWorker", "Kết quả đồng bộ: Thành công=$successCount, Thất bại=$failCount")
            
            // Chỉ thành công khi tất cả đều được đồng bộ
            if (failCount > 0) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Lỗi đồng bộ XML: ${e.message}")
            Result.retry()
        }
    }
    
    /**
     * Lấy device ID duy nhất
     */
    private fun getDeviceUniqueId(): String {
        return Settings.Secure.getString(
            applicationContext.contentResolver, 
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"
    }
    
    /**
     * Thêm phần tử XML vào document
     */
    private fun addXmlElement(doc: Document, parent: org.w3c.dom.Element, name: String, value: String) {
        val element = doc.createElement(name)
        element.appendChild(doc.createTextNode(value))
        parent.appendChild(element)
    }
}
