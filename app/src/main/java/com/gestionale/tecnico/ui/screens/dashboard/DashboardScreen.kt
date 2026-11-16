package com.gestionale.tecnico.ui.screens.dashboard

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.gestionale.tecnico.GestionaleApp
import com.gestionale.tecnico.data.local.entity.*
import com.gestionale.tecnico.ui.components.AddExpiryDialog
import com.gestionale.tecnico.ui.components.AddSubscriptionDialog
import com.gestionale.tecnico.ui.components.AddTransactionDialog
import com.gestionale.tecnico.ui.viewmodel.MainViewModel
import com.gestionale.tecnico.util.BackupManager
import com.gestionale.tecnico.util.DateUtils
import com.gestionale.tecnico.util.toEuro
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(vm: MainViewModel, nav: NavController) {
    val state by vm.dashboard.collectAsState()
    val clients by vm.clients.collectAsState()
    val context = LocalContext.current
    val app = context.applicationContext as GestionaleApp
    val scope = rememberCoroutineScope()

    var showFabMenu by remember { mutableStateOf(false) }
    var showTxDialog by remember { mutableStateOf(false) }
    var txType by remember { mutableStateOf(TransactionType.ENTRATA) }
    var showExpiryDialog by remember { mutableStateOf(false) }
    var showSubscriptionDialog by remember { mutableStateOf(false) }
    var showPendingSheet by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val r = BackupManager.export(context, app.database, uri)
            Toast.makeText(context, if (r.isSuccess) "Backup esportato" else "Errore export: ${r.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val r = BackupManager.import(context, app.database, uri)
            Toast.makeText(context, if (r.isSuccess) "Backup importato (riavvia app)" else "Errore import: ${r.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestionale Tecnico") },
                actions = {
                    IconButton(onClick = { exportLauncher.launch("gestionale_backup.json") }) { Icon(Icons.Filled.Upload, "Export") }
                    IconButton(onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }) { Icon(Icons.Filled.Download, "Import") }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (showFabMenu) {
                    SmallFloatingActionButton(onClick = { txType = TransactionType.ENTRATA; showTxDialog = true; showFabMenu = false }, containerColor = MaterialTheme.colorScheme.primaryContainer) { Icon(Icons.Filled.TrendingUp, null) }
                    Spacer(Modifier.height(8.dp))
                    SmallFloatingActionButton(onClick = { txType = TransactionType.USCITA; showTxDialog = true; showFabMenu = false }, containerColor = MaterialTheme.colorScheme.errorContainer) { Icon(Icons.Filled.TrendingDown, null) }
                    Spacer(Modifier.height(8.dp))
                    SmallFloatingActionButton(onClick = { showSubscriptionDialog = true; showFabMenu = false }, containerColor = MaterialTheme.colorScheme.secondaryContainer) { Icon(Icons.Filled.Autorenew, null) }
                    Spacer(Modifier.height(8.dp))
                    SmallFloatingActionButton(onClick = { showExpiryDialog = true; showFabMenu = false }, containerColor = MaterialTheme.colorScheme.tertiaryContainer) { Icon(Icons.Filled.Event, null) }
                    Spacer(Modifier.height(8.dp))
                }
                FloatingActionButton(onClick = { showFabMenu = !showFabMenu }) { Icon(if (showFabMenu) Icons.Filled.Close else Icons.Filled.Add, "Aggiungi") }
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                MonthSelector(
                    label = DateUtils.formatMonthYear(state.year, state.month),
                    onPrev = { vm.prevMonth() },
                    onNext = { vm.nextMonth() }
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryCard("Entrate", state.income.toEuro(), Color(0xFF2E7D32), Modifier.weight(1f))
                    SummaryCard("Spese", state.expenses.toEuro(), Color(0xFFC62828), Modifier.weight(1f))
                }
            }
            item { SummaryCard("Utile Netto", state.net.toEuro(), if (state.net >= 0) Color(0xFF1565C0) else Color(0xFFC62828), Modifier.fillMaxWidth()) }

            item {
                Card(modifier = Modifier.fillMaxWidth().clickable { showPendingSheet = true }) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Lavori in sospeso", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text("Totale da incassare: ${state.pendingTotal.toEuro()}", color = Color(0xFFEF6C00), fontWeight = FontWeight.Bold)
                        Text("${state.pendingItems.size} lavori non pagati - tocca per gestire", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item {
                val ups = state.upcomingExpiries
                Card(modifier = Modifier.fillMaxWidth().clickable { nav.navigate("expiries") }, colors = CardDefaults.cardColors(containerColor = if (ups.isEmpty()) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFF3E0))) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Scadenze imminenti (30 giorni)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        if (ups.isEmpty()) Text("Nessuna scadenza nei prossimi 30 giorni", style = MaterialTheme.typography.bodyMedium)
                        else {
                            Text("${ups.size} scadenze in arrivo", color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                            ups.take(3).forEach { e -> Text("• ${e.titolo} - ${DateUtils.formatDate(e.dataScadenza)}", style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }

            item {
                val subs = state.upcomingSubscriptions
                Card(modifier = Modifier.fillMaxWidth().clickable { nav.navigate("subscriptions") }, colors = CardDefaults.cardColors(containerColor = if (subs.isEmpty()) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFE8F5E9))) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Abbonamenti teleassistenza in rinnovo (30 giorni)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        if (subs.isEmpty()) Text("Nessun rinnovo imminente", style = MaterialTheme.typography.bodyMedium)
                        else {
                            Text("${subs.size} abbonamenti da rinnovare", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                            subs.take(3).forEach { s -> Text("• ${s.titolo} (${if (s.frequenza == SubscriptionFrequency.MENSILE) "Mensile" else "Semestrale"}) - ${DateUtils.formatDate(s.prossimoRinnovo)}", style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
        }
    }

    if (showTxDialog) {
        AddTransactionDialog(
            type = txType,
            clients = clients,
            onDismiss = { showTxDialog = false },
            onSave = { vm.saveTransaction(it); showTxDialog = false }
        )
    }
    if (showExpiryDialog) {
        AddExpiryDialog(clients = clients, onDismiss = { showExpiryDialog = false }, onSave = { vm.saveExpiry(it); showExpiryDialog = false })
    }
    if (showSubscriptionDialog) {
        AddSubscriptionDialog(clients = clients, onDismiss = { showSubscriptionDialog = false }, onSave = { vm.saveSubscription(it); showSubscriptionDialog = false })
    }
    if (showPendingSheet) {
        ModalBottomSheet(onDismissRequest = { showPendingSheet = false }) {
            Text("Lavori in sospeso", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
            if (state.pendingItems.isEmpty()) {
                Text("Nessun lavoro in sospeso", modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                    items(state.pendingItems) { t ->
                        ListItem(
                            headlineContent = { Text(t.descrizione.ifBlank { "Lavoro #${t.id}" }) },
                            supportingContent = { Text("${DateUtils.formatDate(t.data)} • ${t.importo.toEuro()}") },
                            trailingContent = { TextButton(onClick = { vm.markPaid(t.id) }) { Text("Segna PAGATO") } }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun MonthSelector(label: String, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = onPrev) { Icon(Icons.Filled.ChevronLeft, "Mese precedente") }
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = onNext) { Icon(Icons.Filled.ChevronRight, "Mese successivo") }
    }
}

@Composable
fun SummaryCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
