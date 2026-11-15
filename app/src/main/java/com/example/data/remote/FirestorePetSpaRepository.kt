package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.repository.PetSpaRepository
import com.example.domain.model.Appointment
import com.example.domain.model.BookingCreationResult
import com.example.domain.model.BookingStatus
import com.example.domain.model.ClientProfile
import com.example.domain.model.DogSize
import com.example.domain.model.PetProfile
import com.example.domain.model.SpaCatalog
import com.example.domain.model.SpaDate
import com.example.domain.model.SpaDateUtils
import com.example.domain.model.SpaSchedule
import com.example.domain.model.SpaService
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private class SlotAlreadyTakenException : Exception("Slot already booked")

class FirestorePetSpaRepository(
  private val db: FirebaseFirestore,
  private val currentUserId: String,
  private val currentUserDisplayName: String = "",
  private val currentUserEmail: String = "",
) : PetSpaRepository {

  constructor(
    context: Context,
    currentUserId: String,
    currentUserDisplayName: String = "",
    currentUserEmail: String = "",
  ) : this(
    db = FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    currentUserId = currentUserId,
    currentUserDisplayName = currentUserDisplayName,
    currentUserEmail = currentUserEmail,
  )

  private fun userDocRef() = db.collection("users").document(currentUserId)

  private fun appointmentsCollection() = userDocRef().collection("appointments")

  private fun occupiedSlotDocRef(slotKey: String) = db.collection("occupied_slots").document(slotKey)

  override suspend fun ensureCatalogSeeded() {
    // Catálogo oficial provisto por SpaCatalog; no requiere escritura previa en Firestore.
  }

  override suspend fun refreshPastAppointmentsStatus(nowMillis: Long) {
    val path = "users/$currentUserId/appointments"
    try {
      val snapshot = appointmentsCollection()
        .whereEqualTo("statusCode", BookingStatus.CONFIRMADA.code)
        .get()
        .await()

      val docs = snapshot.toObjects(
        AppointmentDoc::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
      )
      for (doc in docs) {
        val date = SpaDate.fromIsoString(doc.dateIso) ?: continue
        val slotHour = SpaSchedule.hourFor(doc.timeSlotId)
        if (SpaDateUtils.isSlotInPast(date, slotHour, nowMillis)) {
          val apptRef = appointmentsCollection().document(doc.id)
          val slotRef = occupiedSlotDocRef(doc.slotKey)
          db.runTransaction { transaction ->
            val slotSnap = transaction.get(slotRef)
            transaction.update(
              apptRef,
              mapOf(
                "statusCode" to BookingStatus.COMPLETADA.code,
                "updatedAt" to FieldValue.serverTimestamp(),
              ),
            )
            if (slotSnap.exists() && slotSnap.getString("userId") == currentUserId) {
              transaction.delete(slotRef)
            }
            null
          }.await()
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
    }
  }

  override fun observeServices(): Flow<List<SpaService>> = flowOf(SpaCatalog.SERVICES)

  override fun observeAllAppointments(): Flow<List<Appointment>> {
    val path = "users/$currentUserId/appointments"
    return appointmentsCollection()
      .snapshots()
      .map { querySnapshot ->
        querySnapshot
          .toObjects(
            AppointmentDoc::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
          .map { it.toDomain() }
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError != null) {
          handleFirestoreError(firestoreError, OperationType.LIST, path)
          throw firestoreError
        }
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  override fun observeOccupiedSlotsForDate(dateIso: String): Flow<Set<String>> {
    val parsed = SpaDate.fromIsoString(dateIso) ?: return flowOf(emptySet())
    if (parsed.isSunday()) return flowOf(emptySet())

    val slotFlows = SpaSchedule.ALL_SLOTS.map { slot ->
      val key = SpaSchedule.slotKey(dateIso, slot.id)
      val path = "occupied_slots/$key"
      occupiedSlotDocRef(key)
        .snapshots()
        .map { snapshot -> if (snapshot.exists()) slot.id else null }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError != null) {
            handleFirestoreError(firestoreError, OperationType.GET, path)
            emit(null)
          } else if (error is CancellationException) {
            throw error
          } else {
            handleFirestoreError(error, OperationType.GET, path)
            emit(null)
          }
        }
    }

    return combine(slotFlows) { results ->
      results.filterNotNull().toSet()
    }
  }

  override fun observeLatestClientAndPet(): Flow<Pair<ClientProfile, PetProfile?>?> {
    val path = "users/$currentUserId"
    return userDocRef()
      .snapshots()
      .map { snapshot ->
        if (!snapshot.exists()) return@map null
        val profile = snapshot.toObject(
          UserProfileDoc::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        ) ?: return@map null
        val clientName = profile.lastClientName.trim()
        val clientPhone = profile.lastClientPhone.trim()
        if (clientName.isEmpty() || clientPhone.isEmpty()) return@map null

        val client = ClientProfile(
          id = currentUserId,
          fullName = clientName,
          phoneNumber = clientPhone,
        )
        val pet = if (profile.lastPetName.isNotBlank()) {
          PetProfile(
            id = "${currentUserId}_pet",
            clientId = currentUserId,
            name = profile.lastPetName.trim(),
            breed = profile.lastPetBreed.trim(),
            size = DogSize.MEDIANO,
          )
        } else {
          null
        }
        client to pet
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError != null) {
          handleFirestoreError(firestoreError, OperationType.GET, path)
        } else if (error is CancellationException) {
          throw error
        } else {
          handleFirestoreError(error, OperationType.GET, path)
        }
        emit(null)
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
    if (cleanPhone.filter { it.isDigit() }.length !in 7..15) {
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

    val appointmentId = UUID.randomUUID().toString()
    val slotKey = SpaSchedule.slotKey(dateIso, timeSlotId)
    val finalPrice = service.priceFor(dogSize)

    val appointmentRef = appointmentsCollection().document(appointmentId)
    val slotRef = occupiedSlotDocRef(slotKey)
    val profileRef = userDocRef()

    val appointmentData: Map<String, Any> = mapOf(
      "userId" to currentUserId,
      "clientName" to cleanName,
      "clientPhone" to cleanPhone,
      "petName" to cleanPet,
      "petBreed" to cleanBreed,
      "serviceCode" to service.code.code,
      "serviceName" to service.name,
      "dogSizeCode" to dogSize.code,
      "dateIso" to dateIso,
      "timeSlotId" to timeSlotId,
      "timeSlotLabel" to timeSlotLabel,
      "slotKey" to slotKey,
      "priceCop" to finalPrice.toLong(),
      "statusCode" to BookingStatus.CONFIRMADA.code,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )

    val slotData: Map<String, Any> = mapOf(
      "userId" to currentUserId,
      "appointmentId" to appointmentId,
      "dateIso" to dateIso,
      "timeSlotId" to timeSlotId,
      "createdAt" to FieldValue.serverTimestamp(),
    )

    return try {
      db.runTransaction { transaction ->
        val existingSlot = transaction.get(slotRef)
        if (existingSlot.exists()) {
          throw SlotAlreadyTakenException()
        }
        val existingProfile = transaction.get(profileRef)

        transaction.set(appointmentRef, appointmentData)
        transaction.set(slotRef, slotData)

        val profileUpdates = buildMap<String, Any> {
          if (currentUserDisplayName.isNotBlank()) {
            put("displayName", currentUserDisplayName.take(100))
          }
          if (currentUserEmail.isNotBlank()) {
            put("email", currentUserEmail.take(150))
          }
          put("lastClientName", cleanName.take(100))
          put("lastClientPhone", cleanPhone.take(30))
          put("lastPetName", cleanPet.take(80))
          put("lastPetBreed", cleanBreed.take(80))
          put("updatedAt", FieldValue.serverTimestamp())
        }

        if (!existingProfile.exists()) {
          val createProfile = profileUpdates.toMutableMap().apply {
            put("userId", currentUserId)
            put("createdAt", FieldValue.serverTimestamp())
          }
          transaction.set(profileRef, createProfile)
        } else {
          transaction.update(profileRef, profileUpdates)
        }
        null
      }.await()

      BookingCreationResult.Success(
        Appointment(
          id = appointmentId,
          clientId = currentUserId,
          petId = "${currentUserId}_pet",
          clientName = cleanName,
          clientPhone = cleanPhone,
          petName = cleanPet,
          petBreed = cleanBreed,
          serviceCode = service.code,
          serviceName = service.name,
          dogSize = dogSize,
          dateIso = dateIso,
          timeSlotId = timeSlotId,
          timeSlotLabel = timeSlotLabel,
          priceCop = finalPrice,
          status = BookingStatus.CONFIRMADA,
          createdAt = nowMillis,
        )
      )
    } catch (_: SlotAlreadyTakenException) {
      BookingCreationResult.SlotAlreadyTaken
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, appointmentRef.path)
      BookingCreationResult.InvalidData(
        "No pudimos guardar tu reserva en la nube. Verifica tu conexión e intenta nuevamente."
      )
    }
  }

  override suspend fun cancelAppointment(appointmentId: String) {
    if (appointmentId.isBlank()) return
    val apptRef = appointmentsCollection().document(appointmentId)
    try {
      db.runTransaction { transaction ->
        val apptSnap = transaction.get(apptRef)
        if (!apptSnap.exists()) return@runTransaction null
        if (apptSnap.getString("statusCode") != BookingStatus.CONFIRMADA.code) {
          return@runTransaction null
        }

        val slotKey = apptSnap.getString("slotKey").orEmpty()
        val slotSnap = if (slotKey.isNotBlank()) {
          transaction.get(occupiedSlotDocRef(slotKey))
        } else {
          null
        }

        transaction.update(
          apptRef,
          mapOf(
            "statusCode" to BookingStatus.CANCELADA.code,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        if (slotSnap != null && slotSnap.exists() && slotSnap.getString("userId") == currentUserId) {
          transaction.delete(occupiedSlotDocRef(slotKey))
        }
        null
      }.await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, apptRef.path)
    }
  }
}
