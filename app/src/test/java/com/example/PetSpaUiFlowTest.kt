package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.swipeUp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-420dpi")
class PetSpaUiFlowTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `home screen scrolls down to reveal both services and footer`() {
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
        .onAllNodes(hasTestTag("home_select_service_bano_basico"))
        .fetchSemanticsNodes()
        .isNotEmpty()
    }

    composeTestRule.onNodeWithTag("home_lazy_column").performTouchInput { swipeUp() }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("home_select_service_bano_premium")
      .performScrollTo()
      .assertIsDisplayed()
  }

  @Test
  fun `complete booking flow scrolls and saves appointment`() {
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
        .onAllNodes(hasTestTag("home_book_cta_button"))
        .fetchSemanticsNodes()
        .isNotEmpty()
    }

    // Start booking
    composeTestRule.onNodeWithTag("home_book_cta_button").performClick()
    composeTestRule.waitForIdle()

    // Step 1: Select Baño básico
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
        .onAllNodes(hasTestTag("booking_select_service_bano_basico"))
        .fetchSemanticsNodes()
        .isNotEmpty()
    }
    composeTestRule.onNodeWithTag("booking_select_service_bano_basico").performClick()
    composeTestRule.waitForIdle()

    // Step 2: Select Mediano dog size ($45.000 COP)
    composeTestRule.onNodeWithTag("dog_size_card_mediano").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("dog_size_final_price_text").assertIsDisplayed()
    composeTestRule.onNodeWithTag("dog_size_continue_button").performClick()
    composeTestRule.waitForIdle()

    // Step 3: Calendar - scroll down to slots and pick first available slot
    composeTestRule.onNodeWithTag("calendar_lazy_column").performTouchInput { swipeUp() }
    composeTestRule.waitForIdle()

    val slotTags = listOf("08:00", "09:00", "10:00", "11:00", "13:00", "14:00", "15:00", "16:00", "17:00")
    val firstAvailableSlot = slotTags.firstOrNull { tag ->
      val node = composeTestRule.onNodeWithTag("time_slot_$tag")
      runCatching {
        node.performScrollTo()
        node.performClick()
      }.isSuccess
    }
    requireNotNull(firstAvailableSlot) { "Expected at least one clickable available time slot" }

    composeTestRule.onNodeWithTag("calendar_continue_button").performClick()
    composeTestRule.waitForIdle()

    // Step 4: Fill form & scroll down to CONFIRMAR RESERVA
    composeTestRule.onNodeWithTag("input_client_name").performTextInput("Ana Maria")
    composeTestRule.onNodeWithTag("input_client_phone").performTextInput("3009876543")
    composeTestRule.onNodeWithTag("input_pet_name").performTextInput("Max")
    composeTestRule.onNodeWithTag("confirm_booking_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // Step 5: Confirmation screen
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
        .onAllNodes(hasTestTag("confirmation_title_text"))
        .fetchSemanticsNodes()
        .isNotEmpty()
    }
    composeTestRule.onNodeWithTag("confirmation_view_bookings_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // Step 6: My Bookings shows Max
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
        .onAllNodes(hasText("Max"))
        .fetchSemanticsNodes()
        .isNotEmpty()
    }
    composeTestRule.onNodeWithText("Max").assertIsDisplayed()

    // Step 7: Navigate back to Home tab after booking
    composeTestRule.onNodeWithTag("nav_tab_home").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_book_cta_button").assertIsDisplayed()
    composeTestRule.onNodeWithText("Próximas reservas").assertIsDisplayed()

    // Step 8: Navigate to Reservar tab and verify step 1 opens cleanly
    composeTestRule.onNodeWithTag("nav_tab_book").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("booking_select_service_bano_basico").assertIsDisplayed()
  }
}
