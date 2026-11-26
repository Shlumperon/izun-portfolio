package com.example.caloriescounter.data

import android.content.Context
import androidx.core.content.edit

data class WeightSample(
    val timestamp: Long,
    val weightKg: Double
)

class WeightRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSamples(): List<WeightSample> {
        return prefs.getString(KEY_SAMPLES, null)
            ?.split(";")
            ?.mapNotNull { encoded ->
                val parts = encoded.split(":")
                if (parts.size != 2) return@mapNotNull null
                val timestamp = parts[0].toLongOrNull() ?: return@mapNotNull null
                val weight = parts[1].toDoubleOrNull() ?: return@mapNotNull null
                WeightSample(timestamp, weight)
            }
            ?.sortedBy { it.timestamp }
            .orEmpty()
    }

    fun addSample(weightKg: Double, timestamp: Long = System.currentTimeMillis()) {
        if (weightKg <= 0.0) return
        val updated = (getSamples() + WeightSample(timestamp, weightKg))
            .sortedBy { it.timestamp }
            .takeLast(MAX_SAMPLES)
        saveSamples(updated)
    }

    fun deleteSample(timestamp: Long) {
        saveSamples(getSamples().filterNot { it.timestamp == timestamp })
    }

    fun updateSample(
        originalTimestamp: Long,
        weightKg: Double,
        newTimestamp: Long
    ) {
        if (weightKg <= 0.0) return
        val updated = (getSamples().filterNot { it.timestamp == originalTimestamp } + WeightSample(newTimestamp, weightKg))
            .sortedBy { it.timestamp }
            .takeLast(MAX_SAMPLES)
        saveSamples(updated)
    }

    private fun saveSamples(samples: List<WeightSample>) {
        prefs.edit {
            putString(
                KEY_SAMPLES,
                samples.joinToString(";") { "${it.timestamp}:${it.weightKg}" }
            )
        }
    }

    companion object {
        private const val PREFS_NAME = "weight_tracking"
        private const val KEY_SAMPLES = "samples"
        private const val MAX_SAMPLES = 500
    }
}
