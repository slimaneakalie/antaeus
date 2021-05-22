package io.pleo.antaeus.core.helpers

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import kotlinx.coroutines.channels.ReceiveChannel

data class BillingWorkerInput(
        val unpaidInvoicesChannel: ReceiveChannel<Invoice>,
        val paymentProvider: PaymentProvider,
        val maxNumberOfPaymentRetries: Int,
        val paymentRetryDelayMs: Long,
        val dal: AntaeusDal
)