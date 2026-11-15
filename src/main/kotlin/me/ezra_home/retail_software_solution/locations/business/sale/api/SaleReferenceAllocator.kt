package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.util.business.reference_number.ReferenceNumberGenerator
import me.ezra_home.retail_software_solution.util.model.TableName
import org.springframework.stereotype.Component

@Component
class SaleReferenceAllocator(private val referenceNumberGenerator: ReferenceNumberGenerator) {

    // A sequence value is never handed back, so a rolled-back transaction leaves a gap in the sale references.
    fun allocate(): String = referenceNumberGenerator.generateSingle(TableName.SALE)
}
