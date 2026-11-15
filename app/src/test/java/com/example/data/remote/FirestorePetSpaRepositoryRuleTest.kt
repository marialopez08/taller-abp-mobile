package com.example.data.remote

import com.example.base.FirestoreEmulatorTestBase
import com.example.domain.model.BookingCreationResult
import com.example.domain.model.BookingStatus
import com.example.domain.model.DogSize
import com.example.domain.model.ServiceCode
import com.example.domain.model.SpaCatalog
import com.example.domain.model.SpaDate
import com.example.domain.model.SpaDateUtils
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestorePetSpaRepositoryRuleTest : FirestoreEmulatorTestBase() {

  private fun nextUniqueWeekdayDateIso(offsetDays: Int): String {
    var date: SpaDate = SpaDateUtils.today().plusDays(offsetDays)
    if (date.isSunday()) date = date.plusDays(1)
    return date.toIsoString()
  }

  @Test
  fun createAppointment_validPayload_persistsAppointmentSlotAndProfile() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val repo = FirestorePetSpaRepository(firestore, aliceUid, "Alice Gómez", ALICE_EMAIL)
    val premium = SpaCatalog.SERVICES.first { it.code == ServiceCode.BANO_PREMIUM }
    val dateIso = nextUniqueWeekdayDateIso(12)

    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createAppointment(
        clientName = "Alice Gómez",
        clientPhone = "3001234567",
        petName = "Toby",
        petBreed = "Golden Retriever",
        dogSize = DogSize.GRANDE,
        service = premium,
        dateIso = dateIso,
        timeSlotId = "09:00",
        timeSlotLabel = "9:00 a. m.",
      )
    }
    assertTrue("Expected Success, got $result", result is BookingCreationResult.Success)
    val created = (result as BookingCreationResult.Success).appointment
    assertEquals(100_000, created.priceCop)
    assertEquals(BookingStatus.CONFIRMADA, created.status)

    val appointments = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeAllAppointments().first { list -> list.any { it.id == created.id } }
    }
    assertTrue(appointments.any { it.id == created.id && it.petName == "Toby" })

    val profilePair = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeLatestClientAndPet().first { it != null }
    }
    assertNotNull(profilePair)
    assertEquals("Alice Gómez", profilePair!!.first.fullName)
    assertEquals("Toby", profilePair.second?.name)

    // Cleanup slot lock so repeated runs stay idempotent
    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.cancelAppointment(created.id)
    }
  }

  @Test
  fun createAppointment_doubleBookingBySecondUser_returnsSlotAlreadyTaken_andCancellationFreesSlot() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = FirestorePetSpaRepository(firestore, aliceUid, "Alice", ALICE_EMAIL)
    val basic = SpaCatalog.SERVICES.first { it.code == ServiceCode.BANO_BASICO }
    val dateIso = nextUniqueWeekdayDateIso(15)

    val firstResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.createAppointment(
        clientName = "Alice",
        clientPhone = "3001112233",
        petName = "Luna",
        petBreed = "",
        dogSize = DogSize.MEDIANO,
        service = basic,
        dateIso = dateIso,
        timeSlotId = "10:00",
        timeSlotLabel = "10:00 a. m.",
      )
    }
    assertTrue(firstResult is BookingCreationResult.Success)
    val aliceAppt = (firstResult as BookingCreationResult.Success).appointment

    // Switch to Bob and observe slot is marked occupied
    val bobUid = signInTestUser(BOB_EMAIL)
    val bobRepo = FirestorePetSpaRepository(firestore, bobUid, "Bob", BOB_EMAIL)

    val occupied = withTimeout(FLOW_TIMEOUT_MS) {
      bobRepo.observeOccupiedSlotsForDate(dateIso).first { "10:00" in it }
    }
    assertTrue("10:00" in occupied)

    val duplicateAttempt = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.createAppointment(
        clientName = "Bob Pérez",
        clientPhone = "3117654321",
        petName = "Rocky",
        petBreed = "Beagle",
        dogSize = DogSize.PEQUENO,
        service = basic,
        dateIso = dateIso,
        timeSlotId = "10:00",
        timeSlotLabel = "10:00 a. m.",
      )
    }
    assertTrue(duplicateAttempt is BookingCreationResult.SlotAlreadyTaken)

    // Alice cancels her appointment -> slot becomes free
    signInTestUser(ALICE_EMAIL)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.cancelAppointment(aliceAppt.id)
    }

    signInTestUser(BOB_EMAIL)
    val freedSlots = withTimeout(FLOW_TIMEOUT_MS) {
      bobRepo.observeOccupiedSlotsForDate(dateIso).first { "10:00" !in it }
    }
    assertFalse("10:00" in freedSlots)

    val rebooked = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.createAppointment(
        clientName = "Bob Pérez",
        clientPhone = "3117654321",
        petName = "Rocky",
        petBreed = "Beagle",
        dogSize = DogSize.PEQUENO,
        service = basic,
        dateIso = dateIso,
        timeSlotId = "10:00",
        timeSlotLabel = "10:00 a. m.",
      )
    }
    assertTrue(rebooked is BookingCreationResult.Success)
    val bobAppt = (rebooked as BookingCreationResult.Success).appointment

    // Clean up Bob's slot lock
    withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.cancelAppointment(bobAppt.id)
    }
  }

  @Test
  fun crossUserAppointmentAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = FirestorePetSpaRepository(firestore, aliceUid, "Alice", ALICE_EMAIL)
    val basic = SpaCatalog.SERVICES.first { it.code == ServiceCode.BANO_BASICO }
    val dateIso = nextUniqueWeekdayDateIso(18)

    val created = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.createAppointment(
        clientName = "Alice Privado",
        clientPhone = "3005556677",
        petName = "Coco",
        petBreed = "Poodle",
        dogSize = DogSize.PEQUENO,
        service = basic,
        dateIso = dateIso,
        timeSlotId = "11:00",
        timeSlotLabel = "11:00 a. m.",
      )
    } as BookingCreationResult.Success

    // Switch to Bob and verify Bob cannot read Alice's appointments collection
    signInTestUser(BOB_EMAIL)
    val bobReadingAlice = runCatching {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("users").document(aliceUid)
          .collection("appointments")
          .get()
          .await()
      }
    }
    assertTrue(bobReadingAlice.isFailure)
    val firestoreException = bobReadingAlice.exceptionOrNull()?.unwrapFirestoreException()
    assertNotNull(firestoreException)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException!!.code)

    // Cleanup Alice's booking
    signInTestUser(ALICE_EMAIL)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.cancelAppointment(created.appointment.id)
    }
  }

  @Test
  fun observeAllAppointments_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = FirestorePetSpaRepository(firestore, "some_uid")

    val result = runCatching {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeAllAppointments().first()
      }
    }
    assertTrue(result.isFailure)
    val firestoreException = result.exceptionOrNull()?.unwrapFirestoreException()
    assertNotNull("Expected nested FirebaseFirestoreException, got ${result.exceptionOrNull()}", firestoreException)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException!!.code)
  }

  private companion object {
    const val ALICE_EMAIL = "alice_petspa@test.com"
    const val BOB_EMAIL = "bob_petspa@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
