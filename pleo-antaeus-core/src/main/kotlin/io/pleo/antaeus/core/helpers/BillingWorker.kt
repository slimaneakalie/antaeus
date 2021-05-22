package io.pleo.antaeus.core.helpers

import io.pleo.antaeus.models.InvoiceStatus
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

            val invoiceIsPaid = processInvoicePayment(paymentProcessingInput)
            if (invoiceIsPaid) {
                // TODO: handle the case where the write to db fails
                workerInput.dal.updateInvoiceStatus(invoice.id, InvoiceStatus.PAID)
            } else {
                // TODO: notify the customer by an email or an sms
                println("Payment failure for invoice id: ${invoice.id} and customer id: ${invoice.customerId}, amount: ${invoice.amount.value} ${invoice.amount.currency}" )
            }
        }
    }
}


suspend fun processInvoicePayment(
    paymentProcessingInput: PaymentProcessingInput
): Boolean{
    val invoiceToProcess = paymentProcessingInput.invoiceToProcess
    var i = 0
    var invoiceIsPaid = false
    while (!invoiceIsPaid){
        invoiceIsPaid = paymentProcessingInput.paymentProvider.charge(invoiceToProcess)
        if (invoiceIsPaid){
            break
        }

        i++
        if (i < paymentProcessingInput.maxNumberOfPaymentRetries){
            delay(paymentProcessingInput.paymentRetryDelayMs)
        } else {
            return false
        }
    }

    return true
}
