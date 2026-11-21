package app.myliberty.easy_records.locations.business.retail.opening_stock

import app.myliberty.easy_records.locations.business.retail.opening_stock.api.OpeningStockLineDto
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import java.util.UUID

object OpeningStockValidator {

    fun guardNotEmpty(lines: List<OpeningStockLineDto>) {
        if (lines.isEmpty()) throw LibertyGenericException("Opening stock declaration must have at least one line")
    }

    fun guardNoDuplicateProducts(lines: List<OpeningStockLineDto>) {
        val duplicates = lines.groupingBy { it.locationProductId }.eachCount().filterValues { it > 1 }.keys
        if (duplicates.isNotEmpty()) throw LibertyGenericException("Duplicate products in opening stock declaration: $duplicates")
    }

    fun guardNotAlreadyDeclared(
        alreadyDeclaredProductIds: Set<UUID>,
        requestedProductIds: Collection<UUID>,
        labelsByProductId: Map<UUID, String>
    ) {
        val conflicts = requestedProductIds.filter { it in alreadyDeclaredProductIds }
        if (conflicts.isNotEmpty()) {
            val labels = conflicts.map { " - ${labelsByProductId.getValue(it)}" }
            throw LibertyGenericException("Opening stock already declared for the products below:", labels)
        }
    }
}
