package com.gestionale.tecnico.ui.screens.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gestionale.tecnico.data.local.entity.TransactionCategory
import com.gestionale.tecnico.data.local.entity.TransactionType
import com.gestionale.tecnico.ui.viewmodel.MainViewModel
import com.gestionale.tecnico.util.DateUtils
import com.gestionale.tecnico.util.toEuro
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.max

data class CatStat(val label: String, val current: Double, val previous: Double) {
    val pct: Double? get() = if (previous == 0.0) null else ((current - previous) / abs(previous)) * 100.0
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(vm: MainViewModel) {
    val now = LocalDate.now()
    var mode by remember { mutableStateOf("Annuale") } // "Annuale" or "Q1"-"Q4"
    var year by remember { mutableIntStateOf(now.year) }

    val (start, end, prevStart, prevEnd, label) = remember(mode, year) {
        if (mode == "Annuale") Quint(DateUtils.startOfYear(year), DateUtils.endOfYearExclusive(year),
            DateUtils.startOfYear(year - 1), DateUtils.endOfYearExclusive(year - 1), "Anno $year")
        else {
            val q = mode.removePrefix("Q").toInt()
            Quint(DateUtils.startOfQuarter(year, q), DateUtils.endOfQuarterExclusive(year, q),
                DateUtils.startOfQuarter(year - 1, q), DateUtils.endOfQuarterExclusive(year - 1, q), "Q$q $year")
        }
    }

    val stats by produceState(initialValue = emptyList<CatStat>(), mode, year) {
        suspend fun sum(tipo: TransactionType, cat: TransactionCategory, s: Long, e: Long, onlyPaid: Boolean) =
            vm.sumByCategory(tipo, cat, s, e, onlyPaid)
        val curAss = sum(TransactionType.ENTRATA, TransactionCategory.ASSISTENZA, start, end, true)
        val prevAss = sum(TransactionType.ENTRATA, TransactionCategory.ASSISTENZA, prevStart, prevEnd, true)
        val curRiv = sum(TransactionType.ENTRATA, TransactionCategory.RIVENDITA, start, end, true)
        val prevRiv = sum(TransactionType.ENTRATA, TransactionCategory.RIVENDITA, prevStart, prevEnd, true)
        val curSpese = vm.sumExpenses(start, end)
        val prevSpese = vm.sumExpenses(prevStart, prevEnd)
        val curProv = sum(TransactionType.USCITA, TransactionCategory.PROVVIGIONE_COLLEGA, start, end, false)
        val prevProv = sum(TransactionType.USCITA, TransactionCategory.PROVVIGIONE_COLLEGA, prevStart, prevEnd, false)
        value = listOf(
            CatStat("Assistenza", curAss, prevAss),
            CatStat("Rivendita", curRiv, prevRiv),
            CatStat("Spese", curSpese, prevSpese),
            CatStat("Provvigioni colleghi", curProv, prevProv),
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Analisi") }) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Annuale", "Q1", "Q2", "Q3", "Q4").forEach { m ->
                    FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m) })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { year-- }) { Text("-") }
                Text("Anno: $year", fontWeight = FontWeight.SemiBold)
                OutlinedButton(onClick = { year++ }) { Text("+") }
            }
            Text("Confronto: $label vs anno precedente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            if (stats.isEmpty()) {
                CircularProgressIndicator()
            } else {
                stats.forEach { s -> StatRow(s) }
                Spacer(Modifier.height(8.dp))
                Text("Grafico comparativo", style = MaterialTheme.typography.titleSmall)
                SimpleBarChart(stats)
            }
        }
    }
}

data class Quint<A,B,C,D,E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

@Composable
fun StatRow(s: CatStat) {
    val pctText = when {
        s.pct == null && s.current == 0.0 && s.previous == 0.0 -> "n/d"
        s.pct == null -> if (s.current > 0) "+∞" else "n/d"
        else -> "%+.1f%%".format(s.pct)
    }
    val pctColor = when {
        s.pct == null -> Color.Gray
        s.pct >= 0 -> Color(0xFF2E7D32)
        else -> Color(0xFFC62828)
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(s.label, fontWeight = FontWeight.SemiBold)
            Text("Attuale: ${s.current.toEuro()}  |  Precedente: ${s.previous.toEuro()}", style = MaterialTheme.typography.bodySmall)
            Text("Variazione: $pctText", color = pctColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SimpleBarChart(stats: List<CatStat>) {
    val maxVal = max(1.0, stats.maxOf { max(it.current, it.previous) })
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        stats.forEach { s ->
            Text(s.label, style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(16.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE0E0E0))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth((s.current / maxVal).toFloat().coerceIn(0f, 1f)).background(Color(0xFF1565C0)))
                }
                Spacer(Modifier.width(8.dp)); Text("Att.", style = MaterialTheme.typography.labelSmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(16.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE0E0E0))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth((s.previous / maxVal).toFloat().coerceIn(0f, 1f)).background(Color(0xFF90CAF9)))
                }
                Spacer(Modifier.width(8.dp)); Text("Prec.", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
