package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CustomerServiceTest {
    @BeforeEach
    fun setUp(){
        unmockkAll()
    }

    private val dal = mockk<AntaeusDal> {
        every { fetchCustomer(404) } returns null
    }

    private val customerService = CustomerService(dal = dal)

    @Test
    fun `will throw if customer is not found`() {
        assertThrows<CustomerNotFoundException> {
            customerService.fetch(404)
        }
    }
}
