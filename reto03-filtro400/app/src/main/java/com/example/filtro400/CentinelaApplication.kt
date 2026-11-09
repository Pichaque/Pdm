package com.example.filtro400

import android.app.Application
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.util.Log

class CentinelaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        programarPurgaNumerosTemporales()
    }

    private fun programarPurgaNumerosTemporales() {
        val scheduler = getSystemService(JobScheduler::class.java) ?: return
        val trabajo = JobInfo.Builder(
            ID_TRABAJO_PURGA,
            ComponentName(this, NumeroCompletoPurgeJobService::class.java)
        )
            .setPersisted(true)
            .setPeriodic(INTERVALO_PURGA_MS)
            .build()

        if (scheduler.schedule(trabajo) != JobScheduler.RESULT_SUCCESS) {
            Log.w(TAG, "No se pudo programar la purga de numeros temporales")
        }
    }

    private companion object {
        const val ID_TRABAJO_PURGA = 4800
        const val INTERVALO_PURGA_MS = 15 * 60 * 1000L
        const val TAG = "Centinela"
    }
}
