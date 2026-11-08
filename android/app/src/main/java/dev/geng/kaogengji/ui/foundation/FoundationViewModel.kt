package dev.geng.kaogengji.ui.foundation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.geng.kaogengji.application.diagnostics.BackendHealth
import dev.geng.kaogengji.application.diagnostics.CheckBackendConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ConnectionState { NOT_CHECKED, CHECKING, REACHABLE, UNREACHABLE }

class FoundationViewModel(private val checkBackend: CheckBackendConnection) : ViewModel() {
    private val mutableState = MutableStateFlow(ConnectionState.NOT_CHECKED)
    val state = mutableState.asStateFlow()

    fun checkConnection() {
        if (mutableState.value == ConnectionState.CHECKING) return
        mutableState.value = ConnectionState.CHECKING
        viewModelScope.launch {
            mutableState.value = if (checkBackend() == BackendHealth.REACHABLE) ConnectionState.REACHABLE else ConnectionState.UNREACHABLE
        }
    }

    class Factory(private val useCase: CheckBackendConnection) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass == FoundationViewModel::class.java)
            @Suppress("UNCHECKED_CAST")
            return FoundationViewModel(useCase) as T
        }
    }
}
