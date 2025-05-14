package com.example.mobileapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobileapp.db.MonitorDatabase
import com.example.mobileapp.model.MonitorLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LogViewModel(application: Application) : AndroidViewModel(application) {

    private val _logs = MutableStateFlow<List<MonitorLog>>(emptyList())
    val logs: StateFlow<List<MonitorLog>> = _logs

    private val dao = MonitorDatabase.getDatabase(application).monitorLogDao()

    init {
        loadLogs()
    }

    private fun loadLogs() {
        viewModelScope.launch {
            _logs.value = dao.getAllLogs()
        }
    }

    fun reloadLogs() {
        loadLogs()
    }
}
