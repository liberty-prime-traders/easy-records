package app.myliberty.easy_records.platform.business.jurisdiction_tax_type.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnPlatformSchema
import app.myliberty.easy_records.platform.business.jurisdiction.api.JurisdictionFetcher
import app.myliberty.easy_records.platform.business.jurisdiction_tax_type.JurisdictionTaxTypeCache
import app.myliberty.easy_records.platform.business.tax_type.api.TaxTypeFetcher
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnPlatformSchema
class JurisdictionTaxTypeService(
    private val jurisdictionTaxTypeCache: JurisdictionTaxTypeCache,
    private val jurisdictionFetcher: JurisdictionFetcher,
    private val taxTypeFetcher: TaxTypeFetcher
) {

    fun createAll(dtos: List<JurisdictionTaxTypeInsertDto>) {
        if (dtos.isEmpty()) return
        val taxTypeIds = taxTypeFetcher.getAllDtos().mapTo(HashSet()) { it.id }
        val jurisdictionIds = jurisdictionFetcher.getAllDtos().mapTo(HashSet()) { it.id }
        val existingLinks = jurisdictionTaxTypeCache.getAll().mapTo(HashSet()) { it.taxTypeId to it.jurisdictionId }
        dtos.forEach { dto ->
            if (dto.taxTypeId !in taxTypeIds) throw RtsGenericException("Tax type not found")
            if (dto.jurisdictionId !in jurisdictionIds) throw RtsGenericException("Jurisdiction not found")
            if ((dto.taxTypeId to dto.jurisdictionId) in existingLinks)
                throw RtsGenericException("A link between this tax type and jurisdiction already exists")
        }
        jurisdictionTaxTypeCache.createAll(dtos)
    }

    fun addOrReactivate(jurisdictionId: UUID, taxTypeIds: List<UUID>) {
        if (taxTypeIds.isEmpty()) return
        val taxTypeIdSet = taxTypeFetcher.getAllDtos().mapTo(HashSet()) { it.id }
        val existingJurisdictionTaxTypes = jurisdictionTaxTypeCache.getAll()
            .filter { it.jurisdictionId == jurisdictionId }
            .associateBy { it.taxTypeId }
        val toReactivate = mutableListOf<JurisdictionTaxTypeDto>()
        val toCreate = mutableListOf<JurisdictionTaxTypeInsertDto>()
        taxTypeIds.forEach { taxTypeId ->
            if (taxTypeId !in taxTypeIdSet) throw RtsGenericException("Tax type not found")
            val existing = existingJurisdictionTaxTypes[taxTypeId]
            when {
                existing == null -> toCreate.add(JurisdictionTaxTypeInsertDto(taxTypeId, jurisdictionId))
                !existing.active -> toReactivate.add(existing.copy(active = true))
            }
        }
        if (toReactivate.isNotEmpty()) {
            jurisdictionTaxTypeCache.saveAll(toReactivate)
        }
        if (toCreate.isNotEmpty()) {
            jurisdictionTaxTypeCache.createAll(toCreate)
        }
    }

    fun stopByTaxTypeIds(jurisdictionId: UUID, taxTypeIds: List<UUID>) {
        val linkIndex = jurisdictionTaxTypeCache.getAll()
            .filter { it.jurisdictionId == jurisdictionId }
            .associateBy { it.taxTypeId }
        val dtos = taxTypeIds.map { taxTypeId ->
            linkIndex[taxTypeId]?.copy(active = false)
                ?: throw RtsGenericException("Tax type $taxTypeId is not linked to this jurisdiction")
        }
        jurisdictionTaxTypeCache.saveAll(dtos)
    }
}
