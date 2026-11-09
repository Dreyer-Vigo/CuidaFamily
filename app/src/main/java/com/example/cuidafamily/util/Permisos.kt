package com.example.cuidafamily.util

import com.example.cuidafamily.model.Role

object Permisos {
    /**
     * Requerimiento 15: El acceso a la información financiera es para los FAMILIARES del grupo,
     * es decir ADMINISTRADOR_FAMILIAR y COLABORADOR. El CUIDADOR_EXTERNO NO debe tener acceso.
     */
    fun puedeVerFinanzas(rol: Role?): Boolean {
        return rol == Role.ADMINISTRADOR_FAMILIAR || rol == Role.COLABORADOR
    }
}
