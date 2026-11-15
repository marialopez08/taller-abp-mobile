package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.Appointment
import com.example.domain.model.SpaDateUtils
import com.example.domain.model.SpaService
import com.example.ui.components.BookingStatusBadge
import com.example.ui.components.LocalStorageNoticeBanner
import com.example.ui.components.ServiceShowcaseCard

@Composable
fun HomeScreen(
  services: List<SpaService>,
  upcomingAppointments: List<Appointment>,
  onBookAppointmentClick: () -> Unit,
  onSelectServiceClick: (SpaService) -> Unit,
  onViewAllBookingsClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val scrollState = rememberScrollState()

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 720.dp)
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(horizontal = 20.dp, vertical = 16.dp)
        .testTag("home_lazy_column"),
      verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      HomeBrandHeader()

      HomeHeroCard(onBookAppointmentClick = onBookAppointmentClick)

      if (upcomingAppointments.isNotEmpty()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Próximas reservas",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
          )
          TextButton(
            onClick = onViewAllBookingsClick,
            modifier = Modifier.testTag("home_view_all_bookings_button"),
          ) {
            Text("Ver todas")
          }
        }

        upcomingAppointments.take(2).forEach { appointment ->
          UpcomingAppointmentPreviewCard(
            appointment = appointment,
            onClick = onViewAllBookingsClick,
          )
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = "Servicios disponibles",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text = "Baños y cuidados pensados para el bienestar de tu perro.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      services.forEach { service ->
        ServiceShowcaseCard(
          service = service,
          onSelectClick = { onSelectServiceClick(service) },
          buttonTestTag = "home_select_service_${service.code.code.lowercase()}",
        )
      }

      LocalStorageNoticeBanner()
      Spacer(modifier = Modifier.height(8.dp))
    }
  }
}

@Composable
private fun HomeBrandHeader() {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(
            Brush.linearGradient(
              colors = listOf(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.secondaryContainer,
              )
            )
          ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Rounded.Pets,
          contentDescription = "Logotipo PET SPA",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(26.dp),
        )
      }
      Column {
        Text(
          text = "PET SPA",
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onBackground,
          fontWeight = FontWeight.ExtraBold,
        )
        Text(
          text = "Baño, peluquería y bienestar canino",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    Surface(
      shape = RoundedCornerShape(50),
      color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.Spa,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.tertiary,
          modifier = Modifier.size(16.dp),
        )
        Text(
          text = "Lun - Sáb",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onTertiaryContainer,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }
  }
}

@Composable
private fun HomeHeroCard(
  onBookAppointmentClick: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(28.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column {
      Image(
        painter = painterResource(id = R.drawable.img_hero_pet_spa),
        contentDescription = "Ilustración de perro feliz en PET SPA",
        modifier = Modifier
          .fillMaxWidth()
          .height(164.dp),
        contentScale = ContentScale.Crop,
      )

      Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "¡El bienestar de tu mascota comienza aquí!",
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = "Agenda en pocos pasos un baño relajante o arreglo estético con productos suaves y atención dedicada.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
          onClick = onBookAppointmentClick,
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("home_book_cta_button"),
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
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Reservar una cita",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}

@Composable
private fun UpcomingAppointmentPreviewCard(
  appointment: Appointment,
  onClick: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .clickable(onClick = onClick)
      .testTag("home_upcoming_card_${appointment.id}"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface,
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.Pets,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(20.dp),
            )
          }
          Column {
            Text(
              text = appointment.petName,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "${appointment.serviceName} • ${appointment.dogSize.displayName}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        BookingStatusBadge(status = appointment.status)
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.CalendarMonth,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = SpaDateUtils.formatShortSpanish(appointment.dateIso),
            style = MaterialTheme.typography.bodyMedium,
          )
          Spacer(modifier = Modifier.width(6.dp))
          Icon(
            imageVector = Icons.Rounded.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = appointment.timeSlotLabel,
            style = MaterialTheme.typography.bodyMedium,
          )
        }
        Text(
          text = SpaDateUtils.formatCop(appointment.priceCop),
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}
