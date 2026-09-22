package com.example.cuidafamily.repository

import android.util.Log
import com.example.cuidafamily.model.FamilyGroup
import com.example.cuidafamily.model.GroupMember
import com.example.cuidafamily.model.Role
import com.example.cuidafamily.model.User
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Clase sellada para el manejo de estados de éxito o error en las operaciones.
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
}

/**
 * Envoltorio para un miembro del grupo con información del perfil de usuario.
 */
data class GroupMemberInfo(
    val member: GroupMember,
    val userName: String
)

/**
 * Repositorio de autenticación y gestión de grupos familiares usando Firebase (Auth y Firestore).
 */
object AuthRepository {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    /**
     * Registra un nuevo usuario en Firebase Auth y guarda su perfil inicial en Firestore.
     */
    suspend fun registrar(nombre: String, email: String, contrasenia: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val authResult = auth.createUserWithEmailAndPassword(email, contrasenia).await()
            val userId = authResult.user?.uid ?: throw Exception("No se pudo obtener el ID de usuario.")

            val nuevoUsuario = User(
                id = userId,
                nombre = nombre,
                correo = email,
                telefono = ""
            )

            db.collection("users").document(userId).set(nuevoUsuario).await()
            Result.Success(nuevoUsuario)
        } catch (e: Exception) {
            handleFirebaseException("registrar", e)
            Result.Error(traducirErrorAuth(e))
        }
    }

    /**
     * Realiza el inicio de sesión y recupera el perfil completo del usuario.
     */
    suspend fun login(email: String, contrasenia: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val authResult = auth.signInWithEmailAndPassword(email, contrasenia).await()
            val userId = authResult.user?.uid ?: throw Exception("Error de sesión.")

            val userDoc = db.collection("users").document(userId).get().await()
            val usuario = try {
                userDoc.toObject(User::class.java)
            } catch (e: Exception) {
                Log.e("AuthRepository", "Error deserializing User [login] for UID: $userId", e)
                null
            }

            if (usuario != null) {
                Log.d("AuthRepository", "Login exitoso - familyGroupId=${usuario.familyGroupId}, rol=${usuario.role}")
                Result.Success(usuario)
            } else {
                Result.Error(Exception("Perfil de usuario no encontrado o incompatible en la base de datos."))
            }
        } catch (e: Exception) {
            handleFirebaseException("login", e)
            Result.Error(traducirErrorAuth(e))
        }
    }

    /**
     * Crea un nuevo grupo familiar y actualiza el perfil del usuario con el ID del grupo y el rol de Admin.
     */
    suspend fun crearFamilyGroup(nombre: String, adminUserId: String, subtipo: String): Result<FamilyGroup> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida, vuelve a iniciar sesión."))

        try {
            val groupId = db.collection("familyGroups").document().id
            val codigoInvitacion = generarCodigoInvitacionUnico()

            val nuevoGrupo = FamilyGroup(
                id = groupId,
                nombre = nombre,
                codigoInvitacion = codigoInvitacion,
                adminId = adminUserId
            )

            val miembroAdmin = GroupMember(
                userId = adminUserId,
                nombre = (db.collection("users").document(adminUserId).get().await().toObject(User::class.java))?.nombre ?: "Admin",
                role = Role.ADMINISTRADOR_FAMILIAR,
                subtipo = subtipo,
                fechaIngreso = obtenerFechaActualISO8601()
            )

            val batch = db.batch()
            val groupRef = db.collection("familyGroups").document(groupId)
            val userRef = db.collection("users").document(adminUserId)

            batch.set(groupRef, nuevoGrupo)
            batch.set(groupRef.collection("members").document(adminUserId), miembroAdmin)
            
            // Actualización del perfil del usuario
            batch.update(userRef, "familyGroupId", groupId)
            batch.update(userRef, "role", Role.ADMINISTRADOR_FAMILIAR.name)
            batch.update(userRef, "subtipo", subtipo)
            
            batch.commit().await()

            Result.Success(nuevoGrupo)
        } catch (e: Exception) {
            handleFirebaseException("crearFamilyGroup", e)
            Result.Error(Exception("Error al crear el grupo familiar. Inténtelo de nuevo."))
        }
    }

    /**
     * Une a un usuario a un grupo existente y actualiza su perfil con el ID del grupo y el rol elegido.
     */
    suspend fun unirseAGrupo(codigoInvitacion: String, userId: String, rol: Role, subtipo: String): Result<FamilyGroup> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))

        try {
            val query = db.collection("familyGroups")
                .whereEqualTo("codigoInvitacion", codigoInvitacion)
                .limit(1)
                .get()
                .await()

            if (query.isEmpty) {
                return@withContext Result.Error(Exception("Código de invitación inválido."))
            }

            val groupDoc = query.documents.first()
            val grupo = try {
                groupDoc.toObject(FamilyGroup::class.java)
            } catch (e: Exception) {
                Log.e("AuthRepository", "Error deserializing FamilyGroup [unirseAGrupo] for Code: $codigoInvitacion", e)
                null
            } ?: throw Exception("Error al procesar el grupo.")

            val nuevoMiembro = GroupMember(
                userId = userId,
                nombre = (db.collection("users").document(userId).get().await().toObject(User::class.java))?.nombre ?: "Miembro",
                role = rol,
                subtipo = subtipo,
                fechaIngreso = obtenerFechaActualISO8601()
            )

            val batch = db.batch()
            val memberRef = db.collection("familyGroups").document(grupo.id)
                .collection("members").document(userId)
            val userRef = db.collection("users").document(userId)

            batch.set(memberRef, nuevoMiembro)
            // Actualización del perfil del usuario
            batch.update(userRef, "familyGroupId", grupo.id)
            batch.update(userRef, "role", rol.name)
            batch.update(userRef, "subtipo", subtipo)
            
            batch.commit().await()

            Result.Success(grupo)
        } catch (e: Exception) {
            handleFirebaseException("unirseAGrupo", e)
            Result.Error(Exception("No se pudo unir al grupo. Verifique el código."))
        }
    }

    /**
     * Obtiene los miembros de un grupo familiar.
     */
    suspend fun obtenerMiembrosDelGrupo(familyGroupId: String): Result<List<GroupMemberInfo>> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))

        try {
            val membersSnapshot = db.collection("familyGroups").document(familyGroupId)
                .collection("members").get().await()

            val listaInfo = membersSnapshot.documents.mapNotNull { doc ->
                val member = try {
                    doc.toObject(GroupMember::class.java)
                } catch (e: Exception) {
                    Log.e("AuthRepository", "Error deserializing GroupMember in Group: $familyGroupId", e)
                    null
                }
                member?.let { GroupMemberInfo(it, it.nombre) }
            }
            
            Result.Success(listaInfo)
        } catch (e: Exception) {
            handleFirebaseException("obtenerMiembrosDelGrupo", e)
            Result.Error(Exception("No se pudo recuperar la lista de miembros."))
        }
    }

    /**
     * Obtiene los detalles de un grupo por ID.
     */
    suspend fun obtenerGrupoPorId(familyGroupId: String): FamilyGroup? = withContext(Dispatchers.IO) {
        try {
            val doc = db.collection("familyGroups").document(familyGroupId).get().await()
            doc.toObject(FamilyGroup::class.java)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error deserializing FamilyGroup in obtenerGrupoPorId: $familyGroupId", e)
            handleFirebaseException("obtenerGrupoPorId", e)
            null
        }
    }

    /**
     * Busca el rol y grupo al que pertenece un usuario.
     */
    suspend fun obtenerInfoMiembroParaUsuario(userId: String): GroupMember? = withContext(Dispatchers.IO) {
        if (auth.currentUser == null) return@withContext null

        try {
            val groupsSnapshot = db.collectionGroup("members")
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .await()
            
            groupsSnapshot.documents.firstOrNull()?.toObject(GroupMember::class.java)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error deserializing GroupMember in obtenerInfoMiembroParaUsuario for UID: $userId", e)
            handleFirebaseException("obtenerInfoMiembroParaUsuario", e)
            null
        }
    }

    /**
     * Busca el ID del grupo al que pertenece un miembro.
     */
    suspend fun obtenerGroupIdDeMiembro(userId: String): String? = withContext(Dispatchers.IO) {
        if (auth.currentUser == null) return@withContext null

        try {
            val groupsSnapshot = db.collectionGroup("members")
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .await()
            
            // members doc -> members collection -> group doc
            groupsSnapshot.documents.firstOrNull()?.reference?.parent?.parent?.id
        } catch (e: Exception) {
            handleFirebaseException("obtenerGroupIdDeMiembro", e)
            null
        }
    }

    /**
     * Cierra la sesión activa.
     */
    fun cerrarSesion() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error al cerrar sesión", e)
        }
    }

    /**
     * Genera un código aleatorio de 6 dígitos único.
     */
    private suspend fun generarCodigoInvitacionUnico(): String {
        var codigo: String
        var existe: Boolean
        do {
            codigo = (100000..999999).random().toString()
            val check = db.collection("familyGroups")
                .whereEqualTo("codigoInvitacion", codigo)
                .limit(1)
                .get()
                .await()
            existe = !check.isEmpty
        } while (existe)
        return codigo
    }

    private fun obtenerFechaActualISO8601(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    /**
     * Traduce las excepciones de Firebase Auth.
     */
    private fun traducirErrorAuth(e: Exception): Exception {
        val mensaje = when (e) {
            is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con este correo. Intenta iniciar sesión o usa otro correo."
            is FirebaseAuthWeakPasswordException -> "La contraseña debe tener al menos 6 caracteres."
            is FirebaseAuthInvalidCredentialsException -> "El formato del correo no es válido o las credenciales son incorrectas."
            is FirebaseAuthInvalidUserException -> "No existe una cuenta con ese correo."
            else -> "Ocurrió un error de autenticación. Intente de nuevo."
        }
        return Exception(mensaje)
    }

    private fun handleFirebaseException(method: String, e: Exception) {
        if (e is FirebaseFirestoreException) {
            Log.e("AuthRepository", "Firestore Error [$method]: Code=${e.code} - ${e.message}")
        } else if (e is FirebaseException) {
            Log.e("AuthRepository", "Firebase Error [$method]: ${e.javaClass.simpleName} - ${e.message}")
        } else {
            Log.e("AuthRepository", "General Error [$method]: ${e.javaClass.simpleName} - ${e.message}")
        }
    }
}
