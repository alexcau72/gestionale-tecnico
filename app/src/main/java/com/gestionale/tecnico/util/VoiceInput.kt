package com.gestionale.tecnico.util

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.gestionale.tecnico.data.local.entity.*

/**
 * Composable helper che fornisce un pulsante microfono per dettare del testo in italiano.
 * Gestisce automaticamente il permesso RECORD_AUDIO.
 */
@Composable
fun VoiceInputButton(
    onResult: (String) -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val context = LocalContext.current
    var pendingStart by remember { mutableStateOf(false) }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val text = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!text.isNullOrBlank()) onResult(text)
    }

    fun launchSpeech() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it-IT")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Parla ora…")
        }
        runCatching { speechLauncher.launch(intent) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && pendingStart) launchSpeech()
        pendingStart = false
    }

    IconButton(
        modifier = modifier,
        onClick = {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (granted) launchSpeech() else { pendingStart = true; permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
        }
    ) {
        Icon(Icons.Filled.Mic, contentDescription = "Inserisci vocalmente")
    }
}

// ---------- Parsing vocale ----------

object VoiceParser {

    private val numberWords = mapOf(
        "zero" to 0, "uno" to 1, "una" to 1, "due" to 2, "tre" to 3, "quattro" to 4,
        "cinque" to 5, "sei" to 6, "sette" to 7, "otto" to 8, "nove" to 9, "dieci" to 10,
        "venti" to 20, "trenta" to 30, "quaranta" to 40, "cinquanta" to 50,
        "sessanta" to 60, "settanta" to 70, "ottanta" to 80, "novanta" to 90, "cento" to 100
    )

    fun normalize(text: String): String = text.lowercase().trim()

    /** Estrae il primo importo numerico dalla frase (supporta "50", "50,50", "50.50", "50 euro"). */
    fun parseAmount(text: String): Double? {
        val normalized = normalize(text).replace("€", " euro ")
        val regex = Regex("(\\d+(?:[.,]\\d{1,2})?)")
        val match = regex.find(normalized) ?: return null
        return match.groupValues[1].replace(',', '.').toDoubleOrNull()
    }

    fun parseType(text: String): TransactionType? {
        val t = normalize(text)
        return when {
            t.contains("uscita") || t.contains("spesa") || t.contains("speso") || t.contains("acquisto") || t.contains("comprato") -> TransactionType.USCITA
            t.contains("entrata") || t.contains("incass") || t.contains("guadagn") || t.contains("assistenza") || t.contains("rivendita") || t.contains("venduto") -> TransactionType.ENTRATA
            else -> null
        }
    }

    fun parseIncomeCategory(text: String): TransactionCategory? {
        val t = normalize(text)
        return when {
            t.contains("rivendita") || t.contains("venduto") || t.contains("vendita") || t.contains("hardware") -> TransactionCategory.RIVENDITA
            t.contains("assistenza") || t.contains("intervento") || t.contains("riparazione") -> TransactionCategory.ASSISTENZA
            else -> null
        }
    }

    fun parseExpenseCategory(text: String): TransactionCategory? {
        val t = normalize(text)
        return when {
            t.contains("provvigione") || t.contains("collega") -> TransactionCategory.PROVVIGIONE_COLLEGA
            t.contains("materiale") || t.contains("acquisto") || t.contains("component") -> TransactionCategory.ACQUISTO_MATERIALE
            t.contains("gestione") || t.contains("spese") || t.contains("bolletta") || t.contains("abbonamento") -> TransactionCategory.SPESE_GESTIONE
            else -> TransactionCategory.ALTRO
        }
    }

    fun parsePaymentStatus(text: String): PaymentStatus? {
        val t = normalize(text)
        return when {
            t.contains("sospeso") || t.contains("non pagato") || t.contains("da pagare") || t.contains("da incassare") -> PaymentStatus.IN_SOSPESO
            t.contains("pagato") || t.contains("incassato") || t.contains("saldata") -> PaymentStatus.PAGATO
            else -> null
        }
    }

    fun parseExpiryType(text: String): ExpiryType? {
        val t = normalize(text)
        return when {
            t.contains("firma") || t.contains("digitale") || t.contains("cns") || t.contains("token") -> ExpiryType.FIRMA_DIGITALE
            t.contains("software") || t.contains("licenza") || t.contains("gestionale") || t.contains("antivirus") -> ExpiryType.SOFTWARE
            else -> null
        }
    }

    fun parseExpiryDuration(text: String): ExpiryDuration? {
        val t = normalize(text)
        return when {
            t.contains("triennale") || t.contains("tre anni") || t.contains("3 anni") -> ExpiryDuration.TRIENNALE
            t.contains("annuale") || t.contains("un anno") || t.contains("1 anno") -> ExpiryDuration.ANNUALE
            else -> null
        }
    }

    fun parseSubscriptionFrequency(text: String): SubscriptionFrequency? {
        val t = normalize(text)
        return when {
            t.contains("semestrale") || t.contains("sei mesi") || t.contains("6 mesi") || t.contains("semestre") -> SubscriptionFrequency.SEMESTRALE
            t.contains("mensile") || t.contains("ogni mese") || t.contains("al mese") || t.contains("mese") -> SubscriptionFrequency.MENSILE
            else -> null
        }
    }

    fun parseExpiryDate(text: String): Long? {
        val t = normalize(text)
        val now = java.time.LocalDate.now()
        val months = Regex("(\\d+)\\s*mese").find(t)?.groupValues?.get(1)?.toLongOrNull()
        val years = Regex("(\\d+)\\s*ann").find(t)?.groupValues?.get(1)?.toLongOrNull()
        return when {
            t.contains("domani") -> DateUtils.toMillis(now.plusDays(1))
            t.contains("dopodomani") -> DateUtils.toMillis(now.plusDays(2))
            t.contains("oggi") -> DateUtils.toMillis(now)
            months != null -> DateUtils.toMillis(now.plusMonths(months))
            years != null -> DateUtils.toMillis(now.plusYears(years))
            t.contains("un anno") || t.contains("1 anno") -> DateUtils.toMillis(now.plusYears(1))
            t.contains("sei mesi") || t.contains("6 mesi") -> DateUtils.toMillis(now.plusMonths(6))
            else -> null
        }
    }

    /** Cerca il cliente più probabile tra quelli esistenti in base al nome contenuto nella frase. */
    fun matchClient(text: String, clients: List<Client>): Client? {
        val t = normalize(text)
        return clients
            .filter { it.nome.isNotBlank() }
            .sortedByDescending { it.nome.length }
            .firstOrNull { c ->
                val parts = normalize(c.nome).split(" ").filter { it.length > 2 }
                parts.isNotEmpty() && parts.any { t.contains(it) }
            }
    }

    /** Ripulisce la descrizione rimuovendo parole chiave di tipo/importo. */
    fun extractDescription(text: String): String {
        val stopWords = listOf("entrata", "uscita", "spesa", "spese", "euro", "€", "assistenza", "rivendita", "pagato", "sospeso", "da", "per", "di", "a", "il", "la", "cliente")
        var result = normalize(text)
        result = result.replace(Regex("\\d+(?:[.,]\\d{1,2})?"), " ")
        stopWords.forEach { result = result.replace(Regex("\\b${Regex.escape(it)}\\b"), " ") }
        return result.replace(Regex("\\s+"), " ").trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
