package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.DogSize
import com.example.domain.model.SpaDateUtils
import com.example.domain.model.SpaService
import com.example.ui.components.BookingStepHeader
import com.example.ui.components.DogSizeIllustration

@Composable
fun DogSizeScreen(
  selectedService: SpaService,
  selectedDogSize: DogSize?,
  onSelectSize: (DogSize) -> Unit,
  onContinue: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBack)

  val scrollState = rememberScrollState()
  val finalPrice = selectedDogSize?.let { selectedService.priceFor(it) }

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
          .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        BookingStepHeader(
          currentStep = 2,
          title = "Tamaño de tu perro",
          subtitle = "Calculamos el precio exacto según el tamaño de tu mascota.",
          onBack = onBack,
        )

        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Spa,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Column {
              Text(
                text = "Servicio elegido",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
              Text(
                text = selectedService.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
            }
          }
        }

        DogSize.entries.forEach { size ->
          val isSelected = selectedDogSize == size
          val priceForSize = selectedService.priceFor(size)
          DogSizeOptionCard(
            dogSize = size,
            priceCop = priceForSize,
            isSelected = isSelected,
            onClick = { onSelectSize(size) },
          )
        }

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
            Text(
              text = "Precio final estimado",
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = if (finalPrice != null) SpaDateUtils.formatCop(finalPrice) else "Elige un tamaño",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.ExtraBold,
              color = if (finalPrice != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.testTag("dog_size_final_price_text"),
            )
          }

          Button(
            onClick = onContinue,
            enabled = selectedDogSize != null,
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("dog_size_continue_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
          ) {
            Text(
              text = "Elegir fecha y hora",
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
private fun DogSizeOptionCard(
  dogSize: DogSize,
  priceCop: Int,
  isSelected: Boolean,
  onClick: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(22.dp))
      .clickable(onClick = onClick)
      .testTag("dog_size_card_${dogSize.code.lowercase()}"),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
      } else {
        MaterialTheme.colorScheme.surface
      },
    ),
    border = BorderStroke(
      width = if (isSelected) 2.dp else 1.dp,
      color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      DogSizeIllustration(
        dogSize = dogSize,
        containerSize = 78.dp,
      )

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = dogSize.displayName,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
          if (isSelected) {
            Icon(
              imageVector = Icons.Rounded.CheckCircle,
              contentDescription = "Seleccionado",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(22.dp),
            )
          }
        }
        Text(
          text = dogSize.weightHint,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.secondary,
        )
        Text(
          text = dogSize.breedExamples,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
          Text(
            text = SpaDateUtils.formatCop(priceCop),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }
    }
  }
}
