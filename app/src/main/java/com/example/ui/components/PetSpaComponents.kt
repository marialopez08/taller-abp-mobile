package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Spa
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.BookingStatus
import com.example.domain.model.ServiceCode
import com.example.domain.model.SpaDateUtils
import com.example.domain.model.SpaService
import com.example.ui.theme.StatusCancelledBg
import com.example.ui.theme.StatusCancelledText
import com.example.ui.theme.StatusCompletedBg
import com.example.ui.theme.StatusCompletedText
import com.example.ui.theme.StatusConfirmedBg
import com.example.ui.theme.StatusConfirmedText

@DrawableRes
fun serviceDrawableRes(code: ServiceCode): Int =
  when (code) {
    ServiceCode.BANO_BASICO -> R.drawable.img_service_basic_bath
    ServiceCode.BANO_PREMIUM -> R.drawable.img_service_premium_bath
  }

@Composable
fun ServiceShowcaseCard(
  service: SpaService,
  onSelectClick: () -> Unit,
  modifier: Modifier = Modifier,
  isSelected: Boolean = false,
  buttonTestTag: String = "select_service_${service.code.code.lowercase()}",
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface,
    ),
    border = BorderStroke(
      width = if (isSelected) 2.dp else 1.dp,
      color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
  ) {
    Column {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(168.dp),
      ) {
        Image(
          painter = painterResource(id = serviceDrawableRes(service.code)),
          contentDescription = service.name,
          modifier = Modifier
            .fillMaxWidth()
            .height(168.dp),
          contentScale = ContentScale.Crop,
        )
        Surface(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(12.dp),
          shape = RoundedCornerShape(50),
          color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = if (service.code == ServiceCode.BANO_PREMIUM) Icons.Rounded.Spa else Icons.Rounded.Pets,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Desde ${SpaDateUtils.formatCop(service.basePriceCop)}",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }

      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = service.name,
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = service.description,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = "El valor final depende del tamaño de tu perro.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.tertiary,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Button(
          onClick = onSelectClick,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag(buttonTestTag),
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
          contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        ) {
          Text(
            text = "Seleccionar servicio",
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }
    }
  }
}

@Composable
fun BookingStepHeader(
  currentStep: Int,
  title: String,
  subtitle: String,
  onBack: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val stepLabels = listOf("Servicio", "Tamaño", "Agenda", "Datos")
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(bottom = 8.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      if (onBack != null) {
        IconButton(
          onClick = onBack,
          modifier = Modifier
            .size(48.dp)
            .testTag("booking_back_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Volver al paso anterior",
            tint = MaterialTheme.colorScheme.onBackground,
          )
        }
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      stepLabels.forEachIndexed { index, label ->
        val stepNum = index + 1
        val isDone = stepNum < currentStep
        val isActive = stepNum == currentStep
        val pillBg = when {
          isActive -> MaterialTheme.colorScheme.primary
          isDone -> MaterialTheme.colorScheme.primaryContainer
          else -> MaterialTheme.colorScheme.surfaceVariant
        }
        val pillText = when {
          isActive -> MaterialTheme.colorScheme.onPrimary
          isDone -> MaterialTheme.colorScheme.onPrimaryContainer
          else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        Surface(
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(14.dp),
          color = pillBg,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (isDone) {
              Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = pillText,
                modifier = Modifier.size(14.dp),
              )
              Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
              text = "$stepNum. $label",
              style = MaterialTheme.typography.labelSmall,
              color = pillText,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              fontWeight = if (isActive || isDone) FontWeight.Bold else FontWeight.Medium,
            )
          }
        }
      }
    }
  }
}

@Composable
fun BookingStatusBadge(
  status: BookingStatus,
  modifier: Modifier = Modifier,
) {
  val (bg, fg) = when (status) {
    BookingStatus.CONFIRMADA -> StatusConfirmedBg to StatusConfirmedText
    BookingStatus.CANCELADA -> StatusCancelledBg to StatusCancelledText
    BookingStatus.COMPLETADA -> StatusCompletedBg to StatusCompletedText
  }
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(50),
    color = bg,
  ) {
    Text(
      text = status.displayName,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Bold,
      color = fg,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
    )
  }
}

@Composable
fun LocalStorageNoticeBanner(
  text: String = "Tus reservas se almacenan localmente en este dispositivo. Aún no existe sincronización en tiempo real entre diferentes clientes.",
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Box(
        modifier = Modifier
          .size(30.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Outlined.Info,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.tertiary,
          modifier = Modifier.size(18.dp),
        )
      }
      Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
      )
    }
  }
}
