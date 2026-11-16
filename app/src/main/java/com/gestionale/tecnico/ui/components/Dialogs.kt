package com.gestionale.tecnico.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gestionale.tecnico.data.local.entity.*
import com.gestionale.tecnico.util.DateUtils
import com.gestionale.tecnico.util.VoiceInputButton
import com.gestionale.tecnico.util.VoiceParser
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    type: TransactionType,
    clients: List<Client>,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit
) {
    val context = LocalContext.current
    var date by remember { mutableStateOf(LocalDate.now()) }
    var categoria by remember { mutableStateOf(if (type == TransactionType.ENTRATA) TransactionCategory.ASSISTENZA else TransactionCategory.ACQUISTO_MATERIALE) }
    var importo by remember { mutableStateOf("") }
    var descrizione by remember { mutableStateOf("") }
    var selectedClientId by remember { mutableStateOf<Long?>(null) }
    var stato by remember { mutableStateOf(PaymentStatus.PAGATO) }
    var procurato by remember { mutableStateOf(false) }
    var collega by remember { mutableStateOf("") }
    var provvigione by remember { mutableStateOf("") }
    var expandedClient by remember { mutableStateOf(false) }
    var expandedCat by remember { mutableStateOf(false) }
    var voiceText by remember { mutableStateOf<String?>(null) }

    val cats = if (type == TransactionType.ENTRATA)
        listOf(TransactionCategory.ASSISTENZA, TransactionCategory.RIVENDITA)
    else
        listOf(TransactionCategory.ACQUISTO_MATERIALE, TransactionCategory.SPESE_GESTIONE, TransactionCategory.PROVVIGIONE_COLLEGA, TransactionCategory.ALTRO)

    fun applyVoice(text: String) {
        voiceText = text
        VoiceParser.parseAmount(text)?.let { importo = "%.2f".format(it).replace(',', '.') }
        if (type == TransactionType.ENTRATA) {
            VoiceParser.parseIncomeCategory(text)?.let { categoria = it }
            VoiceParser.parsePaymentStatus(text)?.let { stato = it }
        } else {
            VoiceParser.parseExpenseCategory(text)?.let { categoria = it }
        }
        VoiceParser.matchClient(text, clients)?.let { selectedClientId = it.id }
        val desc = VoiceParser.extractDescription(text)
        if (desc.isNotBlank()) descrizione = desc
        if (VoiceParser.normalize(text).contains("collega")) procurato = true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (type == TransactionType.ENTRATA) "Nuova Entrata" else "Nuova Spesa")
                VoiceInputButton(onResult = { applyVoice(it) })
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                voiceText?.let { Text("Dettato: \"$it\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                OutlinedButton(onClick = {
                    DatePickerDialog(context, { _, y, m, d -> date = LocalDate.of(y, m + 1, d) }, date.year, date.monthValue - 1, date.dayOfMonth).show()
                }, modifier = Modifier.fillMaxWidth()) { Text("Data: ${date.dayOfMonth}/${date.monthValue}/${date.year}") }

                ExposedDropdownMenuBox(expanded = expandedCat, onExpandedChange = { expandedCat = it }) {
                    OutlinedTextField(value = categoria.name, onValueChange = {}, readOnly = true, label = { Text("Categoria") }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = expandedCat, onDismissRequest = { expandedCat = false }) {
                        cats.forEach { c -> DropdownMenuItem(text = { Text(c.name) }, onClick = { categoria = c; expandedCat = false }) }
                    }
                }

                OutlinedTextField(value = importo, onValueChange = { importo = it }, label = { Text("Importo (€)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = descrizione, onValueChange = { descrizione = it }, label = { Text("Descrizione") }, modifier = Modifier.fillMaxWidth())

                ExposedDropdownMenuBox(expanded = expandedClient, onExpandedChange = { expandedClient = it }) {
                    val name = clients.find { it.id == selectedClientId }?.nome ?: "Nessun cliente"
                    OutlinedTextField(value = name, onValueChange = {}, readOnly = true, label = { Text("Cliente (opzionale)") }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = expandedClient, onDismissRequest = { expandedClient = false }) {
                        DropdownMenuItem(text = { Text("Nessun cliente") }, onClick = { selectedClientId = null; expandedClient = false })
                        clients.forEach { c -> DropdownMenuItem(text = { Text(c.nome) }, onClick = { selectedClientId = c.id; expandedClient = false }) }
                    }
                }

                if (type == TransactionType.ENTRATA) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Stato: ")
                        FilterChip(selected = stato == PaymentStatus.PAGATO, onClick = { stato = PaymentStatus.PAGATO }, label = { Text("PAGATO") })
                        Spacer(Modifier.width(8.dp))
                        FilterChip(selected = stato == PaymentStatus.IN_SOSPESO, onClick = { stato = PaymentStatus.IN_SOSPESO }, label = { Text("IN SOSPESO") })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = procurato, onCheckedChange = { procurato = it })
                        Text("Procurato da collega")
                    }
                    if (procurato) {
                        OutlinedTextField(value = collega, onValueChange = { collega = it }, label = { Text("Nome collega") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = provvigione, onValueChange = { provvigione = it }, label = { Text("Importo provvigione (€)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val imp = importo.replace(',', '.').toDoubleOrNull() ?: 0.0
                val prov = if (procurato) provvigione.replace(',', '.').toDoubleOrNull() else null
                val t = Transaction(
                    data = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    tipo = type,
                    categoria = categoria,
                    importo = imp,
                    descrizione = descrizione,
                    clientId = selectedClientId,
                    statoPagamento = if (type == TransactionType.ENTRATA) stato else PaymentStatus.PAGATO,
                    collegaProcuratore = if (procurato) collega else null,
                    importoProvvigioneCollega = prov
                )
                onSave(t)
            }) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpiryDialog(
    clients: List<Client>,
    onDismiss: () -> Unit,
    onSave: (ExpiryItem) -> Unit
) {
    val context = LocalContext.current
    var titolo by remember { mutableStateOf("") }
    var selectedClientId by remember { mutableStateOf<Long?>(clients.firstOrNull()?.id) }
    var tipo by remember { mutableStateOf(ExpiryType.FIRMA_DIGITALE) }
    var durata by remember { mutableStateOf(ExpiryDuration.ANNUALE) }
    var date by remember { mutableStateOf(LocalDate.now().plusYears(1)) }
    var expandedClient by remember { mutableStateOf(false) }
    var voiceText by remember { mutableStateOf<String?>(null) }

    fun applyVoice(text: String) {
        voiceText = text
        VoiceParser.parseExpiryType(text)?.let { tipo = it }
        VoiceParser.parseExpiryDuration(text)?.let { durata = it }
        VoiceParser.parseExpiryDate(text)?.let { ms -> date = java.time.Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate() }
        VoiceParser.matchClient(text, clients)?.let { selectedClientId = it.id }
        val desc = VoiceParser.extractDescription(text)
        if (desc.isNotBlank()) titolo = desc else if (titolo.isBlank()) titolo = text
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Nuova Scadenza")
                VoiceInputButton(onResult = { applyVoice(it) })
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                voiceText?.let { Text("Dettato: \"$it\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                OutlinedTextField(value = titolo, onValueChange = { titolo = it }, label = { Text("Titolo") }, modifier = Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(expanded = expandedClient, onExpandedChange = { expandedClient = it }) {
                    val name = clients.find { it.id == selectedClientId }?.nome ?: "Seleziona cliente"
                    OutlinedTextField(value = name, onValueChange = {}, readOnly = true, label = { Text("Cliente") }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = expandedClient, onDismissRequest = { expandedClient = false }) {
                        clients.forEach { c -> DropdownMenuItem(text = { Text(c.nome) }, onClick = { selectedClientId = c.id; expandedClient = false }) }
                    }
                }
                Text("Tipo scadenza")
                Row { FilterChip(selected = tipo == ExpiryType.FIRMA_DIGITALE, onClick = { tipo = ExpiryType.FIRMA_DIGITALE }, label = { Text("Firma digitale") }); Spacer(Modifier.width(8.dp)); FilterChip(selected = tipo == ExpiryType.SOFTWARE, onClick = { tipo = ExpiryType.SOFTWARE }, label = { Text("Software") }) }
                Text("Durata")
                Row { FilterChip(selected = durata == ExpiryDuration.ANNUALE, onClick = { durata = ExpiryDuration.ANNUALE }, label = { Text("Annuale") }); Spacer(Modifier.width(8.dp)); FilterChip(selected = durata == ExpiryDuration.TRIENNALE, onClick = { durata = ExpiryDuration.TRIENNALE }, label = { Text("Triennale") }) }
                OutlinedButton(onClick = {
                    DatePickerDialog(context, { _, y, m, d -> date = LocalDate.of(y, m + 1, d) }, date.year, date.monthValue - 1, date.dayOfMonth).show()
                }, modifier = Modifier.fillMaxWidth()) { Text("Scadenza: ${date.dayOfMonth}/${date.monthValue}/${date.year}") }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cid = selectedClientId ?: return@TextButton
                onSave(ExpiryItem(titolo = titolo.ifBlank { tipo.name }, clientId = cid, tipoScadenza = tipo, dataScadenza = DateUtils.toMillis(date), durata = durata))
            }, enabled = selectedClientId != null && titolo.isNotBlank()) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}

@Composable
fun AddClientDialog(onDismiss: () -> Unit, onSave: (Client) -> Unit, existing: Client? = null) {
    var nome by remember { mutableStateOf(existing?.nome ?: "") }
    var tel by remember { mutableStateOf(existing?.telefono ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var voiceText by remember { mutableStateOf<String?>(null) }

    fun applyVoice(text: String) {
        voiceText = text
        // estrai numeri come telefono, resto come nome/note
        val phone = Regex("(\\+?\\d[\\d\\s-]{5,})").find(text)?.groupValues?.get(1)?.replace(" ", "") ?: ""
        if (phone.isNotBlank() && tel.isBlank()) tel = phone
        val cleaned = text.replace(phone, "").trim()
        if (nome.isBlank() && cleaned.isNotBlank()) nome = cleaned else if (note.isBlank()) note = cleaned
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (existing == null) "Nuovo Cliente" else "Modifica Cliente")
                VoiceInputButton(onResult = { applyVoice(it) })
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                voiceText?.let { Text("Dettato: \"$it\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = tel, onValueChange = { tel = it }, label = { Text("Telefono") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { if (nome.isNotBlank()) onSave(Client(id = existing?.id ?: 0, nome = nome, telefono = tel, note = note)) }, enabled = nome.isNotBlank()) { Text("Salva") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubscriptionDialog(
    clients: List<Client>,
    existing: SubscriptionContract? = null,
    onDismiss: () -> Unit,
    onSave: (SubscriptionContract) -> Unit
) {
    val context = LocalContext.current
    var titolo by remember { mutableStateOf(existing?.titolo ?: "Teleassistenza") }
    var selectedClientId by remember { mutableStateOf<Long?>(existing?.clientId ?: clients.firstOrNull()?.id) }
    var frequenza by remember { mutableStateOf(existing?.frequenza ?: SubscriptionFrequency.MENSILE) }
    var importo by remember { mutableStateOf(existing?.importo?.let { "%.2f".format(it) } ?: "") }
    var dataInizio by remember { mutableStateOf(existing?.dataInizio?.let { java.time.Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() } ?: LocalDate.now()) }
    var prossimo by remember { mutableStateOf(existing?.prossimoRinnovo?.let { java.time.Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() } ?: LocalDate.now().plusMonths(1)) }
    var attivo by remember { mutableStateOf(existing?.attivo ?: true) }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var expandedClient by remember { mutableStateOf(false) }
    var voiceText by remember { mutableStateOf<String?>(null) }

    fun applyVoice(text: String) {
        voiceText = text
        VoiceParser.parseSubscriptionFrequency(text)?.let { f ->
            frequenza = f
            prossimo = dataInizio.plusMonths(if (f == SubscriptionFrequency.MENSILE) 1 else 6)
        }
        VoiceParser.parseAmount(text)?.let { importo = "%.2f".format(it).replace(',', '.') }
        VoiceParser.matchClient(text, clients)?.let { selectedClientId = it.id }
        val desc = VoiceParser.extractDescription(text)
        if (desc.isNotBlank()) { titolo = "Teleassistenza - $desc"; note = desc }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (existing == null) "Nuovo Abbonamento" else "Modifica Abbonamento")
                VoiceInputButton(onResult = { applyVoice(it) })
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                voiceText?.let { Text("Dettato: \"$it\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                OutlinedTextField(value = titolo, onValueChange = { titolo = it }, label = { Text("Titolo") }, modifier = Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(expanded = expandedClient, onExpandedChange = { expandedClient = it }) {
                    val name = clients.find { it.id == selectedClientId }?.nome ?: "Seleziona cliente"
                    OutlinedTextField(value = name, onValueChange = {}, readOnly = true, label = { Text("Cliente") }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = expandedClient, onDismissRequest = { expandedClient = false }) {
                        clients.forEach { c -> DropdownMenuItem(text = { Text(c.nome) }, onClick = { selectedClientId = c.id; expandedClient = false }) }
                    }
                }
                Text("Frequenza")
                Row {
                    FilterChip(selected = frequenza == SubscriptionFrequency.MENSILE, onClick = { frequenza = SubscriptionFrequency.MENSILE; prossimo = dataInizio.plusMonths(1) }, label = { Text("Mensile") })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = frequenza == SubscriptionFrequency.SEMESTRALE, onClick = { frequenza = SubscriptionFrequency.SEMESTRALE; prossimo = dataInizio.plusMonths(6) }, label = { Text("Semestrale") })
                }
                OutlinedTextField(value = importo, onValueChange = { importo = it }, label = { Text("Importo (€)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = {
                    DatePickerDialog(context, { _, y, m, d -> dataInizio = LocalDate.of(y, m + 1, d) }, dataInizio.year, dataInizio.monthValue - 1, dataInizio.dayOfMonth).show()
                }, modifier = Modifier.fillMaxWidth()) { Text("Data inizio: ${dataInizio.dayOfMonth}/${dataInizio.monthValue}/${dataInizio.year}") }
                OutlinedButton(onClick = {
                    DatePickerDialog(context, { _, y, m, d -> prossimo = LocalDate.of(y, m + 1, d) }, prossimo.year, prossimo.monthValue - 1, prossimo.dayOfMonth).show()
                }, modifier = Modifier.fillMaxWidth()) { Text("Prossimo rinnovo: ${prossimo.dayOfMonth}/${prossimo.monthValue}/${prossimo.year}") }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = attivo, onCheckedChange = { attivo = it })
                    Text("Abbonamento attivo")
                }
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cid = selectedClientId ?: return@TextButton
                val imp = importo.replace(',', '.').toDoubleOrNull() ?: 0.0
                onSave(
                    SubscriptionContract(
                        id = existing?.id ?: 0,
                        clientId = cid,
                        titolo = titolo.ifBlank { "Teleassistenza" },
                        frequenza = frequenza,
                        importo = imp,
                        dataInizio = DateUtils.toMillis(dataInizio),
                        prossimoRinnovo = DateUtils.toMillis(prossimo),
                        attivo = attivo,
                        note = note
                    )
                )
            }, enabled = selectedClientId != null) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}
