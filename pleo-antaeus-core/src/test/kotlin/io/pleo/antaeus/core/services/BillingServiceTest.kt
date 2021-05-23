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
    fun `schedule calls to the billing processor and stop the processor on demand`(){
        // Create the billing service
        val billingProcessor = mockkClass(BillingProcessor::class)

        val billingService = BillingService(
                billingProcessor = billingProcessor,
                getNextBillingDate = nextBillingDateFuncFactory()
        )

        // Mock the billing processor functions
        var i = 0
        every { billingProcessor.startNewBillingOperation() } answers {
            i++
            if(i == 3) {
                billingService.stopBillingScheduler()
            }
        }

        every { billingProcessor.close() } just Runs

        // start the billing scheduler
        billingService.startBillingScheduler()

        // Check if the billing scheduler if currently active
        Assertions.assertEquals(true, billingService.schedulerIsActive())

        // Wait for the the service fo finish
        while (billingService.schedulerIsActive());

        // Check for processor calls
        verify(atLeast = 3) { billingProcessor.startNewBillingOperation() }
        verify(exactly = 1) { billingProcessor.close() }
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

