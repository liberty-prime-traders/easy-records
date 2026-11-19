package app.myliberty.easy_records.platform.business.jurisdiction.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnPlatformSchema
import app.myliberty.easy_records.platform.business.jurisdiction.JurisdictionCache
import app.myliberty.easy_records.platform.business.jurisdiction.JurisdictionMapper
import app.myliberty.easy_records.platform.business.jurisdiction.JurisdictionMappingContext
import app.myliberty.easy_records.platform.business.jurisdiction.JurisdictionValidator
import app.myliberty.easy_records.platform.business.jurisdiction_tax_type.api.JurisdictionTaxTypeFetcher
import app.myliberty.easy_records.platform.business.jurisdiction_tax_type.api.JurisdictionTaxTypeInsertDto
import app.myliberty.easy_records.platform.business.jurisdiction_tax_type.api.JurisdictionTaxTypeService
import app.myliberty.easy_records.platform.business.jurisdiction_type.api.JurisdictionTypeService
import app.myliberty.easy_records.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnPlatformSchema
class JurisdictionService(
    private val jurisdictionMapper: JurisdictionMapper,
    private val jurisdictionCache: JurisdictionCache,
    private val jurisdictionValidator: JurisdictionValidator,
    private val jurisdictionTaxTypeFetcher: JurisdictionTaxTypeFetcher,
    private val jurisdictionTaxTypeService: JurisdictionTaxTypeService,
    private val jurisdictionTypeService: JurisdictionTypeService
) {

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getAll(): Collection<JurisdictionResponseDto> {
        val ctx = buildMappingContext()
        return jurisdictionCache.getAll().map { jurisdictionMapper.toResponseDto(it, ctx) }
    }

    private fun buildMappingContext() = JurisdictionMappingContext(
        typeNames = jurisdictionTypeService.getAll().associate { it.id to it.name },
        jurisdictionNames = jurisdictionCache.getAll().associate { it.id to it.name },
        taxTypesByJurisdiction = jurisdictionTaxTypeFetcher.getActiveTaxTypeIdsByJurisdiction()
    )

    fun create(dto: JurisdictionInsertDto): JurisdictionResponseDto {
        jurisdictionValidator.validateName(dto.name)
        jurisdictionValidator.validateTypeExists(dto.jurisdictionTypeId)
        jurisdictionValidator.validateParent(dto.parentJurisdictionId)
        val saved = jurisdictionCache.create(dto)
        dto.taxTypesToAddOrReactivate
            ?.map { JurisdictionTaxTypeInsertDto(it, saved.id) }
            ?.let { jurisdictionTaxTypeService.createAll(it) }
        return jurisdictionMapper.toResponseDto(saved, buildMappingContext())
    }

    fun update(dto: JurisdictionUpdateDto): JurisdictionResponseDto {
        val existing = jurisdictionCache.getAll().find { it.id == dto.id } ?: throw UpdatingNonExistingRecordException()
        dto.jurisdictionTypeId?.let { jurisdictionTypeId ->
            jurisdictionTypeId.ifPresent { jurisdictionValidator.validateTypeExists(it) }
        }
        jurisdictionValidator.validateParent(dto.parentJurisdictionId?.orElse(null), dto.id)
        val updated = dto.applyTo(existing)
        jurisdictionValidator.validateName(updated.name)
        val saved = jurisdictionCache.save(updated)
        dto.taxTypesToAddOrReactivate?.let {
            jurisdictionTaxTypeService.addOrReactivate(dto.id, it)
        }
        dto.taxTypesToDiscontinue?.let {
            jurisdictionTaxTypeService.stopByTaxTypeIds(dto.id, it)
        }
        return jurisdictionMapper.toResponseDto(saved, buildMappingContext())
    }

    fun delete(id: UUID) = jurisdictionCache.delete(id)
}
