package io.pleo.antaeus.models

import java.math.BigDecimal

data class Money(
    val value: BigDecimal,
    val currency: Currency
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Money

        if (value != other.value) return false
        if (currency != other.currency) return false

        return true
    }
}
