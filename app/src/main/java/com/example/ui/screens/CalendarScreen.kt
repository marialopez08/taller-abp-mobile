package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.model.SpaDate
import com.example.domain.model.SpaDateUtils
import com.example.domain.model.SpaSchedule
import com.example.domain.model.SpaYearMonth
import com.example.domain.model.TimeSlotUi
import com.example.ui.components.BookingStepHeader
import com.example.ui.components.CloudSyncNoticeBanner
import com.example.ui.theme.SlotAvailableBg
import com.example.ui.theme.SlotAvailableBorder
import com.example.ui.theme.SlotAvailableText
import com.example.ui.theme.SlotOccupiedBg
import com.example.ui.theme.SlotOccupiedBorder
import com.example.ui.theme.SlotOccupiedText
import com.example.ui.theme.SundayRestBg
import com.example.ui.theme.SundayRestText

@Composable
fun CalendarScreen(
  displayedMonth: SpaYearMonth,
  selectedDate: SpaDate,
  selectedSlotId: String?,
  timeSlots: List<TimeSlotUi>,
  occupiedSlotsByDate: Map<String, Set<String>>,
  finalPriceCop: Int,
  onNavigateMonth: (Int) -> Unit,
  onSelectDate: (SpaDate) -> Unit,
  onSelectSlot: (String) -> Unit,
  onContinue: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBack)

  val scrollState = rememberScrollState()
  val today = SpaDateUtils.today()
  val currentMonth = SpaDateUtils.currentYearMonth()
  val canGoPrevMonth = displayedMonth > currentMonth

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 720.dp)
        .fillMaxSize(),
    ) {
      Column(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .verticalScroll(scrollState)
          .padding(horizontal = 20.dp, vertical = 16.dp)
          .testTag("calendar_lazy_column"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        BookingStepHeader(
          currentStep = 3,
          title = "Calendario de reservas",
          subtitle = "Atendemos de lunes a sábado. Los domingos son días de descanso.",
          onBack = onBack,
        )

        MonthlyCalendarCard(
          displayedMonth = displayedMonth,
          selectedDate = selectedDate,
          today = today,
          canGoPrevMonth = canGoPrevMonth,
          occupiedSlotsByDate = occupiedSlotsByDate,
          onNavigateMonth = onNavigateMonth,
          onSelectDate = onSelectDate,
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Column {
            Text(
              text = "Horarios disponibles",
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
              text = SpaDateUtils.formatReadableSpanish(selectedDate),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.secondary,
              fontWeight = FontWeight.SemiBold,
            )
          }

          SlotLegendRow()
        }

        if (selectedDate.isSunday()) {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = SundayRestBg,
          ) {
            Row(
              modifier = Modifier.padding(18.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.EventBusy,
                contentDescription = null,
                tint = SundayRestText,
              )
              Text(
                text = "Los domingos son días de descanso en PET SPA. Por favor elige un día de lunes a sábado.",
                style = MaterialTheme.typography.bodyMedium,
                color = SundayRestText,
              )
            }
          }
        } else {
          TimeSlotGrid(
            slots = timeSlots,
            selectedSlotId = selectedSlotId,
            onSelectSlot = onSelectSlot,
          )
        }

        CloudSyncNoticeBanner(
          text = "La disponibilidad de horarios se consulta en tiempo real en Firebase Firestore.",
        )

        Spacer(modifier = Modifier.height(6.dp))
      }

      Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (selectedSlotId != null) {
                  "${SpaDateUtils.formatShortSpanish(selectedDate.toIsoString())} • ${SpaSchedule.labelFor(selectedSlotId)}"
                } else {
                  "Selecciona una hora disponible"
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "Total del servicio",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Text(
              text = SpaDateUtils.formatCop(finalPriceCop),
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.primary,
            )
          }

          Button(
            onClick = onContinue,
            enabled = selectedSlotId != null && !selectedDate.isSunday(),
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("calendar_continue_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
          ) {
            Text(
              text = "Continuar con tus datos",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
              contentDescription = null,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun MonthlyCalendarCard(
  displayedMonth: SpaYearMonth,
  selectedDate: SpaDate,
  today: SpaDate,
  canGoPrevMonth: Boolean,
  occupiedSlotsByDate: Map<String, Set<String>>,
  onNavigateMonth: (Int) -> Unit,
  onSelectDate: (SpaDate) -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          onClick = { onNavigateMonth(-1) },
          enabled = canGoPrevMonth,
          modifier = Modifier
            .size(48.dp)
            .testTag("calendar_prev_month_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
            contentDescription = "Mes anterior",
          )
        }
        Text(
          text = SpaDateUtils.monthYearTitleSpanish(displayedMonth),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        IconButton(
          onClick = { onNavigateMonth(1) },
          modifier = Modifier
            .size(48.dp)
            .testTag("calendar_next_month_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = "Mes siguiente",
          )
        }
      }

      Row(modifier = Modifier.fillMaxWidth()) {
        SpaDateUtils.WEEKDAY_HEADERS_SHORT.forEachIndexed { index, header ->
          val isSundayCol = index == 6
          Text(
            text = header,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSundayCol) SundayRestText else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      val daysInMonth = displayedMonth.daysInMonth()
      val leadingBlanks = displayedMonth.firstDayOffsetMondayFirst()
      val totalCells = leadingBlanks + daysInMonth
      val weeks = (totalCells + 6) / 7

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (week in 0 until weeks) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            for (col in 0 until 7) {
              val cellIndex = week * 7 + col
              val dayNum = cellIndex - leadingBlanks + 1
              if (dayNum in 1..daysInMonth) {
                val cellDate = SpaDate(displayedMonth.year, displayedMonth.month, dayNum)
                val isPast = cellDate < today
                val isSunday = cellDate.isSunday()
                val isEnabled = !isPast && !isSunday
                val isSelected = cellDate == selectedDate
                val isToday = cellDate == today
                val occupiedCount = occupiedSlotsByDate[cellDate.toIsoString()]?.size ?: 0
                val isFullyBooked = occupiedCount >= SpaSchedule.ALL_SLOTS.size

                CalendarDayCell(
                  day = dayNum,
                  isSelected = isSelected,
                  isToday = isToday,
                  isSunday = isSunday,
                  isEnabled = isEnabled && !isFullyBooked,
                  hasBookings = occupiedCount > 0,
                  onClick = { onSelectDate(cellDate) },
                  modifier = Modifier
                    .weight(1f)
                    .testTag("calendar_day_${cellDate.toIsoString()}"),
                )
              } else {
                Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
              }
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(SundayRestBg)
            .border(1.dp, SundayRestText.copy(alpha = 0.4f), CircleShape),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Domingos: Día de descanso (no disponible)",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun CalendarDayCell(
  day: Int,
  isSelected: Boolean,
  isToday: Boolean,
  isSunday: Boolean,
  isEnabled: Boolean,
  hasBookings: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val bgColor = when {
    isSelected -> MaterialTheme.colorScheme.primary
    isSunday -> SundayRestBg
    !isEnabled -> Color.Transparent
    isToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
    else -> MaterialTheme.colorScheme.surfaceContainerLow
  }

  val textColor = when {
    isSelected -> MaterialTheme.colorScheme.onPrimary
    isSunday -> SundayRestText.copy(alpha = 0.7f)
    !isEnabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    else -> MaterialTheme.colorScheme.onSurface
  }

  Box(
    modifier = modifier
      .aspectRatio(1f)
      .clip(RoundedCornerShape(14.dp))
      .background(bgColor)
      .then(
        if (isToday && !isSelected) {
          Modifier.border(1.5.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(14.dp))
        } else {
          Modifier
        }
      )
      .clickable(enabled = isEnabled, onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = day.toString(),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
        color = textColor,
      )
      if (hasBookings && isEnabled && !isSelected) {
        Box(
          modifier = Modifier
            .padding(top = 2.dp)
            .size(5.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.tertiary),
        )
      }
    }
  }
}

@Composable
private fun SlotLegendRow() {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    LegendChip(color = SlotAvailableBg, borderColor = SlotAvailableBorder, label = "Disponible")
    LegendChip(color = SlotOccupiedBg, borderColor = SlotOccupiedBorder, label = "Ocupado")
    LegendChip(color = MaterialTheme.colorScheme.primary, borderColor = MaterialTheme.colorScheme.primary, label = "Seleccionado")
  }
}

@Composable
private fun LegendChip(
  color: Color,
  borderColor: Color,
  label: String,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Box(
      modifier = Modifier
        .size(14.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(color)
        .border(1.dp, borderColor, RoundedCornerShape(4.dp)),
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun TimeSlotGrid(
  slots: List<TimeSlotUi>,
  selectedSlotId: String?,
  onSelectSlot: (String) -> Unit,
) {
  val rows = slots.chunked(3)
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    rows.forEach { rowSlots ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        rowSlots.forEach { slot ->
          val isSelected = slot.id == selectedSlotId
          val bgColor = when {
            isSelected -> MaterialTheme.colorScheme.primary
            slot.isAvailable -> SlotAvailableBg
            else -> SlotOccupiedBg
          }
          val borderColor = when {
            isSelected -> MaterialTheme.colorScheme.primary
            slot.isAvailable -> SlotAvailableBorder
            else -> SlotOccupiedBorder
          }
          val textColor = when {
            isSelected -> MaterialTheme.colorScheme.onPrimary
            slot.isAvailable -> SlotAvailableText
            else -> SlotOccupiedText
          }

          Surface(
            modifier = Modifier
              .weight(1f)
              .height(62.dp)
              .clip(RoundedCornerShape(16.dp))
              .clickable(enabled = slot.isAvailable) { onSelectSlot(slot.id) }
              .testTag("time_slot_${slot.id}"),
            shape = RoundedCornerShape(16.dp),
            color = bgColor,
            border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
          ) {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Schedule,
                  contentDescription = null,
                  tint = textColor,
                  modifier = Modifier.size(14.dp),
                )
                Text(
                  text = slot.label,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  color = textColor,
                )
              }
              Text(
                text = when {
                  isSelected -> "Elegido"
                  slot.isAvailable -> "Disponible"
                  slot.isPastHourToday -> "Pasado"
                  else -> "Ocupado"
                },
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = if (isSelected) 0.9f else 0.8f),
              )
            }
          }
        }
        repeat(3 - rowSlots.size) {
          Spacer(modifier = Modifier.weight(1f))
        }
      }
    }
  }
}
