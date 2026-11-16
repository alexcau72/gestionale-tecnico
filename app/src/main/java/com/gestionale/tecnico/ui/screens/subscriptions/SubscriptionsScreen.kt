package com.gestionale.tecnico.ui.screens.subscriptions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gestionale.tecnico.data.local.entity.SubscriptionContract
import com.gestionale.tecnico.data.local.entity.SubscriptionFrequency
import com.gestionale.tecnico.ui.components.AddSubscriptionDialog
import com.gestionale.tecnico.ui.viewmodel.MainViewModel
import com.gestionale.tecnico.util.DateUtils
import com.gestionale.tecnico.util.toEuro

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(vm: MainViewModel) {
    val subs by vm.allSubscriptions.collectAsState()
    val clients by vm.clients.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<SubscriptionContract?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Abbonamenti Teleassistenza") }) },
        floatingActionButton = { FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, "Nuovo abbonamento") } }
    ) { pad ->
        if (subs.isEmpty()) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Nessun abbonamento. Aggiungine uno con +")
            }
        } else {
            LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(subs, key = { it.id }) { s ->
                    val clientName = clients.find { it.id == s.clientId }?.nome ?: "Cliente #${s.clientId}"
                    SubscriptionCard(
                        sub = s,
                        clientName = clientName,
                        onRenew = { vm.renewSubscription(s, registerPayment = true) },
                        onEdit = { editItem = s },
                        onDelete = { vm.deleteSubscription(s) }
                    )
                }
            }
        }
    }

    if (showAdd) {
        AddSubscriptionDialog(clients = clients, onDismiss = { showAdd = false }, onSave = { vm.saveSubscription(it); showAdd = false })
    }
    if (editItem != null) {
        AddSubscriptionDialog(clients = clients, existing = editItem, onDismiss = { editItem = null }, onSave = { vm.saveSubscription(it); editItem = null })
    }
}

@Composable
fun SubscriptionCard(sub: SubscriptionContract, clientName: String, onRenew: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val days = DateUtils.daysUntil(sub.prossimoRinnovo)
    val color = when {
        !sub.attivo -> Color.Gray
        days < 0 -> Color(0xFFC62828)
        days <= 15 -> Color(0xFFEF6C00)
        else -> Color(0xFF2E7D32)
    }
    val status = when {
        !sub.attivo -> "Non attivo"
        days < 0 -> "Scaduto da ${-days} giorni"
        days == 0L -> "Scade oggi"
        else -> "Rinnovo tra $days giorni"
    }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(sub.titolo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(sub.importo.toEuro(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Text("$clientName • ${if (sub.frequenza == SubscriptionFrequency.MENSILE) "Mensile" else "Semestrale"}", style = MaterialTheme.typography.bodySmall)
            Text("Prossimo rinnovo: ${DateUtils.formatDate(sub.prossimoRinnovo)}", style = MaterialTheme.typography.bodyMedium)
            Text(status, color = color, fontWeight = FontWeight.Bold)
            if (sub.note.isNotBlank()) Text(sub.note, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                Button(onClick = onRenew, enabled = sub.attivo) { Text("Registra rinnovo") }
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Modifica") }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Elimina") }
            }
        }
    }
}
