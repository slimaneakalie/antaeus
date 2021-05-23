/*
    Defines the main() entry point of the app.
    Configures the database and sets up the REST web service.
 */

@file:JvmName("AntaeusApp")

package io.pleo.antaeus.app

import getPaymentProvider
import getNextBillingDate
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.data.CustomerTable
import io.pleo.antaeus.data.InvoiceTable
import io.pleo.antaeus.core.helpers.BillingConfig
import io.pleo.antaeus.core.helpers.BillingProcessor
import io.pleo.antaeus.rest.AntaeusRest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import setupInitialData
import java.io.File
import java.sql.Connection

fun main() {
    var db = initDb()

    // dal: data access layer.
    val dal = AntaeusDal(db = db)
    setupInitialData(dal = dal)

    val invoiceService = InvoiceService(dal = dal)
    val customerService = CustomerService(dal = dal)
    val billingService = createBillingService(dal = dal)
    billingService.startBillingScheduler()

    AntaeusRest(
        invoiceService = invoiceService,
        customerService = customerService
    ).run()
}

fun initDb(): Database{
    // The tables to create in the database.
    val tables = arrayOf(InvoiceTable, CustomerTable)

    val dbFile: File = File.createTempFile("antaeus-db", ".sqlite")
    // Connect to the database and create the needed tables. Drop any existing data.
    val db = Database
            .connect(url = "jdbc:sqlite:${dbFile.absolutePath}",
                    driver = "org.sqlite.JDBC",
                    user = "root",
                    password = "")
            .also {
                TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
                transaction(it) {
                    addLogger(StdOutSqlLogger)
                    // Drop all existing tables to ensure a clean slate on each run
                    SchemaUtils.drop(*tables)
                    // Create all tables
                    SchemaUtils.create(*tables)
                }
            }
    return db
}

fun createBillingService(dal: AntaeusDal): BillingService {
    val paymentProvider = getPaymentProvider()
    val billingConfig = BillingConfig(
            paymentProvider = paymentProvider,
            dal = dal,
            minDaysToBillInvoice = 15,
            workerPoolSize = 100,
            maxNumberOfPaymentRetries = 4,
            paymentRetryDelayMs = 10000
    )

    val billingProcessor = BillingProcessor(billingConfig = billingConfig)

    return BillingService(
        billingProcessor = billingProcessor,
        getNextBillingDate = { getNextBillingDate(billingConfig.minDaysToBillInvoice) },
    )
}