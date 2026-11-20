package com.niamniam.kbjy

object PortionCalculator {
    fun scale(valuePerHundred: Double, amount: Double): Double {
        require(valuePerHundred.isFinite() && valuePerHundred >= 0)
        require(amount.isFinite() && amount > 0)
        return valuePerHundred * amount / 100
    }

    fun perHundred(total: Double, preparedWeight: Double): Double {
        require(total.isFinite() && total >= 0)
        require(preparedWeight.isFinite() && preparedWeight > 0)
        return total * 100 / preparedWeight
    }
}
