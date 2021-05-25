
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import java.math.BigDecimal
import java.sql.Date
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.*
import kotlin.random.Random

// This will create all schemas and setup initial data
internal fun setupInitialData(dal: AntaeusDal) {
    val customers = (1..100).mapNotNull {
        dal.createCustomer(
            currency = Currency.values()[Random.nextInt(0, Currency.values().size)]
        )
    }

    val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)


    customers.forEach { customer ->
        (1..10).forEach {
            val invoice = Invoice(
                id = 0,
                amount = Money(
                        value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                        currency = customer.currency
                ),
                customerId = customer.id,
                status = if (it == 1) InvoiceStatus.PENDING else InvoiceStatus.PAID,
                month = if (it == 1) currentMonth else Random.nextInt(1, 12),
                year = if (it == 1) currentYear else Random.nextInt(2000, currentYear)
            )
            dal.createInvoice(invoice)
        }
    }
}

// This is the mocked instance of the payment provider
internal fun getPaymentProvider(): PaymentProvider {
    return object : PaymentProvider {
        override fun charge(invoice: Invoice): Boolean {
                return Random.nextBoolean()
        }
    }
}

fun getLastDayOfMonth(year: Int, month: Int): LocalDate{
    val monthDate = LocalDate.of(year, month, 1)
    val temporalAdjuster = TemporalAdjusters.lastDayOfMonth()
    return monthDate.with(temporalAdjuster)
}

fun getNextBillingDate(minDaysToBillInvoice: Int): Date {
    val currentDate = LocalDate.now()
    var lastDayOfMonth = getLastDayOfMonth(year = currentDate.year, month = currentDate.monthValue)

    // handle the case where we call this method late in the current month
    // e.g: call at 20th of june, we want the billing to start at 1st August instead of 1st July
    if (lastDayOfMonth.dayOfMonth - currentDate.dayOfMonth < minDaysToBillInvoice) {
        val nextMonth = if(currentDate.monthValue == 12) 1 else currentDate.monthValue+1
        val year = if(currentDate.monthValue == 12) currentDate.year+1 else currentDate.year

        lastDayOfMonth = getLastDayOfMonth(year = year, month = nextMonth)
    }

    val firstDayOfNextMonth = lastDayOfMonth.plusDays(1)
    return Date.valueOf(firstDayOfNextMonth)
}