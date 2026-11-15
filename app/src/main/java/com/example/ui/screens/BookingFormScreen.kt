package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.model.ClientProfile
import com.example.domain.model.PetProfile
import com.example.domain.model.SpaDateUtils
import com.example.domain.model.SpaSchedule
import com.example.ui.BookingDraftState
import com.example.ui.components.BookingStepHeader

@Composable
fun BookingFormScreen(
  draft: BookingDraftState,
  latestClientAndPet: Pair<ClientProfile, PetProfile?>?,
  onClientNameChange: (String) -> Unit,
  onClientPhoneChange: (String) -> Unit,
  onPetNameChange: (String) -> Unit,
  onPetBreedChange: (String) -> Unit,
  onApplySavedProfile: (ClientProfile, PetProfile?) -> Unit,
  onConfirmBooking: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBack)

  val scrollState = rememberScrollState()
  val service = draft.selectedService
  val dogSize = draft.selectedDogSize
  val slotId = draft.selectedSlotId
  val finalPrice = draft.calculatedPriceCop ?: 0

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 720.dp)
        .fillMaxSize()
        .imePadding()
        .verticalScroll(scrollState)
        .padding(horizontal = 20.dp, vertical = 16.dp)
        .testTag("booking_form_lazy_column"),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      BookingStepHeader(
        currentStep = 4,
        title = "Datos y resumen",
        subtitle = "Ingresa tus datos y revisa el detalle antes de confirmar tu cita.",
        onBack = onBack,
      )

      if (latestClientAndPet != null && draft.clientName.isBlank()) {
        val (savedClient, savedPet) = latestClientAndPet
        AssistChip(
          onClick = { onApplySavedProfile(savedClient, savedPet) },
          label = {
            Text(
              text = if (savedPet != null) {
                "Usar datos guardados: ${savedClient.fullName} y ${savedPet.name}"
              } else {
                "Usar datos guardados: ${savedClient.fullName}"
              },
            )
          },
          leadingIcon = {
            Icon(
              imageVector = Icons.Rounded.AutoAwesome,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
          },
          modifier = Modifier.testTag("use_saved_profile_chip"),
        )
      }

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Text(
            text = "Información del cliente y mascota",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )

          OutlinedTextField(
            value = draft.clientName,
            onValueChange = onClientNameChange,
            label = { Text("Nombre del cliente *") },
            placeholder = { Text("Ej. Laura Gómez") },
            leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
            isError = draft.clientNameError != null,
            supportingText = draft.clientNameError?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
              capitalization = KeyboardCapitalization.Words,
              imeAction = ImeAction.Next,
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_client_name"),
          )

          OutlinedTextField(
            value = draft.clientPhone,
            onValueChange = onClientPhoneChange,
            label = { Text("Número de celular *") },
            placeholder = { Text("Ej. 3001234567") },
            leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
            isError = draft.clientPhoneError != null,
            supportingText = draft.clientPhoneError?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Phone,
              imeAction = ImeAction.Next,
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_client_phone"),
          )

          OutlinedTextField(
            value = draft.petName,
            onValueChange = onPetNameChange,
            label = { Text("Nombre de la mascota *") },
            placeholder = { Text("Ej. Toby") },
            leadingIcon = { Icon(Icons.Rounded.Pets, contentDescription = null) },
            isError = draft.petNameError != null,
            supportingText = draft.petNameError?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
              capitalization = KeyboardCapitalization.Words,
              imeAction = ImeAction.Next,
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_pet_name"),
          )

          OutlinedTextField(
            value = draft.petBreed,
            onValueChange = onPetBreedChange,
            label = { Text("Raza de la mascota (opcional)") },
            placeholder = { Text("Ej. Golden Retriever, Criollo") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
              capitalization = KeyboardCapitalization.Words,
              imeAction = ImeAction.Done,
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_pet_breed"),
          )
        }
      }

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "Resumen de tu reserva",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          SummaryRow(
            label = "Cliente",
            value = draft.clientName.trim().ifEmpty { "Por ingresar" },
          )
          SummaryRow(
            label = "Mascota",
            value = buildString {
              append(draft.petName.trim().ifEmpty { "Por ingresar" })
              if (draft.petBreed.trim().isNotEmpty()) {
                append(" (${draft.petBreed.trim()})")
              }
            },
          )
          SummaryRow(
            label = "Servicio",
            value = service?.name ?: "No seleccionado",
          )
          SummaryRow(
            label = "Tamaño del perro",
            value = dogSize?.displayName ?: "No seleccionado",
          )
          SummaryRow(
            label = "Fecha de la cita",
            value = SpaDateUtils.formatReadableSpanish(draft.selectedDate),
          )
          SummaryRow(
            label = "Hora de la cita",
            value = slotId?.let { SpaSchedule.labelFor(it) } ?: "No seleccionada",
          )

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Precio final",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = SpaDateUtils.formatCop(finalPrice),
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.testTag("summary_final_price"),
            )
          }
        }
      }

      if (draft.submissionError != null) {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.errorContainer,
        ) {
          Text(
            text = draft.submissionError,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(14.dp),
          )
        }
      }

      Button(
        onClick = onConfirmBooking,
        enabled = !draft.isSubmitting,
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .testTag("confirm_booking_button"),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
      ) {
        if (draft.isSubmitting) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.5.dp,
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text("Guardando reserva...")
        } else {
          Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "CONFIRMAR RESERVA",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@Composable
private fun SummaryRow(
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
