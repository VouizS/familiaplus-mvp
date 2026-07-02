package com.sw.vidalink.companion

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class LocationShareService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationManager: LocationManager

    private val listener = LocationListener { location ->
        val config = loadConfig(this)
        scope.launch { pushLocation(this@LocationShareService, config, location, sos = false, message = "Monitoramento ativo") }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        ensureChannel()
        val notification = buildNotification("Proteção familiar ativa", "Compartilhando localização com consentimento.")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1001, notification)
        }
        startLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_SOS) {
            scope.launch { pushLastKnown(this@LocationShareService, sos = true, message = "SOS enviado pelo usuário") }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { locationManager.removeUpdates(listener) }
        scope.cancel()
        super.onDestroy()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (!hasLocationPermission(this)) return
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        providers.forEach { provider ->
            if (locationManager.isProviderEnabled(provider)) {
                runCatching {
                    locationManager.requestLocationUpdates(provider, 15_000L, 15f, listener, Looper.getMainLooper())
                }
            }
        }
        scope.launch { pushLastKnown(this@LocationShareService, sos = false, message = "Serviço iniciado") }
    }

    @SuppressLint("MissingPermission")
    private suspend fun pushLastKnown(context: Context, sos: Boolean, message: String) {
        if (!hasLocationPermission(context)) return
        val last = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
        if (last != null) pushLocation(context, loadConfig(context), last, sos, message)
        else pushHeartbeat(context, loadConfig(context), sos, message)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Bem-estar", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "vidalink_location"
        const val ACTION_SOS = "com.sw.vidalink.companion.SOS"
        const val ACTION_STOP = "com.sw.vidalink.companion.STOP"

        fun start(context: Context) {
            val intent = Intent(context, LocationShareService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, LocationShareService::class.java).setAction(ACTION_STOP))
        }

        fun sendSos(context: Context) {
            val intent = Intent(context, LocationShareService::class.java).setAction(ACTION_SOS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }
    }
}

data class CompanionConfig(
    val backendUrl: String,
    val familyCode: String,
    val displayName: String,
    val deviceId: String
)

fun loadConfig(context: Context): CompanionConfig {
    val prefs = context.getSharedPreferences("vidalink_companion", Context.MODE_PRIVATE)
    val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "android"
    return CompanionConfig(
        backendUrl = prefs.getString("backendUrl", "") ?: "",
        familyCode = prefs.getString("familyCode", "familia-teste") ?: "familia-teste",
        displayName = prefs.getString("displayName", "Acompanhado") ?: "Acompanhado",
        deviceId = "device-$androidId"
    )
}

fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

suspend fun pushLocation(context: Context, config: CompanionConfig, location: Location, sos: Boolean, message: String) {
    val payload = basePayload(context, config, sos, message)
        .put("lat", location.latitude)
        .put("lng", location.longitude)
        .put("accuracy", location.accuracy.toDouble())
        .put("provider", location.provider ?: "unknown")
    pushJson(config, "latest", payload, method = "PUT")
    pushJson(config, "locations/${System.currentTimeMillis()}", payload, method = "PUT")
}

suspend fun pushHeartbeat(context: Context, config: CompanionConfig, sos: Boolean, message: String) {
    val payload = basePayload(context, config, sos, message)
    pushJson(config, "latest", payload, method = "PUT")
}

private fun basePayload(context: Context, config: CompanionConfig, sos: Boolean, message: String): JSONObject {
    return JSONObject()
        .put("displayName", config.displayName)
        .put("batteryPct", batteryPct(context))
        .put("sos", sos)
        .put("message", message)
        .put("ts", System.currentTimeMillis())
}

private fun batteryPct(context: Context): Int {
    val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return -1
    val level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    return if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt() else -1
}

private fun pushJson(config: CompanionConfig, path: String, json: JSONObject, method: String) {
    val cleanBase = config.backendUrl.trim().removeSuffix("/")
    if (cleanBase.isBlank() || config.familyCode.isBlank()) return
    val url = "$cleanBase/families/${enc(config.familyCode)}/members/${enc(config.deviceId)}/$path.json"
    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = method
        doOutput = true
        connectTimeout = 10000
        readTimeout = 10000
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
    }
    try {
        conn.outputStream.use { it.write(json.toString().toByteArray(Charsets.UTF_8)) }
        conn.inputStream.close()
    } catch (_: Exception) {
        runCatching { conn.errorStream?.close() }
    } finally {
        conn.disconnect()
    }
}

private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
