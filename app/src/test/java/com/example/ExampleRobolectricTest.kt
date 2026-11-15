package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.PetSpaRepository
import com.example.domain.model.Appointment
import com.example.domain.model.BookingCreationResult
import com.example.domain.model.BookingStatus
import com.example.domain.model.ClientProfile
import com.example.domain.model.DogSize
import com.example.domain.model.PetProfile
import com.example.domain.model.ServiceCode
import com.example.domain.model.SpaCatalog
import com.example.domain.model.SpaDate
import com.example.domain.model.SpaDateUtils
import com.example.domain.model.SpaService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `app name matches PET SPA`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("PET SPA", appName)
  }

  @Test
  fun `catalog defines services and exact COP prices`() = runBlocking {
    val repo = InMemoryTestPetSpaRepository()
    val services = repo.observeServices().first()
    assertEquals(2, services.size)

    val basic = services.first { it.code == ServiceCode.BANO_BASICO }
    assertEquals(35_000, basic.priceFor(DogSize.PEQUENO))
    assertEquals(45_000, basic.priceFor(DogSize.MEDIANO))
    assertEquals(55_000, basic.priceFor(DogSize.GRANDE))

    val premium = services.first { it.code == ServiceCode.BANO_PREMIUM }
    assertEquals(70_000, premium.priceFor(DogSize.PEQUENO))
    assertEquals(85_000, premium.priceFor(DogSize.MEDIANO))
    assertEquals(100_000, premium.priceFor(DogSize.GRANDE))

    val appointments = repo.observeAllAppointments().first()
    assertTrue(appointments.isEmpty())
  }

  @Test
  fun `sunday and cop formatting rules work as expected`() {
    assertEquals("$35.000 COP", SpaDateUtils.formatCop(35_000))
    assertEquals("$100.000 COP", SpaDateUtils.formatCop(100_000))

    val sunday = SpaDate(2026, 11, 15)
    assertTrue(sunday.isSunday())

    val saturday = SpaDate(2026, 11, 14)
    assertFalse(saturday.isSunday())
  }
}

class InMemoryTestPetSpaRepository : PetSpaRepository {
  private val appointmentsState = MutableStateFlow<List<Appointment>>(emptyList())
  private val profileState = MutableStateFlow<Pair<ClientProfile, PetProfile?>?>(null)

  override suspend fun ensureCatalogSeeded() = Unit

  override suspend fun refreshPastAppointmentsStatus(nowMillis: Long) = Unit

  override fun observeServices(): Flow<List<SpaService>> = flowOf(SpaCatalog.SERVICES)

  override fun observeAllAppointments(): Flow<List<Appointment>> = appointmentsState

  override fun observeOccupiedSlotsForDate(dateIso: String): Flow<Set<String>> =
    appointmentsState.map { list ->
      list.filter { it.dateIso == dateIso && it.status == BookingStatus.CONFIRMADA }
        .map { it.timeSlotId }
        .toSet()
    }

  override fun observeLatestClientAndPet(): Flow<Pair<ClientProfile, PetProfile?>?> = profileState

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
    val alreadyTaken = appointmentsState.value.any {
      it.dateIso == dateIso && it.timeSlotId == timeSlotId && it.status == BookingStatus.CONFIRMADA
    }
    if (alreadyTaken) return BookingCreationResult.SlotAlreadyTaken

    val created = Appointment(
      id = UUID.randomUUID().toString(),
      clientId = "test_user",
      petId = "test_pet",
      clientName = clientName.trim(),
      clientPhone = clientPhone.trim(),
      petName = petName.trim(),
      petBreed = petBreed.trim(),
      serviceCode = service.code,
      serviceName = service.name,
      dogSize = dogSize,
      dateIso = dateIso,
      timeSlotId = timeSlotId,
      timeSlotLabel = timeSlotLabel,
      priceCop = service.priceFor(dogSize),
      status = BookingStatus.CONFIRMADA,
      createdAt = nowMillis,
    )
    appointmentsState.value = appointmentsState.value + created
    profileState.value = ClientProfile("test_user", created.clientName, created.clientPhone) to
      PetProfile("test_pet", "test_user", created.petName, created.petBreed, dogSize)
    return BookingCreationResult.Success(created)
  }

  override suspend fun cancelAppointment(appointmentId: String) {
    appointmentsState.value = appointmentsState.value.map { appt ->
      if (appt.id == appointmentId) appt.copy(status = BookingStatus.CANCELADA) else appt
    }
  }
}
