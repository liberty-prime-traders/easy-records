package app.myliberty.easy_records.organizations.business.product.api

import app.myliberty.easy_records.util.enums.HasCode

enum class ProductStatus(override val code: String) : HasCode {
    ACTIVE("A"),
    DISCONTINUED("X"),
    AWAITING_FINAL_SALE("AFS")
}
