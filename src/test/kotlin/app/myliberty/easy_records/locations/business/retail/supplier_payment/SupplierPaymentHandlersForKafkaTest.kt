package app.myliberty.easy_records.locations.business.retail.supplier_payment

import app.myliberty.easy_records.configuration.session.LocationSession
import app.myliberty.easy_records.configuration.session.OrgSession
import app.myliberty.easy_records.configuration.session.SessionContext
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.locations.business.retail.purchase.api.delivery.DeliveryHandlerForPurchase
import app.myliberty.easy_records.locations.business.retail.purchase.api.PurchaseDataFetcher
import app.myliberty.easy_records.messaging.kafka.transaction.events.SupplierPaymentRecordedEvent
import app.myliberty.easy_records.messaging.kafka.transaction.events.SupplierPaymentVoidedEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class SupplierPaymentHandlersForKafkaTest {

    private val eventPublisher = mock(ApplicationEventPublisher::class.java)
    private val supplierPaymentHandlerForKafka = SupplierPaymentHandlerForKafka(
        mock(SupplierPaymentRepository::class.java), mock(DeliveryHandlerForPurchase::class.java), eventPublisher
    )
    private val supplierPaymentVoidHandlerForKafka = SupplierPaymentVoidHandlerForKafka(
        mock(SupplierPaymentRepository::class.java), mock(SupplierPaymentVoidRepository::class.java),
        mock(PurchaseDataFetcher::class.java), eventPublisher
    )
    private val supplierId = UUID.randomUUID()

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

    @Test
    fun `a recorded payment is posted to the account copied when it was recorded`() {
        supplierPaymentHandlerForKafka.publish(payment("001.099"), supplierId)

        val publishedEvent = mockingDetails(eventPublisher).invocations.single().arguments[0] as SupplierPaymentRecordedEvent
        assertEquals("001.099", publishedEvent.paymentMethodAccountCode)
    }

    @Test
    fun `a payment void is posted to the account copied when the payment was recorded`() {
        supplierPaymentVoidHandlerForKafka.publish(voidOf(), payment("001.099"), supplierId)

        val publishedEvent = mockingDetails(eventPublisher).invocations.single().arguments[0] as SupplierPaymentVoidedEvent
        assertEquals("001.099", publishedEvent.paymentMethodAccountCode)
    }

    private fun payment(paymentMethodAccountCode: String) = SupplierPaymentEntity(
        purchaseId = UUID.randomUUID(),
        paymentMethodId = UUID.randomUUID(),
        paymentMethodAccountCode = paymentMethodAccountCode,
        amount = BigDecimal("25.0000"),
        paymentDate = OffsetDateTime.now()
    ).apply {
        id = UUID.randomUUID()
        referenceNumber = "SPAY01"
    }

    private fun voidOf() = SupplierPaymentVoidEntity(UUID.randomUUID(), "wrong method").apply {
        id = UUID.randomUUID()
        createdOn = OffsetDateTime.now()
    }
}
