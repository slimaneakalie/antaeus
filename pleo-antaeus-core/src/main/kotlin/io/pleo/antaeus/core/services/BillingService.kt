package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.helpers.BillingProcessor
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.core.helpers.BillingConfig

import java.sql.Date
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.util.*
import kotlin.concurrent.timerTask


class BillingService(
    private val billingConfig: BillingConfig,
    private val getNextBillingDate: () -> LocalDate,
){
    private val timer = Timer("schedule", true);
    private val billingProcessor = BillingProcessor(
            billingConfig = billingConfig,
    )

    private fun processUnpaidInvoices() {
        billingProcessor.startNewBillingOperation()
    }

    private fun executeBilling(){
        processUnpaidInvoices()
        scheduleNextBilling()
    }

    fun scheduleNextBilling() {
        val nextBillingDate = getNextBillingDate()
        timer.schedule(timerTask {
            executeBilling()
        }, Date.valueOf((nextBillingDate)))
    }
}
