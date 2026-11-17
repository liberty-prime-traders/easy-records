package app.myliberty.easy_records.organizations.business.retail.product.api

import jakarta.persistence.Converter
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class ProductStatusConverter : EnumConverter<ProductStatus>(ProductStatus::class.java)
