package io.pleo.antaeus.core.helpers

import io.mockk.*
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.*
import kotlin.random.Random

class BillingWorkerTest {
    @kotlinx.coroutines.ObsoleteCoroutinesApi
    private val mainThreadSurrogate = newSingleThreadContext("Billing worker")

    @BeforeEach
    @kotlinx.coroutines.ExperimentalCoroutinesApi
    @kotlinx.coroutines.ObsoleteCoroutinesApi
    fun setUp(){
        Dispatchers.setMain(mainThreadSurrogate)
        unmockkAll()
    }

    @Test
    @kotlinx.coroutines.ExperimentalCoroutinesApi
    fun `billing worker test - should update invoices status to paid if the payment provider charges the customer`()= runBlockingTest{
        // Create a sample of this month unpaid invoices
        val thisMonthUnpaidInvoicesNumber = 20
        val thisMonthUnpaidInvoices = buildUnpaidInvoiceMap(thisMonthUnpaidInvoicesNumber)

        // Create expectations of the test
        val expectedInvoicesToCharge = HashSet<Invoice>()
        thisMonthUnpaidInvoices.values.forEach{ invoice ->
            if (Random.nextBoolean()){
                expectedInvoicesToCharge.add(invoice)
            }
        }

        // Mock the payment provider
        val paymentProviderMock = mockPaymentProvider(expectedInvoicesToCharge = expectedInvoicesToCharge)

        // Mock the dal
        val actualInvoicesToCharge = HashSet<Invoice?>()
        val dalMock = mockDal(thisMonthUnpaidInvoices = thisMonthUnpaidInvoices, actualInvoicesToCharge = actualInvoicesToCharge)

        // Create the billing worker
        val unpaidInvoicesChannel = Channel<Invoice>()
        val workerInput = BillingWorkerInput(
            unpaidInvoicesChannel = unpaidInvoicesChannel,
            paymentProvider = paymentProviderMock,
            dal = dalMock,
            maxNumberOfPaymentRetries = 0,
            paymentRetryDelayMs = 100,
        )
        val job = Job()
        val worker = BillingWorker(job)

        // Start the billing worker
        worker.start(workerInput = workerInput)

        // Send the invoices in the channel
        runBlocking{
            thisMonthUnpaidInvoices.values.forEach{ invoice ->
                unpaidInvoicesChannel.send(invoice)
            }

            unpaidInvoicesChannel.close()

            // Wait for the worker to finish
            while (worker.isActive);
        }

        // Assertions
        Assertions.assertEquals(expectedInvoicesToCharge.size, actualInvoicesToCharge.size)
        expectedInvoicesToCharge.forEach{ invoice ->
            Assertions.assertTrue(actualInvoicesToCharge.contains(invoice))
        }
    }

    private fun mockPaymentProvider(expectedInvoicesToCharge: HashSet<Invoice>): PaymentProvider{
        val invoiceSlot = slot<Invoice>()
        return mockk {
            every { charge(invoice = capture(invoiceSlot)) } answers {
                val invoice = invoiceSlot.captured
                expectedInvoicesToCharge.contains(invoice)
            }
        }
    }

    private fun mockDal(thisMonthUnpaidInvoices: Map<Int, Invoice>, actualInvoicesToCharge: HashSet<Invoice?>): AntaeusDal {
        val invoiceIdSlot = slot<Int>()
        val invoiceStatusSlot = slot<InvoiceStatus>()
        return mockk {
            every { updateInvoiceStatus(id = capture(invoiceIdSlot), status = capture(invoiceStatusSlot)) } answers {
                Assertions.assertEquals(InvoiceStatus.PAID, invoiceStatusSlot.captured)
                val invoice = thisMonthUnpaidInvoices[invoiceIdSlot.captured]
                actualInvoicesToCharge.add(invoice)
                invoice
            }
        }
    }

    private fun buildUnpaidInvoiceMap(thisMonthUnpaidInvoicesNumber: Int): Map<Int, Invoice>{
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val unpaidInvoices = mutableMapOf<Int, Invoice>()

        for (i in 1..thisMonthUnpaidInvoicesNumber){
            val invoice = Invoice(i, i, Money(BigDecimal(i*100), Currency.EUR), InvoiceStatus.PENDING, currentMonth, currentYear)
            unpaidInvoices.put(invoice.id, invoice)
        }

        return unpaidInvoices
    }
}