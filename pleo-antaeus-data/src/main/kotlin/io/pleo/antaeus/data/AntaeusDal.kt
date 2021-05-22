/*
    Implements the data access layer (DAL).
    The data access layer generates and executes requests to the database.

    See the `mappings` module for the conversions between database rows and Kotlin objects.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class AntaeusDal(private val db: Database) {
    fun fetchInvoice(id: Int): Invoice? {
        // transaction(db) runs the internal query as a new database transaction.
        return transaction(db) {
            // Returns the first invoice with matching id.
            InvoiceTable
                .select { InvoiceTable.id.eq(id) }
                .firstOrNull()
                ?.toInvoice()
        }
    }

    fun fetchUnpaidInvoices(): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                    .select { InvoiceTable.status.eq(InvoiceStatus.PENDING.toString()) }
                    .map { it.toInvoice() }
        }
    }

    fun fetchInvoices(): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                .selectAll()
                .map { it.toInvoice() }
        }
    }

    fun createInvoice(invoice: Invoice): Invoice? {
        val id = transaction(db) {
            // Insert the invoice and returns its new id.
            InvoiceTable
                .insert {
                    it[this.value] = invoice.amount.value
                    it[this.currency] = invoice.amount.currency.toString()
                    it[this.status] = invoice.status.toString()
                    it[this.customerId] = invoice.customerId
                    it[this.month] = invoice.month
                    it[this.year] = invoice.year
                } get InvoiceTable.id
        }

        return fetchInvoice(id)
    }

    fun fetchCustomer(id: Int): Customer? {
        return transaction(db) {
            CustomerTable
                .select { CustomerTable.id.eq(id) }
                .firstOrNull()
                ?.toCustomer()
        }
    }

    fun updateInvoiceStatus(id: Int, status: InvoiceStatus): Invoice? {
        val invoiceDbId = transaction(db) {
            // Returns the first invoice with matching id.
            InvoiceTable.update({ InvoiceTable.id eq id}) {
                it[this.status] = status.toString()
            }
        }

        return fetchInvoice(invoiceDbId)
    }

    fun fetchCustomers(): List<Customer> {
        return transaction(db) {
            CustomerTable
                .selectAll()
                .map { it.toCustomer() }
        }
    }

    fun createCustomer(currency: Currency): Customer? {
        val id = transaction(db) {
            // Insert the customer and return its new id.
            CustomerTable.insert {
                it[this.currency] = currency.toString()
            } get CustomerTable.id
        }

        return fetchCustomer(id)
    }
}
