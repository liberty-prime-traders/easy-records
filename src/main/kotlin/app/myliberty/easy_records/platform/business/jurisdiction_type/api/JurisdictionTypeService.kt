package app.myliberty.easy_records.platform.business.jurisdiction_type.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnPlatformSchema
import app.myliberty.easy_records.platform.business.jurisdiction_type.JurisdictionTypeCache
import app.myliberty.easy_records.platform.business.jurisdiction_type.JurisdictionTypeMapper
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import app.myliberty.easy_records.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnPlatformSchema
class JurisdictionTypeService(
    private val jurisdictionTypeMapper: JurisdictionTypeMapper,
    private val jurisdictionTypeCache: JurisdictionTypeCache
) {

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getAll(): Collection<JurisdictionTypeResponseDto> =
        jurisdictionTypeCache.getAll().map { jurisdictionTypeMapper.toResponseDto(it) }

    fun create(dto: JurisdictionTypeInsertDto): JurisdictionTypeResponseDto {
        StringUtils.requireHasValue(dto.name, "Name must not be blank")
        if (jurisdictionTypeCache.getAll().any { StringUtils.isEquivalent(it.name, dto.name) })
            throw RtsGenericException("A jurisdiction type named '${dto.name}' already exists")
        val saved = jurisdictionTypeCache.create(dto)
        return jurisdictionTypeMapper.toResponseDto(saved)
    }

    fun update(dto: JurisdictionTypeUpdateDto): JurisdictionTypeResponseDto {
        val existing = jurisdictionTypeCache.getAll().find { it.id == dto.id }
            ?: throw UpdatingNonExistingRecordException()
        val updated = dto.applyTo(existing)
        StringUtils.requireHasValue(updated.name, "Name must not be blank")
        if (jurisdictionTypeCache.getAll().any { StringUtils.isEquivalent(it.name, updated.name) && it.id != updated.id })
            throw RtsGenericException("A jurisdiction type named '${updated.name}' already exists")
        val saved = jurisdictionTypeCache.save(updated)
        return jurisdictionTypeMapper.toResponseDto(saved)
    }

    fun delete(id: UUID) = jurisdictionTypeCache.delete(id)
}
