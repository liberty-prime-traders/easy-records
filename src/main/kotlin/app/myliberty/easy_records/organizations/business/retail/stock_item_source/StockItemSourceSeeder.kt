package app.myliberty.easy_records.organizations.business.retail.stock_item_source

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.core.org_profile.api.OrgDataSeeder
import app.myliberty.easy_records.organizations.business.retail.stock_item_source.api.StockItemSource
import org.springframework.stereotype.Component

@Component
@TransactionalOnOrganizationSchema
class StockItemSourceSeeder(
    private val stockItemSourceRepository: StockItemSourceRepository
) : OrgDataSeeder {

    override fun seed() {
        val existingCodes = stockItemSourceRepository.findAll().map { it.code }.toSet()
        val toInsert = StockItemSource.entries
            .filter { it !in existingCodes }
            .map {
                StockItemSourceEntity(
                    code = it,
                    name = it.displayName,
                    description = it.description,
                    systemDefined = true
                )
            }
        if (toInsert.isNotEmpty()) {
            stockItemSourceRepository.saveAll(toInsert)
        }
    }
}
