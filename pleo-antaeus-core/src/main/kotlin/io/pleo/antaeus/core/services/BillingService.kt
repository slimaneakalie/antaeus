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

    private fun getNextBillingDate(): LocalDate {
        val currentDateTime = LocalDateTime.now()
        val temporalAdjuster = TemporalAdjusters.lastDayOfMonth()
        var lastDayOfMonth = currentDateTime.with(temporalAdjuster)
        // handle the case where we call this method late in the current month
        // e.g: call at 20th of june, we want the billing to start at 1st August instead of 1st July
        if (lastDayOfMonth.dayOfMonth - currentDateTime.dayOfMonth < billingConfig.minDaysToBillInvoice) {
            lastDayOfMonth = lastDayOfMonth.plusMonths(1)
        }

        val firstDayOfNextMonth = lastDayOfMonth.plusDays(1)
        return firstDayOfNextMonth.toLocalDate()
    }

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
