package com.example.mobileapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monitor_logs")
data class MonitorLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String,        // "sms" hoặc "call"
    val phone: String,
    val content: String,
    val timestamp: Long,
    val synced: Boolean = false
)
