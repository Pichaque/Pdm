package com.example.filtro400

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log
import com.example.filtro400.data.BaseDatos
import com.example.filtro400.data.RegistroLlamadasRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NumeroCompletoPurgeJobService : JobService() {

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var trabajo: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        trabajo = alcance.launch {
            try {
                RegistroLlamadasRepository(BaseDatos.obtener(this@NumeroCompletoPurgeJobService))
                    .purgarRegistrosExpirados()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "No se pudieron purgar los numeros temporales: ${e.javaClass.simpleName}")
            } finally {
                if (currentCoroutineContext().isActive) {
                    jobFinished(params, false)
                }
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        trabajo?.cancel()
        return true
    }

    private companion object {
        const val TAG = "Centinela"
    }
}
