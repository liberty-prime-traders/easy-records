package app.myliberty.easy_records.locations.business.retail.sale.api

import app.myliberty.easy_records.locations.business.retail.sale_payment.api.PersistedSalePayment
import java.time.OffsetDateTime
import java.util.UUID

data class SaleSaveResult(
    val saleId: UUID,
    val saleReferenceNumber: String,
    val newVersion: Long,
    val saleStatus: SaleStatus,
    val dateSold: OffsetDateTime?,
    val soldById: UUID?,
    val saleLineIdsByClientKey: Map<UUID, UUID>,
    val saleAdjustmentIdsByClientKey: Map<UUID, UUID>,
    val persistedSalePaymentsByClientKey: Map<UUID, PersistedSalePayment>,
)
