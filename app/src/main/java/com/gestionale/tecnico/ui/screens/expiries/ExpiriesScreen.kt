package com.gestionale.tecnico.ui.screens.expiries

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gestionale.tecnico.data.local.entity.ExpiryDuration
import com.gestionale.tecnico.data.local.entity.ExpiryItem
import com.gestionale.tecnico.ui.viewmodel.MainViewModel
import com.gestionale.tecnico.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpiriesScreen(vm: MainViewModel) {
    val expiries by vm.allExpiries.collectAsState()
    val clients by vm.clients.collectAsState()
    var filter by remember { mutableStateOf<String>("TUTTE") }

    val filtered = when (filter) {
        "ANNUALE" -> expiries.filter { it.durata == ExpiryDuration.ANNUALE }
        "TRIENNALE" -> expiries.filter { it.durata == ExpiryDuration.TRIENNALE }
        else -> expiries
    }.sortedBy { it.dataScadenza }

    Scaffold(topBar = { TopAppBar(title = { Text("Scadenze") }) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("TUTTE", "ANNUALE", "TRIENNALE").forEach { f ->
                    FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f.lowercase().replaceFirstChar { it.uppercase() }) })
                }
            }
            if (filtered.isEmpty()) {
                Text("Nessuna scadenza trovata")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered, key = { it.id }) { e ->
                        ExpiryCard(e, clientName = clients.find { it.id == e.clientId }?.nome ?: "Cliente #${e.clientId}",
                            onRenew = { vm.renewExpiry(e) }, onDelete = { vm.deleteExpiry(e) })
                    }
                }
            }
        }
    }
}

@Composable
fun ExpiryCard(e: ExpiryItem, clientName: String, onRenew: () -> Unit, onDelete: () -> Unit) {
    val days = DateUtils.daysUntil(e.dataScadenza)
    val color = when {
        days < 0 -> Color(0xFFC62828)
        days <= 30 -> Color(0xFFEF6C00)
        else -> Color(0xFF2E7D32)
    }
    val status = when {
        days < 0 -> "Scaduta"
        days <= 30 -> "In scadenza ($days giorni)"
        else -> "Attiva ($days giorni)"
    }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(e.titolo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("$clientName • ${e.tipoScadenza.name.replace('_', ' ')} • ${e.durata}", style = MaterialTheme.typography.bodySmall)
            Text("Scadenza: ${DateUtils.formatDate(e.dataScadenza)}", style = MaterialTheme.typography.bodyMedium)
            Text(status, color = color, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRenew) { Text("Rinnova (+${if (e.durata == ExpiryDuration.ANNUALE) "1 anno" else "3 anni"})") }
                OutlinedButton(onClick = onDelete) { Text("Elimina") }
            }
        }
    }
}
