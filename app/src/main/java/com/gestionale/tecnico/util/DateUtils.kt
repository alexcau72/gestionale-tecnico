package com.gestionale.tecnico.util

import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    private val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ITALY)
    private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ITALY)

    fun nowMillis(): Long = System.currentTimeMillis()
    fun today(): LocalDate = LocalDate.now()

    fun startOfMonth(year: Int, month: Int): Long =
        LocalDate.of(year, month, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun endOfMonthExclusive(year: Int, month: Int): Long =
        LocalDate.of(year, month, 1).plusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun startOfYear(year: Int): Long =
        LocalDate.of(year, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    fun endOfYearExclusive(year: Int): Long =
        LocalDate.of(year + 1, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun startOfQuarter(year: Int, q: Int): Long {
        val m = (q - 1) * 3 + 1
        return startOfMonth(year, m)
    }
    fun endOfQuarterExclusive(year: Int, q: Int): Long {
        val m = (q - 1) * 3 + 1
        return if (q == 4) endOfYearExclusive(year) else startOfMonth(year, m + 3)
    }

    fun formatDate(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFmt)

    fun formatMonthYear(year: Int, month: Int): String =
        LocalDate.of(year, month, 1).format(monthFmt).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ITALY) else it.toString() }

    fun toMillis(date: LocalDate): Long = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun plusDuration(millis: Long, annuale: Boolean): Long {
        val d = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        val nd = if (annuale) d.plusYears(1) else d.plusYears(3)
        return toMillis(nd)
    }

    fun plusMonths(millis: Long, months: Long): Long {
        val d = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        return toMillis(d.plusMonths(months))
    }

    fun daysUntil(millis: Long): Long {
        val d = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), d)
    }
}

fun Double.toEuro(): String = "€ %.2f".format(this)
