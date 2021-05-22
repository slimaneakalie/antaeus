package io.pleo.antaeus.core.helpers

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice

data class PaymentProcessingInput(
        val invoiceToProcess: Invoice,
        val paymentProvider: PaymentProvider,
        val maxNumberOfPaymentRetries: Int,
        val paymentRetryDelayMs: Int
)