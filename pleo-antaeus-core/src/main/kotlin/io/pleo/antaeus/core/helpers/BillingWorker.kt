package io.pleo.antaeus.core.helpers

import io.pleo.antaeus.models.InvoiceStatus
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class BillingWorker (
    parentJob: Job
) : CoroutineScope{
    private var job = Job(parentJob)

    override val coroutineContext: CoroutineContext
        get() = job

    fun start(workerInput: BillingWorkerInput){
        if (job.isCancelled){
            println("Error: can't start the billing worker, the parent job is cancelled" )
            return
        }

        launch{
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

            job.complete()
        }
    }

    private suspend fun processInvoicePayment(
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

}
