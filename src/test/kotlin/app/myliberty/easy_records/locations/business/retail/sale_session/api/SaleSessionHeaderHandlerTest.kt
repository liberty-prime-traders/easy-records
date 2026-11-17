package app.myliberty.easy_records.locations.business.retail.sale_session.api

import app.myliberty.easy_records.locations.business.retail.sale.api.SaleStatus
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleUpdater
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionStore
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionUpdateFinalizer
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionValidator
import app.myliberty.easy_records.organizations.business.retail.adjustment_reason.api.AdjustmentReasonService
import app.myliberty.easy_records.organizations.business.contact.api.ContactService
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductService
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class SaleSessionHeaderHandlerTest {

    private val sessionId = UUID.randomUUID()
    private val saleSessionStore = mock(SaleSessionStore::class.java)
    private val saleSessionUpdateFinalizer = mock(SaleSessionUpdateFinalizer::class.java)
    private val saleUpdater = mock(SaleUpdater::class.java)
    private val saleSessionHeaderHandler = SaleSessionHeaderHandler(
        saleSessionStore = saleSessionStore,
        saleSessionValidator = SaleSessionValidator(
            mock(AdjustmentReasonService::class.java), mock(LocationProductService::class.java)
        ),
        contactService = mock(ContactService::class.java),
        saleSessionUpdateFinalizer = saleSessionUpdateFinalizer,
        saleUpdater = saleUpdater,
    )

    private val savedSaleId = UUID.randomUUID()
    private val savedSaleVersion = 3L

    private fun session(
        kind: SaleSessionKind,
        originalStatus: SaleStatus = SaleStatus.DRAFT,
        saleId: UUID? = null,
    ): SaleSession {
        val saleSession = SaleSession(
            sessionId = sessionId,
            locationId = UUID.randomUUID(),
            saleId = saleId,
            saleVersion = savedSaleVersion.takeIf { saleId != null },
            productsVersion = 0L,
            productsReservedAtVersion = 0L,
            originalStatus = originalStatus,
            createdById = UUID.randomUUID(),
            createdAt = OffsetDateTime.now(),
            lastUpdatedAt = OffsetDateTime.now(),
            lastAccessedById = UUID.randomUUID(),
            lastAccessedAt = OffsetDateTime.now(),
            header = SaleSessionHeader(
                referenceNumber = null, contactId = UUID.randomUUID(), soldById = null, dateSold = null, notes = null
            ),
            saleLines = emptyList(),
            saleAdjustments = emptyList(),
            salePayments = emptyList(),
            totals = SaleSessionTotals.ZERO,
            kind = kind,
            pendingConsolidation = if (kind == SaleSessionKind.PENDING_CONSOLIDATION) {
                PendingConsolidation(emptyList(), emptyMap())
            } else {
                null
            },
        )
        `when`(saleSessionStore.load(sessionId)).thenReturn(saleSession)
        return saleSession
    }

    @Test
    fun `a pending consolidation keeps its contact, sold by and date sold`() {
        session(SaleSessionKind.PENDING_CONSOLIDATION)

        listOf(
            SaleSessionHeaderUpdateDto(contactId = Optional.of(UUID.randomUUID())),
            SaleSessionHeaderUpdateDto(soldById = Optional.of(UUID.randomUUID())),
            SaleSessionHeaderUpdateDto(dateSold = Optional.of(OffsetDateTime.now())),
        ).forEach { headerUpdateDto ->
            assertThrows(RtsGenericException::class.java) {
                saleSessionHeaderHandler.update(sessionId, headerUpdateDto)
            }
        }
        verifyNoInteractions(saleSessionUpdateFinalizer)
    }

    @Test
    fun `a pending consolidation accepts a notes change`() {
        session(SaleSessionKind.PENDING_CONSOLIDATION)

        saleSessionHeaderHandler.update(sessionId, SaleSessionHeaderUpdateDto(notes = Optional.of("S1 - gate")))
    }

    @Test
    fun `a sale still accepts a contact change`() {
        session(SaleSessionKind.SALE)

        saleSessionHeaderHandler.update(
            sessionId, SaleSessionHeaderUpdateDto(contactId = Optional.of(UUID.randomUUID()))
        )
    }

    @Test
    fun `notes on a cart with no sale yet stay in the session until it is saved`() {
        session(SaleSessionKind.SALE, SaleStatus.DRAFT, saleId = null)

        saleSessionHeaderHandler.update(sessionId, SaleSessionHeaderUpdateDto(notes = Optional.of("gate")))

        verifyNoInteractions(saleUpdater)
    }

    @Test
    fun `notes on a sale that exists are written to it at once, whatever its status`() {
        listOf(SaleStatus.DRAFT, SaleStatus.CONFIRMED, SaleStatus.VOIDED, SaleStatus.DISCARDED).forEach { status ->
            session(SaleSessionKind.SALE, status, savedSaleId)

            saleSessionHeaderHandler.update(sessionId, SaleSessionHeaderUpdateDto(notes = Optional.of("gate")))

            verify(saleUpdater).updateNotesAtVersion(savedSaleId, "gate", savedSaleVersion)
            clearInvocations(saleUpdater)
        }
    }

    @Test
    fun `a read-only sale still refuses a contact change`() {
        session(SaleSessionKind.SALE, SaleStatus.CONFIRMED, savedSaleId)

        assertThrows(RtsGenericException::class.java) {
            saleSessionHeaderHandler.update(
                sessionId, SaleSessionHeaderUpdateDto(contactId = Optional.of(UUID.randomUUID()))
            )
        }
        verifyNoInteractions(saleUpdater)
    }
}
