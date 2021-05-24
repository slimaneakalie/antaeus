package io.pleo.antaeus.core.models

import io.pleo.antaeus.models.Invoice

data class BillingProcessorTestExpectations(
    val expectedNumberOfLaunchedWorkers: Int,
    val expectedInvoicesToProcess: Map<Int, Invoice>,
    val expectedNextMonthCreatedInvoices: Map<Int, Invoice>,
)