package app.myliberty.easy_records.locations.business.retail.reports.payments.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.reports.payments.PaymentMethodTotals
import app.myliberty.easy_records.locations.business.retail.reports.payments.PaymentRow
import app.myliberty.easy_records.locations.business.retail.reports.payments.PaymentSearchFetcher
import app.myliberty.easy_records.locations.business.retail.reports.payments.PaymentSearchValidator
import app.myliberty.easy_records.locations.business.retail.reports.payments.PaymentSource
import app.myliberty.easy_records.organizations.business.payment_method.api.PaymentMethodDataFetcher
import app.myliberty.easy_records.util.business.mappers.NameResolution
import app.myliberty.easy_records.util.paging.PageRequest
import app.myliberty.easy_records.util.paging.PageResponse
import app.myliberty.easy_records.util.queries.KeysetSearchCursor
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema(readOnly = true)
class PaymentSearchService(
  private val paymentSearchFetcher: PaymentSearchFetcher,
  private val paymentMethodDataFetcher: PaymentMethodDataFetcher,
  private val nameResolution: NameResolution
) {

  fun search(pageRequest: PageRequest<PaymentSearchParameters, String>): PageResponse<PaymentRowDto, String> {
    val paymentSearchParameters = pageRequest.parameters
    PaymentSearchValidator.guardValidParameters(paymentSearchParameters)
    PaymentSearchValidator.guardValidPageSize(pageRequest.requestedSize)
    val keysetSearchCursor = KeysetSearchCursor.decode(pageRequest.previousCursor)

    val paymentRows = paymentSearchFetcher.search(
      includedSources(paymentSearchParameters), paymentSearchParameters, keysetSearchCursor, pageRequest.requestedSize
    )

    val pagePaymentRows = paymentRows.take(pageRequest.requestedSize)
    val paymentMethodNamesById = paymentMethodDataFetcher.getNamesById()
    val contactNamesById = nameResolution.organizationContacts(pagePaymentRows.map { it.contactId })

    return PageResponse(
      currentCursor = pagePaymentRows.lastOrNull()?.let { KeysetSearchCursor(it.recordedOn, it.id).encode() }
        ?: pageRequest.previousCursor,
      hasMore = paymentRows.size > pageRequest.requestedSize,
      contents = pagePaymentRows.map { toRowDto(it, paymentMethodNamesById, contactNamesById) }
    )
  }

  fun summarize(paymentSearchParameters: PaymentSearchParameters): PaymentSummaryResponseDto {
    PaymentSearchValidator.guardValidParameters(paymentSearchParameters)
    val paymentMethodNamesById = paymentMethodDataFetcher.getNamesById()

    return PaymentSummaryResponseDto(
      summaries = includedSources(paymentSearchParameters).map { paymentSource ->
        val paymentMethodTotals = paymentSearchFetcher.summarize(paymentSource, paymentSearchParameters)
        buildTypeSummary(paymentSource, paymentMethodTotals, paymentMethodNamesById)
      }
    )
  }

  private fun includedSources(paymentSearchParameters: PaymentSearchParameters) =
    PaymentSource.entries.filter { paymentSearchParameters.includes(it.type) }

  private fun toRowDto(
    paymentRow: PaymentRow,
    paymentMethodNamesById: Map<UUID, String>,
    contactNamesById: Map<UUID, String>
  ) = PaymentRowDto(
    id = paymentRow.id,
    type = paymentRow.type,
    parentReferenceNumber = paymentRow.parentReferenceNumber,
    contactName = contactNamesById[paymentRow.contactId],
    paymentMethodName = paymentMethodNamesById.getValue(paymentRow.paymentMethodId),
    amount = paymentRow.amount,
    balanceEffect = PaymentSource.forType(paymentRow.type).balanceEffect,
    paymentDate = paymentRow.paymentDate,
    recordedOn = paymentRow.recordedOn,
    voided = paymentRow.voidReason != null,
    voidedReason = paymentRow.voidReason
  )

  private fun buildTypeSummary(
    paymentSource: PaymentSource,
    paymentMethodTotals: List<PaymentMethodTotals>,
    paymentMethodNamesById: Map<UUID, String>
  ) = PaymentTypeSummaryDto(
    type = paymentSource.type,
    balanceEffect = paymentSource.balanceEffect,
    activeTotal = paymentMethodTotals.sumOf { it.activeTotal },
    voidedTotal = paymentMethodTotals.sumOf { it.voidedTotal },
    activeCount = paymentMethodTotals.sumOf { it.activeCount },
    voidedCount = paymentMethodTotals.sumOf { it.voidedCount },
    methods = paymentMethodTotals
      .map { paymentMethodTotal ->
        PaymentMethodSummaryDto(
          paymentMethodId = paymentMethodTotal.paymentMethodId,
          paymentMethodName = paymentMethodNamesById.getValue(paymentMethodTotal.paymentMethodId),
          activeTotal = paymentMethodTotal.activeTotal,
          voidedTotal = paymentMethodTotal.voidedTotal
        )
      }
      .sortedBy { it.paymentMethodName }
  )
}
