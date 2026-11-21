package app.myliberty.easy_records.organizations.business.retail.product.api

import java.io.Serializable
import java.util.UUID

data class TagSummaryDto(
    val id: UUID,
    val tagName: String?
) : Serializable
