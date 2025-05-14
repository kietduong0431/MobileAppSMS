package com.example.mobileapp.db

import androidx.room.*
import com.example.mobileapp.model.MonitorLog

@Dao
interface MonitorLogDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLog(log: MonitorLog)

    @Query("SELECT * FROM monitor_logs WHERE synced = 0")
    suspend fun getUnsyncedLogs(): List<MonitorLog>

    @Update
    suspend fun updateLog(log: MonitorLog)

    @Delete
    suspend fun deleteLog(log: MonitorLog)

    @Query("SELECT * FROM monitor_logs ORDER BY timestamp DESC")
    suspend fun getAllLogs(): List<MonitorLog>
    
    @Query("SELECT COUNT(*) FROM monitor_logs")
    suspend fun getLogCount(): Int
    
    @Query("SELECT * FROM monitor_logs ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getOldestLogs(limit: Int): List<MonitorLog>
    
    @Query("DELETE FROM monitor_logs WHERE id IN (:logIds)")
    suspend fun deleteLogs(logIds: List<Int>)
    
    @Query("DELETE FROM monitor_logs WHERE synced = 1 AND timestamp < :olderThanTimestamp")
    suspend fun deleteOldSyncedLogs(olderThanTimestamp: Long): Int
    
    @Query("DELETE FROM monitor_logs WHERE timestamp < :olderThanTimestamp")
    suspend fun deleteAllOldLogs(olderThanTimestamp: Long): Int
    
    /**
     * Kiểm soát số lượng logs trong database
     * @param maxRecords Số lượng bản ghi tối đa
     * @param deleteOnlySynced Chỉ xóa các bản ghi đã được đồng bộ
     * @return Số lượng bản ghi đã xóa
     */
    suspend fun limitDatabaseSize(maxRecords: Int, deleteOnlySynced: Boolean = true): Int {
        val currentCount = getLogCount()
        
        if (currentCount <= maxRecords) {
            return 0 // Không cần xóa
        }
        
        val toDelete = currentCount - maxRecords
        
        if (deleteOnlySynced) {
            val oldestSynced = getOldestLogs(toDelete * 2)
                .filter { it.synced }
                .take(toDelete)
                
            if (oldestSynced.isNotEmpty()) {
                deleteLogs(oldestSynced.map { it.id })
                return oldestSynced.size
            }
            return 0
        } else {
            val oldestLogs = getOldestLogs(toDelete)
            deleteLogs(oldestLogs.map { it.id })
            return oldestLogs.size
        }
    }
    
    /**
     * Xóa logs cũ hơn số ngày nhất định
     * @param days Số ngày (logs cũ hơn số ngày này sẽ bị xóa)
     * @param onlySynced Chỉ xóa các bản ghi đã đồng bộ
     * @return Số lượng bản ghi đã xóa
     */
    suspend fun deleteLogsOlderThan(days: Int, onlySynced: Boolean = true): Int {
        val cutoffTime = System.currentTimeMillis() - (days * 86400000L) // days to milliseconds
        
        return if (onlySynced) {
            deleteOldSyncedLogs(cutoffTime)
        } else {
            deleteAllOldLogs(cutoffTime)
        }
    }
}
