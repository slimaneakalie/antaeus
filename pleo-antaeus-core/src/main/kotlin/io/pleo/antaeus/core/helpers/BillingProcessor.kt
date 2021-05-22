package io.pleo.antaeus.core.helpers

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
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

    fun startNewBilling(){
        val unpaidInvoices = dal.fetchUnpaidInvoices()
        var numberOfWorkers = min(unpaidInvoices.size, billingConfig.workerPoolSize)

        launchBillingWorkers(numberOfWorkers)
        sendUnpaidInvoicesToChannel(unpaidInvoices)
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
                    dal = dal
            )

            billingWorker(workerInput)
        }
    }
}
