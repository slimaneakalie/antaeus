package io.pleo.antaeus.app
import getNextBillingDate
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class UtilsTest {
    @BeforeEach
    fun setUp(){
        unmockkAll()
    }
    
    @Test
    fun `getNextBillingDate - get the first day of next month`(){
        val localDateTime = LocalDateTime.of(2021, 6, 20, 0, 0)
        mockkStatic(LocalDateTime::class)
        every { LocalDateTime.now() } returns localDateTime
        val nextBillingDate = getNextBillingDate(15)
        println("nextBillingDate $nextBillingDate")
    }
}