package com.example.mobileapp.db

import android.content.Context
import android.util.Log
import androidx.work.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Quản lý việc dọn dẹp dữ liệu định kỳ
 */
class DataCleanupManager(private val context: Context) {

    companion object {
        const val WORKER_NAME = "data_cleanup_worker"
        
        // Các giá trị mặc định
        const val DEFAULT_MAX_RECORDS = 1000 // Số bản ghi tối đa
        const val DEFAULT_RETENTION_DAYS = 30 // Lưu trữ tối đa 30 ngày
        const val DEFAULT_CLEANUP_INTERVAL_HOURS = 24 // Dọn dẹp mỗi 24 giờ
    }

    /**
     * Thực hiện dọn dẹp ngay lập tức
     * @param maxRecords Số bản ghi tối đa được giữ lại
     * @param retentionDays Số ngày tối đa giữ lại dữ liệu
     * @param onlySynced Chỉ xóa dữ liệu đã được đồng bộ
     */
    fun cleanupNow(
        maxRecords: Int = DEFAULT_MAX_RECORDS,
        retentionDays: Int = DEFAULT_RETENTION_DAYS,
        onlySynced: Boolean = true
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = MonitorDatabase.getDatabase(context)
                val dao = db.monitorLogDao()
                
                // Giới hạn số lượng bản ghi
                val recordsDeleted = dao.limitDatabaseSize(maxRecords, onlySynced)
                
                // Xóa logs cũ
                val oldLogsDeleted = dao.deleteLogsOlderThan(retentionDays, onlySynced)
                
                Log.d("DataCleanupManager", "Đã xóa $recordsDeleted bản ghi để giới hạn kích thước DB")
                Log.d("DataCleanupManager", "Đã xóa $oldLogsDeleted bản ghi quá $retentionDays ngày")
            } catch (e: Exception) {
                Log.e("DataCleanupManager", "Lỗi khi dọn dẹp dữ liệu: ${e.message}")
            }
        }
    }

    /**
     * Lập lịch dọn dẹp dữ liệu tự động
     * @param intervalHours Thời gian giữa các lần dọn dẹp (tính bằng giờ)
     * @param maxRecords Số bản ghi tối đa được giữ lại
     * @param retentionDays Số ngày tối đa giữ lại dữ liệu
     * @param onlySynced Chỉ xóa dữ liệu đã được đồng bộ
     */
    fun schedulePeriodicCleanup(
        intervalHours: Int = DEFAULT_CLEANUP_INTERVAL_HOURS,
        maxRecords: Int = DEFAULT_MAX_RECORDS,
        retentionDays: Int = DEFAULT_RETENTION_DAYS,
        onlySynced: Boolean = true
    ) {
        val inputData = workDataOf(
            "max_records" to maxRecords,
            "retention_days" to retentionDays,
            "only_synced" to onlySynced
        )

        val cleanupRequest = PeriodicWorkRequestBuilder<DataCleanupWorker>(
            intervalHours.toLong(), TimeUnit.HOURS
        )
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORKER_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Giữ worker cũ nếu đã được lập lịch
            cleanupRequest
        )
        
        Log.d("DataCleanupManager", "Đã lập lịch dọn dẹp mỗi $intervalHours giờ, giới hạn $maxRecords bản ghi, giữ lại $retentionDays ngày")
    }

    /**
     * Hủy lịch dọn dẹp tự động
     */
    fun cancelPeriodicCleanup() {
        WorkManager.getInstance(context).cancelUniqueWork(WORKER_NAME)
        Log.d("DataCleanupManager", "Đã hủy lịch dọn dẹp tự động")
    }
}

/**
 * Worker để thực hiện dọn dẹp dữ liệu
 */
class DataCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            val maxRecords = inputData.getInt("max_records", DataCleanupManager.DEFAULT_MAX_RECORDS)
            val retentionDays = inputData.getInt("retention_days", DataCleanupManager.DEFAULT_RETENTION_DAYS)
            val onlySynced = inputData.getBoolean("only_synced", true)

            val db = MonitorDatabase.getDatabase(applicationContext)
            val dao = db.monitorLogDao()
            
            // Giới hạn số lượng bản ghi
            val recordsDeleted = dao.limitDatabaseSize(maxRecords, onlySynced)
            
            // Xóa logs cũ
            val oldLogsDeleted = dao.deleteLogsOlderThan(retentionDays, onlySynced)
            
            Log.d("DataCleanupWorker", "Đã xóa $recordsDeleted bản ghi để giới hạn kích thước DB")
            Log.d("DataCleanupWorker", "Đã xóa $oldLogsDeleted bản ghi quá $retentionDays ngày")
            
            return Result.success()
        } catch (e: Exception) {
            Log.e("DataCleanupWorker", "Lỗi khi dọn dẹp dữ liệu: ${e.message}")
            return Result.failure()
        }
    }
} 