package app.myliberty.easy_records.organizations.business.core.loan

import app.myliberty.easy_records.organizations.business.core.loan.api.LoanSearchParameters
import app.myliberty.easy_records.organizations.business.core.loan.search.LoanSearchValidator
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

class LoanSearchValidatorTest {

    private val createdFrom = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
    private val boundedParameters = LoanSearchParameters(
        createdFrom = createdFrom,
        createdBefore = createdFrom.plusDays(1)
    )

    @Test
    fun `a created range of up to a year is accepted`() {
        val oneYearParameters = boundedParameters.copy(createdBefore = createdFrom.plusYears(1))

        assertDoesNotThrow { LoanSearchValidator.guardValidParameters(oneYearParameters) }
    }

    @Test
    fun `a missing created range is rejected`() {
        assertEquals("createdFrom and createdBefore are required", rejectionOf(LoanSearchParameters()))
    }

    @Test
    fun `a created range longer than a year is rejected`() {
        val tooLongParameters = boundedParameters.copy(createdBefore = createdFrom.plusYears(1).plusDays(1))

        assertEquals("createdFrom and createdBefore must be at most 1 year apart", rejectionOf(tooLongParameters))
    }

    @Test
    fun `a created range that ends before it starts is rejected`() {
        val reversedParameters = boundedParameters.copy(createdBefore = createdFrom.minusDays(1))

        assertEquals("createdFrom must not be after createdBefore", rejectionOf(reversedParameters))
    }

    @Test
    fun `a loan date range that ends before it starts is rejected`() {
        val reversedLoanDates = boundedParameters.copy(
            loanDateFrom = LocalDate.of(2026, 2, 1),
            loanDateBefore = LocalDate.of(2026, 1, 1)
        )

        assertEquals("loanDateFrom must not be after loanDateBefore", rejectionOf(reversedLoanDates))
    }

    @Test
    fun `a loan date range in order, or on a single day, is accepted`() {
        val orderedLoanDates = boundedParameters.copy(
            loanDateFrom = LocalDate.of(2026, 1, 1),
            loanDateBefore = LocalDate.of(2026, 2, 1)
        )
        val sameDayLoanDates = boundedParameters.copy(
            loanDateFrom = LocalDate.of(2026, 1, 1),
            loanDateBefore = LocalDate.of(2026, 1, 1)
        )

        assertDoesNotThrow { LoanSearchValidator.guardValidParameters(orderedLoanDates) }
        assertDoesNotThrow { LoanSearchValidator.guardValidParameters(sameDayLoanDates) }
    }

    @Test
    fun `more than fifty lender references are rejected`() {
        val tooManyLenders = boundedParameters.copy(lenderContactReferences = List(51) { index -> "CT$index" })

        assertEquals("A maximum of 50 lender contact references may be supplied", rejectionOf(tooManyLenders))
    }

    @Test
    fun `more than twenty references are rejected`() {
        val tooManyReferences = boundedParameters.copy(references = List(21) { index -> "LN$index" })

        assertEquals("A maximum of 20 references may be supplied", rejectionOf(tooManyReferences))
    }

    @Test
    fun `a page size above five hundred is rejected`() {
        assertDoesNotThrow { LoanSearchValidator.guardValidPageSize(500) }

        val failure = assertThrows(LibertyGenericException::class.java) { LoanSearchValidator.guardValidPageSize(501) }

        assertEquals("requestedSize must be between 1 and 500", failure.message)
    }

    private fun rejectionOf(loanSearchParameters: LoanSearchParameters): String =
        assertThrows(LibertyGenericException::class.java) {
            LoanSearchValidator.guardValidParameters(loanSearchParameters)
        }.message
}
