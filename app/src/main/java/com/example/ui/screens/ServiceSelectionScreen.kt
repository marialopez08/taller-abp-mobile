package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.domain.model.SpaService
import com.example.ui.components.BookingStepHeader
import com.example.ui.components.ServiceShowcaseCard

@Composable
fun ServiceSelectionScreen(
  services: List<SpaService>,
  selectedService: SpaService?,
  onSelectService: (SpaService) -> Unit,
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
        .padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      BookingStepHeader(
        currentStep = 1,
        title = "Selecciona el servicio",
        subtitle = "Elige el cuidado ideal para tu perro. Los precios están en pesos colombianos (COP).",
      )

      services.forEach { service ->
        ServiceShowcaseCard(
          service = service,
          isSelected = selectedService?.code == service.code,
          onSelectClick = { onSelectService(service) },
          buttonTestTag = "booking_select_service_${service.code.code.lowercase()}",
        )
      }

      Spacer(modifier = Modifier.height(8.dp))
    }
  }
}
