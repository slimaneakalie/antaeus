package io.pleo.antaeus.core.services

import io.mockk.*
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.helpers.BillingConfig
import io.pleo.antaeus.core.helpers.BillingProcessor
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Date
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.random.Random

class BillingServiceTest {
    @Test
    fun `schedule calls to the billing processor`(){
        mockkStatic(BillingProcessor::class)
        val billingProcessor = mockkClass(BillingProcessor::class)

        val billingService = BillingService(
                billingProcessor = billingProcessor,
                getNextBillingDate = nextBillingDateFuncFactory()
        )

        var i = 0
        every { billingProcessor.startNewBillingOperation() } answers {
            i++
            if(i == 3) {
                billingService.stopBillingScheduler()
            }
        }

        billingService.startBillingScheduler()

        while (i < 3);
        verify(atLeast = 3) { billingProcessor.startNewBillingOperation() }
    }

    private fun nextBillingDateFuncFactory(): () -> Date {
        return { ->
            val now = Instant.now()
            val fixedClock = Clock.fixed(now, ZoneId.systemDefault())

            mockkStatic(Clock::class)
            every { Clock.systemUTC() } returns fixedClock
            Date(now.toEpochMilli())
        }
    }
}

