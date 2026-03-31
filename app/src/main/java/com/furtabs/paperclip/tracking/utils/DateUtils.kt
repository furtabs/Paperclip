package com.furtabs.paperclip.tracking.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.util.Locale

object DateUtils {
    fun parseToLocalDate(dateStr: String?): LocalDate? {
        if (dateStr.isNullOrBlank()) return null
        val defaultLocale = Locale.getDefault()
        val cleanStr = dateStr.trim().lowercase(defaultLocale)

        try {
            var loggiDate = cleanStr.split(" -", " |")[0].trim()

            loggiDate = loggiDate.replace(",", "").replace(".", "")
            val meses = mapOf(
                "jan" to "01", "fev" to "02", "mar" to "03", "abr" to "04",
                "mai" to "05", "jun" to "06", "jul" to "07", "ago" to "08",
                "set" to "09", "out" to "10", "nov" to "11", "dez" to "12"
            )

            for ((nome, numero) in meses) {
                if (loggiDate.contains(nome)) {
                    loggiDate = loggiDate.replace(nome, numero)
                    break
                }
            }

            loggiDate = loggiDate.replace(Regex("\\s+"), " ").trim()

            val loggiFormatter = DateTimeFormatter.ofPattern("d MM yyyy")
            return LocalDate.parse(loggiDate, loggiFormatter)

        } catch (e: Exception) {
        }

        try {
            val stdStr = cleanStr.take(10)
            val formatter = if (stdStr.contains("-")) {
                DateTimeFormatter.ofPattern("dd-MM-yyyy")
            } else {
                DateTimeFormatter.ofPattern("dd/MM/yyyy")
            }
            return LocalDate.parse(stdStr, formatter)
        } catch (e: Exception) { }

        try {
            val defaultLocale = Locale.getDefault()
            val textFormatter = DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("d 'de' MMM")
                .parseDefaulting(ChronoField.YEAR, LocalDate.now().year.toLong())
                .toFormatter(defaultLocale)

            val textDate = cleanStr.split(",")[0].trim()
            return LocalDate.parse(textDate, textFormatter)
        } catch (e: Exception) { }

        return null
    }
    fun formatDatePretty(dateStr: String?, timeStr: String?): String {
        val defaultLocale = Locale.getDefault()
        val isUs = defaultLocale.country == "US" || defaultLocale.language == "en"

        // try to parse combined date/time first
        val combinedDateTime = try {
            if (!dateStr.isNullOrBlank() && !timeStr.isNullOrBlank()) {
                val iso = if (timeStr.contains("T")) timeStr else "${dateStr}T${timeStr}"
                try {
                    val zoned = java.time.ZonedDateTime.parse(iso)
                    val outFmt = if (isUs) DateTimeFormatter.ofPattern("MMMM d yyyy h:mm a", Locale.US) else DateTimeFormatter.ofPattern("dd 'de' MMM, HH:mm", defaultLocale)
                    return zoned.withZoneSameInstant(ZoneId.systemDefault()).format(outFmt)
                } catch (_: Exception) {
                    // fallback
                }
                try {
                    val local = java.time.LocalDateTime.parse(iso)
                    val outFmt = if (isUs) DateTimeFormatter.ofPattern("MMMM d yyyy h:mm a", Locale.US) else DateTimeFormatter.ofPattern("dd 'de' MMM, HH:mm", defaultLocale)
                    return local.format(outFmt)
                } catch (_: Exception) {
                    // no-op
                }
            }
            null
        } catch (_: Exception) {
            null
        }

        if (!combinedDateTime.isNullOrBlank()) return combinedDateTime

        val dateObj = parseToLocalDate(dateStr) ?: return "$dateStr, $timeStr"
        val outputFormat = if (isUs) DateTimeFormatter.ofPattern("MMMM d yyyy", Locale.US) else DateTimeFormatter.ofPattern("dd 'de' MMM", defaultLocale)
        val formattedDate = dateObj.format(outputFormat)
        return if (timeStr.isNullOrBlank()) formattedDate else {
            val time = try {
                val parsedTime = java.time.LocalTime.parse(timeStr)
                val formatter = if (isUs) DateTimeFormatter.ofPattern("h:mm a", Locale.US) else DateTimeFormatter.ofPattern("HH:mm", defaultLocale)
                parsedTime.format(formatter)
            } catch (_: Exception) {
                timeStr
            }
            "$formattedDate $time"
        }
    }
    fun calculateDays(lastDateStr: String?, firstDateStr: String?, savedAt: Long, isDelivered: Boolean): String {
        return try {
            if (isDelivered) {
                val dataEntrega = parseToLocalDate(lastDateStr) ?: LocalDate.now()
                val dias = kotlin.math.abs(ChronoUnit.DAYS.between(dataEntrega, LocalDate.now()))
                dias.toString()
            } else {
                val dataInicial = parseToLocalDate(firstDateStr) ?: Instant.ofEpochMilli(savedAt).atZone(ZoneId.systemDefault()).toLocalDate()
                val dias = kotlin.math.abs(ChronoUnit.DAYS.between(dataInicial, LocalDate.now()))
                dias.toString()
            }
        } catch (e: Exception) {
            "--"
        }
    }
}