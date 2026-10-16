# Mapa de Funcionalidades y Código Fuente - CuidaFamily

Este documento es una guía exhaustiva e índice del proyecto **CuidaFamily**, diseñada para ubicar de forma rápida cada pantalla, componente de interfaz, lógica de negocio, repositorios y modelos de datos.

---

## 1. Módulos y Componentes del Sistema

| Funcionalidad | Archivo | Función/Composable principal | Carpeta |
|---|---|---|---|
| **Splash / Verificación de Sesión** | `SplashScreen.kt` | `SplashScreen()` | `ui/auth/` |
| **Inicio de Sesión (Login)** | `AuthScreens.kt` | `LoginScreen()` | `ui/auth/` |
| **Registro de Usuario (Datos Personales)** | `AuthScreens.kt` | `RegistroScreen()` | `ui/auth/` |
| **Selección de Rol de Usuario** | `AuthScreens.kt` | `SeleccionRolScreen()` | `ui/auth/` |
| **Sub-selección de Parentesco / Perfil** | `AuthScreens.kt` | `SubtipoRolScreen()` | `ui/auth/` |
| **Crear o Unirse a Grupo Familiar** | `AuthScreens.kt` | `ConfiguracionGrupoScreen()` | `ui/auth/` |
| **Contenedor Flujo de Autenticación** | `AuthScreens.kt` | `AuthFlowContainer()` | `ui/auth/` |
| **ViewModel de Autenticación y Grupos** | `AuthViewModel.kt` | `AuthViewModel` | `ui/auth/` |
| **Repositorio de Usuarios y Grupos (Firestore)** | `AuthRepository.kt` | `AuthRepository` | `repository/` |
| **Modelos de Usuario, Grupo y Roles** | `Models.kt` | `User`, `FamilyGroup`, `Role` | `model/` |
| **Pantalla Principal / Menú de Bienvenida** | `MainActivity.kt` | `WelcomeScreen()`, `MainActivity` | `com.example.cuidafamily` |
| **Contenedor Ficha Médica** | `PatientScreens.kt` | `PatientScreenContainer()` | `ui/patient/` |
| **Ficha Médica (Estado Vacío)** | `PatientScreens.kt` | `PatientEmptyState()` | `ui/patient/` |
| **Ficha Médica (Formulario Alta/Edición)** | `PatientScreens.kt` | `PatientFormScreen()` | `ui/patient/` |
| **Ficha Médica (Lectura y Detalles)** | `PatientScreens.kt` | `PatientDetailsScreen()` | `ui/patient/` |
| **Ficha Médica (Edición Rápida por Sección)** | `PatientScreens.kt` | `DialogoEditarFichaSeccion()` | `ui/patient/` |
| **ViewModel Ficha Médica** | `PatientViewModel.kt` | `PatientViewModel` | `ui/patient/` |
| **Repositorio Ficha Médica y Alergias** | `PatientRepository.kt` | `PatientRepository` | `repository/` |
| **Modelos Paciente, Alergias y Peso** | `MedicalModels.kt` | `Patient`, `Allergy`, `WeightRecord` | `model/` |
| **Contenedor Calendario / Agenda** | `CalendarScreens.kt` | `CalendarScreenContainer()` | `ui/calendar/` |
| **Grilla Visual del Calendario** | `CalendarScreens.kt` | `GrillaCalendario()` | `ui/calendar/` |
| **Formulario Crear / Editar Evento** | `CalendarScreens.kt` | `FormularioCrearEventoDialog()` | `ui/calendar/` |
| **Eliminar Evento del Calendario** | `CalendarScreens.kt` | `DialogoConfirmacionEliminarEvento()` | `ui/calendar/` |
| **Confirmación de Asistencia (Pantalla y Botones)** | `ConfirmacionAsistenciaScreen.kt` | `ConfirmacionAsistenciaScreen()` | `ui/calendar/` |
| **ViewModel Calendario y Eventos** | `CalendarViewModel.kt` | `CalendarViewModel` | `ui/calendar/` |
| **Repositorio Calendario en Tiempo Real** | `CalendarRepository.kt` | `CalendarRepository` | `repository/` |
| **Modelos Evento de Calendario** | `CalendarModels.kt` | `CalendarEvent`, `TipoEvento`, `EstadoConfirmacion` | `model/` |
| **Botón Flotante SOS** | `SosComponents.kt` | `SosFloatingButton()` | `ui/sos/` |
| **Diálogo Confirmación Disparo SOS** | `SosComponents.kt` | `SosConfirmationDialog()` | `ui/sos/` |
| **Alerta Recibida SOS (Overlay de Emergencia)** | `SosComponents.kt` | `SosIncomingAlertOverlayDialog()` | `ui/sos/` |
| **ViewModel Alertas SOS** | `SosViewModel.kt` | `SosViewModel` | `ui/sos/` |
| **Repositorio Alertas SOS (Escucha Activa)** | `SosRepository.kt` | `SosRepository` | `repository/` |
| **Reproductor de Alarma Sonora SOS** | `AlarmSoundPlayer.kt` | `AlarmSoundPlayer` | `util/` |
| **Modelo Alerta SOS** | `SosAlert.kt` | `SosAlert` | `model/` |
| **Contenedor Bitácora de Novedades** | `BitacoraScreens.kt` | `BitacoraScreenContainer()` | `ui/bitacora/` |
| **Formulario Nueva Publicación** | `BitacoraScreens.kt` | `CrearPublicacionDialog()` | `ui/bitacora/` |
| **Tarjeta Publicación de Novedad** | `BitacoraScreens.kt` | `TarjetaPublicacionBitacora()` | `ui/bitacora/` |
| **ViewModel Bitácora de Novedades** | `BitacoraViewModel.kt` | `BitacoraViewModel` | `ui/bitacora/` |
| **Repositorio Bitácora en Firestore** | `BitacoraRepository.kt` | `BitacoraRepository` | `repository/` |
| **Modelos Entrada de Bitácora y Estado Anímico** | `BitacoraModels.kt` | `BitacoraEntrada`, `EstadoAnimo` | `model/` |
| **Contenedor Documentos Médicos** | `DocumentoScreens.kt` | `DocumentoScreenContainer()` | `ui/document/` |
| **Formulario Registrar Documento Médico** | `DocumentoScreens.kt` | `SubirDocumentoDialog()` | `ui/document/` |
| **Tarjeta / Visor Documento Médico** | `DocumentoScreens.kt` | `TarjetaDocumentoMedico()` | `ui/document/` |
| **Exportar Historial (Pantalla Opciones)** | `ExportScreens.kt` | `ExportarHistorialScreen()` | `ui/export/` |
| **Generador de Reportes PDF y CSV** | `ExportHelper.kt` | `ExportHelper` | `util/` |
| **ViewModel Documentos Médicos** | `DocumentoViewModel.kt` | `DocumentoViewModel` | `ui/document/` |
| **Repositorio Documentos Médicos** | `DocumentoRepository.kt` | `DocumentoRepository` | `repository/` |
| **Modelos Documento Médico** | `DocumentoModels.kt` | `DocumentoMedico`, `TipoDocumento` | `model/` |
| **Contenedor Gestión de Turnos** | `TurnoScreens.kt` | `TurnoScreenContainer()` | `ui/turnos/` |
| **Barra Selector de Semana** | `TurnoScreens.kt` | `SemanaSelectorBar()` | `ui/turnos/` |
| **Formulario Asignar / Editar Turno** | `TurnoScreens.kt` | `CrearTurnoDialog()` | `ui/turnos/` |
| **Tarjeta de Turno (Editar, Eliminar, Avisos)** | `TurnoScreens.kt` | `TurnoCard()` | `ui/turnos/` |
| **Tarjeta Pendiente de Respuesta (Cuidador)** | `TurnoScreens.kt` | `TarjetaTurnoPendienteRespuesta()` | `ui/turnos/` |
| **Tarjeta Respuesta Reciente (Admin)** | `TurnoScreens.kt` | `TarjetaRespuestaRecienteAdmin()` | `ui/turnos/` |
| **Configuración Domicilio Referencia GPS** | `TurnoScreens.kt` | `ConfigurarDomicilioDialog()` | `ui/turnos/` |
| **Reasignación de Turno** | `TurnoScreens.kt` | `ReasignarTurnoDialog()` | `ui/turnos/` |
| **ViewModel Gestión de Turnos** | `TurnoViewModel.kt` | `TurnoViewModel` | `ui/turnos/` |
| **Repositorio Turnos y Check-In GPS** | `TurnoRepository.kt` | `TurnoRepository` | `repository/` |
| **Modelos Turno y Check-In GPS** | `TurnoModels.kt` | `Turno`, `TurnoCheckIn`, `EstadoTurno` | `model/` |
| **Grupo Familiar (Código Invitación y Miembros)** | `GrupoFamiliarScreens.kt` | `GrupoFamiliarScreen()` | `ui/group/` |
| **Helper Notificaciones y Canales** | `NotificationHelper.kt` | `NotificationHelper` | `util/` |
| **Servicio de Monitoreo en Segundo Plano** | `CuidaFamilyMonitorService.kt` | `CuidaFamilyMonitorService` | `service/` |
| **Selector de Fecha en Español** | `SelectorFechaDialog.kt` | `SelectorFechaDialog()` | `ui/util/` |
| **Selector de Hora con Rueda** | `WheelPicker.kt` | `WheelTimePicker()` | `ui/util/` |
| **Fondo Decorado y Avatares de Usuario** | `BackgroundComponents.kt` | `AppBackgroundDecorated()`, `UserAvatar()` | `ui/util/` |
| **Paleta de Colores del Sistema** | `Color.kt` | `Color.kt` | `ui/theme/` |
| **Tema Compose (Modo Claro / Oscuro)** | `Theme.kt` | `CuidaFamilyTheme()` | `ui/theme/` |
| **Estilos Tipográficos Reutilizables** | `Type.kt` | `Typography`, `SeccionTituloStyle` | `ui/theme/` |

---

## 2. ¿Dónde cambio...? (Guía para Modificaciones Frecuentes)

Esta sección orienta paso a paso dónde realizar modificaciones según el tipo de solicitud:

### 1. "Agregar un campo al registro de usuarios"
- **Modelo**: [Models.kt](file:///C:/Users/efe/CuidaFamily/app/src/main/java/com/example/cuidafamily/model/Models.kt) -> Agregar la propiedad a la data class `User`.
- **Formulario UI**: [AuthScreens.kt](file:///C:/Users/efe/CuidaFamily/app/src/main/java/com/example/cuidafamily/ui/auth/AuthScreens.kt) -> Modificar `RegistroScreen()` para incluir el `OutlinedTextField` o control UI.
- **Estado ViewModel**: [AuthViewModel.kt](file:///C:/Users/efe/CuidaFamily/app/src/main/java/com/example/cuidafamily/ui/auth/AuthViewModel.kt) -> Agregar el campo a `AuthUiState` y la función para actualizarlo.
- **Guardado en Firestore**: [AuthRepository.kt](file:///C:/Users/efe/CuidaFamily/app/src/main/java/com/example/cuidafamily/repository/AuthRepository.kt) -> Incluir el campo en la función `registrar()`.

---

### 2. "Agregar un botón o campo nuevo en la Ficha Médica"
- **Modelo**: [MedicalModels.kt](file:///C:/Users/efe/CuidaFamily/app/src/main/java/com/example/cuidafamily/model/MedicalModels.kt) -> Agregar el atributo a `Patient`.
- **Formulario Alta/Edición**: [PatientScreens.kt](file:///C:/Users/efe/CuidaFamily/app/src/main/java/com/example/cuidafamily/ui/patient/PatientScreens.kt) -> Modificar `PatientFormScreen()` o `DialogoEditarFichaSeccion()`.
- **Vista Lectura**: [PatientScreens.kt](file:///C:/Users/efe/CuidaFamily/app/src/main/java/com/example/cuidafamily/ui/patient/PatientScreens.kt) -> Incluir la visualización en `PatientDetailsScreen()`.
- **Guardado**: [PatientRepository.kt](file:///C:/Users/efe/CuidaFamily/app/src/main/java/com/example/cuidafamily/repository/PatientRepository.kt) -> Actualizar en `crearOEditarPatient()`.

---

### 3. "Cambiar los colores o estilos visuales de la app"
- **Paleta de Colores**: [Color.kt](file:///C:/Users/efe/CuidaFamily/app/src/main/java/com/example/cuidafamily/ui/theme/Color.kt) -> Modificar constantes como `LavandaPrimary`, `RosaPastelBackground`, etc.
- **Tema Compose**: [Theme.kt](file:///C:/Users/efe/CuidaFamily/app/src/main/java/com/example/cuidafamily/ui/theme/Theme.kt) -> Ajustar `LightColorScheme` y `DarkColorScheme`.
- **Tipografías y Tamaños**: [Type.kt](file:///C:/Users/efe/CuidaFamily/app/src/main/java/com/example/cuidafamily/ui/theme/Type.kt) -> Modificar `SeccionTituloStyle`, `NombreDestacadoStyle`, etc.

---

### 4. "Agregar un tipo nuevo de evento al Calendario"
- **Modelo Enum**: [CalendarModels.kt](file:///C:/Users/efe/CuidaFamily/app/src/main/java/com/example/cuidafamily/model/CalendarModels.kt) -> Agregar el nuevo valor al `enum class TipoEvento(val etiqueta: String, val emoji: String, val colorHex: String)`.
- **Formulario UI**: [CalendarScreens.kt](file:///C:/Users/efe/CuidaFamily/app/src/main/java/com/example/cuidafamily/ui/calendar/CalendarScreens.kt) -> `FormularioCrearEventoDialog()` lo incluirá automáticamente en la lista de opciones.

---

### 5. "Agregar un rol nuevo de usuario (ej. Médico Tratante)"
- **Enum de Roles**: [Models.kt](file:///C:/Users/efe/CuidaFamily/app/src/main/java/com/example/cuidafamily/model/Models.kt) -> Agregar el valor al `enum class Role`.
- **Selección en Registro**: [AuthScreens.kt](file:///C:/Users/efe/CuidaFamily/app/src/main/java/com/example/cuidafamily/ui/auth/AuthScreens.kt) -> Actualizar `SeleccionRolScreen()` para agregar la tarjeta del rol.
- **Sub-selección**: [AuthScreens.kt](file:///C:/Users/efe/CuidaFamily/app/src/main/java/com/example/cuidafamily/ui/auth/AuthScreens.kt) -> `SubtipoRolScreen()` para definir sus opciones de perfil.

---

### 6. "Agregar un campo nuevo a los Turnos de cuidado"
- **Modelo**: [TurnoModels.kt](file:///C:/Users/efe/CuidaFamily/app/src/main/java/com/example/cuidafamily/model/TurnoModels.kt) -> Agregar la propiedad a `Turno`.
- **Formulario UI**: [TurnoScreens.kt](file:///C:/Users/efe/CuidaFamily/app/src/main/java/com/example/cuidafamily/ui/turnos/TurnoScreens.kt) -> Modificar `CrearTurnoDialog()`.
- **Visualización**: [TurnoScreens.kt](file:///C:/Users/efe/CuidaFamily/app/src/main/java/com/example/cuidafamily/ui/turnos/TurnoScreens.kt) -> Incluirlo en `TurnoCard()`.
- **ViewModel / Guardado**: [TurnoViewModel.kt](file:///C:/Users/efe/CuidaFamily/app/src/main/java/com/example/cuidafamily/ui/turnos/TurnoViewModel.kt) y [TurnoRepository.kt](file:///C:/Users/efe/CuidaFamily/app/src/main/java/com/example/cuidafamily/repository/TurnoRepository.kt) -> Actualizar `crearTurno()` y `editarTurno()`.

---

### 7. "Modificar el sonido, canal o comportamiento de notificaciones"
- **Creación de Canales y Sonidos**: [NotificationHelper.kt](file:///C:/Users/efe/CuidaFamily/app/src/main/java/com/example/cuidafamily/util/NotificationHelper.kt) -> Editar `inicializarCanales()`.
- **Emisión de Notificaciones**: [NotificationHelper.kt](file:///C:/Users/efe/CuidaFamily/app/src/main/java/com/example/cuidafamily/util/NotificationHelper.kt) -> Funciones `mostrarNotificacionBitacora()`, `mostrarNotificacionTurno()`, `mostrarNotificacionRespuestaTurno()`, `mostrarNotificacionCambioTurno()`, `mostrarNotificacionSos()`.
- **Escucha en Segundo Plano**: [CuidaFamilyMonitorService.kt](file:///C:/Users/efe/CuidaFamily/app/src/main/java/com/example/cuidafamily/service/CuidaFamilyMonitorService.kt) -> Editar `iniciarListeners()`.
