package app.myliberty.easy_records.platform.business.jurisdiction_tax_type.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnPlatformSchema
import app.myliberty.easy_records.organizations.business.core.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeFetcher
import app.myliberty.easy_records.platform.business.jurisdiction.api.JurisdictionDto
import app.myliberty.easy_records.platform.business.jurisdiction.api.JurisdictionFetcher
import app.myliberty.easy_records.platform.business.jurisdiction_tax_type.JurisdictionTaxTypeCache
import app.myliberty.easy_records.platform.business.tax_type.api.TaxRecoveryType
import app.myliberty.easy_records.platform.business.tax_type.api.TaxTypeFetcher
import app.myliberty.easy_records.util.ui_models.TreeNodeWithData
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnPlatformSchema(readOnly = true)
class JurisdictionTaxesTreeBuilder(
    private val orgJurisdictionTaxTypeFetcher: OrgJurisdictionTaxTypeFetcher,
    private val taxTypeFetcher: TaxTypeFetcher,
    private val jurisdictionTaxTypeCache: JurisdictionTaxTypeCache,
    private val jurisdictionFetcher: JurisdictionFetcher
) {

    fun getAvailableTaxTypes(): List<TreeNodeWithData<UUID, TaxRecoveryType>> {
        val assignedIds = orgJurisdictionTaxTypeFetcher.getAlreadyAssignedIds()
        val taxTypeIndex = taxTypeFetcher.getAllDtos().associateBy { it.id }
        val linksByJurisdiction = jurisdictionTaxTypeCache.getActive()
            .filterNot { it.id in assignedIds }
            .groupBy { it.jurisdictionId }
        val allJurisdictions = jurisdictionFetcher.getAllDtos()
        val childJurisdictions = allJurisdictions.groupBy { it.parentJurisdictionId }

        fun buildJurisdictionNode(jurisdiction: JurisdictionDto): TreeNodeWithData<UUID, TaxRecoveryType>? {
            val taxTypeNodes = linksByJurisdiction[jurisdiction.id].orEmpty().map { link ->
                val taxType = taxTypeIndex[link.taxTypeId]
                val taxTypeLabel = "${jurisdiction.name} - ${taxType?.name}"
                TreeNodeWithData(link.id, taxTypeLabel, selectable = true, data = taxType?.taxRecoveryType)
            }
            val childNodes = childJurisdictions[jurisdiction.id].orEmpty().mapNotNull { buildJurisdictionNode(it) }
            if (taxTypeNodes.isEmpty() && childNodes.isEmpty()) return null
            return TreeNodeWithData(jurisdiction.id, jurisdiction.name, selectable = false, children = taxTypeNodes + childNodes)
        }

        return allJurisdictions
            .filter { it.parentJurisdictionId == null }
            .mapNotNull { buildJurisdictionNode(it) }
    }
}
