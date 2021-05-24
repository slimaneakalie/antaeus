package io.pleo.antaeus.core.helpers

import io.mockk.*
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.*
import kotlin.math.min

class BillingProcessorTest {
    @Test
    @kotlinx.coroutines.ExperimentalCoroutinesApi
    fun `send unpaid invoices to workers and create the unpaid invoices of the next month`() = runBlockingTest{
        // Mock the calendar
        val mockedCurrentMonth = 12
        val mockedCurrentYear = 2021
        val calendarMock = mockk<Calendar> {
            every { get(Calendar.MONTH) } returns mockedCurrentMonth - 1
            every { get(Calendar.YEAR) } returns mockedCurrentYear
        }

        mockkStatic(Calendar::class)
        every { Calendar.getInstance() } returns calendarMock

        // Create the variables to store the input sent from the processor
        val actualNextMonthCreatedInvoices= mutableMapOf<Int, Invoice>()
        val actualInvoicesToProcess= mutableMapOf<Int, Invoice>()
        var actualNumberOfLaunchedWorkers = 0

        // worker pool size
        val workerPoolSize = 100

        // Create the static unpaid invoices
        val thisMonthUnpaidInvoicesNumber = 20
        val thisMonthUnpaidInvoices = buildUnpaidInvoiceList(thisMonthUnpaidInvoicesNumber)

        // Build the expectations of the test
        val expectedNumberOfLaunchedWorkers = min(workerPoolSize, thisMonthUnpaidInvoicesNumber)
        val expectedInvoicesToProcess= mutableMapOf<Int, Invoice>()
        val expectedNextMonthCreatedInvoices= mutableMapOf<Int, Invoice>()

        thisMonthUnpaidInvoices.forEach{ invoice ->
            expectedInvoicesToProcess[invoice.id] = invoice
            val nextMonth = if (mockedCurrentMonth == 12) 1 else mockedCurrentMonth+1
            val nextYear = if (mockedCurrentMonth == 12) mockedCurrentYear+1 else mockedCurrentYear
            val expectedNextMonthInvoice =
                invoice.copy(
                    month = nextMonth,
                    year = nextYear,
                )
            expectedNextMonthCreatedInvoices[expectedNextMonthInvoice.id] = expectedNextMonthInvoice
        }

        // Mock data access layer
        val dalMock = mockk<AntaeusDal> {
            every { fetchUnpaidInvoices() } returns thisMonthUnpaidInvoices
            val invoiceSlot = slot<Invoice>()
            every { createInvoice(invoice = capture(invoiceSlot)) } answers {
                actualNextMonthCreatedInvoices.put(invoiceSlot.captured.id, invoiceSlot.captured)
                invoiceSlot.captured
            }
        }

        // Mock the billing workers
        val workerInputSlot = slot<BillingWorkerInput>()
        mockkConstructor(BillingWorker::class)
        every {
            anyConstructed<BillingWorker>().start(workerInput = capture(workerInputSlot))
        } answers {
            actualNumberOfLaunchedWorkers++
            launch{
                for (invoice in workerInputSlot.captured.unpaidInvoicesChannel){
                    actualInvoicesToProcess.put(invoice.id, invoice)
                }
            }
        }

        // Launch the processor
        val billingConfig = BillingConfig(
            paymentProvider = mockk(),
            dal = dalMock,
            minDaysToBillInvoice = 15,
            workerPoolSize = workerPoolSize,
            maxNumberOfPaymentRetries = 3,
            paymentRetryDelayMs = 1000
        )

        val billingProcessor = BillingProcessor(billingConfig = billingConfig)
        runBlocking {
            billingProcessor.startNewBillingOperation()
        }

        // Assertions
        Assertions.assertEquals(expectedNumberOfLaunchedWorkers, actualNumberOfLaunchedWorkers)
        Assertions.assertEquals(expectedInvoicesToProcess, actualInvoicesToProcess)
        Assertions.assertEquals(expectedNextMonthCreatedInvoices, actualNextMonthCreatedInvoices)

    }
    
    private fun buildUnpaidInvoiceList(thisMonthUnpaidInvoicesNumber: Int): List<Invoice>{
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val unpaidInvoices: MutableList<Invoice> = mutableListOf()

        for (i in 1..thisMonthUnpaidInvoicesNumber){
            unpaidInvoices.add(Invoice(i, i, Money(BigDecimal(i*100), Currency.EUR), InvoiceStatus.PENDING, currentMonth, currentYear))
        }

        return unpaidInvoices
    }

}