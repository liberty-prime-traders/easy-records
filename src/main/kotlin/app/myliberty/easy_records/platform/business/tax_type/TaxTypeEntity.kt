package app.myliberty.easy_records.platform.business.tax_type

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.platform.business.tax_type.api.TaxCalculationMethod
import app.myliberty.easy_records.platform.business.tax_type.api.TaxCalculationMethodConverter
import app.myliberty.easy_records.platform.business.tax_type.api.TaxApplicationLevel
import app.myliberty.easy_records.platform.business.tax_type.api.TaxRecoveryType
import app.myliberty.easy_records.platform.business.tax_type.api.TaxTrigger
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.HasReferenceEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import org.hibernate.envers.Audited

@Audited
@Entity
@Table(name = TableNames.TAX_TYPE)
@HasReference(tableName = TableName.TAX_TYPE)
class TaxTypeEntity(

    @Column(name = "name", length = 100, nullable = false)
    var name: String,

    @Column(name = "description", length = 500)
    var description: String? = null,

    @Convert(converter = TaxCalculationMethodConverter::class)
    @Column(name = "calculation_method", length = 5, nullable = false)
    var calculationMethod: TaxCalculationMethod,

    @Convert(converter = TaxRecoveryTypeConverter::class)
    @Column(name = "tax_recovery_type", length = 5, nullable = false)
    var taxRecoveryType: TaxRecoveryType,

    @Convert(converter = TaxApplicationLevelConverter::class)
    @Column(name = "tax_application_level", length = 5, nullable = false)
    var taxApplicationLevel: TaxApplicationLevel,

    @Convert(converter = TaxTriggerConverter::class)
    @Column(name = "tax_triggers", nullable = false)
    var taxTriggers: Set<TaxTrigger>

) : HasReferenceEntity()
