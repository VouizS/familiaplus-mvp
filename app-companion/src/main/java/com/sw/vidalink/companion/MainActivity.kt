package com.sw.vidalink.companion

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CompanionApp() }
    }
}

@Composable
private fun CompanionApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("vidalink_companion", Context.MODE_PRIVATE) }
    var backendUrl by remember { mutableStateOf(prefs.getString("backendUrl", "") ?: "") }
    var familyCode by remember { mutableStateOf(prefs.getString("familyCode", "familia-teste") ?: "familia-teste") }
    var displayName by remember { mutableStateOf(prefs.getString("displayName", "Acompanhado") ?: "Acompanhado") }
    var status by remember { mutableStateOf(if (hasLocationPermission(context)) "Permissão de localização concedida." else "Permita a localização para iniciar.") }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        status = if (granted) "Permissão concedida. Agora toque em iniciar." else "Permissão negada. O app não pode acompanhar localização."
    }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFF0FDF4), Color(0xFFFFFFFF))))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Bem-estar", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("App visível de proteção familiar. O acompanhamento só funciona com permissões concedidas e notificação ativa.")

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
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Nome exibido") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text(status)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = {
                            val permissions = buildList {
                                add(Manifest.permission.ACCESS_FINE_LOCATION)
                                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                                if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                            }.toTypedArray()
                            locationPermissionLauncher.launch(permissions)
                        }) { Text("Permitir") }
                        Button(onClick = {
                            prefs.edit()
                                .putString("backendUrl", backendUrl.trim())
                                .putString("familyCode", familyCode.trim())
                                .putString("displayName", displayName.trim().ifBlank { "Acompanhado" })
                                .apply()
                            if (hasLocationPermission(context)) {
                                LocationShareService.start(context)
                                status = "Monitoramento ativo. Uma notificação fixa mostra que o app está compartilhando localização."
                            } else {
                                status = "Conceda a permissão de localização primeiro."
                            }
                        }) { Text("Iniciar") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(onClick = {
                            LocationShareService.sendSos(context)
                            status = "SOS enviado com a última localização disponível."
                        }) { Text("Enviar SOS") }
                        TextButton(onClick = {
                            LocationShareService.stop(context)
                            status = "Monitoramento pausado."
                        }) { Text("Pausar") }
                    }
                }
            }

            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Transparência", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Este MVP não grava microfone escondido e não roda disfarçado. O uso correto é proteção familiar consentida.")
                    Spacer(Modifier.height(4.dp))
                    Text("Batimentos cardíacos entram na fase 2 via relógio/Wear OS ou Health Connect, porque celular comum não mede BPM real com qualidade.")
                }
            }
        }
    }
}
