package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.Appointment
import com.example.domain.model.BookingStatus
import com.example.domain.model.SpaDateUtils
import com.example.ui.BookingsFilter
import com.example.ui.components.BookingStatusBadge
import com.example.ui.components.LocalStorageNoticeBanner

@Composable
fun MyBookingsScreen(
  appointments: List<Appointment>,
  activeFilter: BookingsFilter,
  onSelectFilter: (BookingsFilter) -> Unit,
  onCancelAppointment: (Long) -> Unit,
  onBookNewClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var appointmentToCancel by remember { mutableStateOf<Appointment?>(null) }

  appointmentToCancel?.let { target ->
    AlertDialog(
      onDismissRequest = { appointmentToCancel = null },
      title = {
        Text(
          text = "¿Cancelar esta reserva?",
          style = MaterialTheme.typography.titleLarge,
        )
      },
      text = {
        Text(
          text = "Se cancelará la cita de ${target.petName} el ${SpaDateUtils.formatShortSpanish(target.dateIso)} a las ${target.timeSlotLabel}. El horario quedará libre automáticamente.",
          style = MaterialTheme.typography.bodyMedium,
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onCancelAppointment(target.id)
            appointmentToCancel = null
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
          ),
          modifier = Modifier.testTag("confirm_cancel_dialog_button"),
        ) {
          Text("Sí, cancelar")
        }
      },
      dismissButton = {
        TextButton(onClick = { appointmentToCancel = null }) {
          Text("Mantener cita")
        }
      },
    )
  }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .widthIn(max = 720.dp)
        .fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "Mis reservas",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            text = "Consulta el estado de tus citas, organizadas mostrando primero las más próximas.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      item {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(BookingsFilter.entries) { filter ->
            FilterChip(
              selected = activeFilter == filter,
              onClick = { onSelectFilter(filter) },
              label = { Text(filter.label) },
              modifier = Modifier.testTag("bookings_filter_${filter.name.lowercase()}"),
            )
          }
        }
      }

      if (appointments.isEmpty()) {
        item {
          EmptyBookingsStateCard(
            filter = activeFilter,
            onBookNewClick = onBookNewClick,
          )
        }
      } else {
        items(appointments, key = { it.id }) { appointment ->
          BookingCard(
            appointment = appointment,
            onCancelClick = { appointmentToCancel = appointment },
          )
        }
      }

      item {
        LocalStorageNoticeBanner()
      }
    }
  }
}

@Composable
private fun BookingCard(
  appointment: Appointment,
  onCancelClick: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("booking_card_${appointment.id}"),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(
      width = 1.dp,
      color = if (appointment.status == BookingStatus.CONFIRMADA) {
        MaterialTheme.colorScheme.primaryContainer
      } else {
        MaterialTheme.colorScheme.outlineVariant
      },
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.weight(1f),
        ) {
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.Pets,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(22.dp),
            )
          }
          Column {
            Text(
              text = appointment.petName,
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = buildString {
                append("Cliente: ${appointment.clientName}")
                if (appointment.petBreed.isNotBlank()) {
                  append(" • ${appointment.petBreed}")
                }
              },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        BookingStatusBadge(
          status = appointment.status,
          modifier = Modifier.testTag("booking_status_${appointment.id}"),
        )
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BookingInfoRow(
          icon = Icons.Rounded.Spa,
          label = "Servicio",
          value = appointment.serviceName,
        )
        BookingInfoRow(
          icon = Icons.Rounded.Straighten,
          label = "Tamaño",
          value = appointment.dogSize.displayName,
        )
        BookingInfoRow(
          icon = Icons.Rounded.CalendarMonth,
          label = "Fecha",
          value = SpaDateUtils.formatReadableSpanish(appointment.dateIso),
        )
        BookingInfoRow(
          icon = Icons.Rounded.Schedule,
          label = "Hora",
          value = appointment.timeSlotLabel,
        )
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "Valor total",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = SpaDateUtils.formatCop(appointment.priceCop),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
          )
        }

        if (appointment.status == BookingStatus.CONFIRMADA) {
          OutlinedButton(
            onClick = onCancelClick,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
            colors = ButtonDefaults.outlinedButtonColors(
              contentColor = MaterialTheme.colorScheme.error,
            ),
            modifier = Modifier
              .height(48.dp)
              .testTag("cancel_booking_button_${appointment.id}"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Cancel,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Cancelar reserva")
          }
        }
      }
    }
  }
}

@Composable
private fun BookingInfoRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  value: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.secondary,
      modifier = Modifier.size(18.dp),
    )
    Text(
      text = "$label:",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.width(72.dp),
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}

@Composable
private fun EmptyBookingsStateCard(
  filter: BookingsFilter,
  onBookNewClick: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_empty_bookings),
        contentDescription = "Aún no hay reservas registradas",
        modifier = Modifier
          .fillMaxWidth()
          .height(170.dp)
          .clip(RoundedCornerShape(18.dp)),
        contentScale = ContentScale.Crop,
      )
      Text(
        text = if (filter == BookingsFilter.TODAS) {
          "Aún no tienes reservas"
        } else {
          "No hay citas en ${filter.label.lowercase()}"
        },
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
      )
      Text(
        text = "Cuando agendes el baño de tu mascota, podrás consultar aquí todos los detalles y gestionar tu cita.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      Button(
        onClick = onBookNewClick,
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("empty_bookings_cta_button"),
        shape = RoundedCornerShape(16.dp),
      ) {
        Text(
          text = "Reservar una cita",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}
