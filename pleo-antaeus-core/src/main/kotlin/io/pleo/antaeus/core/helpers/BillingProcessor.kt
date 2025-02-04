package io.pleo.antaeus.core.helpers

import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.math.min

class BillingProcessor(
        private val billingConfig: BillingConfig,
) : CoroutineScope{
    private var job = Job()

    override val coroutineContext: CoroutineContext
        get() = job

    fun startNewBillingOperation(){
        val unpaidInvoicesChannel = Channel<Invoice>()
        if (job.isCancelled){
            job = Job()
        }

        val unpaidInvoices = billingConfig.dal.fetchUnpaidInvoices()
        // if we have 100 unpaid invoices and 500 in the size of workers pool, we should start just 100 workers
        var numberOfWorkers = min(unpaidInvoices.size, billingConfig.workerPoolSize)

        launchBillingWorkers(numberOfWorkers, unpaidInvoicesChannel)

        launch {
            sendUnpaidInvoicesToChannel(unpaidInvoices, unpaidInvoicesChannel)
            unpaidInvoicesChannel.close()
        }

        addNextMonthInvoices(currentUnpaidInvoices = unpaidInvoices, dal = billingConfig.dal)
    }

    fun close() {
        job.cancel()
    }

    private suspend fun sendUnpaidInvoicesToChannel(unpaidInvoices: List<Invoice>, unpaidInvoicesChannel: SendChannel<Invoice>){
        unpaidInvoices.forEach{ invoice ->
            unpaidInvoicesChannel.send(invoice)
        }
    }

    private fun launchBillingWorkers(numberOfWorkers: Int, unpaidInvoicesChannel: ReceiveChannel<Invoice>){
        repeat(numberOfWorkers){
            val workerInput = BillingWorkerInput(
                    unpaidInvoicesChannel = unpaidInvoicesChannel,
                    paymentProvider = billingConfig.paymentProvider,
                    maxNumberOfPaymentRetries = billingConfig.maxNumberOfPaymentRetries,
                    paymentRetryDelayMs = billingConfig.paymentRetryDelayMs,
                    dal = billingConfig.dal
            )

            BillingWorker(this.job).start(workerInput)
        }
    }

    private fun addNextMonthInvoices(currentUnpaidInvoices: List<Invoice>, dal: AntaeusDal){
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        currentUnpaidInvoices.forEach{ invoice ->
            if (invoice.month != currentMonth) {
                return
            }

            var month = if (invoice.month == 12) 1 else invoice.month+1
            var year = if (invoice.month == 12) invoice.year+1 else invoice.year
            val nextMonthInvoice = invoice.copy(status = InvoiceStatus.PENDING, month = month, year = year)
            // TODO: handle write to db error
            dal.createInvoice(nextMonthInvoice)
        }
    }
}