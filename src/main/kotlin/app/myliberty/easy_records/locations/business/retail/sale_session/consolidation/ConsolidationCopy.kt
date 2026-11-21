package app.myliberty.easy_records.locations.business.retail.sale_session.consolidation

import app.myliberty.easy_records.locations.business.retail.sale.api.SaleLineDto
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSession
import java.math.BigDecimal
import java.util.UUID

data class ConsolidationCopy(
    val saleSession: SaleSession,
    val sourceSaleReferenceByLineKey: Map<UUID, String>,
    val sourceSaleLinesBySaleId: Map<UUID, List<SaleLineDto>>,
    val orderLevelAmountByAdjustmentKey: Map<UUID, BigDecimal>,
    val saleReferencesWithOrderLevelAdjustments: Set<String>,
)
