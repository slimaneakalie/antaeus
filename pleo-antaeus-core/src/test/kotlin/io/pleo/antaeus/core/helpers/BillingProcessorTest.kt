package io.pleo.antaeus.core.helpers

import io.mockk.*
import io.pleo.antaeus.core.models.BillingProcessorTestExpectations
import io.pleo.antaeus.core.models.CalendarMockInput
import io.pleo.antaeus.core.models.BillingProcessorTestInput
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import kotlinx.coroutines.CoroutineScope
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
    fun `send unpaid invoices to workers and create the unpaid invoices of the next month`()= runBlockingTest{
        val testInput = BillingProcessorTestInput(
            mockedCurrentMonth = 12,
            mockedCurrentYear = 2021,
            workerPoolSize = 100,
            numberUnpaidInvoicesThisMonth = 20,
        )
        testBillingProcessor(testInput)
    }


    private suspend fun CoroutineScope.testBillingProcessor(testInput: BillingProcessorTestInput){
        // Mock the calendar
        val calendarMockInputs = listOf<CalendarMockInput>(
            CalendarMockInput(arg = Calendar.MONTH, returnValue = testInput.mockedCurrentMonth - 1),
            CalendarMockInput(arg = Calendar.YEAR, returnValue = testInput.mockedCurrentYear)
        )

        mockCalendarGetFunction(calendarMockInputs)

        // Create the variables to store the actual input sent from the processor
        val actualNextMonthCreatedInvoices= mutableMapOf<Int, Invoice>()
        val actualInvoicesToProcess= mutableMapOf<Int, Invoice>()
        var actualNumberOfLaunchedWorkers = 0

        // Create the static unpaid invoices
        val thisMonthUnpaidInvoices = buildUnpaidInvoiceList(testInput.numberUnpaidInvoicesThisMonth)

        // Build the expectations of the test
        val testExpectations = buildBillingTestExpectations(testInput, thisMonthUnpaidInvoices)

        // Mock data access layer
        val dalMock = mockDal(thisMonthUnpaidInvoices, actualNextMonthCreatedInvoices)

        // Mock the billing workers
        mockBillingWorkers(actualInvoicesToProcess){ actualNumberOfLaunchedWorkers++ }

        // Launch the processor
        val billingConfig = BillingConfig(
            paymentProvider = mockk(),
            dal = dalMock,
            minDaysToBillInvoice = 15,
            workerPoolSize = testInput.workerPoolSize,
            maxNumberOfPaymentRetries = 3,
            paymentRetryDelayMs = 1000
        )

        val billingProcessor = BillingProcessor(billingConfig = billingConfig)
        runBlocking {
            billingProcessor.startNewBillingOperation()
        }

        // Assertions
        Assertions.assertEquals(testExpectations.expectedNumberOfLaunchedWorkers, actualNumberOfLaunchedWorkers)
        Assertions.assertEquals(testExpectations.expectedInvoicesToProcess, actualInvoicesToProcess)
        Assertions.assertEquals(testExpectations.expectedNextMonthCreatedInvoices, actualNextMonthCreatedInvoices)

    }

    private fun CoroutineScope.mockBillingWorkers(actualInvoicesToProcess: MutableMap<Int, Invoice>, markWorkerCall: () -> Unit){
        val workerInputSlot = slot<BillingWorkerInput>()
        mockkConstructor(BillingWorker::class)
        every {
            anyConstructed<BillingWorker>().start(workerInput = capture(workerInputSlot))
        } answers {
            markWorkerCall()
            launch{
                for (invoice in workerInputSlot.captured.unpaidInvoicesChannel){
                    actualInvoicesToProcess.put(invoice.id, invoice)
                }
            }
        }
    }

    private fun mockDal(thisMonthUnpaidInvoices: List<Invoice>, actualNextMonthCreatedInvoices: MutableMap<Int, Invoice>) : AntaeusDal {
        return mockk {
            every { fetchUnpaidInvoices() } returns thisMonthUnpaidInvoices
            val invoiceSlot = slot<Invoice>()
            every { createInvoice(invoice = capture(invoiceSlot)) } answers {
                actualNextMonthCreatedInvoices.put(invoiceSlot.captured.id, invoiceSlot.captured)
                invoiceSlot.captured
            }
        }
    }

    private fun buildBillingTestExpectations(testInput: BillingProcessorTestInput, thisMonthUnpaidInvoices:  List<Invoice>) : BillingProcessorTestExpectations{
        val expectedNumberOfLaunchedWorkers = min(testInput.workerPoolSize, testInput.numberUnpaidInvoicesThisMonth)
        val expectedInvoicesToProcess= mutableMapOf<Int, Invoice>()
        val expectedNextMonthCreatedInvoices= mutableMapOf<Int, Invoice>()

        thisMonthUnpaidInvoices.forEach{ invoice ->
            expectedInvoicesToProcess[invoice.id] = invoice
            val nextMonth = if (testInput.mockedCurrentMonth == 12) 1 else testInput.mockedCurrentMonth+1
            val nextYear = if (testInput.mockedCurrentMonth == 12) testInput.mockedCurrentYear+1 else testInput.mockedCurrentYear
            val expectedNextMonthInvoice =
                invoice.copy(
                    month = nextMonth,
                    year = nextYear,
                )
            expectedNextMonthCreatedInvoices[expectedNextMonthInvoice.id] = expectedNextMonthInvoice
        }

        return BillingProcessorTestExpectations(
            expectedNumberOfLaunchedWorkers = expectedNumberOfLaunchedWorkers,
            expectedInvoicesToProcess = expectedInvoicesToProcess,
            expectedNextMonthCreatedInvoices = expectedNextMonthCreatedInvoices,
        )
    }

    private fun mockCalendarGetFunction(mockInputs: List<CalendarMockInput>){
        val calendarMock = mockk<Calendar> {
            mockInputs.forEach{ input ->
                every { get(input.arg) } returns input.returnValue
            }
        }
        mockkStatic(Calendar::class)
        every { Calendar.getInstance() } returns calendarMock
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