package io.pleo.antaeus.app
import getLastDayOfMonth
import getNextBillingDate
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Date
import java.time.LocalDate

class UtilsTest {
    @BeforeEach
    fun setUp(){
        unmockkAll()
    }

    @Test
    fun `getLastDayOfMonth - test getting the last day of the month for different use cases`(){
        val testTable = listOf<GetLastDayOfMonthTestElement>(
            GetLastDayOfMonthTestElement(inputYear = 2021, inputMonth = 6, expectedOutput = LocalDate.of(2021, 6, 30)),
            GetLastDayOfMonthTestElement(inputYear = 2021, inputMonth = 7, expectedOutput = LocalDate.of(2021, 7, 31)),
            GetLastDayOfMonthTestElement(inputYear = 2021, inputMonth = 2, expectedOutput = LocalDate.of(2021, 2, 28)),
            GetLastDayOfMonthTestElement(inputYear = 2020, inputMonth = 2, expectedOutput = LocalDate.of(2020, 2, 29)),
        )

        testTable.forEach{ testElement ->
            val actualOutput = getLastDayOfMonth(year = testElement.inputYear, month = testElement.inputMonth)
            Assertions.assertEquals(testElement.expectedOutput, actualOutput)
        }
    }

    @Test
    fun `getNextBillingDate - get the first day of the other month not the next one`(){
        val testInput = GetNextBillingDateTestInput(
            mockNowDate = LocalDate.of(2021, 6, 20),
            minDaysToBillInvoice = 15,
            expectedNextBillingDate = Date.valueOf(LocalDate.of(2021, 8, 1))
        )
        getNextBillingDateTest(testInput = testInput)
    }

    @Test
    fun `getNextBillingDate - get the first day of next month`(){
        val testInput = GetNextBillingDateTestInput(
            mockNowDate = LocalDate.of(2021, 6, 15),
            minDaysToBillInvoice = 15,
            expectedNextBillingDate = Date.valueOf(LocalDate.of(2021, 7, 1))
        )
        getNextBillingDateTest(testInput = testInput)
    }

    @Test
    fun `getNextBillingDate - get the first day of next year`(){
        val testInput = GetNextBillingDateTestInput(
            mockNowDate = LocalDate.of(2021, 12, 10),
            minDaysToBillInvoice = 15,
            expectedNextBillingDate = Date.valueOf(LocalDate.of(2022, 1, 1))
        )
        getNextBillingDateTest(testInput = testInput)
    }


    private fun getNextBillingDateTest(testInput: GetNextBillingDateTestInput){
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns testInput.mockNowDate

        val actualNextBillingDate = getNextBillingDate(testInput.minDaysToBillInvoice)
        Assertions.assertEquals(testInput.expectedNextBillingDate, actualNextBillingDate)
    }
}

data class GetNextBillingDateTestInput(
    val mockNowDate: LocalDate,
    val minDaysToBillInvoice: Int,
    val expectedNextBillingDate: Date,
)

data class GetLastDayOfMonthTestElement(
    val inputYear: Int,
    val inputMonth: Int,
    val expectedOutput: LocalDate,
)