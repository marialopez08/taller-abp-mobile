package com.example.domain.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Calendar
import java.util.Locale

data class SpaDate(
  val year: Int,
  val month: Int, // 1..12
  val day: Int,   // 1..31
) : Comparable<SpaDate> {

  fun toIsoString(): String =
    String.format(Locale.US, "%04d-%02d-%02d", year, month, day)

  fun toCalendar(): Calendar =
    Calendar.getInstance().apply {
      clear()
      set(year, month - 1, day, 0, 0, 0)
    }

  /**
   * Returns 1 for Monday ... 7 for Sunday.
   */
  fun dayOfWeekMondayFirst(): Int {
    val dow = toCalendar().get(Calendar.DAY_OF_WEEK)
    return when (dow) {
      Calendar.MONDAY -> 1
      Calendar.TUESDAY -> 2
      Calendar.WEDNESDAY -> 3
      Calendar.THURSDAY -> 4
      Calendar.FRIDAY -> 5
      Calendar.SATURDAY -> 6
      Calendar.SUNDAY -> 7
      else -> 1
    }
  }

  fun isSunday(): Boolean = dayOfWeekMondayFirst() == 7

  fun plusDays(days: Int): SpaDate {
    val cal = toCalendar()
    cal.add(Calendar.DAY_OF_MONTH, days)
    return SpaDate(
      year = cal.get(Calendar.YEAR),
      month = cal.get(Calendar.MONTH) + 1,
      day = cal.get(Calendar.DAY_OF_MONTH),
    )
  }

  override fun compareTo(other: SpaDate): Int {
    if (year != other.year) return year.compareTo(other.year)
    if (month != other.month) return month.compareTo(other.month)
    return day.compareTo(other.day)
  }

  companion object {
    fun fromIsoString(iso: String): SpaDate? {
      val parts = iso.split("-")
      if (parts.size != 3) return null
      val y = parts[0].toIntOrNull() ?: return null
      val m = parts[1].toIntOrNull() ?: return null
      val d = parts[2].toIntOrNull() ?: return null
      return SpaDate(y, m, d)
    }
  }
}

data class SpaYearMonth(
  val year: Int,
  val month: Int, // 1..12
) : Comparable<SpaYearMonth> {

  fun plusMonths(delta: Int): SpaYearMonth {
    val cal = Calendar.getInstance().apply {
      clear()
      set(year, month - 1, 1)
      add(Calendar.MONTH, delta)
    }
    return SpaYearMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
  }

  fun daysInMonth(): Int {
    val cal = Calendar.getInstance().apply {
      clear()
      set(year, month - 1, 1)
    }
    return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
  }

  fun firstDayOffsetMondayFirst(): Int =
    SpaDate(year, month, 1).dayOfWeekMondayFirst() - 1

  override fun compareTo(other: SpaYearMonth): Int {
    if (year != other.year) return year.compareTo(other.year)
    return month.compareTo(other.month)
  }
}

object SpaDateUtils {

  private val MONTHS_FULL =
    listOf(
      "enero", "febrero", "marzo", "abril", "mayo", "junio",
      "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
    )

  private val MONTHS_SHORT =
    listOf(
      "ene", "feb", "mar", "abr", "may", "jun",
      "jul", "ago", "sep", "oct", "nov", "dic",
    )

  private val DAYS_FULL =
    listOf(
      "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo",
    )

  val WEEKDAY_HEADERS_SHORT = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

  fun today(nowMillis: Long = System.currentTimeMillis()): SpaDate {
    val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
    return SpaDate(
      year = cal.get(Calendar.YEAR),
      month = cal.get(Calendar.MONTH) + 1,
      day = cal.get(Calendar.DAY_OF_MONTH),
    )
  }

  fun currentHour24(nowMillis: Long = System.currentTimeMillis()): Int {
    val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
    return cal.get(Calendar.HOUR_OF_DAY)
  }

  fun currentMinute(nowMillis: Long = System.currentTimeMillis()): Int {
    val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
    return cal.get(Calendar.MINUTE)
  }

  fun currentYearMonth(nowMillis: Long = System.currentTimeMillis()): SpaYearMonth {
    val t = today(nowMillis)
    return SpaYearMonth(t.year, t.month)
  }

  fun findFirstBookableDate(
    nowMillis: Long = System.currentTimeMillis(),
    occupiedSlotsByDate: Map<String, Set<String>> = emptyMap(),
  ): SpaDate {
    var candidate = today(nowMillis)
    val currentHour = currentHour24(nowMillis)
    for (offset in 0..30) {
      if (!candidate.isSunday()) {
        val occupied = occupiedSlotsByDate[candidate.toIsoString()].orEmpty()
        val hasFreeSlot =
          SpaSchedule.ALL_SLOTS.any { slot ->
            val pastToday = offset == 0 && slot.hour24 <= currentHour
            !pastToday && slot.id !in occupied
          }
        if (hasFreeSlot) return candidate
      }
      candidate = candidate.plusDays(1)
    }
    var fallback = today(nowMillis)
    while (fallback.isSunday()) fallback = fallback.plusDays(1)
    return fallback
  }

  fun isSlotInPast(
    date: SpaDate,
    slotHour24: Int,
    nowMillis: Long = System.currentTimeMillis(),
  ): Boolean {
    val todayDate = today(nowMillis)
    return when {
      date < todayDate -> true
      date > todayDate -> false
      else -> slotHour24 <= currentHour24(nowMillis)
    }
  }

  fun monthYearTitleSpanish(ym: SpaYearMonth): String {
    val monthName = MONTHS_FULL[ym.month - 1].replaceFirstChar { it.uppercase() }
    return "$monthName ${ym.year}"
  }

  fun formatReadableSpanish(date: SpaDate): String {
    val dowName = DAYS_FULL[date.dayOfWeekMondayFirst() - 1]
    val monthName = MONTHS_FULL[date.month - 1]
    return "$dowName, ${date.day} de $monthName de ${date.year}"
  }

  fun formatReadableSpanish(iso: String): String {
    val parsed = SpaDate.fromIsoString(iso) ?: return iso
    return formatReadableSpanish(parsed)
  }

  fun formatShortSpanish(iso: String): String {
    val parsed = SpaDate.fromIsoString(iso) ?: return iso
    val monthShort = MONTHS_SHORT[parsed.month - 1]
    return "${parsed.day} $monthShort ${parsed.year}"
  }

  fun formatCop(amount: Int): String {
    val symbols = DecimalFormatSymbols(Locale.US).apply {
      groupingSeparator = '.'
    }
    val formatter = DecimalFormat("#,###", symbols)
    return "$${formatter.format(amount)} COP"
  }
}
