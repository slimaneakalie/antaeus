package io.pleo.antaeus.core.helpers

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal

data class BillingConfig (
    val paymentProvider: PaymentProvider,
    val dal: AntaeusDal,
    val minDaysToBillInvoice: Int,
    val workerPoolSize: Int,
    val maxNumberOfPaymentRetries: Int,
    val paymentRetryDelayMs: Long
)
