package app.myliberty.easy_records.organizations.business.core.loan

import app.myliberty.easy_records.organizations.business.core.account.api.AccountDataFetcher
import app.myliberty.easy_records.organizations.business.core.account.api.SystemAccount
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactDto
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactType
import app.myliberty.easy_records.organizations.business.core.loan.api.LoanStatus
import app.myliberty.easy_records.support.anyNonNull
import app.myliberty.easy_records.util.business.mappers.StringQualifier
import app.myliberty.easy_records.util.business.mappers.UserQualifier
import app.myliberty.easy_records.util.model.HasReferenceEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class LoanResponseBuilderTest {

    private val loanRepaymentRepository = mock(LoanRepaymentRepository::class.java)
    private val loanStatusRepository = mock(LoanStatusRepository::class.java)
    private val loanVoidRepository = mock(LoanVoidRepository::class.java)
    private val loanRepaymentVoidRepository = mock(LoanRepaymentVoidRepository::class.java)
    private val accountDataFetcher = mock(AccountDataFetcher::class.java)
    private val userQualifier = mock(UserQualifier::class.java)

    private val loanMapper = LoanMapperImpl(StringQualifier)
    private val loanResponseBuilder = LoanResponseBuilder(
        loanRepaymentRepository, loanStatusRepository, loanVoidRepository, loanRepaymentVoidRepository, loanMapper, accountDataFetcher, userQualifier
    )

    private val lender = ContactDto(
        id = UUID.randomUUID(), createdById = UUID.randomUUID(), createdOn = OffsetDateTime.now(), referenceNumber = "CT000001",
        contactTypes = setOf(ContactType.LENDER), companyName = "First Bank"
    )
    private val cashCode = SystemAccount.CASH.code
    private val repaymentEntities = mutableListOf<LoanRepaymentEntity>()
    private val loanStatusEntities = mutableListOf<LoanStatusEntity>()
    private val loanVoidEntities = mutableListOf<LoanVoidEntity>()
    private val repaymentVoidEntities = mutableListOf<LoanRepaymentVoidEntity>()

    @BeforeEach
    fun setUp() {
        `when`(accountDataFetcher.getLabelsByCodes(setOf(cashCode))).thenReturn(mapOf(cashCode to "Cash"))
        `when`(userQualifier.getCreatorFullName(anyNonNull(UUID::class.java))).thenReturn("Pat Doe")
        `when`(loanRepaymentRepository.findByLoanIdIn(anyNonNullCollection())).thenAnswer { repaymentEntities.toList() }
        `when`(loanStatusRepository.findAllById(anyNonNullCollection())).thenAnswer { loanStatusEntities.toList() }
        `when`(loanVoidRepository.findByLoanIdIn(anyNonNullCollection())).thenAnswer { loanVoidEntities.toList() }
        `when`(loanRepaymentVoidRepository.findByLoanRepaymentIdIn(anyNonNullCollection())).thenAnswer { repaymentVoidEntities.toList() }
    }

    @Test
    fun `a loan with no repayments is open with its whole amount outstanding`() {
        val response = build(loan("100.0000").withStatus("0", "100", LoanStatus.OPEN)).single()

        assertEquals(LoanStatus.OPEN, response.status)
        assertEquals(0, BigDecimal.ZERO.compareTo(response.amountRepaid))
        assertEquals(0, BigDecimal("100").compareTo(response.outstanding))
        assertEquals("First Bank", response.lender.displayName)
        assertEquals("CT000001", response.lender.contactReference)
        assertEquals("Cash", response.assetAccount.label)
        assertNull(response.void)
    }

    @Test
    fun `a partial repayment leaves the loan open with the rest outstanding`() {
        val loanDto = loan("100.0000")
        repayment(loanDto, "30.0000")
        loanDto.withStatus("30", "70", LoanStatus.OPEN)

        val response = build(loanDto).single()

        assertEquals(LoanStatus.OPEN, response.status)
        assertEquals(0, BigDecimal("30").compareTo(response.amountRepaid))
        assertEquals(0, BigDecimal("70").compareTo(response.outstanding))
    }

    @Test
    fun `repayments that add up to the amount settle the loan`() {
        val loanDto = loan("100.0000")
        repayment(loanDto, "60.0000")
        repayment(loanDto, "40.0000")
        loanDto.withStatus("100", "0", LoanStatus.SETTLED)

        val response = build(loanDto).single()

        assertEquals(LoanStatus.SETTLED, response.status)
        assertEquals(0, BigDecimal.ZERO.compareTo(response.outstanding))
        assertEquals(2, response.repayments.size)
    }

    @Test
    fun `voiding a repayment returns the loan to open, and the voided repayment stays listed with its void`() {
        val loanDto = loan("100.0000")
        val repayment = repayment(loanDto, "100.0000")
        repaymentVoid(repayment)
        loanDto.withStatus("0", "100", LoanStatus.OPEN)

        val response = build(loanDto).single()

        assertEquals(LoanStatus.OPEN, response.status)
        assertEquals(0, BigDecimal.ZERO.compareTo(response.amountRepaid))
        assertEquals(0, BigDecimal("100").compareTo(response.outstanding))
        assertNotNull(response.repayments.single().void)
        assertEquals("Pat Doe", response.repayments.single().void!!.voidedBy)
    }

    @Test
    fun `a voided loan is voided with nothing outstanding`() {
        val loanDto = loan("100.0000")
        loanVoidEntities.add(LoanVoidEntity(loanDto.id, LocalDate.of(2026, 3, 5), "entered twice").also { stamp(it, "LNVD000001") })
        loanDto.withStatus("0", "0", LoanStatus.VOIDED)

        val response = build(loanDto).single()

        assertEquals(LoanStatus.VOIDED, response.status)
        assertEquals(0, BigDecimal.ZERO.compareTo(response.outstanding))
        assertEquals("entered twice", response.void!!.reason)
    }

    @Test
    fun `a page is built over one load of repayments, voids, lenders and labels`() {
        val loanDtos = listOf(loan("100.0000"), loan("50.0000"), loan("10.0000"))
        repayment(loanDtos[1], "50.0000")
        loanDtos[0].withStatus("0", "100", LoanStatus.OPEN)
        loanDtos[1].withStatus("50", "0", LoanStatus.SETTLED)
        loanDtos[2].withStatus("0", "10", LoanStatus.OPEN)

        val responses = build(*loanDtos.toTypedArray())

        assertEquals(listOf(LoanStatus.OPEN, LoanStatus.SETTLED, LoanStatus.OPEN), responses.map { it.status })
        verify(loanRepaymentRepository, times(1)).findByLoanIdIn(anyNonNullCollection())
        verify(loanStatusRepository, times(1)).findAllById(anyNonNullCollection())
        verify(loanVoidRepository, times(1)).findByLoanIdIn(anyNonNullCollection())
        verify(loanRepaymentVoidRepository, times(1)).findByLoanRepaymentIdIn(anyNonNullCollection())
        verify(accountDataFetcher, times(1)).getLabelsByCodes(setOf(cashCode))
    }

    @Test
    fun `an empty page reads nothing`() {
        val responses = loanResponseBuilder.buildResponses(emptyList(), emptyMap())

        assertEquals(emptyList<Any>(), responses)
        verifyNoInteractions(loanRepaymentRepository, loanStatusRepository, loanVoidRepository, loanRepaymentVoidRepository)
    }

    @Test
    fun `loan note, repayment note and repayment void reason are carried into the response`() {
        val loanEntity = LoanEntity(lender.id, cashCode, BigDecimal("100.0000"), LocalDate.of(2026, 3, 1), "Working capital")
            .also { stamp(it, "LOAN000010") }
        val loanDto = loanMapper.toDto(loanEntity).withStatus("0", "100", LoanStatus.OPEN)
        val repaymentEntity = LoanRepaymentEntity(loanDto.id, cashCode, BigDecimal("10.0000"), LocalDate.of(2026, 3, 2), "First instalment")
            .also { stamp(it, "LNRP000010") }
            .also { repaymentEntities.add(it) }
        repaymentVoidEntities.add(
            LoanRepaymentVoidEntity(repaymentEntity.id!!, LocalDate.of(2026, 3, 3), "Wrong account").also { stamp(it, "LRPV000010") }
        )

        val response = build(loanDto).single()

        assertEquals("Working capital", response.note)
        assertEquals("First instalment", response.repayments.single().note)
        assertEquals("Wrong account", response.repayments.single().void!!.reason)
    }

    @Test
    fun `repayments created at the same instant are listed by id`() {
        val loanDto = loan("100.0000").withStatus("20", "80", LoanStatus.OPEN)
        val sharedCreatedOn = OffsetDateTime.parse("2026-03-02T10:00:00Z")
        val repaymentIds = listOf(
            UUID.fromString("00000000-0000-0000-0000-000000000002"),
            UUID.fromString("00000000-0000-0000-0000-000000000001")
        )
        repaymentIds.forEach { repaymentId ->
            val repaymentEntity = repayment(loanDto, "10.0000")
            repaymentEntity.id = repaymentId
            repaymentEntity.createdOn = sharedCreatedOn
        }

        val response = build(loanDto).single()

        assertEquals(repaymentIds.sorted(), response.repayments.map { it.id })
    }

    @Test
    fun `repayments are listed oldest first whatever their ids`() {
        val loanDto = loan("100.0000").withStatus("20", "80", LoanStatus.OPEN)
        val olderRepayment = repayment(loanDto, "10.0000")
        olderRepayment.id = UUID.fromString("00000000-0000-0000-0000-000000000009")
        olderRepayment.createdOn = OffsetDateTime.parse("2026-03-02T10:00:00Z")
        val newerRepayment = repayment(loanDto, "10.0000")
        newerRepayment.id = UUID.fromString("00000000-0000-0000-0000-000000000001")
        newerRepayment.createdOn = OffsetDateTime.parse("2026-03-02T11:00:00Z")

        val response = build(loanDto).single()

        assertEquals(listOf(olderRepayment.id, newerRepayment.id), response.repayments.map { it.id })
    }

    @Test
    fun `a response built from supplied status figures never reads the view`() {
        val loanDto = loan("100.0000")

        val response = loanResponseBuilder.buildResponse(loanDto, lender, LoanStatusFigures(BigDecimal("40"), BigDecimal("60"), LoanStatus.OPEN))

        assertEquals(0, BigDecimal("40").compareTo(response.amountRepaid))
        assertEquals(0, BigDecimal("60").compareTo(response.outstanding))
        assertEquals(LoanStatus.OPEN, response.status)
        verifyNoInteractions(loanStatusRepository)
    }

    private fun build(vararg loanDtos: LoanDto) = loanResponseBuilder.buildResponses(loanDtos.toList(), mapOf(lender.id to lender))

    private fun loan(amount: String): LoanDto {
        val loanEntity = LoanEntity(lender.id, cashCode, BigDecimal(amount), LocalDate.of(2026, 3, 1), null)
        stamp(loanEntity, "LOAN${UUID.randomUUID().toString().take(6)}")
        return loanMapper.toDto(loanEntity)
    }

    private fun LoanDto.withStatus(amountRepaid: String, outstanding: String, loanStatus: LoanStatus): LoanDto {
        loanStatusEntities.add(LoanStatusEntity(id, BigDecimal(amountRepaid), BigDecimal(outstanding), loanStatus))
        return this
    }

    private fun repayment(loanDto: LoanDto, amount: String): LoanRepaymentEntity =
        LoanRepaymentEntity(loanDto.id, cashCode, BigDecimal(amount), LocalDate.of(2026, 3, 2), null)
            .also { stamp(it, "LNRP${UUID.randomUUID().toString().take(6)}") }
            .also { repaymentEntities.add(it) }

    private fun repaymentVoid(loanRepaymentEntity: LoanRepaymentEntity) {
        repaymentVoidEntities.add(
            LoanRepaymentVoidEntity(loanRepaymentEntity.id!!, LocalDate.of(2026, 3, 3), null).also { stamp(it, "LRPV000001") }
        )
    }

    private fun stamp(entity: HasReferenceEntity, reference: String) {
        entity.id = UUID.randomUUID()
        entity.referenceNumber = reference
        entity.createdById = UUID.randomUUID()
        entity.createdOn = OffsetDateTime.now()
    }


    private fun anyNonNullCollection(): Collection<UUID> {
        any(Collection::class.java)
        return emptyList()
    }
}
