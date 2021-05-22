package io.pleo.antaeus.core.helpers

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.math.min

class BillingProcessor(
        private val billingConfig: BillingConfig,
        private val dal: AntaeusDal,
        private val paymentProvider: PaymentProvider
) : CoroutineScope{
    private val unpaidInvoicesChannel = Channel<Invoice>()
    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = job

    fun startNewBillingOperation(){
        val unpaidInvoices = dal.fetchUnpaidInvoices()
        var numberOfWorkers = min(unpaidInvoices.size, billingConfig.workerPoolSize)

        launchBillingWorkers(numberOfWorkers)
        sendUnpaidInvoicesToChannel(unpaidInvoices)
        addNextMonthInvoices(currentUnpaidInvoices = unpaidInvoices, dal = dal)
    }

    private fun sendUnpaidInvoicesToChannel(unpaidInvoices: List<Invoice>){
        launch {
            unpaidInvoices.forEach{ invoice ->
                unpaidInvoicesChannel.send(invoice)
            }
        }
    }

    private fun launchBillingWorkers(numberOfWorkers: Int){
        repeat(numberOfWorkers){
            val workerInput = BillingWorkerInput(
                    unpaidInvoicesChannel = unpaidInvoicesChannel,
                    paymentProvider = paymentProvider,
                    maxNumberOfPaymentRetries = billingConfig.maxNumberOfPaymentRetries,
                    paymentRetryDelayMs = billingConfig.paymentRetryDelayMs,
                    dal = dal
            )

            billingWorker(workerInput)
        }
    }

    private fun addNextMonthInvoices(currentUnpaidInvoices: List<Invoice>, dal: AntaeusDal){
        currentUnpaidInvoices.forEach{ invoice ->
            var month = if (invoice.month == 12) 1 else invoice.month+1
            var year = if (invoice.month == 12) invoice.year+1 else invoice.year
            val nextMonthInvoice = invoice.copy(status = InvoiceStatus.PENDING, month = month, year = year)
            dal.createInvoice(nextMonthInvoice)
        }
    }
}