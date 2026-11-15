package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.model.Appointment
import com.example.domain.model.SpaDateUtils

@Composable
fun BookingConfirmationScreen(
  appointment: Appointment,
  onViewMyBookings: () -> Unit,
  onBackToHome: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBackToHome)

  val scrollState = rememberScrollState()
  var appeared by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) { appeared = true }
  val iconScale by animateFloatAsState(
    targetValue = if (appeared) 1f else 0.6f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium,
    ),
    label = "confirmation_icon_scale",
  )

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 720.dp)
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(horizontal = 20.dp, vertical = 28.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Box(
          modifier = Modifier
            .size(96.dp)
            .scale(iconScale)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = "Cita reservada",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(58.dp),
          )
        }

        Text(
          text = "¡Tu cita ha sido reservada exitosamente!",
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onBackground,
          textAlign = TextAlign.Center,
          modifier = Modifier.testTag("confirmation_title_text"),
        )

        Text(
          text = "Guardamos tu cita en este dispositivo. El horario seleccionado quedó reservado para ${appointment.petName}.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
      }

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = "Comprobante de cita",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          ConfirmationRow(label = "Mascota", value = appointment.petName)
          ConfirmationRow(label = "Servicio contratado", value = appointment.serviceName)
          ConfirmationRow(label = "Tamaño", value = appointment.dogSize.displayName)
          ConfirmationRow(
            label = "Fecha de la cita",
            value = SpaDateUtils.formatReadableSpanish(appointment.dateIso),
          )
          ConfirmationRow(label = "Hora de la cita", value = appointment.timeSlotLabel)

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Precio de la reserva",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = SpaDateUtils.formatCop(appointment.priceCop),
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }

      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Button(
          onClick = onViewMyBookings,
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("confirmation_view_bookings_button"),
          shape = RoundedCornerShape(18.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
        ) {
          Icon(
            imageVector = Icons.Rounded.CalendarMonth,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Ver mis reservas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }

        OutlinedButton(
          onClick = onBackToHome,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("confirmation_home_button"),
          shape = RoundedCornerShape(16.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Home,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Volver al inicio",
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }
    }
  }
}

@Composable
private fun ConfirmationRow(
  label: String,
  value: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Top,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.weight(0.42f),
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.weight(0.58f),
    )
  }
}
