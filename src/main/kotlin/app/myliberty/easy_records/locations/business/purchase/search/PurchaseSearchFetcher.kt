package app.myliberty.easy_records.locations.business.purchase.search

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.purchase.PurchaseEntity
import app.myliberty.easy_records.locations.business.purchase.api.search.PurchaseSearchParameters
import app.myliberty.easy_records.util.queries.KeysetSearchCursor
import org.springframework.stereotype.Component

@Component
@TransactionalOnLocationSchema(readOnly = true)
class PurchaseSearchFetcher(
  private val purchaseSearchListExecutor: PurchaseSearchListExecutor,
  private val purchaseSearchSummaryExecutor: PurchaseSearchSummaryExecutor
) {

  fun search(
    purchaseSearchParameters: PurchaseSearchParameters,
    cursor: KeysetSearchCursor?,
    requestedSize: Int,
    oldestFirst: Boolean = false
  ): List<PurchaseEntity> {
    val predicate = PurchaseSearchQueryBuilder.buildPredicate(purchaseSearchParameters)
    val sqlQuery = PurchaseSearchQueryBuilder.buildListQuery(predicate, cursor, oldestFirst)
    return purchaseSearchListExecutor.execute(sqlQuery, requestedSize + 1, setTimeout = true)
  }

  fun summarizeByPaymentStatus(purchaseSearchParameters: PurchaseSearchParameters): List<PurchasePaymentStatusSummaryRawRow> {
    val predicate = PurchaseSearchQueryBuilder.buildPredicate(purchaseSearchParameters)
    val sqlQuery = PurchaseSearchQueryBuilder.buildPaymentStatusSummaryQuery(predicate)
    return purchaseSearchSummaryExecutor.executeUnpaged(sqlQuery, setTimeout = true)
      .map { PurchaseSearchRowMapper.paymentStatusSummaryFromTuple(it) }
  }

  fun summarizeBySupplier(purchaseSearchParameters: PurchaseSearchParameters): List<PurchaseSupplierSummaryRawRow> {
    val predicate = PurchaseSearchQueryBuilder.buildPredicate(purchaseSearchParameters)
    val sqlQuery = PurchaseSearchQueryBuilder.buildSupplierSummaryQuery(predicate)
    return purchaseSearchSummaryExecutor.executeUnpaged(sqlQuery, setTimeout = true)
      .map { PurchaseSearchRowMapper.supplierSummaryFromTuple(it) }
  }
}
