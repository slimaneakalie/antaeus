package io.pleo.antaeus.core.helpers

import io.mockk.*
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.*

class BillingProcessorTest {
    @Test
    @kotlinx.coroutines.ExperimentalCoroutinesApi
    fun `send unpaid invoices to workers`() = runBlockingTest{
        // Mock the calendar
        val calendarMock = mockk<Calendar> {
            every { get(Calendar.MONTH) } returns 11
            every { get(Calendar.YEAR) } returns 2021
        }

        mockkStatic(Calendar::class)
        every { Calendar.getInstance() } returns calendarMock

        // Create the lists to store the input sent from the processor
        val newCreatedInvoices= mutableListOf<Invoice>()
        val invoicesToProcess= mutableListOf<Invoice>()
        var createdBillingWorkers = 0

        // Create the static unpaid invoices
        val thisMonthUnpaidInvoices = buildUnpaidInvoiceList()


        // Mock data access layer
        val dalMock = mockk<AntaeusDal> {
            every { fetchUnpaidInvoices() } returns thisMonthUnpaidInvoices
            val invoiceSlot = slot<Invoice>()
            every { createInvoice(invoice = capture(invoiceSlot)) } answers {
                println("createdInvoice capture: $invoiceSlot.captured")
                newCreatedInvoices.add(invoiceSlot.captured)
                invoiceSlot.captured
            }
        }

        val workerInputSlot = slot<BillingWorkerInput>()
        mockkConstructor(BillingWorker::class)
        every {
            anyConstructed<BillingWorker>().start(workerInput = capture(workerInputSlot))
        } answers {
            createdBillingWorkers++
            println("createdBillingWorkers: $createdBillingWorkers")
            launch{
                for (invoice in workerInputSlot.captured.unpaidInvoicesChannel){
                    invoicesToProcess.add(invoice)
                    println("invoice to process: $invoice")
                }
            }
        }


        val billingConfig = BillingConfig(
                paymentProvider = mockk(),
                dal = dalMock,
                minDaysToBillInvoice = 15,
                workerPoolSize = thisMonthUnpaidInvoices.size * 3,
                maxNumberOfPaymentRetries = 3,
                paymentRetryDelayMs = 1000
        )

        val billingProcessor = BillingProcessor(billingConfig = billingConfig)
        runBlocking {
            billingProcessor.startNewBillingOperation()
        }

        println("2- createdBillingWorkers: $createdBillingWorkers")

    }

    private fun buildUnpaidInvoiceList(): List<Invoice>{
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val unpaidInvoices: MutableList<Invoice> = mutableListOf()

        for (i in 1..20){
            unpaidInvoices.add(Invoice(i, i, Money(BigDecimal(i*100), Currency.EUR), InvoiceStatus.PENDING, currentMonth, currentYear))
        }

        return unpaidInvoices
    }

}