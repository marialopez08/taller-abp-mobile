package com.example.data.local

import com.example.domain.model.Appointment
import com.example.domain.model.BookingCreationResult
import com.example.domain.model.ClientProfile
import com.example.domain.model.DogSize
import com.example.domain.model.PetProfile
import com.example.domain.model.SpaService
import kotlinx.coroutines.flow.Flow

/**
 * Abstracción del repositorio de datos para PET SPA.
 * Permite reemplazar el almacenamiento local Room por Firebase Firestore u otro backend
 * en futuras versiones sin modificar los ViewModels ni la interfaz de usuario.
 */
interface PetSpaRepository {
  suspend fun ensureCatalogSeeded()
  suspend fun refreshPastAppointmentsStatus(nowMillis: Long = System.currentTimeMillis())

  fun observeServices(): Flow<List<SpaService>>
  fun observeAllAppointments(): Flow<List<Appointment>>
  fun observeOccupiedSlotsByDate(): Flow<Map<String, Set<String>>>
  fun observeLatestClientAndPet(): Flow<Pair<ClientProfile, PetProfile?>?>

  suspend fun createAppointment(
    clientName: String,
    clientPhone: String,
    petName: String,
    petBreed: String,
    dogSize: DogSize,
    service: SpaService,
    dateIso: String,
    timeSlotId: String,
    timeSlotLabel: String,
    nowMillis: Long = System.currentTimeMillis(),
  ): BookingCreationResult

  suspend fun cancelAppointment(appointmentId: Long)
}
