package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.PetSpaRepository
import com.example.domain.model.Appointment
import com.example.domain.model.BookingCreationResult
import com.example.domain.model.BookingStatus
import com.example.domain.model.ClientProfile
import com.example.domain.model.DogSize
import com.example.domain.model.PetProfile
import com.example.domain.model.SpaDate
import com.example.domain.model.SpaDateUtils
import com.example.domain.model.SpaSchedule
import com.example.domain.model.SpaService
import com.example.domain.model.SpaYearMonth
import com.example.domain.model.TimeSlotUi
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BookingsFilter(val label: String) {
  TODAS("Todas"),
  CONFIRMADAS("Confirmadas"),
  COMPLETADAS("Completadas"),
  CANCELADAS("Canceladas"),
}

data class BookingDraftState(
  val selectedService: SpaService? = null,
  val selectedDogSize: DogSize? = null,
  val displayedMonth: SpaYearMonth = SpaDateUtils.currentYearMonth(),
  val selectedDate: SpaDate = SpaDateUtils.findFirstBookableDate(),
  val selectedSlotId: String? = null,
  val clientName: String = "",
  val clientPhone: String = "",
  val petName: String = "",
  val petBreed: String = "",
  val clientNameError: String? = null,
  val clientPhoneError: String? = null,
  val petNameError: String? = null,
  val submissionError: String? = null,
  val isSubmitting: Boolean = false,
  val confirmedAppointment: Appointment? = null,
) {
  val calculatedPriceCop: Int?
    get() = if (selectedService != null && selectedDogSize != null) {
      selectedService.priceFor(selectedDogSize)
    } else {
      null
    }
}

class PetSpaViewModel(
  private val repository: PetSpaRepository,
) : ViewModel() {

  private val _bookingDraft = MutableStateFlow(BookingDraftState())
  val bookingDraft: StateFlow<BookingDraftState> = _bookingDraft.asStateFlow()

  private val _bookingsFilter = MutableStateFlow(BookingsFilter.TODAS)
  val bookingsFilter: StateFlow<BookingsFilter> = _bookingsFilter.asStateFlow()

  val services: StateFlow<List<SpaService>> =
    repository.observeServices()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

  val allAppointments: StateFlow<List<Appointment>> =
    repository.observeAllAppointments()
      .map { list -> sortAppointmentsUpcomingFirst(list) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        Log.w(TAG, "Error observing appointments", firestoreError ?: error)
        emit(emptyList())
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

  @OptIn(ExperimentalCoroutinesApi::class)
  private val occupiedSlotsForSelectedDate: StateFlow<Pair<String, Set<String>>> =
    _bookingDraft
      .map { it.selectedDate.toIsoString() }
      .distinctUntilChanged()
      .flatMapLatest { dateIso ->
        repository.observeOccupiedSlotsForDate(dateIso).map { slots -> dateIso to slots }
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        Log.w(TAG, "Error observing occupied slots", firestoreError ?: error)
        emit(_bookingDraft.value.selectedDate.toIsoString() to emptySet())
      }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        _bookingDraft.value.selectedDate.toIsoString() to emptySet(),
      )

  val occupiedSlotsByDate: StateFlow<Map<String, Set<String>>> =
    combine(allAppointments, occupiedSlotsForSelectedDate) { appointments, (activeDateIso, activeDaySlots) ->
      val map = appointments
        .filter { it.status == BookingStatus.CONFIRMADA }
        .groupBy(keySelector = { it.dateIso }, valueTransform = { it.timeSlotId })
        .mapValues { (_, list) -> list.toSet() }
        .toMutableMap()
      map[activeDateIso] = map[activeDateIso].orEmpty() + activeDaySlots
      map.toMap()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyMap())

  val upcomingAppointments: StateFlow<List<Appointment>> =
    allAppointments
      .map { list -> list.filter { it.status == BookingStatus.CONFIRMADA } }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

  val filteredAppointments: StateFlow<List<Appointment>> =
    combine(allAppointments, _bookingsFilter) { list, filter ->
      when (filter) {
        BookingsFilter.TODAS -> list
        BookingsFilter.CONFIRMADAS -> list.filter { it.status == BookingStatus.CONFIRMADA }
        BookingsFilter.COMPLETADAS -> list.filter { it.status == BookingStatus.COMPLETADA }
        BookingsFilter.CANCELADAS -> list.filter { it.status == BookingStatus.CANCELADA }
      }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

  val latestClientAndPet: StateFlow<Pair<ClientProfile, PetProfile?>?> =
    repository.observeLatestClientAndPet()
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        Log.w(TAG, "Error observing latest client profile", firestoreError ?: error)
        emit(null)
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

  val timeSlotsForSelectedDate: StateFlow<List<TimeSlotUi>> =
    combine(_bookingDraft, occupiedSlotsByDate) { draft, occupiedMap ->
      buildTimeSlotsForDate(draft.selectedDate, occupiedMap)
    }.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      buildTimeSlotsForDate(_bookingDraft.value.selectedDate, emptyMap()),
    )

  init {
    viewModelScope.launch {
      repository.ensureCatalogSeeded()
      repository.refreshPastAppointmentsStatus()
      val initialDate = SpaDateUtils.findFirstBookableDate(
        occupiedSlotsByDate = occupiedSlotsByDate.value,
      )
      _bookingDraft.update {
        it.copy(
          selectedDate = initialDate,
          displayedMonth = SpaYearMonth(initialDate.year, initialDate.month),
        )
      }
    }
  }

  fun startNewBooking(preselectedService: SpaService? = null) {
    val firstDate = SpaDateUtils.findFirstBookableDate(
      occupiedSlotsByDate = occupiedSlotsByDate.value,
    )
    _bookingDraft.value = BookingDraftState(
      selectedService = preselectedService,
      displayedMonth = SpaYearMonth(firstDate.year, firstDate.month),
      selectedDate = firstDate,
    )
  }

  fun selectService(service: SpaService) {
    _bookingDraft.update {
      it.copy(
        selectedService = service,
        submissionError = null,
      )
    }
  }

  fun selectDogSize(size: DogSize) {
    _bookingDraft.update {
      it.copy(
        selectedDogSize = size,
        submissionError = null,
      )
    }
  }

  fun navigateCalendarMonth(deltaMonths: Int) {
    _bookingDraft.update { draft ->
      val currentMonth = SpaDateUtils.currentYearMonth()
      val target = draft.displayedMonth.plusMonths(deltaMonths)
      if (target < currentMonth) draft else draft.copy(displayedMonth = target)
    }
  }

  fun selectCalendarDate(date: SpaDate) {
    val today = SpaDateUtils.today()
    if (date < today || date.isSunday()) return
    _bookingDraft.update { draft ->
      val keepSlot = draft.selectedDate == date
      draft.copy(
        selectedDate = date,
        displayedMonth = SpaYearMonth(date.year, date.month),
        selectedSlotId = if (keepSlot) draft.selectedSlotId else null,
        submissionError = null,
      )
    }
  }

  fun selectTimeSlot(slotId: String) {
    val slots = timeSlotsForSelectedDate.value
    val target = slots.firstOrNull { it.id == slotId } ?: return
    if (!target.isAvailable) return
    _bookingDraft.update {
      it.copy(
        selectedSlotId = slotId,
        submissionError = null,
      )
    }
  }

  fun updateClientName(value: String) {
    _bookingDraft.update {
      it.copy(
        clientName = value,
        clientNameError = null,
        submissionError = null,
      )
    }
  }

  fun updateClientPhone(value: String) {
    val filtered = value.filter { it.isDigit() || it == '+' || it == ' ' }
    _bookingDraft.update {
      it.copy(
        clientPhone = filtered,
        clientPhoneError = null,
        submissionError = null,
      )
    }
  }

  fun updatePetName(value: String) {
    _bookingDraft.update {
      it.copy(
        petName = value,
        petNameError = null,
        submissionError = null,
      )
    }
  }

  fun updatePetBreed(value: String) {
    _bookingDraft.update {
      it.copy(
        petBreed = value,
        submissionError = null,
      )
    }
  }

  fun applySavedProfile(client: ClientProfile, pet: PetProfile?) {
    _bookingDraft.update {
      it.copy(
        clientName = client.fullName,
        clientPhone = client.phoneNumber,
        petName = pet?.name ?: it.petName,
        petBreed = pet?.breed ?: it.petBreed,
        clientNameError = null,
        clientPhoneError = null,
        petNameError = null,
        submissionError = null,
      )
    }
  }

  fun confirmBooking(onConfirmed: () -> Unit) {
    val draft = _bookingDraft.value
    val service = draft.selectedService
    val size = draft.selectedDogSize
    val slotId = draft.selectedSlotId

    if (service == null || size == null || slotId == null) {
      _bookingDraft.update {
        it.copy(submissionError = "Faltan datos de servicio, tamaño u horario por seleccionar.")
      }
      return
    }

    val nameTrimmed = draft.clientName.trim()
    val phoneDigits = draft.clientPhone.filter { it.isDigit() }
    val petTrimmed = draft.petName.trim()

    var nameErr: String? = null
    var phoneErr: String? = null
    var petErr: String? = null

    if (nameTrimmed.length < 2) {
      nameErr = "Ingresa tu nombre completo."
    }
    if (phoneDigits.length !in 7..15) {
      phoneErr = "Ingresa un número de celular válido (ej. 3001234567)."
    }
    if (petTrimmed.isEmpty()) {
      petErr = "Ingresa el nombre de tu perro."
    }

    if (nameErr != null || phoneErr != null || petErr != null) {
      _bookingDraft.update {
        it.copy(
          clientNameError = nameErr,
          clientPhoneError = phoneErr,
          petNameError = petErr,
        )
      }
      return
    }

    viewModelScope.launch {
      _bookingDraft.update { it.copy(isSubmitting = true, submissionError = null) }
      val result = repository.createAppointment(
        clientName = nameTrimmed,
        clientPhone = draft.clientPhone.trim(),
        petName = petTrimmed,
        petBreed = draft.petBreed.trim(),
        dogSize = size,
        service = service,
        dateIso = draft.selectedDate.toIsoString(),
        timeSlotId = slotId,
        timeSlotLabel = SpaSchedule.labelFor(slotId),
      )

      when (result) {
        is BookingCreationResult.Success -> {
          _bookingDraft.update {
            it.copy(
              isSubmitting = false,
              confirmedAppointment = result.appointment,
              submissionError = null,
            )
          }
          onConfirmed()
        }
        BookingCreationResult.SlotAlreadyTaken -> {
          _bookingDraft.update {
            it.copy(
              isSubmitting = false,
              selectedSlotId = null,
              submissionError = "Este horario ya no está disponible. Por favor regresa al calendario y elige otro.",
            )
          }
        }
        is BookingCreationResult.InvalidData -> {
          _bookingDraft.update {
            it.copy(
              isSubmitting = false,
              submissionError = result.message,
            )
          }
        }
      }
    }
  }

  fun setBookingsFilter(filter: BookingsFilter) {
    _bookingsFilter.value = filter
  }

  fun cancelAppointment(appointmentId: String) {
    viewModelScope.launch {
      repository.cancelAppointment(appointmentId)
    }
  }

  private fun buildTimeSlotsForDate(
    date: SpaDate,
    occupiedMap: Map<String, Set<String>>,
  ): List<TimeSlotUi> {
    val today = SpaDateUtils.today()
    if (date < today || date.isSunday()) return emptyList()

    val occupied = occupiedMap[date.toIsoString()].orEmpty()
    return SpaSchedule.ALL_SLOTS.map { slot ->
      val isPast = SpaDateUtils.isSlotInPast(date, slot.hour24)
      val isOccupied = slot.id in occupied
      TimeSlotUi(
        id = slot.id,
        label = slot.label,
        hour24 = slot.hour24,
        isAvailable = !isPast && !isOccupied,
        isPastHourToday = isPast,
      )
    }
  }

  private fun sortAppointmentsUpcomingFirst(list: List<Appointment>): List<Appointment> {
    val confirmed = list
      .filter { it.status == BookingStatus.CONFIRMADA }
      .sortedWith(compareBy({ it.dateIso }, { it.timeSlotId }))
    val history = list
      .filter { it.status != BookingStatus.CONFIRMADA }
      .sortedWith(compareByDescending<Appointment> { it.dateIso }.thenByDescending { it.timeSlotId })
    return confirmed + history
  }

  class Factory(
    private val repository: PetSpaRepository,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      if (modelClass.isAssignableFrom(PetSpaViewModel::class.java)) {
        return PetSpaViewModel(repository) as T
      }
      throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
  }

  private companion object {
    const val TAG = "PetSpaViewModel"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
