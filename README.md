# PET SPA — Aplicación Móvil Android de Reservas Caninas

**Autora:** María Isabel López Figueroa  
**Curso:** Diseño de Aplicaciones Móviles · Fundación Universitaria Católica del Norte (UCN)  
**Actividad:** Taller ABP – Entrega 1 · Noviembre de 2026  
**IMPORTANTE: No me fue posible adjuntar la ppt ya que github no lo abre bien, se compartio al correo institucional por medio de OneDrive donde se accede en la siguiente ruta:**
**Presentación:** [Click en presentacion](https://soyucnedu-my.sharepoint.com/:p:/g/personal/milopezf_soyucn_edu_co/cQo56pwAr_GbSKr22aD6VbShEgUC8NoqYC7TsFHJuz8jPqtHuw)

Aplicación nativa para Android desarrollada en **Kotlin** y **Jetpack Compose (Material Design 3)** que permite a los clientes consultar servicios de baño y arreglo estético para perros, calcular precios en **pesos colombianos (COP)** según el tamaño de la mascota, consultar disponibilidad en un calendario interactivo y gestionar sus citas desde el celular.

---

## Situación problema

Escogí el caso de un spa canino pequeño que agenda sus citas por WhatsApp y llamadas. Esto causa tres problemas:

- **Citas cruzadas:** los mensajes se pierden y a veces dos clientes quedan con la misma hora.
- **Precios poco claros:** el valor cambia según el tamaño del perro, pero el cliente no lo sabe hasta llegar al spa.
- **Sin historial:** ni el cliente ni el spa ven fácil qué citas están confirmadas, canceladas o ya pasaron.

**Objetivo:** crear una app móvil donde el cliente vea los servicios con su precio exacto, escoja una hora libre en tiempo real y gestione sus citas, guardando todo en la nube de forma segura.

---

## 1. Descripción del proyecto

**PET SPA** está diseñada con una estética limpia y amigable basada en tonos **verde menta, blanco, beige y azul claro**, tipografía redondeada (`Nunito` + `Plus Jakarta Sans`), ícono adaptativo propio y diseño responsivo compatible tanto con teléfonos (`NavigationBar`) como con pantallas medianas/grandes (`NavigationRail`).

### Funcionalidades principales

- **Inicio de sesión con cuenta Google**:
  - Pantalla de bienvenida y acceso (`AuthScreen`) con inicio de sesión mediante **Google Sign-In (Jetpack Credential Manager)** y detección silenciosa de cuentas previamente autorizadas una vez estabilizada la ventana inicial.
  - Aislamiento del estado de navegación y datos por usuario autenticado (`uid`), con cierre de sesión seguro desde la barra superior que limpia el estado de credenciales antes de desconectar Firebase Auth.
- **Inicio**:
  - Saludo personalizado para la cuenta registrada (ej. *"Hola, Laura Gómez"*), insignia de horario (**Lun - Sáb**) y botón de cierre de sesión.
  - Ilustración principal de **PET SPA** con el mensaje *"¡El bienestar de tu mascota comienza aquí!"* y botón de acceso rápido **"Reservar una cita"**.
  - Sección **Próximas reservas** sincronizada en tiempo real desde Firestore (visible cuando el cliente tiene citas confirmadas).
  - Catálogo de **Servicios disponibles** con acceso directo al flujo de reserva.
- **Catálogo de servicios y tarifas dinámicas (COP)**:
  - **Baño básico** (desde `$35.000 COP`): baño completo, limpieza de pelaje, productos especiales y secado.
  - **Baño premium** (desde `$70.000 COP`): baño completo, limada de uñas, corte de pelo y arreglo estético.
  - Cálculo automático según el tamaño del perro:

| Servicio | Pequeño | Mediano | Grande |
|---|---:|---:|---:|
| **Baño básico** | $35.000 COP | $45.000 COP | $55.000 COP |
| **Baño premium** | $70.000 COP | $85.000 COP | $100.000 COP |

- **Calendario interactivo de reservas**:
  - Navegación entre meses (bloqueando meses anteriores al actual).
  - Atención de **lunes a sábado**; los **domingos** se muestran marcados como día de descanso y no son seleccionables.
  - Bloqueo automático de fechas anteriores al día actual y de horarios ya transcurridos en el día en curso.
  - Consulta en tiempo real de horarios ocupados en `occupied_slots` para la fecha seleccionada, impidiendo cruces entre distintos clientes.
  - Franjas horarias individuales (`8:00 a. m.`, `9:00 a. m.`, `10:00 a. m.`, `11:00 a. m.`, `1:00 p. m.`, `2:00 p. m.`, `3:00 p. m.`, `4:00 p. m.`, `5:00 p. m.`) diferenciadas por color: **verde** (disponible), **gris** (ocupado/pasado) y **menta oscuro** (seleccionado).
- **Formulario validado, resumen y confirmación**:
  - Captura de nombre del cliente, número de celular, nombre de la mascota y raza (opcional), con chip de autocompletado (`"Usar datos guardados"`) alimentado por el perfil guardado en Firestore.
  - Resumen completo con precio final antes de confirmar.
  - Transacción atómica en Cloud Firestore que registra la cita, bloquea el horario y actualiza el perfil del cliente; pantalla de comprobante con *"¡Tu cita ha sido reservada exitosamente!"*.
- **Mis reservas**:
  - Listado en tiempo real ordenado mostrando primero las próximas citas confirmadas y marcando automáticamente como `COMPLETADA` las citas cuyo horario ya pasó.
  - Filtros por estado (**Todas**, **Confirmadas**, **Completadas**, **Canceladas**).
  - Opción de **Cancelar reserva** con diálogo de confirmación que ejecuta una transacción en Firestore para actualizar el estado y liberar de inmediato el horario en `occupied_slots`.

---

## 2. Persistencia en la nube con Firebase

La aplicación reemplaza el almacenamiento local por una arquitectura en la nube respaldada por **Firebase Cloud Firestore** (edición Enterprise con base de datos dedicada configurada en `R.string.firestore_database_id`) y **Firebase Authentication**:

- **Autenticación segura con Google (`AuthScreen` + `PetSpaRoot`)**:
  - Usa **Jetpack Credential Manager** (`GetGoogleIdOption` para inicio silencioso tras el renderizado inicial y `GetSignInWithGoogleOption` para el botón **"Continuar con Google"**) sin autenticación anónima ni contraseñas locales.
  - En `PetSpaRoot`, toda la UI principal y la instanciación de `PetSpaViewModel` están protegidas detrás de la compuerta `user != null` y aisladas con `key(user.uid)`.
- **Sincronización en tiempo real y transacciones atómicas (`FirestorePetSpaRepository`)**:
  - Observa citas, perfil de cliente/mascota y disponibilidad por fecha mediante `snapshots()` (con `ServerTimestampBehavior.ESTIMATE`).
  - Al confirmar una cita, `db.runTransaction` verifica que `/occupied_slots/{slotKey}` no exista, crea la cita en `/users/{uid}/appointments/{appointmentId}`, bloquea el horario en `/occupied_slots/{slotKey}` y guarda los últimos datos de contacto en `/users/{uid}` en una sola operación atómica.
  - Al cancelar una cita (`CONFIRMADA` → `CANCELADA`), la transacción elimina el bloqueo en `/occupied_slots/{slotKey}` solo si pertenece al mismo usuario.
- **Reglas de seguridad Zero-Trust (`firestore.rules`)**:
  - Aislamiento estricto por usuario (`request.auth.uid == userId`) en `/users/{userId}` y `/users/{userId}/appointments/{appointmentId}` (sin borrado de historial).
  - Validación del catálogo y tarifas exactas en COP directamente en las reglas del servidor (`isValidAppointment`).
  - Atomicidad cruzada (`existsAfter`) entre cada cita y su documento en `/occupied_slots/{slotKey}`, el cual no almacena PII y bloquea listados globales sin filtro de propietario.

---

## 3. Arquitectura y estructura técnica

La aplicación sigue el patrón **MVVM (Model–View–ViewModel)** combinado con el **Patrón Repositorio** para desacoplar la lógica de negocio y el acceso a datos de la capa de presentación.

### Flujo de datos y seguridad

```text
┌──────────────────────────────────────────────────────────┐
│          Compuerta de Autenticación (PetSpaRoot)         │
│  AuthScreen (Google Sign-In via Credential Manager)      │
└────────────────────────────┬─────────────────────────────┘
                             │ FirebaseUser autenticado (uid)
                             ▼
┌──────────────────────────────────────────────────────────┐
│                 Capa de UI (Jetpack Compose)             │
│  HomeScreen • ServiceSelectionScreen • DogSizeScreen     │
│  CalendarScreen • BookingFormScreen • MyBookingsScreen   │
└────────────────────────────┬─────────────────────────────┘
                             │ observa StateFlow / eventos
                             ▼
┌──────────────────────────────────────────────────────────┐
│              PetSpaViewModel (StateFlow)                 │
│  • Estado del borrador de reserva (BookingDraftState)    │
│  • Validaciones de formulario y reglas de calendario     │
│  • Filtros, ordenamiento y escucha por fecha activa      │
└────────────────────────────┬─────────────────────────────┘
                             │ interfaz PetSpaRepository
                             ▼
┌──────────────────────────────────────────────────────────┐
│       FirestorePetSpaRepository (Implementación)         │
│  • Listeners en tiempo real con snapshots()              │
│  • Transacciones atómicas (cita + occupied_slots + perfil)│
│  • Registro estructurado de errores (handleFirestoreError)│
└────────────────────────────┬─────────────────────────────┘
                             │ Firestore SDK (named DB ID)
                             ▼
┌──────────────────────────────────────────────────────────┐
│      Firebase Cloud Firestore + Security Rules v2        │
│  users/{uid} • users/{uid}/appointments • occupied_slots │
└──────────────────────────────────────────────────────────┘
```

### Modelo de datos en Cloud Firestore (`firebase-blueprint.json`)

- `users/{userId}`: Perfil privado del cliente autenticado con sus últimos datos de contacto y mascota para agilizar nuevas reservas.
- `users/{userId}/appointments/{appointmentId}`: Citas del cliente autenticado (acceso estricto `request.auth.uid == userId`, validación de tarifas COP en reglas y bloqueo de estado terminal para citas `CANCELADA` / `COMPLETADA`).
- `occupied_slots/{slotKey}`: Índice atómico sin PII (`YYYY-MM-DD_HH-00`) que evita reservas duplicadas entre diferentes clientes mediante transacciones Firestore y libera el horario al cancelar una cita.

### Estructura de paquetes principales

```text
app/src/main/java/com/example/
├── MainActivity.kt                          # Punto de entrada Edge-to-Edge -> PetSpaRoot
├── domain/model/
│   ├── SpaModels.kt                         # Entidades de dominio, SpaCatalog y SpaSchedule
│   └── SpaDateUtils.kt                      # Utilidades de calendario (API 24+) y formato COP
├── data/
│   ├── repository/
│   │   └── PetSpaRepository.kt              # Contrato abstracto del repositorio
│   └── remote/
│       ├── FirestoreModels.kt               # DTOs tipados (@DocumentId, @ServerTimestamp)
│       ├── FirestoreErrorHandler.kt         # Diagnóstico JSON estructurado para errores Firestore
│       └── FirestorePetSpaRepository.kt     # Repositorio Cloud Firestore con transacciones atómicas
└── ui/
    ├── PetSpaViewModel.kt                   # Gestión centralizada de estados con StateFlow
    ├── navigation/PetSpaApp.kt              # Compuerta PetSpaRoot + grafo Navigation Compose
    ├── components/
    │   ├── DogSizeIllustration.kt           # Ilustraciones en Canvas para Pequeño, Mediano y Grande
    │   └── PetSpaComponents.kt              # Tarjetas de servicio, indicador de pasos, badges y avisos
    ├── screens/
    │   ├── AuthScreen.kt                    # Inicio de sesión con Google (Credential Manager)
    │   ├── HomeScreen.kt
    │   ├── ServiceSelectionScreen.kt
    │   ├── DogSizeScreen.kt
    │   ├── CalendarScreen.kt
    │   ├── BookingFormScreen.kt
    │   ├── BookingConfirmationScreen.kt
    │   └── MyBookingsScreen.kt
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

### Stack tecnológico

- **Lenguaje:** Kotlin `2.2.10`
- **Build & Tooling:** Android Gradle Plugin `9.1.1`, Version Catalog (`gradle/libs.versions.toml`), Google Services `4.5.0`
- **UI:** Jetpack Compose (BOM `2024.09.00`), Material Design 3, Material Icons Extended
- **Navegación:** Navigation Compose `2.8.9`
- **Backend y Persistencia:** Firebase BoM `34.17.0` (`firebase-firestore`, `firebase-auth`)
- **Autenticación:** Google Sign-In con Jetpack `CredentialManager` (`1.5.0`) y `googleid` (`1.1.1`)
- **Asincronía:** Kotlin Coroutines & Flow `1.10.2`
- **Pruebas:** JUnit 4, Robolectric `4.16.1`, Compose UI Test (`ui-test-junit4`), `@firebase/rules-unit-testing` y Firebase Local Emulator Suite (Firestore + Auth)
- **Compatibilidad SDK:** `minSdk = 24` (Android 7.0) • `targetSdk = 36` • `compileSdk = 36`
- **Application ID:** `com.aistudio.petspa.vqmzrk`

### Justificación tecnológica

| Herramienta | Por qué la escogí |
|---|---|
| Kotlin + Jetpack Compose | Es el lenguaje oficial de Android y permite hacer pantallas con menos código. |
| Cloud Firestore | Varios clientes reservan al tiempo y todos deben ver las mismas horas ocupadas en tiempo real, sin montar un servidor propio. |
| Firebase Auth con Google | El cliente entra con la cuenta que ya tiene, sin crear otra contraseña, y cada uno ve solo sus citas. |
| Reglas de seguridad de Firestore | Protegen los datos en el servidor, aunque alguien modifique la app. |
| MVVM + Repositorio | Separa pantallas, lógica y datos. Gracias a esto pasé de Room a Firestore cambiando solo el repositorio. |
| Emulador de Firebase + Robolectric | Me permiten probar las reglas y el flujo completo sin usar la base real ni un celular físico. |

---

## 4. Instrucciones de despliegue y ejecución

### Requisitos previos

- **Android Studio** (Meerkat / Narwhal o superior recomendado para AGP 9.1).
- **JDK 17** (o superior, configurado para compilar bytecode Java 11).
- **Android SDK 36** instalado con al menos un emulador Android con Google Play Services (API 24–36) o un dispositivo físico con depuración USB habilitada.
- **Firebase configurado**: `app/google-services.json`, `app/src/main/res/values/firebase_applet_config.xml` (con `firestore_database_id`) y reglas `firestore.rules` desplegadas en el proyecto Firebase.

### Ejecución desde Android Studio

1. Abrir la carpeta raíz del proyecto en **Android Studio**.
2. Verificar que el certificado SHA-1/SHA-256 de depuración coincida con el registrado en Firebase (`app/google-services.json`).
3. Esperar a que finalice la sincronización de **Gradle** (`Sync Project with Gradle Files`).
4. Seleccionar un dispositivo virtual (AVD) con Google Play Services o teléfono físico.
5. Hacer clic en **Run (`app`)** para compilar, instalar e iniciar **PET SPA**.

### Compilación y pruebas por línea de comandos

Desde la raíz del proyecto:

```bash
# 1. Compilar el APK de depuración
gradle :app:assembleDebug

# 2. Pruebas unitarias de reglas Firestore (Nivel 1 - Node.js contra emulador en 127.0.0.1:8085)
FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js

# 3. Pruebas de integración Kotlin + Robolectric contra emuladores Firebase (Nivel 2)
gradle :app:testDebugUnitTest --tests '*Rule*'

# 4. Ejecutar toda la suite unitaria y UI Compose (Robolectric)
gradle :app:testDebugUnitTest

# 5. Generar APK o Bundle (AAB) de release (requiere variables de firma KEYSTORE_PATH, STORE_PASSWORD, KEY_PASSWORD)
gradle :app:assembleRelease
gradle :app:bundleRelease
```

El APK de depuración generado se ubica en:
`app/build/outputs/apk/debug/app-debug.apk`

> **Nota de firma:** El archivo `app/build.gradle.kts` utiliza `debug.keystore` para compilaciones `debug` y variables de entorno (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`) para `release`. No se requieren archivos `local.properties` para ejecutar la aplicación.

---

## 5. Evidencias de verificación y pruebas

El proyecto valida tanto las reglas de seguridad de **Cloud Firestore** como la integración del repositorio Kotlin, el catálogo de precios y los flujos completos de interfaz en Jetpack Compose.

### Cobertura de pruebas verificadas

| Suite / Archivo | Casos verificados | Resultado |
|---|---|---|
| `firestore.test.js` (Nivel 1 — JS Rules Unit Test) | • Rechazo de lecturas/escrituras no autenticadas.<br>• Creación atómica en lote (`appointments` + `occupied_slots`) y bloqueo de registros huérfanos.<br>• Rechazo de campos fantasma (*shadow update*) y manipulación de tarifas COP.<br>• Aislamiento estricto de PII entre usuarios distintos y bloqueo de listado global en `occupied_slots`.<br>• Cancelación de cita, liberación del horario y bloqueo de estado terminal (`CANCELADA` → `CONFIRMADA` denegado). | Aprobada (9/9) |
| `FirestorePetSpaRepositoryRuleTest` (Nivel 2 — Kotlin + Robolectric) | • Creación atómica de cita, bloqueo de horario y perfil en Firestore.<br>• Prevención de doble reserva entre dos usuarios (`SlotAlreadyTaken`) y liberación automática al cancelar.<br>• Aislamiento entre usuarios (`PERMISSION_DENIED` al leer citas ajenas) y rechazo sin sesión (`auth.signOut()`). | Aprobada (4/4) |
| `ExampleUnitTest` y `ExampleRobolectricTest` | • Verificación base de entorno JUnit y `app_name = "PET SPA"`.<br>• Catálogo oficial (`Baño básico` y `Baño premium`) y tarifas exactas en COP (`$35.000`–`$100.000`).<br>• Reglas de domingos (día de descanso) y formateo de moneda COP. | Aprobada (4/4) |
| `PetSpaUiFlowTest` | • Desplazamiento vertical en **Inicio** mostrando héroe, ambos servicios y aviso de sincronización en la nube.<br>• Flujo completo de reserva: **Inicio** → **Servicio** → **Tamaño Mediano (`$45.000 COP`)** → **Calendario** → **Formulario** → **Confirmación** → **Mis reservas** → regreso a **Inicio** (**Próximas reservas** visible) → apertura limpia de **Reservar**. | Aprobada (2/2) |

### Mockups pantalla por pantalla (con cuenta registrada: Laura Gómez)

Los mockups visuales de las 8 pantallas del flujo autenticado en la nube se encuentran organizados en la carpeta [`mockups/`](./mockups/README.md):

- [`mockups/00_inicio_sesion.jpg`](./mockups/00_inicio_sesion.jpg) — Pantalla de Inicio de sesión con Google (Credential Manager + Firebase)
- [`mockups/01_inicio.jpg`](./mockups/01_inicio.jpg) — Pantalla de Inicio con cuenta registrada (*"Hola, Laura Gómez"*), cierre de sesión y próximas reservas
- [`mockups/02_seleccion_servicio.jpg`](./mockups/02_seleccion_servicio.jpg) — Selección de servicio
- [`mockups/03_tamano_perro.jpg`](./mockups/03_tamano_perro.jpg) — Tamaño del perro y tarifa en COP
- [`mockups/04_calendario_horarios.jpg`](./mockups/04_calendario_horarios.jpg) — Calendario interactivo y horarios
- [`mockups/05_formulario_resumen.jpg`](./mockups/05_formulario_resumen.jpg) — Formulario de reserva y resumen de Laura Gómez y Toby
- [`mockups/06_confirmacion.jpg`](./mockups/06_confirmacion.jpg) — Confirmación de cita sincronizada en Firebase
- [`mockups/07_mis_reservas.jpg`](./mockups/07_mis_reservas.jpg) — Mis reservas sincronizadas en la nube y gestión de estados
