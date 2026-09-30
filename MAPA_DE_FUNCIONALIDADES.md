# Mapa de Funcionalidades - CuidaFamily

Este documento sirve como índice y guía rápida para ubicar el código fuente de cada funcionalidad del proyecto.

## Pantallas y Componentes de Interfaz (UI)

| Funcionalidad | Archivo | Ubicación | Composable / Función Principal |
|---|---|---|---|
| Splash / Verificación de sesión inicial | `SplashScreen.kt` | `ui/auth/` | `SplashScreen()` |
| Inicio de sesión (Login) | `AuthScreens.kt` | `ui/auth/` | `LoginScreen()` |
| Crear cuenta (Datos personales) | `AuthScreens.kt` | `ui/auth/` | `RegistroScreen()` |
| Selección de rol de usuario | `AuthScreens.kt` | `ui/auth/` | `SeleccionRolScreen()` |
| Sub-selección de parentesco / perfil | `AuthScreens.kt` | `ui/auth/` | `SubtipoRolScreen()` |
| Crear o unirse a grupo familiar | `AuthScreens.kt` | `ui/auth/` | `ConfiguracionGrupoScreen()` |
| Flujo principal de autenticación | `AuthScreens.kt` | `ui/auth/` | `AuthFlowContainer()` |
| Menú principal / Pantalla de bienvenida | `MainActivity.kt` | `com.example.cuidafamily` | `WelcomeScreen()` |
| Contenedor principal Ficha Médica | `PatientScreens.kt` | `ui/patient/` | `PatientScreenContainer()` |
| Ficha médica (Estado vacío) | `PatientScreens.kt` | `ui/patient/` | `PatientEmptyState()` |
| Ficha médica (Formulario alta / edición) | `PatientScreens.kt` | `ui/patient/` | `PatientFormScreen()` |
| Ficha médica (Lectura y detalles) | `PatientScreens.kt` | `ui/patient/` | `PatientDetailsScreen()` |
| Calendario y Agenda (Vista general) | `CalendarScreens.kt` | `ui/calendar/` | `CalendarScreenContainer()` |
| Crear / editar evento de agenda | `CalendarScreens.kt` | `ui/calendar/` | `FormularioCrearEventoDialog()` |
| Grupo Familiar y Miembros | `GrupoFamiliarScreens.kt` | `ui/group/` | `GrupoFamiliarScreen()` |

## ViewModel y Gestión de Estado

| Capa / Funcionalidad | Archivo | Ubicación | Clase |
|---|---|---|---|
| Estado y flujo de autenticación | `AuthViewModel.kt` | `ui/auth/` | `AuthViewModel` |
| Estado de Ficha Médica y Alergias | `PatientViewModel.kt` | `ui/patient/` | `PatientViewModel` |
| Estado del Calendario y Eventos | `CalendarViewModel.kt` | `ui/calendar/` | `CalendarViewModel` |

## Repositorios y Lógica de Datos (Firebase)

| Funcionalidad | Archivo | Ubicación | Objeto / Repositorio |
|---|---|---|---|
| Autenticación, Usuarios y Grupos | `AuthRepository.kt` | `repository/` | `AuthRepository` |
| Ficha Médica y Alergias en Firestore | `PatientRepository.kt` | `repository/` | `PatientRepository` |
| Eventos de Calendario en Firestore | `CalendarRepository.kt` | `repository/` | `CalendarRepository` |

## Modelos de Datos

| Modelo | Archivo | Ubicación |
|---|---|---|
| Usuario, Roles y Grupo Familiar | `Models.kt` | `model/` |
| Paciente y Alergias | `MedicalModels.kt` | `model/` |
| Eventos y Tipos de Evento | `CalendarModels.kt` | `model/` |
