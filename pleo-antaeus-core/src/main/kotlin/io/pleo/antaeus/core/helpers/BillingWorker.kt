package io.pleo.antaeus.core.helpers

import kotlinx.coroutines.CoroutineScope
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
                    dal = workerInput.dal
            )

            processInvoicePayment(paymentProcessingInput)
        }
    }
}


fun processInvoicePayment(
        paymentProcessingInput: PaymentProcessingInput
): Boolean{
    return true
}
