package com.example.filtro400.domain

import com.example.filtro400.ReglaBloqueo
import com.example.filtro400.data.Accion
import com.example.filtro400.data.Filtro
import com.example.filtro400.data.FranjaNoMolestar
import com.example.filtro400.data.NumeroLista
import java.time.LocalDateTime

// Motor de decision puro: no conoce Android ni la base de datos ni el reloj.
//
// Orden de evaluacion:
// 1. Numero vacio/oculto -> permitir (OCULTO), salvo ajuste de bloquear ocultos.
// 2. Lista blanca exacta -> permitir siempre (LISTA_BLANCA).
// 3. Numero bloqueado exacto -> bloquear (NUMERO/HORARIO si tiene franja).
// 4. Prefijos activos dentro de su horario, por prioridad descendente; en empate
//    gana el prefijo mas largo y luego el id.
// 5. Modo No molestar global activo y dentro de una franja -> bloquear (NO_MOLESTAR).
// 6. Sin coincidencias -> permitir (NINGUNA).
object MotorFiltrado {

    private val momentoCompatibilidad = LocalDateTime.of(2000, 1, 3, 12, 0)

    fun evaluar(
        numero: String,
        listaBlanca: List<NumeroLista>,
        numerosBloqueados: List<NumeroLista>,
        filtros: List<Filtro>,
        franjasNoMolestar: List<FranjaNoMolestar> = emptyList(),
        modoNoMolestarActivo: Boolean = false,
        momento: LocalDateTime = momentoCompatibilidad,
        bloquearOcultos: Boolean = false
    ): Decision {
        val normalizado = ReglaBloqueo.normalizar(numero)
        if (normalizado.isEmpty()) {
            if (bloquearOcultos) {
                return Decision(true, ReglaTipo.OCULTO, null)
            }
            if (noMolestarActivo(franjasNoMolestar, modoNoMolestarActivo, momento)) {
                return Decision(true, ReglaTipo.NO_MOLESTAR, null)
            }
            return Decision(false, ReglaTipo.OCULTO, null)
        }

        val enListaBlanca = listaBlanca.firstOrNull {
            it.activo && ReglaBloqueo.normalizar(it.numero) == normalizado
        }
        if (enListaBlanca != null) {
            return Decision(false, ReglaTipo.LISTA_BLANCA, normalizado)
        }

        val bloqueado = numerosBloqueados.firstOrNull {
            it.activo &&
                ReglaBloqueo.normalizar(it.numero) == normalizado &&
                (it.horario == null || it.horario.contiene(momento))
        }
        if (bloqueado != null) {
            return Decision(
                bloquear = true,
                reglaTipo = if (bloqueado.horario != null) ReglaTipo.HORARIO else ReglaTipo.NUMERO,
                reglaValor = normalizado
            )
        }

        val prefijoCoincidente = filtros.asSequence()
            .filter { it.activo && (it.horario == null || it.horario.contiene(momento)) }
            .map { it to ReglaBloqueo.normalizar(it.prefijo) }
            .filter { (_, prefijo) -> prefijo.isNotEmpty() && normalizado.startsWith(prefijo) }
            .sortedWith(
                compareByDescending<Pair<Filtro, String>> { it.first.prioridad }
                    .thenByDescending { it.second.length }
                    .thenBy { it.first.id }
            )
            .firstOrNull()

        if (prefijoCoincidente != null) {
            val (filtro, prefijo) = prefijoCoincidente
            return Decision(
                bloquear = filtro.accion == Accion.BLOQUEAR,
                reglaTipo = if (filtro.horario != null) ReglaTipo.HORARIO else ReglaTipo.PREFIJO,
                reglaValor = prefijo
            )
        }

        if (noMolestarActivo(franjasNoMolestar, modoNoMolestarActivo, momento)) {
            return Decision(true, ReglaTipo.NO_MOLESTAR, null)
        }

        return Decision(false, ReglaTipo.NINGUNA, null)
    }

    private fun noMolestarActivo(
        franjas: List<FranjaNoMolestar>,
        modoActivo: Boolean,
        momento: LocalDateTime
    ): Boolean = modoActivo && franjas.any { it.activo && it.horario.contiene(momento) }
}
