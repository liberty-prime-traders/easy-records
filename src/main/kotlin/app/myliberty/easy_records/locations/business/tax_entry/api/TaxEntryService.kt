package app.myliberty.easy_records.locations.business.tax_entry.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.tax_entry.TaxEntryEntity
import app.myliberty.easy_records.locations.business.tax_entry.TaxEntryRepository
import org.springframework.stereotype.Service

@Service
class TaxEntryService(
    private val taxEntryRepository: TaxEntryRepository,
) {

    @TransactionalOnLocationSchema(readOnly = true)
    fun findBySourceReference(sourceReferenceNumber: String, sourceType: TaxSourceType): List<TaxEntryDto> =
        findBySourceReferences(listOf(sourceReferenceNumber), sourceType)

    @TransactionalOnLocationSchema(readOnly = true)
    fun findBySourceReferences(
        sourceReferenceNumbers: Collection<String>,
        sourceType: TaxSourceType
    ): List<TaxEntryDto> =
        taxEntryRepository.findBySourceReferenceNumberInAndSourceType(sourceReferenceNumbers, sourceType)
            .map { it.toDto() }

    @TransactionalOnLocationSchema
    fun createAll(dtos: List<TaxEntryCreateDto>) {
        if (dtos.isEmpty()) return
        taxEntryRepository.saveAll(dtos.map { it.toEntity() })
    }

    private fun TaxEntryEntity.toDto() = TaxEntryDto(
        sourceReferenceNumber = sourceReferenceNumber,
        sourceType = sourceType,
        direction = direction,
        taxTypeId = taxTypeId,
        taxTypeName = taxTypeName,
        jurisdictionName = jurisdictionName,
        fiscalPeriodId = fiscalPeriodId,
        calculationMethod = calculationMethod,
        rate = rate,
        taxIsBilledToCustomerSeparately = taxIsBilledToCustomerSeparately,
        taxIsIncludedInTaxableAmount = taxIsIncludedInTaxableAmount,
        taxableAmount = taxableAmount,
        resolvedTaxableBase = resolvedTaxableBase,
        taxAmount = taxAmount,
    )

    private fun TaxEntryCreateDto.toEntity() = TaxEntryEntity(
        sourceReferenceNumber = sourceReferenceNumber,
        sourceType = sourceType,
        direction = direction,
        taxTypeId = taxTypeId,
        taxTypeName = taxTypeName,
        jurisdictionName = jurisdictionName,
        fiscalPeriodId = fiscalPeriodId,
        calculationMethod = calculationMethod,
        rate = rate,
        taxIsBilledToCustomerSeparately = taxIsBilledToCustomerSeparately,
        taxIsIncludedInTaxableAmount = taxIsIncludedInTaxableAmount,
        taxableAmount = taxableAmount,
        resolvedTaxableBase = resolvedTaxableBase,
        taxAmount = taxAmount,
    )
}
