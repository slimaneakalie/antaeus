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
    dal: AntaeusDal,
    paymentProvider: PaymentProvider,
    private val billingConfig: BillingConfig
){
    private val timer = Timer("schedule", true);
    private val billingProcessor = BillingProcessor(
            billingConfig = billingConfig,
            dal = dal,
            paymentProvider = paymentProvider
    )

    init {
        scheduleNextBilling()
    }

    private fun getNextBillingDate(): LocalDate {
        val currentDateTime = LocalDateTime.now()
        val temporalAdjuster = TemporalAdjusters.lastDayOfMonth()
        var lastDayOfMonth = currentDateTime.with(temporalAdjuster)
        if (lastDayOfMonth.dayOfMonth - currentDateTime.dayOfMonth < billingConfig.minDaysToBillInvoice) {
            lastDayOfMonth = lastDayOfMonth.plusMonths(1)
        }

        return lastDayOfMonth.toLocalDate()
    }

    private fun processUnpaidInvoices() {
        billingProcessor.startNewBilling()
    }

    private fun executeBilling(){
        processUnpaidInvoices()
        scheduleNextBilling()
    }

    private fun scheduleNextBilling() {
        val nextBillingDate = getNextBillingDate()
        timer.schedule(timerTask {
            executeBilling()
        }, Date.valueOf((nextBillingDate)))
    }
}
