# PET SPA — Aplicación Móvil Android de Reservas Caninas

Aplicación nativa para Android desarrollada en **Kotlin** y **Jetpack Compose (Material Design 3)** que permite a los clientes consultar servicios de baño y arreglo estético para perros, calcular precios en **pesos colombianos (COP)** según el tamaño de la mascota, consultar disponibilidad en un calendario interactivo y gestionar sus citas desde el celular.

---

## 1. Descripción del proyecto

**PET SPA** está diseñada con una estética limpia y amigable basada en tonos **verde menta, blanco, beige y azul claro**, tipografía redondeada (`Nunito` + `Plus Jakarta Sans`), ícono adaptativo propio y diseño responsivo compatible tanto con teléfonos (`NavigationBar`) como con pantallas medianas/grandes (`NavigationRail`).

### Funcionalidades principales

- **Inicio**:
  - Presentación de la marca **PET SPA** con ilustración principal y el mensaje *"¡El bienestar de tu mascota comienza aquí!"*.
  - Botón de acceso rápido **"Reservar una cita"**.
  - Sección **Próximas reservas** (visible únicamente cuando existen citas confirmadas registradas).
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
  - Franjas horarias individuales (`8:00 a. m.`, `9:00 a. m.`, `10:00 a. m.`, `11:00 a. m.`, `1:00 p. m.`, `2:00 p. m.`, `3:00 p. m.`, `4:00 p. m.`, `5:00 p. m.`) diferenciadas por color: **verde** (disponible), **gris** (ocupado/pasado) y **menta oscuro** (seleccionado).
- **Formulario validado, resumen y confirmación**:
  - Captura de nombre del cliente, número de celular, nombre de la mascota y raza (opcional), con opción de reutilizar datos de cliente/mascota guardados previamente.
  - Resumen completo con precio final antes de confirmar.
  - Prevención atómica de reservas duplicadas en base de datos y pantalla de comprobante con *"¡Tu cita ha sido reservada exitosamente!"*.
- **Mis reservas**:
  - Listado persistente ordenado mostrando primero las próximas citas confirmadas.
  - Filtros por estado (**Todas**, **Confirmadas**, **Completadas**, **Canceladas**).
  - Opción de **Cancelar reserva** con diálogo de confirmación que libera inmediatamente el horario en el calendario.

> **Nota sobre persistencia local:** En esta primera versión, todas las citas, clientes y mascotas se almacenan localmente en el dispositivo mediante **Room Database**. La aplicación informa de forma transparente al usuario que todavía no existe sincronización en tiempo real entre diferentes clientes.

---

## 2. Arquitectura y estructura técnica

La aplicación sigue el patrón **MVVM (Model–View–ViewModel)** combinado con el **Patrón Repositorio** para desacoplar la lógica de negocio y el acceso a datos de la capa de presentación.

### Flujo de datos

```text
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
│  • Filtros y ordenamiento de citas                       │
└────────────────────────────┬─────────────────────────────┘
                             │ interfaz PetSpaRepository
                             ▼
┌──────────────────────────────────────────────────────────┐
│        LocalRoomPetSpaRepository (Implementación)        │
│  • Siembra idempotente de servicios y tarifas COP        │
│  • Actualización automática de citas pasadas a Completada│
│  • Creación atómica sin doble reserva                    │
└────────────────────────────┬─────────────────────────────┘
                             │ Room DAO + Flow
                             ▼
┌──────────────────────────────────────────────────────────┐
│             PetSpaDatabase (SQLite / Room)               │
│  clients • pets • services • service_prices • appointments│
└──────────────────────────────────────────────────────────┘
```

> **Preparada para la nube:** Al depender de la interfaz `PetSpaRepository` y modelos de dominio independientes (`domain/model`), es posible incorporar una implementación remota con **Firebase Firestore** en futuras versiones sin reescribir los ViewModels ni las pantallas Compose.

### Estructura de paquetes principales

```text
app/src/main/java/com/example/
├── MainActivity.kt                      # Punto de entrada Edge-to-Edge e inyección del repositorio
├── domain/model/
│   ├── SpaModels.kt                     # Entidades de dominio (DogSize, SpaService, Appointment, BookingStatus)
│   └── SpaDateUtils.kt                  # Utilidades de calendario (compatible desde API 24) y formato COP
├── data/local/
│   ├── Entities.kt                      # Tablas Room (ClientEntity, PetEntity, ServiceEntity, ServicePriceEntity, AppointmentEntity)
│   ├── PetSpaDao.kt                     # Consultas reactivas Flow y transacción createBookingAtomically
│   ├── PetSpaDatabase.kt                # Base de datos Room singleton
│   ├── PetSpaRepository.kt              # Contrato abstracto del repositorio
│   └── LocalRoomPetSpaRepository.kt     # Implementación local con Room
└── ui/
    ├── PetSpaViewModel.kt               # Gestión centralizada de estados con StateFlow
    ├── navigation/PetSpaApp.kt          # Grafo de Navigation Compose y barra/rail adaptativo
    ├── components/
    │   ├── DogSizeIllustration.kt       # Ilustraciones vectoriales en Canvas para Pequeño, Mediano y Grande
    │   └── PetSpaComponents.kt          # Tarjetas de servicio, indicador de pasos, badges y avisos
    ├── screens/
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
- **Build & Tooling:** Android Gradle Plugin `9.1.1`, KSP `2.3.5`, Version Catalog (`gradle/libs.versions.toml`)
- **UI:** Jetpack Compose (BOM `2024.09.00`), Material Design 3, Material Icons Extended
- **Navegación:** Navigation Compose `2.8.9`
- **Persistencia:** Room `2.7.0` (`room-runtime`, `room-ktx`, `room-compiler`)
- **Asincronía:** Kotlin Coroutines & Flow `1.10.2`
- **Pruebas:** JUnit 4, Robolectric `4.16.1`, Compose UI Test (`ui-test-junit4`)
- **Compatibilidad SDK:** `minSdk = 24` (Android 7.0) • `targetSdk = 36` • `compileSdk = 36`
- **Application ID:** `com.aistudio.petspa.vqmzrk`

---

## 3. Instrucciones de despliegue y ejecución

### Requisitos previos

- **Android Studio** (Meerkat / Narwhal o superior recomendado para AGP 9.1).
- **JDK 17** (o superior, configurado para compilar bytecode Java 11).
- **Android SDK 36** instalado con al menos un emulador Android (API 24–36) o un dispositivo físico con depuración USB habilitada.

### Ejecución desde Android Studio

1. Abrir la carpeta raíz del proyecto en **Android Studio**.
2. Esperar a que finalice la sincronización de **Gradle** (`Sync Project with Gradle Files`).
3. Seleccionar un dispositivo virtual (AVD) o teléfono físico en la barra superior.
4. Hacer clic en **Run (`app`)** para compilar, instalar e iniciar **PET SPA**.

### Compilación y pruebas por línea de comandos

Desde la raíz del proyecto:

```bash
# 1. Compilar el APK de depuración
gradle :app:assembleDebug

# 2. Ejecutar todas las pruebas unitarias, de Room y de flujo UI (Robolectric)
gradle :app:testDebugUnitTest

# 3. Generar APK o Bundle (AAB) de release (requiere variables de firma KEYSTORE_PATH, STORE_PASSWORD, KEY_PASSWORD)
gradle :app:assembleRelease
gradle :app:bundleRelease
```

El APK de depuración generado se ubica en:
`app/build/outputs/apk/debug/app-debug.apk`

> **Nota de firma:** El archivo `app/build.gradle.kts` utiliza `debug.keystore` para compilaciones `debug` y variables de entorno (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`) para `release`. No se requieren claves de API externas ni archivos `local.properties` para ejecutar la aplicación.

---

## 4. Evidencias de verificación y pruebas

El proyecto cuenta con compilación validada y una suite automatizada de **7 pruebas** (unitarias, de persistencia Room y de flujo UI extremo a extremo sobre Robolectric) ejecutadas con éxito mediante `gradle :app:testDebugUnitTest`.

### Cobertura de pruebas verificadas

| Suite / Archivo | Casos verificados | Resultado |
|---|---|---|
| `ExampleUnitTest` | Verificación base de entorno JUnit. | Aprobada |
| `ExampleRobolectricTest` | • Recurso `app_name` configurado como `"PET SPA"`.<br>• Siembra automática de los 2 servicios y las 6 tarifas exactas en COP (`$35.000`–`$100.000`) con `0` reservas ficticias iniciales.<br>• Prevención atómica de reservas duplicadas en misma fecha y hora (`SlotAlreadyTaken`).<br>• Cancelación de cita que cambia su estado a `Cancelada` y libera el horario para una nueva reserva.<br>• Bloqueo de domingos (día de descanso) y formato de moneda COP. | Aprobada |
| `PetSpaUiFlowTest` | • Desplazamiento vertical en **Inicio** mostrando héroe, ambos servicios y aviso de almacenamiento local.<br>• Flujo completo de reserva: **Inicio** → **Servicio** → **Tamaño Mediano (`$45.000 COP`)** → **Calendario** (scroll y selección de hora disponible) → **Formulario** (scroll con teclado y confirmación) → **Confirmación** → **Mis reservas** (cita persistida) → regreso a **Inicio** (sección **Próximas reservas** visible) → apertura limpia de **Reservar**. | Aprobada |

### Comandos de verificación ejecutados

- **Compilación del proyecto (`compile_applet` / `:app:assembleDebug`)**: `BUILD SUCCESSFUL` sin errores de recursos, KSP ni Kotlin.
- **Pruebas automatizadas (`gradle :app:testDebugUnitTest`)**: `BUILD SUCCESSFUL` — 7 pruebas completadas, 0 fallidas.
- **Reporte HTML generado tras ejecutar pruebas**: `app/build/reports/tests/testDebugUnitTest/index.html`.
