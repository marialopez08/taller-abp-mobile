package com.example.domain.model

enum class DogSize(
  val code: String,
  val displayName: String,
  val weightHint: String,
  val breedExamples: String,
) {
  PEQUENO(
    code = "PEQUENO",
    displayName = "Pequeño",
    weightHint = "Hasta 10 kg",
    breedExamples = "Ej. Chihuahua, Pomerania, Shih Tzu, Yorkshire",
  ),
  MEDIANO(
    code = "MEDIANO",
    displayName = "Mediano",
    weightHint = "Entre 10 y 25 kg",
    breedExamples = "Ej. Beagle, Cocker Spaniel, Bulldog Francés",
  ),
  GRANDE(
    code = "GRANDE",
    displayName = "Grande",
    weightHint = "Más de 25 kg",
    breedExamples = "Ej. Golden Retriever, Labrador, Pastor Alemán",
  );

  companion object {
    fun fromCode(code: String): DogSize =
      entries.firstOrNull { it.code == code } ?: MEDIANO
  }
}

enum class ServiceCode(val code: String) {
  BANO_BASICO("BANO_BASICO"),
  BANO_PREMIUM("BANO_PREMIUM");

  companion object {
    fun fromCode(code: String): ServiceCode =
      entries.firstOrNull { it.code == code } ?: BANO_BASICO
  }
}

enum class BookingStatus(val code: String, val displayName: String) {
  CONFIRMADA("CONFIRMADA", "Confirmada"),
  CANCELADA("CANCELADA", "Cancelada"),
  COMPLETADA("COMPLETADA", "Completada");

  companion object {
    fun fromCode(code: String): BookingStatus =
      entries.firstOrNull { it.code == code } ?: CONFIRMADA
  }
}

data class SpaService(
  val id: Long,
  val code: ServiceCode,
  val name: String,
  val description: String,
  val basePriceCop: Int,
  val pricesBySize: Map<DogSize, Int>,
) {
  fun priceFor(size: DogSize): Int = pricesBySize[size] ?: basePriceCop
}

data class TimeSlotDefinition(
  val id: String,
  val label: String,
  val hour24: Int,
)

object SpaSchedule {
  val ALL_SLOTS: List<TimeSlotDefinition> =
    listOf(
      TimeSlotDefinition("08:00", "8:00 a. m.", 8),
      TimeSlotDefinition("09:00", "9:00 a. m.", 9),
      TimeSlotDefinition("10:00", "10:00 a. m.", 10),
      TimeSlotDefinition("11:00", "11:00 a. m.", 11),
      TimeSlotDefinition("13:00", "1:00 p. m.", 13),
      TimeSlotDefinition("14:00", "2:00 p. m.", 14),
      TimeSlotDefinition("15:00", "3:00 p. m.", 15),
      TimeSlotDefinition("16:00", "4:00 p. m.", 16),
      TimeSlotDefinition("17:00", "5:00 p. m.", 17),
    )

  fun labelFor(slotId: String): String =
    ALL_SLOTS.firstOrNull { it.id == slotId }?.label ?: slotId

  fun hourFor(slotId: String): Int =
    ALL_SLOTS.firstOrNull { it.id == slotId }?.hour24 ?: 0
}

data class TimeSlotUi(
  val id: String,
  val label: String,
  val hour24: Int,
  val isAvailable: Boolean,
  val isPastHourToday: Boolean = false,
)

data class Appointment(
  val id: Long,
  val clientId: Long,
  val petId: Long,
  val clientName: String,
  val clientPhone: String,
  val petName: String,
  val petBreed: String,
  val serviceCode: ServiceCode,
  val serviceName: String,
  val dogSize: DogSize,
  val dateIso: String,
  val timeSlotId: String,
  val timeSlotLabel: String,
  val priceCop: Int,
  val status: BookingStatus,
  val createdAt: Long,
)

data class ClientProfile(
  val id: Long,
  val fullName: String,
  val phoneNumber: String,
)

data class PetProfile(
  val id: Long,
  val clientId: Long,
  val name: String,
  val breed: String,
  val size: DogSize,
)

sealed interface BookingCreationResult {
  data class Success(val appointment: Appointment) : BookingCreationResult
  data object SlotAlreadyTaken : BookingCreationResult
  data class InvalidData(val message: String) : BookingCreationResult
}
