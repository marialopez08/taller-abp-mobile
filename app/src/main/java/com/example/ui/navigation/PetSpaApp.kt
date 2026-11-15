package com.example.ui.navigation

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.remote.FirestorePetSpaRepository
import com.example.ui.PetSpaViewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BookingConfirmationScreen
import com.example.ui.screens.BookingFormScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.DogSizeScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MyBookingsScreen
import com.example.ui.screens.ServiceSelectionScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object PetSpaRoutes {
  const val HOME = "inicio"
  const val BOOK_SERVICE = "reservar_servicio"
  const val BOOK_SIZE = "reservar_tamano"
  const val BOOK_CALENDAR = "reservar_calendario"
  const val BOOK_FORM = "reservar_formulario"
  const val BOOK_CONFIRMATION = "reservar_confirmacion"
  const val MY_BOOKINGS = "mis_reservas"
}

private enum class TopLevelTab(
  val route: String,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
) {
  HOME(
    route = PetSpaRoutes.HOME,
    label = "Inicio",
    selectedIcon = Icons.Rounded.Home,
    unselectedIcon = Icons.Outlined.Home,
    testTag = "nav_tab_home",
  ),
  BOOK(
    route = PetSpaRoutes.BOOK_SERVICE,
    label = "Reservar",
    selectedIcon = Icons.Rounded.CalendarMonth,
    unselectedIcon = Icons.Outlined.CalendarMonth,
    testTag = "nav_tab_book",
  ),
  MY_BOOKINGS(
    route = PetSpaRoutes.MY_BOOKINGS,
    label = "Mis reservas",
    selectedIcon = Icons.AutoMirrored.Rounded.EventNote,
    unselectedIcon = Icons.AutoMirrored.Outlined.EventNote,
    testTag = "nav_tab_my_bookings",
  );

  fun matchesRoute(route: String?): Boolean {
    if (route == null) return false
    return when (this) {
      HOME -> route == PetSpaRoutes.HOME
      BOOK -> route.startsWith("reservar_")
      MY_BOOKINGS -> route == PetSpaRoutes.MY_BOOKINGS
    }
  }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
    trySend(firebaseAuth.currentUser)
  }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun PetSpaRoot(
  auth: FirebaseAuth = remember { Firebase.auth },
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser
  if (user == null) {
    AuthScreen(auth = auth)
  } else {
    key(user.uid) {
      val viewModel: PetSpaViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
          initializer {
            val app = checkNotNull(this[APPLICATION_KEY]) {
              "APPLICATION_KEY missing from CreationExtras"
            }
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            val repository = FirestorePetSpaRepository(
              db = db,
              currentUserId = user.uid,
              currentUserDisplayName = user.displayName.orEmpty(),
              currentUserEmail = user.email.orEmpty(),
            )
            PetSpaViewModel(repository)
          }
        },
      )
      val greetingName = user.displayName?.takeIf { it.isNotBlank() } ?: user.email
      PetSpaApp(
        viewModel = viewModel,
        userDisplayName = greetingName,
        onSignOutClick = {
          scope.launch {
            withContext(NonCancellable) {
              try {
                CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
              } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
              } finally {
                auth.signOut()
              }
            }
          }
        },
      )
    }
  }
}

@Composable
fun PetSpaApp(
  viewModel: PetSpaViewModel,
  userDisplayName: String? = null,
  onSignOutClick: (() -> Unit)? = null,
) {
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = navBackStackEntry?.destination
  val currentRoute = currentDestination?.route

  val services by viewModel.services.collectAsStateWithLifecycle()
  val upcomingAppointments by viewModel.upcomingAppointments.collectAsStateWithLifecycle()
  val filteredAppointments by viewModel.filteredAppointments.collectAsStateWithLifecycle()
  val activeFilter by viewModel.bookingsFilter.collectAsStateWithLifecycle()
  val draft by viewModel.bookingDraft.collectAsStateWithLifecycle()
  val occupiedSlotsByDate by viewModel.occupiedSlotsByDate.collectAsStateWithLifecycle()
  val timeSlots by viewModel.timeSlotsForSelectedDate.collectAsStateWithLifecycle()
  val latestClientAndPet by viewModel.latestClientAndPet.collectAsStateWithLifecycle()

  val upcomingCount = upcomingAppointments.size

  fun navigateToTab(tab: TopLevelTab) {
    if (currentRoute == PetSpaRoutes.BOOK_CONFIRMATION) {
      viewModel.startNewBooking()
    }
    if (tab == TopLevelTab.BOOK && currentRoute?.startsWith("reservar_") == true && currentRoute != PetSpaRoutes.BOOK_CONFIRMATION) {
      return
    }
    navController.navigate(tab.route) {
      popUpTo(PetSpaRoutes.HOME) {
        inclusive = false
      }
      launchSingleTop = true
    }
  }

  fun startBookingFlow(preselectedServiceCode: String? = null) {
    val svc = services.firstOrNull { it.code.code == preselectedServiceCode }
    viewModel.startNewBooking(preselectedService = svc)
    val destination = if (svc != null) PetSpaRoutes.BOOK_SIZE else PetSpaRoutes.BOOK_SERVICE
    navController.navigate(destination) {
      popUpTo(PetSpaRoutes.HOME) {
        inclusive = false
      }
      launchSingleTop = true
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavigationRail = maxWidth >= 600.dp

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = MaterialTheme.colorScheme.background,
      contentWindowInsets = WindowInsets.safeDrawing,
      bottomBar = {
        if (!useNavigationRail) {
          NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
          ) {
            TopLevelTab.entries.forEach { tab ->
              val selected = currentDestination?.hierarchy?.any { tab.matchesRoute(it.route) } == true
              NavigationBarItem(
                selected = selected,
                onClick = { navigateToTab(tab) },
                icon = {
                  TabIconWithBadge(
                    tab = tab,
                    selected = selected,
                    upcomingCount = upcomingCount,
                  )
                },
                label = { Text(tab.label) },
                modifier = Modifier.testTag(tab.testTag),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      ) {
        if (useNavigationRail) {
          NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface,
          ) {
            TopLevelTab.entries.forEach { tab ->
              val selected = currentDestination?.hierarchy?.any { tab.matchesRoute(it.route) } == true
              NavigationRailItem(
                selected = selected,
                onClick = { navigateToTab(tab) },
                icon = {
                  TabIconWithBadge(
                    tab = tab,
                    selected = selected,
                    upcomingCount = upcomingCount,
                  )
                },
                label = { Text(tab.label) },
                modifier = Modifier.testTag(tab.testTag),
              )
            }
          }
        }

        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
        ) {
          NavHost(
            navController = navController,
            startDestination = PetSpaRoutes.HOME,
            modifier = Modifier.fillMaxSize(),
          ) {
            composable(PetSpaRoutes.HOME) {
              HomeScreen(
                services = services,
                upcomingAppointments = upcomingAppointments,
                onBookAppointmentClick = { startBookingFlow() },
                onSelectServiceClick = { service -> startBookingFlow(service.code.code) },
                onViewAllBookingsClick = { navigateToTab(TopLevelTab.MY_BOOKINGS) },
                userDisplayName = userDisplayName,
                onSignOutClick = onSignOutClick,
              )
            }

            composable(PetSpaRoutes.BOOK_SERVICE) {
              ServiceSelectionScreen(
                services = services,
                selectedService = draft.selectedService,
                onSelectService = { service ->
                  viewModel.selectService(service)
                  navController.navigate(PetSpaRoutes.BOOK_SIZE)
                },
              )
            }

            composable(PetSpaRoutes.BOOK_SIZE) {
              val activeService = draft.selectedService ?: services.firstOrNull()
              if (activeService != null) {
                DogSizeScreen(
                  selectedService = activeService,
                  selectedDogSize = draft.selectedDogSize,
                  onSelectSize = { size -> viewModel.selectDogSize(size) },
                  onContinue = { navController.navigate(PetSpaRoutes.BOOK_CALENDAR) },
                  onBack = {
                    if (!navController.popBackStack()) {
                      navController.navigate(PetSpaRoutes.BOOK_SERVICE)
                    }
                  },
                )
              }
            }

            composable(PetSpaRoutes.BOOK_CALENDAR) {
              CalendarScreen(
                displayedMonth = draft.displayedMonth,
                selectedDate = draft.selectedDate,
                selectedSlotId = draft.selectedSlotId,
                timeSlots = timeSlots,
                occupiedSlotsByDate = occupiedSlotsByDate,
                finalPriceCop = draft.calculatedPriceCop ?: 0,
                onNavigateMonth = { delta -> viewModel.navigateCalendarMonth(delta) },
                onSelectDate = { date -> viewModel.selectCalendarDate(date) },
                onSelectSlot = { slotId -> viewModel.selectTimeSlot(slotId) },
                onContinue = { navController.navigate(PetSpaRoutes.BOOK_FORM) },
                onBack = { navController.popBackStack() },
              )
            }

            composable(PetSpaRoutes.BOOK_FORM) {
              BookingFormScreen(
                draft = draft,
                latestClientAndPet = latestClientAndPet,
                onClientNameChange = viewModel::updateClientName,
                onClientPhoneChange = viewModel::updateClientPhone,
                onPetNameChange = viewModel::updatePetName,
                onPetBreedChange = viewModel::updatePetBreed,
                onApplySavedProfile = viewModel::applySavedProfile,
                onConfirmBooking = {
                  viewModel.confirmBooking {
                    navController.navigate(PetSpaRoutes.BOOK_CONFIRMATION) {
                      popUpTo(PetSpaRoutes.HOME) { inclusive = false }
                      launchSingleTop = true
                    }
                  }
                },
                onBack = { navController.popBackStack() },
              )
            }

            composable(PetSpaRoutes.BOOK_CONFIRMATION) {
              val confirmed = draft.confirmedAppointment
              if (confirmed != null) {
                BookingConfirmationScreen(
                  appointment = confirmed,
                  onViewMyBookings = {
                    viewModel.startNewBooking()
                    navigateToTab(TopLevelTab.MY_BOOKINGS)
                  },
                  onBackToHome = {
                    viewModel.startNewBooking()
                    navigateToTab(TopLevelTab.HOME)
                  },
                )
              } else {
                LaunchedEffect(Unit) {
                  navController.navigate(PetSpaRoutes.HOME) {
                    popUpTo(PetSpaRoutes.HOME) { inclusive = false }
                    launchSingleTop = true
                  }
                }
              }
            }

            composable(PetSpaRoutes.MY_BOOKINGS) {
              MyBookingsScreen(
                appointments = filteredAppointments,
                activeFilter = activeFilter,
                onSelectFilter = viewModel::setBookingsFilter,
                onCancelAppointment = viewModel::cancelAppointment,
                onBookNewClick = { startBookingFlow() },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun TabIconWithBadge(
  tab: TopLevelTab,
  selected: Boolean,
  upcomingCount: Int,
) {
  val iconVector = if (selected) tab.selectedIcon else tab.unselectedIcon
  if (tab == TopLevelTab.MY_BOOKINGS && upcomingCount > 0) {
    BadgedBox(
      badge = {
        Badge {
          Text(upcomingCount.toString())
        }
      },
    ) {
      Icon(imageVector = iconVector, contentDescription = tab.label)
    }
  } else {
    Icon(imageVector = iconVector, contentDescription = tab.label)
  }
}
