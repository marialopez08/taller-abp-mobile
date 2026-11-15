package com.example.data.local

import com.example.domain.model.Appointment
import com.example.domain.model.BookingCreationResult
import com.example.domain.model.BookingStatus
import com.example.domain.model.ClientProfile
import com.example.domain.model.DogSize
import com.example.domain.model.PetProfile
import com.example.domain.model.ServiceCode
import com.example.domain.model.SpaDate
import com.example.domain.model.SpaDateUtils
import com.example.domain.model.SpaSchedule
import com.example.domain.model.SpaService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LocalRoomPetSpaRepository(
  private val dao: PetSpaDao,
) : PetSpaRepository {

  private val seedMutex = Mutex()

  override suspend fun ensureCatalogSeeded() {
    seedMutex.withLock {
      if (dao.countServices() > 0) return

      val basicId = dao.insertService(
        ServiceEntity(
          code = ServiceCode.BANO_BASICO.code,
          name = "Baño básico",
          description = "Baño completo para perros, limpieza general del pelaje, aplicación de productos especiales para mascotas y secado.",
          basePriceCop = 35_000,
        )
      )
      if (basicId > 0) {
        dao.insertServicePrices(
          listOf(
            ServicePriceEntity(serviceId = basicId, dogSizeCode = DogSize.PEQUENO.code, priceCop = 35_000),
            ServicePriceEntity(serviceId = basicId, dogSizeCode = DogSize.MEDIANO.code, priceCop = 45_000),
            ServicePriceEntity(serviceId = basicId, dogSizeCode = DogSize.GRANDE.code, priceCop = 55_000),
          )
        )
      }

      val premiumId = dao.insertService(
        ServiceEntity(
          code = ServiceCode.BANO_PREMIUM.code,
          name = "Baño premium",
          description = "Baño completo, limada de uñas, corte de pelo y arreglo estético de la mascota.",
          basePriceCop = 70_000,
        )
      )
      if (premiumId > 0) {
        dao.insertServicePrices(
          listOf(
            ServicePriceEntity(serviceId = premiumId, dogSizeCode = DogSize.PEQUENO.code, priceCop = 70_000),
            ServicePriceEntity(serviceId = premiumId, dogSizeCode = DogSize.MEDIANO.code, priceCop = 85_000),
            ServicePriceEntity(serviceId = premiumId, dogSizeCode = DogSize.GRANDE.code, priceCop = 100_000),
          )
        )
      }
    }
  }

  override suspend fun refreshPastAppointmentsStatus(nowMillis: Long) {
    val confirmed = dao.getConfirmedAppointments()
    for (appt in confirmed) {
      val date = SpaDate.fromIsoString(appt.dateIso) ?: continue
      val slotHour = SpaSchedule.hourFor(appt.timeSlotId)
      if (SpaDateUtils.isSlotInPast(date, slotHour, nowMillis)) {
        dao.updateAppointmentStatus(appt.id, BookingStatus.COMPLETADA.code)
      }
    }
  }

  override fun observeServices(): Flow<List<SpaService>> =
    dao.observeServicesWithPrices().map { list ->
      list.map { item ->
        val priceMap = item.prices.associate { priceEntity ->
          DogSize.fromCode(priceEntity.dogSizeCode) to priceEntity.priceCop
        }
        SpaService(
          id = item.service.id,
          code = ServiceCode.fromCode(item.service.code),
          name = item.service.name,
          description = item.service.description,
          basePriceCop = item.service.basePriceCop,
          pricesBySize = priceMap,
        )
      }
    }

  override fun observeAllAppointments(): Flow<List<Appointment>> =
    dao.observeAllAppointments().map { entities ->
      entities.map { it.toDomain() }
    }

  override fun observeOccupiedSlotsByDate(): Flow<Map<String, Set<String>>> =
    dao.observeAllConfirmedSlotPairs().map { pairs ->
      pairs.groupBy(keySelector = { it.dateIso }, valueTransform = { it.timeSlotId })
        .mapValues { (_, slots) -> slots.toSet() }
    }

  override fun observeLatestClientAndPet(): Flow<Pair<ClientProfile, PetProfile?>?> =
    dao.observeLatestClient().map { client ->
      if (client == null) {
        null
      } else {
        val pet = dao.getLatestPetForClient(client.id)
        ClientProfile(client.id, client.fullName, client.phoneNumber) to pet?.let {
          PetProfile(it.id, it.clientId, it.name, it.breed, DogSize.fromCode(it.sizeCode))
        }
      }
    }

  override suspend fun createAppointment(
    clientName: String,
    clientPhone: String,
    petName: String,
    petBreed: String,
    dogSize: DogSize,
    service: SpaService,
    dateIso: String,
    timeSlotId: String,
    timeSlotLabel: String,
    nowMillis: Long,
  ): BookingCreationResult {
    val cleanName = clientName.trim()
    val cleanPhone = clientPhone.trim()
    val cleanPet = petName.trim()
    val cleanBreed = petBreed.trim()

    if (cleanName.length < 2) {
      return BookingCreationResult.InvalidData("Ingresa el nombre del cliente.")
    }
    if (cleanPhone.filter { it.isDigit() }.length < 7) {
      return BookingCreationResult.InvalidData("Ingresa un número de celular válido.")
    }
    if (cleanPet.isEmpty()) {
      return BookingCreationResult.InvalidData("Ingresa el nombre de tu mascota.")
    }

    val parsedDate = SpaDate.fromIsoString(dateIso)
      ?: return BookingCreationResult.InvalidData("Fecha inválida.")
    if (parsedDate.isSunday()) {
      return BookingCreationResult.InvalidData("Los domingos son días de descanso.")
    }
    val slotHour = SpaSchedule.hourFor(timeSlotId)
    if (SpaDateUtils.isSlotInPast(parsedDate, slotHour, nowMillis)) {
      return BookingCreationResult.InvalidData("No puedes reservar en una fecha u hora anterior a la actual.")
    }

    val finalPrice = service.priceFor(dogSize)
    val created = dao.createBookingAtomically(
      clientName = cleanName,
      clientPhone = cleanPhone,
      petName = cleanPet,
      petBreed = cleanBreed,
      dogSizeCode = dogSize.code,
      serviceId = service.id,
      serviceCode = service.code.code,
      serviceName = service.name,
      dateIso = dateIso,
      timeSlotId = timeSlotId,
      timeSlotLabel = timeSlotLabel,
      priceCop = finalPrice,
    )
    return if (created != null) {
      BookingCreationResult.Success(created.toDomain())
    } else {
      BookingCreationResult.SlotAlreadyTaken
    }
  }

  override suspend fun cancelAppointment(appointmentId: Long) {
    dao.updateAppointmentStatus(appointmentId, BookingStatus.CANCELADA.code)
  }

  private fun AppointmentEntity.toDomain(): Appointment =
    Appointment(
      id = id,
      clientId = clientId,
      petId = petId,
      clientName = clientName,
      clientPhone = clientPhone,
      petName = petName,
      petBreed = petBreed,
      serviceCode = ServiceCode.fromCode(serviceCode),
      serviceName = serviceName,
      dogSize = DogSize.fromCode(dogSizeCode),
      dateIso = dateIso,
      timeSlotId = timeSlotId,
      timeSlotLabel = timeSlotLabel,
      priceCop = priceCop,
      status = BookingStatus.fromCode(statusCode),
      createdAt = createdAt,
    )
}
