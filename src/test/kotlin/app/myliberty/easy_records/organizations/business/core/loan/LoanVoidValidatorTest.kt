package app.myliberty.easy_records.organizations.business.core.loan

import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class LoanVoidValidatorTest {

    private val loanVoidRepository = mock(LoanVoidRepository::class.java)
    private val activeLoanRepaymentRepository = mock(ActiveLoanRepaymentRepository::class.java)
    private val loanVoidValidator = LoanVoidValidator(loanVoidRepository, activeLoanRepaymentRepository)

    private val loanDto = LoanDto(
        id = UUID.randomUUID(),
        referenceNumber = "LN000001",
        lenderContactId = UUID.randomUUID(),
        assetAccountCode = "001.001.001",
        amount = BigDecimal("100.00"),
        loanDate = LocalDate.of(2026, 3, 10),
        note = null,
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.now()
    )

    @Test
    fun `a loan with no void and no active repayment can be voided`() {
        assertDoesNotThrow { loanVoidValidator.requireVoidable(loanDto) }
    }

    @Test
    fun `a voided loan cannot be voided again`() {
        `when`(loanVoidRepository.existsByLoanId(loanDto.id)).thenReturn(true)

        val failure = assertThrows(LibertyGenericException::class.java) { loanVoidValidator.requireVoidable(loanDto) }

        assertEquals("Loan LN000001 is already voided", failure.message)
    }

    @Test
    fun `a single active repayment is named in the singular`() {
        `when`(activeLoanRepaymentRepository.findByLoanIdOrderByCreatedOnAscIdAsc(loanDto.id)).thenReturn(
            listOf(activeRepayment("LNRP000001"))
        )

        val failure = assertThrows(LibertyGenericException::class.java) { loanVoidValidator.requireVoidable(loanDto) }

        assertEquals("Loan LN000001 cannot be voided while it has repayment: LNRP000001", failure.message)
    }

    @Test
    fun `active repayments are listed in the order they were made`() {
        `when`(activeLoanRepaymentRepository.findByLoanIdOrderByCreatedOnAscIdAsc(loanDto.id)).thenReturn(
            listOf(activeRepayment("LNRP000001"), activeRepayment("LNRP000002"))
        )

        val failure = assertThrows(LibertyGenericException::class.java) { loanVoidValidator.requireVoidable(loanDto) }

        assertEquals("Loan LN000001 cannot be voided while it has repayments: LNRP000001, LNRP000002", failure.message)
    }

    private fun activeRepayment(reference: String) = ActiveLoanRepaymentEntity(
        id = UUID.randomUUID(), referenceNumber = reference, loanId = loanDto.id, amount = BigDecimal("10.00"),
        createdOn = OffsetDateTime.now()
    )
}
