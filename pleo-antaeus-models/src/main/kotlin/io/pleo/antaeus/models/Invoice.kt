package io.pleo.antaeus.models

data class Invoice(
    val id: Int,
    val customerId: Int,
    val amount: Money,
    val status: InvoiceStatus,
    val month: Int,
    val year: Int
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Invoice

        if (id != other.id) return false
        if (customerId != other.customerId) return false
        if (amount != other.amount) return false
        if (status != other.status) return false
        if (month != other.month) return false
        if (year != other.year) return false

        return true
    }
}
