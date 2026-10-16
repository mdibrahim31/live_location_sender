package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.Rider
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RiderTrackerApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiderTrackerApp(viewModel: MainViewModel = viewModel()) {
    val screenState by viewModel.screenState.collectAsState()
    val loginState by viewModel.loginState.collectAsState()
    val trackingStatus by viewModel.trackingStatus.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Rider Tracker",
                            tint = Color(0xFF10B981)
                        )
                        Text(
                            text = "Rider Tracker",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    // Supabase connection status indicator
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (loginState.isConfigured) Color(0xFF064E3B) else Color(0xFF78350F),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (loginState.isConfigured) Icons.Default.Cloud else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (loginState.isConfigured) Color(0xFF34D399) else Color(0xFFFBBF24),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (loginState.isConfigured) "Supabase Ready" else "No Config",
                                color = if (loginState.isConfigured) Color(0xFF34D399) else Color(0xFFFBBF24),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(onClick = { viewModel.showConfigDialog(true) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Supabase Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = screenState) {
                is ScreenState.Login -> {
                    LoginContent(
                        loginState = loginState,
                        onPhoneChange = viewModel::onPhoneChanged,
                        onPasswordChange = viewModel::onPasswordChanged,
                        onLoginClick = viewModel::login,
                        onOpenSettings = { viewModel.showConfigDialog(true) }
                    )
                }
                is ScreenState.Tracking -> {
                    BackHandler {
                        viewModel.logout(context)
                    }
                    TrackingContent(
                        rider = state.rider,
                        trackingStatus = trackingStatus,
                        onToggleTracking = { enabled ->
                            viewModel.setTrackingEnabled(enabled, context)
                        },
                        onLogout = { viewModel.logout(context) }
                    )
                }
            }

            if (loginState.showConfigDialog) {
                SupabaseConfigDialog(
                    initialUrl = SupabaseManager.configuredUrl,
                    initialKey = SupabaseManager.configuredKey,
                    onDismiss = { viewModel.showConfigDialog(false) },
                    onSave = { url, key ->
                        viewModel.saveSupabaseConfig(url, key)
                    }
                )
            }
        }
    }
}

@Composable
fun LoginContent(
    loginState: LoginUiState,
    onPhoneChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Hero Branding Header
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF10B981), Color(0xFF0F766E))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Rider Live Tracker",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Log in with your rider phone & password to broadcast live coordinates",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Card Container for Inputs
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                OutlinedTextField(
                    value = loginState.phone,
                    onValueChange = onPhoneChange,
                    label = { Text("Phone Number") },
                    placeholder = { Text("e.g. 01882208531") },
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = "Phone")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = loginState.password,
                    onValueChange = onPasswordChange,
                    label = { Text("Password") },
                    placeholder = { Text("Enter your password") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = "Password")
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                AnimatedVisibility(
                    visible = loginState.errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = loginState.errorMessage ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onLoginClick,
                    enabled = !loginState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    )
                ) {
                    if (loginState.isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verifying Rider...", color = Color.White)
                    } else {
                        Text(
                            text = "Login to Start Tracking",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Supabase configuration prompt button
        TextButton(
            onClick = onOpenSettings,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (loginState.isConfigured) "View Supabase Configuration" else "⚠️ Set Supabase URL & Key",
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun TrackingContent(
    rider: Rider,
    trackingStatus: TrackingStatus,
    onToggleTracking: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current

    val permissions = remember {
        val list = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        list.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val fineGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            onToggleTracking(true)
        } else {
            Toast.makeText(context, "Location permission is required to broadcast live coordinates", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Rider Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F766E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    Column {
                        Text(
                            text = if (!rider.name.isNullOrBlank()) rider.name else "Rider Account",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Phone: ${rider.phone}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onLogout) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Logout",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Main Tracking Switch Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (trackingStatus.isTracking) Color(0xFF064E3B).copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                color = if (trackingStatus.isTracking) Color(0xFF10B981) else Color.Transparent
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Live Location Broadcast",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (trackingStatus.isTracking) "Active: sending every 5 seconds" else "Turn ON to send live coordinates",
                            fontSize = 13.sp,
                            color = if (trackingStatus.isTracking) Color(0xFF34D399) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = trackingStatus.isTracking,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                val fineCheck = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                )
                                if (fineCheck == PackageManager.PERMISSION_GRANTED) {
                                    onToggleTracking(true)
                                } else {
                                    permissionLauncher.launch(permissions)
                                }
                            } else {
                                onToggleTracking(false)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF10B981)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Pulsing Tracking Indicator
                if (trackingStatus.isTracking) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.25f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseScale"
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .background(Color(0xFF065F46).copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(Color(0xFF34D399))
                        )
                        Text(
                            text = "Broadcasting to Supabase riders table...",
                            fontSize = 12.sp,
                            color = Color(0xFF34D399),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Telemetry Data Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Live GPS Telemetry",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Latitude Box
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "CURRENT LATITUDE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = trackingStatus.latitude?.let { String.format("%.6f", it) } ?: "--",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (trackingStatus.latitude != null) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Longitude Box
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "CURRENT LONGITUDE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = trackingStatus.longitude?.let { String.format("%.6f", it) } ?: "--",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (trackingStatus.longitude != null) Color(0xFF06B6D4) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Status info row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Updates Sent: ${trackingStatus.successfulUpdatesCount}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val timeFormatted = remember(trackingStatus.lastUpdateEpoch) {
                        if (trackingStatus.lastUpdateEpoch > 0) {
                            val sdf = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
                            sdf.format(Date(trackingStatus.lastUpdateEpoch))
                        } else {
                            "None yet"
                        }
                    }
                    Text(
                        text = "Last: $timeFormatted",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = trackingStatus.lastStatusMessage,
                    fontSize = 12.sp,
                    color = if (trackingStatus.lastStatusMessage.startsWith("Live")) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SupabaseConfigDialog(
    initialUrl: String,
    initialKey: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var url by remember { mutableStateOf(initialUrl) }
    var key by remember { mutableStateOf(initialKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    tint = Color(0xFF10B981)
                )
                Text("Supabase Settings", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "If you didn't add secrets in GitHub Actions, you can paste your Supabase Project URL and Anon Key here directly:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Supabase Project URL") },
                    placeholder = { Text("https://your-project.supabase.co") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("Supabase Anon Key") },
                    placeholder = { Text("eyJhbGciOi...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(url, key) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("Save Credentials")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
