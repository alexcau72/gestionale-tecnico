package com.gestionale.tecnico.ui.screens.clients

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gestionale.tecnico.data.local.entity.*
import com.gestionale.tecnico.ui.components.AddClientDialog
import com.gestionale.tecnico.ui.viewmodel.MainViewModel
import com.gestionale.tecnico.util.DateUtils
import com.gestionale.tecnico.util.toEuro
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(vm: MainViewModel, clientId: Long, onBack: () -> Unit) {
    val clients by vm.clients.collectAsState()
    val client = clients.find { it.id == clientId }
    val txs by vm.transactionsForClient(clientId).collectAsState(initial = emptyList())
    val pending by vm.clientPending(clientId).collectAsState(initial = 0.0)
    val now = LocalDate.now()
    val monthStart = DateUtils.startOfMonth(now.year, now.monthValue)
    val monthEnd = DateUtils.endOfMonthExclusive(now.year, now.monthValue)
    val monthEarn by vm.clientEarnings(clientId, monthStart, monthEnd).collectAsState(initial = 0.0)
    val totalEarn by vm.clientEarnings(clientId, 0L, Long.MAX_VALUE / 2).collectAsState(initial = 0.0)
    var showEdit by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(client?.nome ?: "Cliente") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro") } },
                actions = { IconButton(onClick = { showEdit = true }) { Icon(Icons.Filled.Edit, "Modifica") } }
            )
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Riepilogo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Sospesi attuali: ${pending.toEuro()}", color = Color(0xFFEF6C00), fontWeight = FontWeight.Bold)
                        Text("Guadagni mese corrente: ${monthEarn.toEuro()}", color = Color(0xFF2E7D32))
                        Text("Guadagni storici: ${totalEarn.toEuro()}", color = Color(0xFF1565C0))
                        if (!client?.telefono.isNullOrBlank()) Text("Tel: ${client?.telefono}")
                        if (!client?.note.isNullOrBlank()) Text("Note: ${client?.note}")
                    }
                }
            }
            item { Text("Cronologia lavori", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            if (txs.isEmpty()) {
                item { Text("Nessun movimento registrato") }
            } else {
                items(txs, key = { it.id }) { t ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(t.descrizione.ifBlank { t.categoria.name }, fontWeight = FontWeight.SemiBold)
                                Text(t.importo.toEuro(), color = if (t.tipo == TransactionType.ENTRATA) Color(0xFF2E7D32) else Color(0xFFC62828))
                            }
                            Text("${DateUtils.formatDate(t.data)} • ${t.tipo} • ${t.categoria}", style = MaterialTheme.typography.bodySmall)
                            if (t.tipo == TransactionType.ENTRATA) Text("Stato: ${t.statoPagamento}", style = MaterialTheme.typography.bodySmall)
                            if (!t.collegaProcuratore.isNullOrBlank()) {
                                Text("Procurato da: ${t.collegaProcuratore} (provvigione ${ (t.importoProvvigioneCollega ?: 0.0).toEuro() })", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6A1B9A))
                            }
                            if (t.tipo == TransactionType.ENTRATA && t.statoPagamento == PaymentStatus.IN_SOSPESO) {
                                TextButton(onClick = { vm.markPaid(t.id) }) { Text("Segna come PAGATO") }
                            }
                        }
                    }
                }
            }
        }
    }
    if (showEdit && client != null) {
        AddClientDialog(existing = client, onDismiss = { showEdit = false }, onSave = { vm.saveClient(it); showEdit = false })
    }
}
