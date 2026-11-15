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
  val id: String,
  val code: ServiceCode,
  val name: String,
  val description: String,
  val basePriceCop: Int,
  val pricesBySize: Map<DogSize, Int>,
) {
  fun priceFor(size: DogSize): Int = pricesBySize[size] ?: basePriceCop
}

object SpaCatalog {
  val SERVICES: List<SpaService> = listOf(
    SpaService(
      id = "bano_basico",
      code = ServiceCode.BANO_BASICO,
      name = "Baño básico",
      description = "Baño completo para perros, limpieza general del pelaje, aplicación de productos especiales para mascotas y secado.",
      basePriceCop = 35_000,
      pricesBySize = mapOf(
        DogSize.PEQUENO to 35_000,
        DogSize.MEDIANO to 45_000,
        DogSize.GRANDE to 55_000,
      ),
    ),
    SpaService(
      id = "bano_premium",
      code = ServiceCode.BANO_PREMIUM,
      name = "Baño premium",
      description = "Baño completo, limada de uñas, corte de pelo y arreglo estético de la mascota.",
      basePriceCop = 70_000,
      pricesBySize = mapOf(
        DogSize.PEQUENO to 70_000,
        DogSize.MEDIANO to 85_000,
        DogSize.GRANDE to 100_000,
      ),
    ),
  )
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

  fun slotKey(dateIso: String, timeSlotId: String): String =
    "${dateIso}_${timeSlotId.replace(':', '-')}"
}

data class TimeSlotUi(
  val id: String,
  val label: String,
  val hour24: Int,
  val isAvailable: Boolean,
  val isPastHourToday: Boolean = false,
)

data class Appointment(
  val id: String,
  val clientId: String,
  val petId: String,
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
  val id: String,
  val fullName: String,
  val phoneNumber: String,
)

data class PetProfile(
  val id: String,
  val clientId: String,
  val name: String,
  val breed: String,
  val size: DogSize,
)

sealed interface BookingCreationResult {
  data class Success(val appointment: Appointment) : BookingCreationResult
  data object SlotAlreadyTaken : BookingCreationResult
  data class InvalidData(val message: String) : BookingCreationResult
}
