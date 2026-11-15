package com.example.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.example.domain.model.BookingStatus
import kotlinx.coroutines.flow.Flow

data class ServiceWithPrices(
  @Embedded val service: ServiceEntity,
  @Relation(
    parentColumn = "id",
    entityColumn = "serviceId",
  )
  val prices: List<ServicePriceEntity>,
)

@Dao
interface PetSpaDao {

  @Transaction
  @Query("SELECT * FROM services ORDER BY basePriceCop ASC")
  fun observeServicesWithPrices(): Flow<List<ServiceWithPrices>>

  @Query("SELECT COUNT(*) FROM services")
  suspend fun countServices(): Int

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertService(service: ServiceEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertServicePrices(prices: List<ServicePriceEntity>)

  @Query("SELECT * FROM clients ORDER BY createdAt DESC LIMIT 1")
  fun observeLatestClient(): Flow<ClientEntity?>

  @Query("SELECT * FROM pets WHERE clientId = :clientId ORDER BY createdAt DESC LIMIT 1")
  suspend fun getLatestPetForClient(clientId: Long): PetEntity?

  @Query("SELECT * FROM clients WHERE fullName = :fullName AND phoneNumber = :phoneNumber LIMIT 1")
  suspend fun findClient(fullName: String, phoneNumber: String): ClientEntity?

  @Insert
  suspend fun insertClient(client: ClientEntity): Long

  @Query("SELECT * FROM pets WHERE clientId = :clientId AND name = :name AND sizeCode = :sizeCode LIMIT 1")
  suspend fun findPet(clientId: Long, name: String, sizeCode: String): PetEntity?

  @Insert
  suspend fun insertPet(pet: PetEntity): Long

  @Query("SELECT * FROM appointments ORDER BY dateIso ASC, timeSlotId ASC")
  fun observeAllAppointments(): Flow<List<AppointmentEntity>>

  @Query("SELECT timeSlotId FROM appointments WHERE dateIso = :dateIso AND statusCode = :confirmedStatus")
  fun observeOccupiedSlotsForDate(
    dateIso: String,
    confirmedStatus: String = BookingStatus.CONFIRMADA.code,
  ): Flow<List<String>>

  @Query("SELECT dateIso, timeSlotId FROM appointments WHERE statusCode = :confirmedStatus")
  fun observeAllConfirmedSlotPairs(
    confirmedStatus: String = BookingStatus.CONFIRMADA.code,
  ): Flow<List<OccupiedSlotTuple>>

  @Query("SELECT COUNT(*) FROM appointments WHERE dateIso = :dateIso AND timeSlotId = :timeSlotId AND statusCode = :confirmedStatus")
  suspend fun countConfirmedForSlot(
    dateIso: String,
    timeSlotId: String,
    confirmedStatus: String = BookingStatus.CONFIRMADA.code,
  ): Int

  @Insert
  suspend fun insertAppointment(appointment: AppointmentEntity): Long

  @Query("SELECT * FROM appointments WHERE id = :id LIMIT 1")
  suspend fun getAppointmentById(id: Long): AppointmentEntity?

  @Query("UPDATE appointments SET statusCode = :newStatus WHERE id = :id")
  suspend fun updateAppointmentStatus(id: Long, newStatus: String)

  @Query("SELECT * FROM appointments WHERE statusCode = :confirmedStatus")
  suspend fun getConfirmedAppointments(
    confirmedStatus: String = BookingStatus.CONFIRMADA.code,
  ): List<AppointmentEntity>

  @Transaction
  suspend fun createBookingAtomically(
    clientName: String,
    clientPhone: String,
    petName: String,
    petBreed: String,
    dogSizeCode: String,
    serviceId: Long,
    serviceCode: String,
    serviceName: String,
    dateIso: String,
    timeSlotId: String,
    timeSlotLabel: String,
    priceCop: Int,
  ): AppointmentEntity? {
    val existing = countConfirmedForSlot(dateIso, timeSlotId)
    if (existing > 0) return null

    val client = findClient(clientName, clientPhone)
    val clientId = client?.id ?: insertClient(ClientEntity(fullName = clientName, phoneNumber = clientPhone))

    val pet = findPet(clientId, petName, dogSizeCode)
    val petId = pet?.id ?: insertPet(
      PetEntity(
        clientId = clientId,
        name = petName,
        breed = petBreed,
        sizeCode = dogSizeCode,
      )
    )

    val entity = AppointmentEntity(
      clientId = clientId,
      petId = petId,
      serviceId = serviceId,
      clientName = clientName,
      clientPhone = clientPhone,
      petName = petName,
      petBreed = petBreed,
      serviceCode = serviceCode,
      serviceName = serviceName,
      dogSizeCode = dogSizeCode,
      dateIso = dateIso,
      timeSlotId = timeSlotId,
      timeSlotLabel = timeSlotLabel,
      priceCop = priceCop,
      statusCode = BookingStatus.CONFIRMADA.code,
    )
    val insertedId = insertAppointment(entity)
    return entity.copy(id = insertedId)
  }
}

data class OccupiedSlotTuple(
  val dateIso: String,
  val timeSlotId: String,
)
