package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.Rider
import com.example.repository.RiderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ScreenState {
    object Login : ScreenState()
    data class Tracking(val rider: Rider) : ScreenState()
}

data class LoginUiState(
    val phone: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isConfigured: Boolean = false,
    val configuredUrl: String = "",
    val showConfigDialog: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RiderRepository()

    private val _screenState = MutableStateFlow<ScreenState>(ScreenState.Login)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    val trackingStatus: StateFlow<TrackingStatus> = TrackingStateManager.status

    private var loggedInRider: Rider? = null

    init {
        SupabaseManager.init(application)
        checkConfiguration()
    }

    fun checkConfiguration() {
        val configured = SupabaseManager.isConfigured()
        _loginState.value = _loginState.value.copy(
            isConfigured = configured,
            configuredUrl = SupabaseManager.configuredUrl
        )
    }

    fun onPhoneChanged(newPhone: String) {
        _loginState.value = _loginState.value.copy(
            phone = newPhone,
            errorMessage = null
        )
    }

    fun onPasswordChanged(newPassword: String) {
        _loginState.value = _loginState.value.copy(
            password = newPassword,
            errorMessage = null
        )
    }

    fun showConfigDialog(show: Boolean) {
        _loginState.value = _loginState.value.copy(showConfigDialog = show)
    }

    fun saveSupabaseConfig(url: String, key: String) {
        val success = SupabaseManager.updateCredentials(getApplication(), url, key)
        checkConfiguration()
        if (success) {
            _loginState.value = _loginState.value.copy(
                showConfigDialog = false,
                errorMessage = null
            )
        } else {
            _loginState.value = _loginState.value.copy(
                errorMessage = "Invalid Supabase URL or Key format."
            )
        }
    }

    fun login() {
        val phone = _loginState.value.phone.trim()
        val password = _loginState.value.password.trim()

        if (phone.isBlank() || password.isBlank()) {
            _loginState.value = _loginState.value.copy(
                errorMessage = "Please enter both phone and password"
            )
            return
        }

        if (!SupabaseManager.isConfigured()) {
            _loginState.value = _loginState.value.copy(
                errorMessage = "Supabase is not configured yet. Tap the Settings icon to enter your Supabase URL & Key.",
                showConfigDialog = true
            )
            return
        }

        _loginState.value = _loginState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val result = repository.login(phone, password)
            result.fold(
                onSuccess = { rider ->
                    loggedInRider = rider
                    _loginState.value = _loginState.value.copy(isLoading = false, errorMessage = null)
                    _screenState.value = ScreenState.Tracking(rider)
                },
                onFailure = { error ->
                    _loginState.value = _loginState.value.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Login failed. Please check credentials."
                    )
                }
            )
        }
    }

    fun setTrackingEnabled(enabled: Boolean, context: Context) {
        val rider = loggedInRider ?: return
        if (enabled) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = LocationTrackingService.ACTION_START
                putExtra(LocationTrackingService.EXTRA_PHONE, rider.phone)
            }
            ContextCompat.startForegroundService(context, intent)
        } else {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = LocationTrackingService.ACTION_STOP
            }
            context.startService(intent)
        }
    }

    fun logout(context: Context) {
        setTrackingEnabled(false, context)
        TrackingStateManager.reset()
        loggedInRider = null
        _screenState.value = ScreenState.Login
        _loginState.value = _loginState.value.copy(
            password = "",
            errorMessage = null
        )
    }
}
