package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val AUTH_TAG = "Auth"

private tailrec fun Context.findActivity(): Activity? = when (this) {
  is Activity -> this
  is ContextWrapper -> baseContext.findActivity()
  else -> null
}

@Composable
fun AuthScreen(
  auth: FirebaseAuth,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val windowInfo = LocalWindowInfo.current
  val scope = rememberCoroutineScope()

  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var attemptedSilentSignIn by rememberSaveable { mutableStateOf(false) }

  // Wait until the initial frame and window transition settle before launching Credential Manager
  // so SurfaceSyncGroup on MainActivity is never stalled on cold start.
  LaunchedEffect(windowInfo.isWindowFocused) {
    if (!windowInfo.isWindowFocused || attemptedSilentSignIn || auth.currentUser != null) {
      return@LaunchedEffect
    }
    attemptedSilentSignIn = true
    withFrameNanos { }
    delay(300L)
    try {
      val hostContext = context.findActivity() ?: context
      val credentialManager = CredentialManager.create(hostContext)
      val serverClientId = context.getString(R.string.default_web_client_id)
      val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(serverClientId)
        .setAutoSelectEnabled(true)
        .build()
      val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

      val result = credentialManager.getCredential(hostContext, request)
      val credential = result.credential
      if (credential is CustomCredential &&
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
      ) {
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
        auth.signInWithCredential(firebaseCredential).await()
      }
    } catch (_: NoCredentialException) {
      // Normal when no prior authorized Google account exists on device.
    } catch (e: GetCredentialCancellationException) {
      Log.w(AUTH_TAG, "Silent Google Sign-In cancelled: ${e.message}", e)
    } catch (e: Exception) {
      Log.w(AUTH_TAG, "Silent Google Sign-In skipped: ${e.message}", e)
    }
  }

  fun startInteractiveGoogleSignIn() {
    if (isLoading) return
    scope.launch {
      isLoading = true
      errorMessage = null
      try {
        val hostContext = context.findActivity() ?: context
        val credentialManager = CredentialManager.create(hostContext)
        val serverClientId = context.getString(R.string.default_web_client_id)
        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(serverClientId).build()
        val request = GetCredentialRequest.Builder()
          .addCredentialOption(signInWithGoogleOption)
          .build()

        val result = credentialManager.getCredential(hostContext, request)
        val credential = result.credential
        if (credential is CustomCredential &&
          credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
          val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
          val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
          auth.signInWithCredential(firebaseCredential).await()
        } else {
          errorMessage = "No fue posible validar la credencial de Google. Intenta nuevamente."
          isLoading = false
        }
      } catch (e: GetCredentialCancellationException) {
        Log.w(AUTH_TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
        isLoading = false
        errorMessage = "Inicio de sesión cancelado. Selecciona tu cuenta de Google para continuar."
      } catch (e: GetCredentialException) {
        Log.e(AUTH_TAG, "Credential Manager sign-in error", e)
        isLoading = false
        errorMessage = "No pudimos iniciar sesión con Google. Verifica tu cuenta e intenta nuevamente."
      } catch (e: Exception) {
        Log.e(AUTH_TAG, "Firebase Google Sign-In failed", e)
        isLoading = false
        errorMessage = "Ocurrió un error al conectar con Firebase. Intenta nuevamente."
      }
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 560.dp)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Box(
            modifier = Modifier
              .size(50.dp)
              .clip(CircleShape)
              .background(
                Brush.linearGradient(
                  colors = listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.secondaryContainer,
                  )
                )
              ),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.Pets,
              contentDescription = "Logotipo PET SPA",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(28.dp),
            )
          }
          Column {
            Text(
              text = "PET SPA",
              style = MaterialTheme.typography.headlineMedium,
              color = MaterialTheme.colorScheme.onBackground,
              fontWeight = FontWeight.ExtraBold,
            )
            Text(
              text = "Baño, peluquería y bienestar canino",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(28.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
          ),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
          Column {
            Image(
              painter = painterResource(id = R.drawable.img_hero_pet_spa),
              contentDescription = "Perro feliz en PET SPA",
              modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
              contentScale = ContentScale.Crop,
            )

            Column(
              modifier = Modifier.padding(20.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Text(
                text = "¡El bienestar de tu mascota comienza aquí!",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "Inicia sesión con Google para guardar tus citas en Firebase, consultar horarios disponibles en tiempo real y gestionar tus próximas visitas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )

              AuthBenefitRow(
                icon = Icons.Rounded.CloudDone,
                text = "Tus reservas sincronizadas en la nube con Firebase.",
              )
              AuthBenefitRow(
                icon = Icons.Rounded.CalendarMonth,
                text = "Disponibilidad de horarios en tiempo real (Lun - Sáb).",
              )
              AuthBenefitRow(
                icon = Icons.Rounded.VerifiedUser,
                text = "Acceso privado y seguro para tu información y la de tu perro.",
              )

              if (errorMessage != null) {
                Surface(
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(14.dp),
                  color = MaterialTheme.colorScheme.errorContainer,
                ) {
                  Text(
                    text = errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                  )
                }
              }

              Spacer(modifier = Modifier.height(4.dp))

              Button(
                onClick = { startInteractiveGoogleSignIn() },
                enabled = !isLoading,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(54.dp)
                  .testTag("google_sign_in_button"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
              ) {
                if (isLoading) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Text(
                    text = "Conectando con Google...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                  )
                } else {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Login,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    text = "Continuar con Google",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
            }
          }
        }

        Text(
          text = "Tarifas transparentes en pesos colombianos (COP) desde $35.000.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}

@Composable
private fun AuthBenefitRow(
  icon: ImageVector,
  text: String,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Box(
      modifier = Modifier
        .size(32.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(18.dp),
      )
    }
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}
