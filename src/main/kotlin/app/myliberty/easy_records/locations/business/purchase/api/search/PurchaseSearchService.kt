package app.myliberty.easy_records.locations.business.purchase.api.search

import app.myliberty.easy_records.locations.business.purchase.api.PurchaseResponseDto
import app.myliberty.easy_records.util.enums.PaymentStatus
import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.purchase.PurchaseAssembler
import app.myliberty.easy_records.locations.business.purchase.search.PurchasePaymentStatusSummaryRawRow
import app.myliberty.easy_records.locations.business.purchase.search.PurchaseSearchFetcher
import app.myliberty.easy_records.locations.business.purchase.search.PurchaseSearchValidator
import app.myliberty.easy_records.locations.business.purchase.search.PurchaseSupplierSummaryRawRow
import app.myliberty.easy_records.organizations.business.contact.api.ContactService
import app.myliberty.easy_records.util.paging.PageRequest
import app.myliberty.easy_records.util.paging.PageResponse
import app.myliberty.easy_records.util.queries.KeysetSearchCursor
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema(readOnly = true)
class PurchaseSearchService(
  private val purchaseSearchFetcher: PurchaseSearchFetcher,
  private val purchaseAssembler: PurchaseAssembler,
  private val contactService: ContactService
) {

  fun search(pageRequest: PageRequest<PurchaseSearchParameters, String>): PageResponse<PurchaseResponseDto, String> {
    val purchaseSearchParameters = pageRequest.parameters.sanitized()
    PurchaseSearchValidator.guardValidParameters(purchaseSearchParameters)
    PurchaseSearchValidator.guardValidPageSize(pageRequest.requestedSize)
    val cursor = KeysetSearchCursor.decode(pageRequest.previousCursor)

    val purchases = purchaseSearchFetcher.search(purchaseSearchParameters, cursor, pageRequest.requestedSize)
    val hasMore = purchases.size > pageRequest.requestedSize
    val pageRows = if (hasMore) purchases.take(pageRequest.requestedSize) else purchases

    val contents = purchaseAssembler.buildResponses(pageRows)

    val currentCursor = pageRows.lastOrNull()
      ?.let { KeysetSearchCursor(it.requiredCreatedOn(), it.id!!).encode() }
      ?: pageRequest.previousCursor

    return PageResponse(currentCursor = currentCursor, hasMore = hasMore, contents = contents)
  }

  fun summarize(purchaseSearchParameters: PurchaseSearchParameters): PurchaseSearchSummaryDto {
    val sanitizedPurchaseSearchParameters = purchaseSearchParameters.sanitized()
    PurchaseSearchValidator.guardValidParameters(sanitizedPurchaseSearchParameters)

    val byPaymentStatusRaw = purchaseSearchFetcher.summarizeByPaymentStatus(sanitizedPurchaseSearchParameters)
    val byPaymentStatus = byPaymentStatusRaw.map { it.toDto() }

    val bySupplier = if (sanitizedPurchaseSearchParameters.supplierIds.isNotEmpty()) {
      val supplierNamesById = contactService.getAllContactDtos().associateBy({ it.id }, { it.identity.displayName })
      purchaseSearchFetcher.summarizeBySupplier(sanitizedPurchaseSearchParameters).map { it.toDto(supplierNamesById) }
    } else {
      null
    }

    return PurchaseSearchSummaryDto(
      purchaseCount = byPaymentStatusRaw.sumOf { it.purchaseCount },
      totalOrdered = byPaymentStatusRaw.sumOf { it.totalOrdered },
      totalPaid = byPaymentStatusRaw.sumOf { it.totalPaid },
      byPaymentStatus = byPaymentStatus,
      bySupplier = bySupplier
    )
  }

  private fun PurchasePaymentStatusSummaryRawRow.toDto() = PurchasePaymentStatusSummaryDto(
    paymentStatus = PaymentStatus.entries.first { it.code == paymentStatusCode },
    purchaseCount = purchaseCount,
    totalOrdered = totalOrdered,
    totalPaid = totalPaid
  )

  private fun PurchaseSupplierSummaryRawRow.toDto(supplierNamesById: Map<UUID, String>) = PurchaseSupplierSummaryDto(
    supplierId = supplierId,
    supplierName = supplierNamesById.getValue(supplierId),
    purchaseCount = purchaseCount,
    totalOrdered = totalOrdered,
    totalPaid = totalPaid
  )
}
