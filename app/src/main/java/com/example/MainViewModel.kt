package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.Rider
import com.example.repository.RiderRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val repository = RiderRepository()

    private val _uiState = MutableStateFlow<UiState>(UiState.Login)
    val uiState: StateFlow<UiState> = _uiState

    private var currentRider: Rider? = null
    private var locationJob: Job? = null

    sealed class UiState {
        object Login : UiState()
        data class Tracking(val rider: Rider) : UiState()
    }

    fun login(phone: String, password: String) {
        viewModelScope.launch {
            val rider = repository.login(phone, password)
            if (rider != null) {
                currentRider = rider
                _uiState.value = UiState.Tracking(rider)
            }
        }
    }

    fun startTracking(lat: Double, lon: Double) {
        locationJob = viewModelScope.launch {
            while (true) {
                currentRider?.let {
                    repository.updateLocation(it.id, lat, lon)
                }
                delay(5000)
            }
        }
    }

    fun stopTracking() {
        locationJob?.cancel()
    }
}
