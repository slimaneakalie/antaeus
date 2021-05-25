package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class InvoiceServiceTest {
    @BeforeEach
    fun setUp(){
        unmockkAll()
    }

    private val dal = mockk<AntaeusDal> {
        every { fetchInvoice(404) } returns null
    }

    private val invoiceService = InvoiceService(dal = dal)

    @Test
    fun `will throw if invoice is not found`() {
        assertThrows<InvoiceNotFoundException> {
            invoiceService.fetch(404)
        }
    }
}
