package app.myliberty.easy_records.organizations.business.retail.product_tag

import java.io.Serializable
import java.util.UUID

data class ProductTagInsertDto(
    val orgProductId: UUID,
    val tagId: UUID
) : Serializable
