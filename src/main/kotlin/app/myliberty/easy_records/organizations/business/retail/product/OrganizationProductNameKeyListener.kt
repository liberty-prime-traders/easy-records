package app.myliberty.easy_records.organizations.business.retail.product

import app.myliberty.easy_records.util.business.StringUtils
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate

// A listener rather than a callback on the entity: BaseEntity and ImmutableEntity already declare @PrePersist and @PreUpdate.
class OrganizationProductNameKeyListener {

    @PrePersist
    @PreUpdate
    fun refreshNameKey(organizationProductEntity: OrganizationProductEntity) {
        organizationProductEntity.nameKey = StringUtils.alphanumericKey(organizationProductEntity.productName)
    }
}
