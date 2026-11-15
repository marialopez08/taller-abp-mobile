package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.LocalRoomPetSpaRepository
import com.example.data.local.PetSpaDatabase
import com.example.ui.PetSpaViewModel
import com.example.ui.navigation.PetSpaApp
import com.example.ui.theme.PetSpaTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val database = PetSpaDatabase.getInstance(applicationContext)
    val repository = LocalRoomPetSpaRepository(database.petSpaDao())

    setContent {
      PetSpaTheme {
        val viewModel: PetSpaViewModel = viewModel(
          factory = PetSpaViewModel.Factory(repository),
        )
        PetSpaApp(viewModel = viewModel)
      }
    }
  }
}
