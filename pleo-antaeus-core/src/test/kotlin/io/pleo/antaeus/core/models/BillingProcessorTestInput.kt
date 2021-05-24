package io.pleo.antaeus.core.models

data class BillingProcessorTestInput(
    val mockedCurrentMonth: Int,
    val mockedCurrentYear: Int,
    val workerPoolSize: Int,
    val numberUnpaidInvoicesThisMonth: Int,
)