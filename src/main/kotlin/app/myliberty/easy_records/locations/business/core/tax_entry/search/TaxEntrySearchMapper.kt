package app.myliberty.easy_records.locations.business.core.tax_entry.search

import app.myliberty.easy_records.locations.business.core.tax_entry.api.TaxEntrySearchResultDto
import java.util.UUID

object TaxEntrySearchMapper {

    fun toRowDto(
        row: TaxEntrySearchRawRow,
        fiscalPeriodNamesById: Map<UUID, String>
    ): TaxEntrySearchResultDto = TaxEntrySearchResultDto(
        id = row.id,
        sourceReferenceNumber = row.sourceReferenceNumber,
        sourceType = row.sourceType,
        direction = row.direction,
        taxTypeId = row.taxTypeId,
        taxTypeName = row.taxTypeName,
        jurisdictionName = row.jurisdictionName,
        fiscalPeriodId = row.fiscalPeriodId,
        fiscalPeriodName = fiscalPeriodNamesById.getValue(row.fiscalPeriodId),
        calculationMethod = row.calculationMethod,
        rate = row.rate,
        taxIsBilledToCustomerSeparately = row.taxIsBilledToCustomerSeparately,
        taxIsIncludedInTaxableAmount = row.taxIsIncludedInTaxableAmount,
        taxableAmount = row.taxableAmount,
        resolvedTaxableBase = row.resolvedTaxableBase,
        taxAmount = row.taxAmount,
        recordedOn = row.createdOn
    )
}
