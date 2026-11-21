package app.myliberty.easy_records.organizations.business.core.tax_rate.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.core.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeFetcher
import app.myliberty.easy_records.organizations.business.core.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeStatus
import app.myliberty.easy_records.organizations.business.core.tax_rate.TaxRateCache
import app.myliberty.easy_records.organizations.business.core.tax_rate.TaxRateDto
import app.myliberty.easy_records.organizations.business.core.tax_rate.TaxRateMapper
import app.myliberty.easy_records.organizations.business.core.tax_rate.TaxRateRepository
import app.myliberty.easy_records.organizations.business.core.tax_rate.TaxRateValidator
import app.myliberty.easy_records.platform.business.jurisdiction_tax_type.api.JurisdictionTaxTypeFetcher
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import app.myliberty.easy_records.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class TaxRateService(
    private val taxRateMapper: TaxRateMapper,
    private val taxRateCache: TaxRateCache,
    private val taxRateValidator: TaxRateValidator,
    private val orgJurisdictionTaxTypeFetcher: OrgJurisdictionTaxTypeFetcher,
    private val jurisdictionTaxTypeFetcher: JurisdictionTaxTypeFetcher,
    private val taxRateRepository: TaxRateRepository
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAll(): Collection<TaxRateResponseDto> {
        val platformIndex = jurisdictionTaxTypeFetcher.buildIndex()
        val parentMap = orgJurisdictionTaxTypeFetcher.getAllDtos().associateBy { it.id }
        return taxRateCache.getAll().map { taxRateDto ->
            val parent = parentMap[taxRateDto.orgJurisdictionTaxTypeId]
            val taxLabel = parent?.let { platformIndex[it.jurisdictionTaxTypeId]?.label }
            val parentIsActive = parent?.status == OrgJurisdictionTaxTypeStatus.ACTIVE
            taxRateMapper.toResponseDto(taxRateDto, taxLabel, parentIsActive)
        }
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun findActiveRateForDate(date: LocalDate): Map<UUID, ActiveTaxRateDto> {
        val rates = taxRateRepository.findActiveRateByDate(date)
        return rates.associate { rate ->
            rate.orgJurisdictionTaxTypeId to ActiveTaxRateDto(
                ratePercentage = rate.ratePercentage,
                rateFlatAmount = rate.rateFlatAmount,
                taxIsBilledToCustomerSeparately = rate.taxIsBilledToCustomerSeparately,
                taxIsIncludedInTaxableAmount = rate.taxIsIncludedInTaxableAmount
            )
        }
    }

    fun create(dto: TaxRateInsertDto): TaxRateResponseDto {
        taxRateValidator.validateForCreate(dto)
        val savedDto = taxRateCache.create(dto)
        return toResponseDto(savedDto)
    }

    fun update(dto: TaxRateUpdateDto): TaxRateResponseDto {
        if (dto.name == null && dto.endDate == null) throw LibertyGenericException("Nothing to update")
        val existing = taxRateCache.getAll().firstOrNull { it.id == dto.id } ?: throw UpdatingNonExistingRecordException()
        taxRateValidator.validateForUpdate(existing, dto)
        val updated = dto.applyTo(existing)
        val savedDto = taxRateCache.save(updated)
        return toResponseDto(savedDto)
    }

    private fun toResponseDto(taxRateDto: TaxRateDto): TaxRateResponseDto {
        val platformIndex = jurisdictionTaxTypeFetcher.buildIndex()
        val parent = orgJurisdictionTaxTypeFetcher.getAllDtos().firstOrNull { it.id == taxRateDto.orgJurisdictionTaxTypeId }
        val taxLabel = parent?.let { platformIndex[it.jurisdictionTaxTypeId]?.label }
        val parentIsActive = parent?.status == OrgJurisdictionTaxTypeStatus.ACTIVE
        return taxRateMapper.toResponseDto(taxRateDto, taxLabel, parentIsActive)
    }
}
