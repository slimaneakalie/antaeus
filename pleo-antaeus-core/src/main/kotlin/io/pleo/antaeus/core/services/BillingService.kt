package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.helpers.BillingProcessor
import io.pleo.antaeus.core.helpers.BillingConfig

import java.sql.Date
import java.util.Timer
import kotlin.concurrent.timerTask


class BillingService(
    private val billingProcessor: BillingProcessor,
    private val getNextBillingDate: () -> Date,
){
    private var isActive = false
    private val timer = Timer("schedule")

    private fun processUnpaidInvoices() {
        billingProcessor.startNewBillingOperation()
    }

    private fun executeBilling(){
        processUnpaidInvoices()
        if (isActive) {
            scheduleNextBilling()
        }
    }

    fun stopBillingScheduler(){
        isActive = false
        timer.cancel()
    }

    fun startBillingScheduler(){
        isActive = true
        scheduleNextBilling()
    }

    private fun scheduleNextBilling() {
        val nextBillingDate = getNextBillingDate()
        timer.schedule(timerTask {
            executeBilling()
        }, nextBillingDate)
    }
}
