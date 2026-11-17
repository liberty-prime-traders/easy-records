package app.myliberty.easy_records.locations.business.retail.sale

import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductDataFetcher
import app.myliberty.easy_records.locations.business.retail.sale.api.DraftSalePersister
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleSaveRequest
import app.myliberty.easy_records.locations.business.retail.sale_adjustment.api.RetainedSaleAdjustments
import app.myliberty.easy_records.locations.business.retail.sale_adjustment.api.SaleAdjustmentSyncer
import app.myliberty.easy_records.locations.business.retail.stock_reservation.api.StockReserver
import app.myliberty.easy_records.organizations.business.core.fiscal_period.api.FiscalPeriodService
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

class DraftSalePersisterPreAllocatedReferenceTest {

    private val saleRepository = mock(SaleRepository::class.java)
    private val saleLocker = mock(SaleLocker::class.java)
    private var referenceNumberWhenSaved: String? = "NOT SAVED"
    private val saleAdjustmentSyncer = mock(SaleAdjustmentSyncer::class.java)
    private val saleSaveFinalizer = mock(SaleSaveFinalizer::class.java) {
        SaleSaveFinalizer.SaleSaveFinalizeResult(emptyMap(), emptyMap())
    }

    private val draftSalePersister = DraftSalePersister(
        saleRepository = saleRepository,
        saleLineRepository = mock(SaleLineRepository::class.java),
        stockReserver = mock(StockReserver::class.java),
        saleValidator = mock(SaleValidator::class.java),
        saleLocker = saleLocker,
        locationProductDataFetcher = mock(LocationProductDataFetcher::class.java),
        fiscalPeriodService = mock(FiscalPeriodService::class.java),
        saleSaveFinalizer = saleSaveFinalizer,
        saleAdjustmentSyncer = saleAdjustmentSyncer,
    )

    // The real listener fills the reference at persist time, after the entity has been handed to save.
    private fun stubSaveLikeTheEntityListener() {
        `when`(saleRepository.save(any(SaleEntity::class.java))).thenAnswer { invocation ->
            invocation.getArgument<SaleEntity>(0).also {
                referenceNumberWhenSaved = it.referenceNumber
                it.id = UUID.randomUUID()
                if (it.referenceNumber == null) it.referenceNumber = "FROMLISTENER"
            }
        }
    }

    // Mockito matchers return null, which Kotlin refuses for a non-null parameter.
    private fun anyUuid(): UUID {
        any(UUID::class.java)
        return UUID(0, 0)
    }

    private fun anyRequest(fallback: SaleSaveRequest): SaleSaveRequest {
        any(SaleSaveRequest::class.java)
        return fallback
    }

    private fun saveRequest(saleId: UUID?, preAllocatedReferenceNumber: String?) = SaleSaveRequest(
        saleId = saleId,
        expectedVersion = null,
        contactId = UUID.randomUUID(),
        soldById = null,
        dateSold = null,
        notes = null,
        saleLines = emptyList(),
        saleAdjustments = emptyList(),
        salePayments = emptyList(),
        preAllocatedReferenceNumber = preAllocatedReferenceNumber,
    )

    @Test
    fun `a new draft is saved under the reference number that was allocated for it`() {
        stubSaveLikeTheEntityListener()
        val newDraftRequest = saveRequest(null, "SALE0009")
        `when`(saleAdjustmentSyncer.deleteRemoved(anyUuid(), anyRequest(newDraftRequest)))
            .thenReturn(RetainedSaleAdjustments(emptyList()))

        val saleSaveResult = draftSalePersister.saveDraft(newDraftRequest)

        assertEquals("SALE0009", referenceNumberWhenSaved)
        assertEquals("SALE0009", saleSaveResult.saleReferenceNumber)
    }

    @Test
    fun `a draft saved without an allocated reference leaves it for the entity listener`() {
        val unallocatedRequest = saveRequest(null, null)
        stubSaveLikeTheEntityListener()

        `when`(saleAdjustmentSyncer.deleteRemoved(anyUuid(), anyRequest(unallocatedRequest)))
            .thenReturn(RetainedSaleAdjustments(emptyList()))

        draftSalePersister.saveDraft(unallocatedRequest)

        assertNull(referenceNumberWhenSaved)
    }

    @Test
    fun `an allocated reference on an existing sale is refused before anything is loaded or saved`() {
        assertThrows(RtsGenericException::class.java) {
            draftSalePersister.saveDraft(saveRequest(UUID.randomUUID(), "SALE0009"))
        }

        verify(saleRepository, never()).save(any(SaleEntity::class.java))
    }
}
