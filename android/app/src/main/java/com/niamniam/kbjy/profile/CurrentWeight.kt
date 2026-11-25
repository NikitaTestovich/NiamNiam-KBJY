package com.niamniam.kbjy.profile

import java.time.LocalDate

data class CurrentWeight(val kilograms: Double, val measurementDate: LocalDate?)

fun currentWeight(measurements: List<WeightMeasurement>, profileWeightKg: Double, today: LocalDate): CurrentWeight {
    val latest = measurements.asSequence()
        .map { LocalDate.parse(it.date) to it }
        .filter { (date, _) -> !date.isAfter(today) }
        .maxWithOrNull(compareBy({ it.first }, { it.second.id }))
    return if (latest == null) CurrentWeight(profileWeightKg, null)
    else CurrentWeight(latest.second.kilograms, latest.first)
}
