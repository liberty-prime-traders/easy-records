package app.myliberty.easy_records.platform.business.reserved_subdomain

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.platform.business.reserved_subdomain.api.ReservedDomainStatus
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.HasReferenceEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames

@Entity
@Table(name = TableNames.RESERVED_SUBDOMAIN)
@HasReference(tableName = TableName.RESERVED_SUBDOMAIN)
class ReservedSubdomainEntity(
    @Column(name = "subdomain", updatable = false)
     var subdomain: String? = null,

    @Column(name = "status")
    @Convert(converter = ReservedDomainStatusConverter::class)
     var status: ReservedDomainStatus? = null

): HasReferenceEntity()
