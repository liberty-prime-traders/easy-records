package app.myliberty.easy_records.organizations.business.core.org_jurisdiction_tax_type

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.organizations.business.core.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeStatus
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.HasReferenceEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import org.hibernate.envers.Audited
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.ORG_JURISDICTION_TAX_TYPE)
@HasReference(tableName = TableName.ORG_JURISDICTION_TAX_TYPE)
class OrgJurisdictionTaxTypeEntity(

    @Column(name = "jurisdiction_tax_type_id", nullable = false, updatable = false)
    var jurisdictionTaxTypeId: UUID,

    @Convert(converter = OrgJurisdictionTaxTypeStatusConverter::class)
    @Column(name = "status", length = 5, nullable = false)
    var status: OrgJurisdictionTaxTypeStatus = OrgJurisdictionTaxTypeStatus.ACTIVE,

    @Column(name = "payable_account_code", length = 50)
    var payableAccountCode: String,

    @Column(name = "recoverable_account_code", length = 50)
    var recoverableAccountCode: String? = null

) : HasReferenceEntity()
