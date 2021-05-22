package io.pleo.antaeus.core.helpers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun CoroutineScope.billingWorker(
        workerInput: BillingWorkerInput
){
    launch {
        for (invoice in workerInput.unpaidInvoicesChannel){
            val paymentProcessingInput =  PaymentProcessingInput(
                    invoiceToProcess = invoice,
                    paymentProvider = workerInput.paymentProvider,
                    maxNumberOfPaymentRetries = workerInput.maxNumberOfPaymentRetries,
                    paymentRetryDelayMs = workerInput.paymentRetryDelayMs
            )

            processInvoicePayment(paymentProcessingInput)
        }
    }
}


suspend fun processInvoicePayment(
    paymentProcessingInput: PaymentProcessingInput
): Boolean{
    delay(1000)
    return true
}
