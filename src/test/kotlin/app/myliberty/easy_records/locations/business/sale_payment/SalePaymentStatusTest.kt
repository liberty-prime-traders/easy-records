package app.myliberty.easy_records.locations.business.sale_payment

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID
import app.myliberty.easy_records.configuration.session.LocationSession
import app.myliberty.easy_records.configuration.session.OrgSession
import app.myliberty.easy_records.configuration.session.SessionContext
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.locations.business.sale.api.SaleContext
import app.myliberty.easy_records.locations.business.sale.api.SaleDataFetcher
import app.myliberty.easy_records.locations.business.sale.api.SaleStatus
import app.myliberty.easy_records.locations.business.sale.api.SaleUpdater
import app.myliberty.easy_records.locations.business.sale_payment.api.SalePaymentFetcher
import app.myliberty.easy_records.locations.business.sale_payment.api.SalePaymentService
import app.myliberty.easy_records.locations.business.sale_payment.api.SalePaymentVoidCreateDto
import app.myliberty.easy_records.organizations.business.fiscal_period.api.FiscalPeriodService
import app.myliberty.easy_records.organizations.business.payment_method.api.PaymentMethodDataFetcher
import app.myliberty.easy_records.util.enums.PaymentStatus
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.`when`
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.verify

class SalePaymentStatusTest {

    private val saleId = UUID.randomUUID()
    private val contactId = UUID.randomUUID()
    private val paymentMethodId = UUID.randomUUID()
    private val salePaymentFetcher = mock(SalePaymentFetcher::class.java)

    @BeforeEach
    fun setSession() {
        SessionContextProvider.setSession(
            SessionContext(
                organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"),
                location = LocationSession(id = UUID.randomUUID(), schemaName = "loc-1")
            )
        )
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    private fun newPayment(amount: String) =
        SalePaymentWriter.NewSalePayment(paymentMethodId, BigDecimal(amount), reference = null, paymentDate = null)

    private fun salePaymentEntity(amount: String) = SalePaymentEntity(
        saleId = saleId,
        paymentMethodId = paymentMethodId,
        paymentMethodAccountCode = "CASH",
        amount = BigDecimal(amount),
        paymentDate = OffsetDateTime.now()
    ).also {
        it.id = UUID.randomUUID()
        it.referenceNumber = "SP000001"
    }

    private fun paymentMethodDataFetcherNamingTheMethod() = mock(PaymentMethodDataFetcher::class.java).also {
        `when`(it.getNamesById()).thenReturn(mapOf(paymentMethodId to "Cash"))
    }

    private fun salePaymentWriter(): SalePaymentWriter {
        val salePaymentRepository = mock(SalePaymentRepository::class.java)
        `when`(salePaymentRepository.saveAll(anyList<SalePaymentEntity>())).thenAnswer { it.getArgument<List<SalePaymentEntity>>(0) }
        val paymentMethodDataFetcher = mock(PaymentMethodDataFetcher::class.java)
        `when`(paymentMethodDataFetcher.findAccountCode(paymentMethodId)).thenReturn("CASH")
        return SalePaymentWriter(
            salePaymentRepository,
            salePaymentFetcher,
            mock(SalePaymentHandlerForKafka::class.java),
            paymentMethodDataFetcher
        )
    }

    @Test
    fun `write resolves FULLY_SETTLED from the pre-write paid total plus the new payment`() {
        `when`(salePaymentFetcher.calculatePaidAmount(saleId)).thenReturn(BigDecimal.ZERO)

        val result = salePaymentWriter().write(saleId, contactId, BigDecimal("100"), listOf(newPayment("100")))

        assertEquals(PaymentStatus.FULLY_SETTLED, result.newPaymentStatus)
    }

    @Test
    fun `write resolves PARTIALLY_SETTLED when earlier payments plus the new one are below the total`() {
        `when`(salePaymentFetcher.calculatePaidAmount(saleId)).thenReturn(BigDecimal("30"))

        val result = salePaymentWriter().write(saleId, contactId, BigDecimal("100"), listOf(newPayment("20"), newPayment("10")))

        assertEquals(PaymentStatus.PARTIALLY_SETTLED, result.newPaymentStatus)
    }

    @Test
    fun `write with no new payments resolves from the existing paid total`() {
        `when`(salePaymentFetcher.calculatePaidAmount(saleId)).thenReturn(BigDecimal("100"))

        val result = salePaymentWriter().write(saleId, contactId, BigDecimal("100"), emptyList())

        assertEquals(PaymentStatus.FULLY_SETTLED, result.newPaymentStatus)
    }

    @Test
    fun `voidPayment resolves UNPAID by subtracting the voided amount from the pre-void paid total`() {
        val salePaymentRepository = mock(SalePaymentRepository::class.java)
        val saleDataFetcher = mock(SaleDataFetcher::class.java)
        val saleUpdater = versionStampingSaleUpdater()
        val payment = salePaymentEntity("100")
        `when`(salePaymentRepository.findAllById(setOf(payment.id!!))).thenReturn(listOf(payment))
        `when`(saleDataFetcher.lockAndGetSaleContexts(listOf(saleId)))
            .thenReturn(mapOf(saleId to SaleContext(contactId, BigDecimal("100"), SaleStatus.CONFIRMED)))
        `when`(salePaymentFetcher.calculatePaidAmounts(listOf(saleId))).thenReturn(mapOf(saleId to BigDecimal("100")))
        val salePaymentService = SalePaymentService(
            salePaymentRepository,
            savingVoidRepository(),
            saleDataFetcher,
            saleUpdater,
            salePaymentFetcher,
            salePaymentWriter(),
            mock(SalePaymentVoidHandlerForKafka::class.java),
            paymentMethodDataFetcherNamingTheMethod(),
            mock(FiscalPeriodService::class.java)
        )

        salePaymentService.voidPayment(SalePaymentVoidCreateDto(payment.id!!, "entered twice"))

        verify(saleUpdater).updatePaymentStatuses(mapOf(saleId to PaymentStatus.UNPAID))
    }

    @Test
    fun `voidPayment takes the sale lock before checking whether the payment was already voided`() {
        val salePaymentRepository = mock(SalePaymentRepository::class.java)
        val salePaymentVoidRepository = mock(SalePaymentVoidRepository::class.java)
        val saleDataFetcher = mock(SaleDataFetcher::class.java)
        val payment = salePaymentEntity("100")
        `when`(salePaymentRepository.findAllById(setOf(payment.id!!))).thenReturn(listOf(payment))
        `when`(saleDataFetcher.lockAndGetSaleContexts(listOf(saleId)))
            .thenReturn(mapOf(saleId to SaleContext(contactId, BigDecimal("100"), SaleStatus.CONFIRMED)))
        `when`(salePaymentVoidRepository.findBySalePaymentIdIn(listOf(payment.id!!)))
            .thenReturn(listOf(SalePaymentVoidEntity(payment.id!!, "earlier void")))
        val salePaymentService = SalePaymentService(
            salePaymentRepository,
            salePaymentVoidRepository,
            saleDataFetcher,
            mock(SaleUpdater::class.java),
            salePaymentFetcher,
            salePaymentWriter(),
            mock(SalePaymentVoidHandlerForKafka::class.java),
            paymentMethodDataFetcherNamingTheMethod(),
            mock(FiscalPeriodService::class.java)
        )

        assertThrows(RtsGenericException::class.java) {
            salePaymentService.voidPayment(SalePaymentVoidCreateDto(payment.id!!, "entered twice"))
        }

        val lockBeforeCheck = inOrder(saleDataFetcher, salePaymentVoidRepository)
        lockBeforeCheck.verify(saleDataFetcher).lockAndGetSaleContexts(listOf(saleId))
        lockBeforeCheck.verify(salePaymentVoidRepository).findBySalePaymentIdIn(listOf(payment.id!!))
    }

    @Test
    fun `voidPayment keeps PARTIALLY_SETTLED when other active payments remain`() {
        val salePaymentRepository = mock(SalePaymentRepository::class.java)
        val saleDataFetcher = mock(SaleDataFetcher::class.java)
        val saleUpdater = versionStampingSaleUpdater()
        val payment = salePaymentEntity("40")
        `when`(salePaymentRepository.findAllById(setOf(payment.id!!))).thenReturn(listOf(payment))
        `when`(saleDataFetcher.lockAndGetSaleContexts(listOf(saleId)))
            .thenReturn(mapOf(saleId to SaleContext(contactId, BigDecimal("100"), SaleStatus.CONFIRMED)))
        `when`(salePaymentFetcher.calculatePaidAmounts(listOf(saleId))).thenReturn(mapOf(saleId to BigDecimal("100")))
        val salePaymentService = SalePaymentService(
            salePaymentRepository,
            savingVoidRepository(),
            saleDataFetcher,
            saleUpdater,
            salePaymentFetcher,
            salePaymentWriter(),
            mock(SalePaymentVoidHandlerForKafka::class.java),
            paymentMethodDataFetcherNamingTheMethod(),
            mock(FiscalPeriodService::class.java)
        )

        salePaymentService.voidPayment(SalePaymentVoidCreateDto(payment.id!!, "wrong method"))

        verify(saleUpdater).updatePaymentStatuses(mapOf(saleId to PaymentStatus.PARTIALLY_SETTLED))
    }

    @Test
    fun `voidPayments updates each sale's status once and hands each sale's payments to the publisher once`() {
        val salePaymentRepository = mock(SalePaymentRepository::class.java)
        val saleDataFetcher = mock(SaleDataFetcher::class.java)
        val saleUpdater = versionStampingSaleUpdater()
        val salePaymentVoidHandlerForKafka = mock(SalePaymentVoidHandlerForKafka::class.java)
        val otherSaleId = UUID.randomUUID()
        val firstPayment = salePaymentEntity("30")
        val secondPayment = salePaymentEntity("20")
        val otherSalePayment = salePaymentEntity("50").also { it.saleId = otherSaleId }
        `when`(salePaymentRepository.findAllById(setOf(firstPayment.id!!, secondPayment.id!!, otherSalePayment.id!!)))
            .thenReturn(listOf(firstPayment, secondPayment, otherSalePayment))
        `when`(saleDataFetcher.lockAndGetSaleContexts(anyList<UUID>())).thenReturn(
            listOf(saleId, otherSaleId)
                .associateWith { SaleContext(contactId, BigDecimal("100"), SaleStatus.CONFIRMED) }
        )
        `when`(salePaymentFetcher.calculatePaidAmounts(anyList<UUID>()))
            .thenReturn(mapOf(saleId to BigDecimal("50"), otherSaleId to BigDecimal("50")))
        val salePaymentService = SalePaymentService(
            salePaymentRepository, savingVoidRepository(), saleDataFetcher, saleUpdater, salePaymentFetcher,
            salePaymentWriter(), salePaymentVoidHandlerForKafka, paymentMethodDataFetcherNamingTheMethod(),
            mock(FiscalPeriodService::class.java)
        )

        val responses = salePaymentService.voidPayments(
            listOf(firstPayment, secondPayment, otherSalePayment).map { SalePaymentVoidCreateDto(it.id!!, "batch") }
        )

        assertEquals(3, responses.size)
        verify(saleUpdater).updatePaymentStatuses(
            mapOf(saleId to PaymentStatus.UNPAID, otherSaleId to PaymentStatus.UNPAID)
        )
        val publishedPaymentCounts = mockingDetails(salePaymentVoidHandlerForKafka).invocations
            .filter { it.method.name == "publish" }
            .map { (it.arguments[1] as List<*>).size }
        assertEquals(listOf(2, 1), publishedPaymentCounts)
    }

    private fun savingVoidRepository(): SalePaymentVoidRepository {
        val salePaymentVoidRepository = mock(SalePaymentVoidRepository::class.java)
        `when`(salePaymentVoidRepository.saveAll(anyList<SalePaymentVoidEntity>()))
            .thenAnswer { it.getArgument<List<SalePaymentVoidEntity>>(0) }
        return salePaymentVoidRepository
    }

    private fun versionStampingSaleUpdater(): SaleUpdater = mock(SaleUpdater::class.java) { invocation ->
        if (invocation.method.name == "updatePaymentStatuses") {
            invocation.getArgument<Map<UUID, PaymentStatus>>(0).mapValues { 1L }
        } else null
    }
}
