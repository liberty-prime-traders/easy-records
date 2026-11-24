package app.myliberty.easy_records.organizations.business.core.loan

import app.myliberty.easy_records.organizations.business.core.loan.api.LoanStatus
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class LoanRepaymentValidatorTest {

    private val loanVoidRepository = mock(LoanVoidRepository::class.java)
    private val loanRepaymentVoidRepository = mock(LoanRepaymentVoidRepository::class.java)
    private val loanRepaymentValidator = LoanRepaymentValidator(loanVoidRepository, loanRepaymentVoidRepository)

    private val loanDate = LocalDate.of(2026, 3, 10)
    private val loanDto = LoanDto(
        id = UUID.randomUUID(),
        referenceNumber = "LN000001",
        lenderContactId = UUID.randomUUID(),
        assetAccountCode = "001.001.001",
        amount = BigDecimal("100.00"),
        loanDate = loanDate,
        note = null,
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.now()
    )
    private val figures = LoanStatusFigures(
        amountRepaid = BigDecimal("70.00"),
        outstanding = BigDecimal("30.0000"),
        status = LoanStatus.OPEN
    )

    @Test
    fun `a repayment up to the outstanding amount on or after the loan date is accepted`() {
        assertDoesNotThrow { loanRepaymentValidator.requireRepayable(loanDto, figures, loanDate, BigDecimal("30.00")) }
    }

    @Test
    fun `a repayment above the outstanding amount names both amounts at the money scale`() {
        val failure = assertThrows(LibertyGenericException::class.java) {
            loanRepaymentValidator.requireRepayable(loanDto, figures, loanDate, BigDecimal("30.01"))
        }

        assertEquals("Repayment of KES 30.01 exceeds the outstanding KES 30.00 on loan LN000001", failure.message)
    }

    @Test
    fun `a repayment dated before the loan is rejected`() {
        val failure = assertThrows(LibertyGenericException::class.java) {
            loanRepaymentValidator.requireRepayable(loanDto, figures, loanDate.minusDays(1), BigDecimal("10.00"))
        }

        assertEquals("Repayment date cannot be before the loan date 2026-03-10", failure.message)
    }

    @Test
    fun `a repayment against a voided loan is rejected`() {
        `when`(loanVoidRepository.existsByLoanId(loanDto.id)).thenReturn(true)

        val failure = assertThrows(LibertyGenericException::class.java) {
            loanRepaymentValidator.requireRepayable(loanDto, figures, loanDate, BigDecimal("10.00"))
        }

        assertEquals("Loan LN000001 is voided", failure.message)
    }
}
