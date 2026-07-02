package com.sw.vidalink.admin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class Telemetry(
    val deviceId: String = "",
    val displayName: String = "Acompanhado",
    val lat: Double? = null,
    val lng: Double? = null,
    val accuracy: Double? = null,
    val batteryPct: Int? = null,
    val sos: Boolean = false,
    val ts: Long? = null,
    val message: String = ""
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AdminApp() }
    }
}

@Composable
private fun AdminApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("vidalink_admin", Context.MODE_PRIVATE) }
    var backendUrl by remember { mutableStateOf(prefs.getString("backendUrl", "") ?: "") }
    var familyCode by remember { mutableStateOf(prefs.getString("familyCode", "familia-teste") ?: "familia-teste") }
    var status by remember { mutableStateOf("Configure a URL do Firebase Realtime Database e o código familiar.") }
    var telemetry by remember { mutableStateOf<Telemetry?>(null) }
    var autoRefresh by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(autoRefresh, backendUrl, familyCode) {
        while (autoRefresh) {
            telemetry = Repository.readLatest(backendUrl, familyCode)
            status = if (telemetry?.lat != null) "Última atualização recebida." else "Nenhum dado recebido ainda."
            delay(5000)
        }
    }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFEFF6FF), Color(0xFFFFFFFF))))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Família+", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Painel familiar para acompanhar rotina, localização consentida e SOS.")

            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = backendUrl,
                        onValueChange = { backendUrl = it },
                        label = { Text("Firebase DB URL") },
                        placeholder = { Text("https://seu-projeto-default-rtdb.firebaseio.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = familyCode,
                        onValueChange = { familyCode = it.trim() },
                        label = { Text("Código familiar") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = {
                            prefs.edit().putString("backendUrl", backendUrl).putString("familyCode", familyCode).apply()
                            scope.launch {
                                telemetry = Repository.readLatest(backendUrl, familyCode)
                                status = if (telemetry?.lat != null) "Dados carregados." else "Ainda não há telemetria neste código."
                            }
                        }) { Text("Salvar e atualizar") }
                        TextButton(onClick = { autoRefresh = !autoRefresh }) {
                            Text(if (autoRefresh) "Pausar ao vivo" else "Ativar ao vivo")
                        }
                    }
                }
            }

            DashboardCard(status, telemetry)
        }
    }
}

@Composable
private fun DashboardCard(status: String, telemetry: Telemetry?) {
    val context = LocalContext.current
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Status", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(status)
            Spacer(Modifier.height(6.dp))
            Text("Nome: ${telemetry?.displayName ?: "—"}")
            Text("Bateria: ${telemetry?.batteryPct?.let { "$it%" } ?: "—"}")
            Text("Precisão GPS: ${telemetry?.accuracy?.let { "%.1f m".format(it) } ?: "—"}")
            Text("Último horário: ${telemetry?.ts?.let { formatTime(it) } ?: "—"}")
            if (telemetry?.sos == true) {
                Text("SOS ATIVO", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
            }
            Text("Mensagem: ${telemetry?.message?.ifBlank { "—" } ?: "—"}")
            if (telemetry?.lat != null && telemetry.lng != null) {
                Text("Latitude: ${telemetry.lat}")
                Text("Longitude: ${telemetry.lng}")
                Button(onClick = {
                    val uri = Uri.parse("geo:${telemetry.lat},${telemetry.lng}?q=${telemetry.lat},${telemetry.lng}(Bem-estar)")
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                }) { Text("Abrir no mapa") }
            }
        }
    }
}

private fun formatTime(ts: Long): String = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR")).format(Date(ts))

private object Repository {
    suspend fun readLatest(baseUrl: String, familyCode: String): Telemetry? = withContext(Dispatchers.IO) {
        val cleanBase = baseUrl.trim().removeSuffix("/")
        if (cleanBase.isBlank() || familyCode.isBlank()) return@withContext null
        val url = "$cleanBase/families/${enc(familyCode)}/members.json"
        val raw = httpGet(url) ?: return@withContext null
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return@withContext null
        val keys = root.keys()
        while (keys.hasNext()) {
            val deviceId = keys.next()
            val member = root.optJSONObject(deviceId) ?: continue
            val latest = member.optJSONObject("latest") ?: continue
            return@withContext Telemetry(
                deviceId = deviceId,
                displayName = latest.optString("displayName", "Acompanhado"),
                lat = latest.optDoubleOrNull("lat"),
                lng = latest.optDoubleOrNull("lng"),
                accuracy = latest.optDoubleOrNull("accuracy"),
                batteryPct = latest.optIntOrNull("batteryPct"),
                sos = latest.optBoolean("sos", false),
                ts = latest.optLongOrNull("ts"),
                message = latest.optString("message", "")
            )
        }
        null
    }

    private fun httpGet(urlText: String): String? {
        val conn = (URL(urlText).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 10000
        }
        return try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
}

private fun JSONObject.optDoubleOrNull(name: String): Double? = if (has(name) && !isNull(name)) optDouble(name) else null
private fun JSONObject.optIntOrNull(name: String): Int? = if (has(name) && !isNull(name)) optInt(name) else null
private fun JSONObject.optLongOrNull(name: String): Long? = if (has(name) && !isNull(name)) optLong(name) else null
