package app.myliberty.easy_records.locations.business.retail.sale.api

import app.myliberty.easy_records.util.business.reference_number.ReferenceNumberGenerator
import app.myliberty.easy_records.util.model.TableName
import org.springframework.stereotype.Component

@Component
class SaleReferenceAllocator(private val referenceNumberGenerator: ReferenceNumberGenerator) {

    // A sequence value is never handed back, so a rolled-back transaction leaves a gap in the sale references.
    fun allocate(): String = referenceNumberGenerator.generateSingle(TableName.SALE)
}
