package io.pleo.antaeus.core.helpers

data class BillingConfig (
    val minDaysToBillInvoice: Int,
    val workerPoolSize: Int,
    val maxNumberOfPaymentRetries: Int
)
