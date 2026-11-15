package com.example.data.repository

import com.example.domain.model.Appointment
import com.example.domain.model.BookingCreationResult
import com.example.domain.model.ClientProfile
import com.example.domain.model.DogSize
import com.example.domain.model.PetProfile
import com.example.domain.model.SpaService
import kotlinx.coroutines.flow.Flow

/**
 * Abstracción del repositorio de datos para PET SPA.
 * Implementado con Firebase Cloud Firestore y Firebase Authentication.
 */
interface PetSpaRepository {
  suspend fun ensureCatalogSeeded()
  suspend fun refreshPastAppointmentsStatus(nowMillis: Long = System.currentTimeMillis())

  fun observeServices(): Flow<List<SpaService>>
  fun observeAllAppointments(): Flow<List<Appointment>>
  fun observeOccupiedSlotsForDate(dateIso: String): Flow<Set<String>>
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

  suspend fun cancelAppointment(appointmentId: String)
}
