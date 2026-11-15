package com.example.data.remote

import com.example.domain.model.Appointment
import com.example.domain.model.BookingStatus
import com.example.domain.model.DogSize
import com.example.domain.model.ServiceCode
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfileDoc(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val email: String = "",
  val lastClientName: String = "",
  val lastClientPhone: String = "",
  val lastPetName: String = "",
  val lastPetBreed: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class AppointmentDoc(
  @DocumentId val id: String = "",
  val userId: String = "",
  val clientName: String = "",
  val clientPhone: String = "",
  val petName: String = "",
  val petBreed: String = "",
  val serviceCode: String = "",
  val serviceName: String = "",
  val dogSizeCode: String = "",
  val dateIso: String = "",
  val timeSlotId: String = "",
  val timeSlotLabel: String = "",
  val slotKey: String = "",
  val priceCop: Long = 0L,
  val statusCode: String = BookingStatus.CONFIRMADA.code,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toDomain(): Appointment =
    Appointment(
      id = id,
      clientId = userId,
      petId = "${userId}_pet",
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
      priceCop = priceCop.toInt(),
      status = BookingStatus.fromCode(statusCode),
      createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis(),
    )
}

@IgnoreExtraProperties
data class OccupiedSlotDoc(
  @DocumentId val id: String = "",
  val userId: String = "",
  val appointmentId: String = "",
  val dateIso: String = "",
  val timeSlotId: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)
