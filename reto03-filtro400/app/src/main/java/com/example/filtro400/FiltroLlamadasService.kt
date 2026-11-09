package com.example.filtro400

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.example.filtro400.data.BaseDatos
import com.example.filtro400.data.Ajustes
import com.example.filtro400.data.FiltroRepository
import com.example.filtro400.data.FranjaNoMolestarRepository
import com.example.filtro400.data.ListaBlancaRepository
import com.example.filtro400.data.NumerosBloqueadosRepository
import com.example.filtro400.data.Preferencias
import com.example.filtro400.data.RegistroLlamadasRepository
import com.example.filtro400.domain.Decision
import com.example.filtro400.domain.MotorFiltrado
import com.example.filtro400.domain.ReglaTipo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime

// CallScreeningService: Android avisa antes de que suene una llamada entrante
// y aqui decidimos si se bloquea. Solo aplica a numeros que no estan en contactos.
class FiltroLlamadasService : CallScreeningService() {

    private val helper by lazy { BaseDatos.obtener(applicationContext) }
    private val filtros by lazy { FiltroRepository(helper) }
    private val numerosBloqueados by lazy { NumerosBloqueadosRepository(helper) }
    private val listaBlanca by lazy { ListaBlancaRepository(helper) }
    private val franjas by lazy { FranjaNoMolestarRepository(helper) }
    private val registro by lazy { RegistroLlamadasRepository(helper) }

    // El servicio puede destruirse justo despues de responder; no cancelamos el
    // registro para no perder la escritura en curso.
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        crearCanalNotificaciones()
    }

    override fun onScreenCall(callDetails: Call.Details) {
        var esEntrante = false
        var numero = ""
        var decision = Decision(bloquear = false, reglaTipo = ReglaTipo.NINGUNA, reglaValor = null)
        var ajustes = Ajustes()

        try {
            ajustes = Preferencias.leer(this)
            esEntrante = callDetails.callDirection == Call.Details.DIRECTION_INCOMING
            numero = callDetails.handle?.schemeSpecificPart.orEmpty()

            if (esEntrante) {
                // Leemos las reglas en cada llamada para aplicar cambios al instante.
                decision = MotorFiltrado.evaluar(
                    numero = numero,
                    listaBlanca = listaBlanca.obtenerActivosBloqueante(),
                    numerosBloqueados = numerosBloqueados.obtenerActivosBloqueante(),
                    filtros = filtros.obtenerActivosBloqueante(),
                    franjasNoMolestar = franjas.obtenerActivasBloqueante(),
                    modoNoMolestarActivo = ajustes.modoNoMolestarActivo,
                    momento = LocalDateTime.now(),
                    bloquearOcultos = ajustes.bloquearOcultos
                )
            }
        } catch (e: Exception) {
            // Ante cualquier error, permitimos la llamada y no perdemos la respuesta.
            Log.w("Centinela", "No se pudieron evaluar las reglas: ${e.javaClass.simpleName}")
            decision = Decision(false, ReglaTipo.NINGUNA, null)
        } finally {
            // Respondemos primero, sin esperar a la base de datos ni al registro.
            val respuesta = CallResponse.Builder()
                .setDisallowCall(decision.bloquear)
                .setRejectCall(decision.bloquear && ajustes.desviarBuzon)
                .setSkipNotification(decision.bloquear)
                .build()
            respondToCall(callDetails, respuesta)
        }

        if (esEntrante) {
            val decisionFinal = decision
            val numeroFinal = numero
            val ajustesFinal = ajustes
            alcance.launch {
                try {
                    if (decisionFinal.bloquear || ajustesFinal.registrarPermitidas) {
                        registro.registrar(decisionFinal, numeroFinal, simulada = false)
                    }
                } catch (e: Exception) {
                    Log.w("Centinela", "No se pudo registrar la llamada: ${e.javaClass.simpleName}")
                }
            }
            if (decisionFinal.bloquear && ajustesFinal.notificarBloqueos) {
                notificarBloqueo()
            }
        }
    }

    private fun crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val canal = NotificationChannel(
            CANAL_BLOQUEOS,
            "Llamadas bloqueadas",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Avisos silenciosos cuando Centinela bloquea una llamada"
            setShowBadge(false)
        }
        manager.createNotificationChannel(canal)
    }

    private fun notificarBloqueo() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendiente = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificacion = Notification.Builder(this, CANAL_BLOQUEOS)
            .setSmallIcon(R.drawable.ic_centinela_notificacion)
            .setContentTitle("Centinela")
            .setContentText("Se bloqueo una llamada entrante")
            .setContentIntent(pendiente)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()

        getSystemService(NotificationManager::class.java)?.notify(ID_NOTIFICACION, notificacion)
    }

    private companion object {
        const val CANAL_BLOQUEOS = "bloqueos_centinela"
        const val ID_NOTIFICACION = 400
    }
}
