package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.repository.RiderRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TrackingStatus(
    val isTracking: Boolean = false,
    val phone: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val lastUpdateEpoch: Long = 0L,
    val successfulUpdatesCount: Int = 0,
    val lastStatusMessage: String = "Ready"
)

object TrackingStateManager {
    private val _status = MutableStateFlow(TrackingStatus())
    val status: StateFlow<TrackingStatus> = _status.asStateFlow()

    fun update(transform: (TrackingStatus) -> TrackingStatus) {
        _status.value = transform(_status.value)
    }

    fun reset() {
        _status.value = TrackingStatus()
    }
}

class LocationTrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository = RiderRepository()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var activePhone: String = ""
    private var activeRiderId: String? = null

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val lat = loc.latitude
                val lon = loc.longitude

                TrackingStateManager.update { current ->
                    current.copy(latitude = lat, longitude = lon)
                }

                if (activePhone.isNotBlank() || !activeRiderId.isNullOrBlank()) {
                    serviceScope.launch {
                        val res = repository.updateLocation(activeRiderId, activePhone, lat, lon)
                        val now = System.currentTimeMillis()
                        if (res.isSuccess) {
                            TrackingStateManager.update { current ->
                                current.copy(
                                    lastUpdateEpoch = now,
                                    successfulUpdatesCount = current.successfulUpdatesCount + 1,
                                    lastStatusMessage = "Live location updated (${String.format("%.5f", lat)}, ${String.format("%.5f", lon)})"
                                )
                            }
                            updateNotification("Sending every 5s • Updated ${String.format("%.4f", lat)}, ${String.format("%.4f", lon)}")
                        } else {
                            val err = res.exceptionOrNull()?.message ?: "Update failed"
                            TrackingStateManager.update { current ->
                                current.copy(
                                    lastStatusMessage = "Upload warning: $err"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        when (action) {
            ACTION_STOP -> {
                stopTracking()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_START -> {
                val phone = intent?.getStringExtra(EXTRA_PHONE) ?: ""
                val id = intent?.getStringExtra(EXTRA_RIDER_ID)
                if (phone.isNotBlank() || !id.isNullOrBlank()) {
                    activePhone = phone
                    activeRiderId = id
                    startTrackingForeground()
                }
            }
        }
        return START_STICKY
    }

    private fun startTrackingForeground() {
        val notification = buildNotification("Connecting to GPS...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        TrackingStateManager.update {
            it.copy(
                isTracking = true,
                phone = activePhone,
                lastStatusMessage = "Tracking active (5s interval)"
            )
        }

        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateIntervalMillis(3000L)
            .setMaxUpdateDelayMillis(5000L)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (_: SecurityException) {
            TrackingStateManager.update {
                it.copy(lastStatusMessage = "Location permission missing")
            }
        }
    }

    private fun stopTracking() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (_: Exception) {}

        TrackingStateManager.update {
            it.copy(
                isTracking = false,
                lastStatusMessage = "Tracking paused"
            )
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Rider Live Location Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps live location tracking active in the background"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Rider Tracker • Live")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notification = buildNotification(contentText)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopTracking()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.example.action.START_TRACKING"
        const val ACTION_STOP = "com.example.action.STOP_TRACKING"
        const val EXTRA_PHONE = "extra_rider_phone"
        const val EXTRA_RIDER_ID = "extra_rider_id"
        private const val NOTIFICATION_ID = 4040
        private const val CHANNEL_ID = "rider_live_location_channel"
    }
}
