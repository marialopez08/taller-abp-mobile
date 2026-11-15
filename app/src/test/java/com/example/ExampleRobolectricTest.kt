package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LocalRoomPetSpaRepository
import com.example.data.local.PetSpaDatabase
import com.example.domain.model.BookingCreationResult
import com.example.domain.model.BookingStatus
import com.example.domain.model.DogSize
import com.example.domain.model.ServiceCode
import com.example.domain.model.SpaDate
import com.example.domain.model.SpaDateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var db: PetSpaDatabase
  private lateinit var repository: LocalRoomPetSpaRepository

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, PetSpaDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repository = LocalRoomPetSpaRepository(db.petSpaDao())
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun `app name matches PET SPA`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("PET SPA", appName)
  }

  @Test
  fun `catalog seeds services and exact COP prices with zero fake bookings`() = runBlocking {
    repository.ensureCatalogSeeded()

    val services = repository.observeServices().first()
    assertEquals(2, services.size)

    val basic = services.first { it.code == ServiceCode.BANO_BASICO }
    assertEquals(35_000, basic.priceFor(DogSize.PEQUENO))
    assertEquals(45_000, basic.priceFor(DogSize.MEDIANO))
    assertEquals(55_000, basic.priceFor(DogSize.GRANDE))

    val premium = services.first { it.code == ServiceCode.BANO_PREMIUM }
    assertEquals(70_000, premium.priceFor(DogSize.PEQUENO))
    assertEquals(85_000, premium.priceFor(DogSize.MEDIANO))
    assertEquals(100_000, premium.priceFor(DogSize.GRANDE))

    val appointments = repository.observeAllAppointments().first()
    assertTrue(appointments.isEmpty())
  }

  @Test
  fun `booking prevents double booking and cancellation frees slot`() = runBlocking {
    repository.ensureCatalogSeeded()
    val services = repository.observeServices().first()
    val premium = services.first { it.code == ServiceCode.BANO_PREMIUM }

    // Find a future non-Sunday date
    var targetDate = SpaDateUtils.today().plusDays(2)
    if (targetDate.isSunday()) targetDate = targetDate.plusDays(1)
    val dateIso = targetDate.toIsoString()

    val firstResult = repository.createAppointment(
      clientName = "Laura Gómez",
      clientPhone = "3001234567",
      petName = "Toby",
      petBreed = "Golden Retriever",
      dogSize = DogSize.GRANDE,
      service = premium,
      dateIso = dateIso,
      timeSlotId = "10:00",
      timeSlotLabel = "10:00 a. m.",
    )
    assertTrue(firstResult is BookingCreationResult.Success)
    val created = (firstResult as BookingCreationResult.Success).appointment
    assertEquals(100_000, created.priceCop)
    assertEquals(BookingStatus.CONFIRMADA, created.status)

    // Attempt duplicate booking on same date & time slot
    val duplicateResult = repository.createAppointment(
      clientName = "Carlos Ruiz",
      clientPhone = "3117654321",
      petName = "Luna",
      petBreed = "",
      dogSize = DogSize.PEQUENO,
      service = premium,
      dateIso = dateIso,
      timeSlotId = "10:00",
      timeSlotLabel = "10:00 a. m.",
    )
    assertTrue(duplicateResult is BookingCreationResult.SlotAlreadyTaken)

    // Cancel first booking -> slot should become available again
    repository.cancelAppointment(created.id)
    val occupiedAfterCancel = repository.observeOccupiedSlotsByDate().first()[dateIso].orEmpty()
    assertFalse("10:00" in occupiedAfterCancel)

    val rebookedResult = repository.createAppointment(
      clientName = "Carlos Ruiz",
      clientPhone = "3117654321",
      petName = "Luna",
      petBreed = "",
      dogSize = DogSize.PEQUENO,
      service = premium,
      dateIso = dateIso,
      timeSlotId = "10:00",
      timeSlotLabel = "10:00 a. m.",
    )
    assertTrue(rebookedResult is BookingCreationResult.Success)
  }

  @Test
  fun `sunday and cop formatting rules work as expected`() {
    assertEquals("$35.000 COP", SpaDateUtils.formatCop(35_000))
    assertEquals("$100.000 COP", SpaDateUtils.formatCop(100_000))

    // 2026-11-15 is a Sunday
    val sunday = SpaDate(2026, 11, 15)
    assertTrue(sunday.isSunday())

    // 2026-11-14 is a Saturday
    val saturday = SpaDate(2026, 11, 14)
    assertFalse(saturday.isSunday())
  }
}
