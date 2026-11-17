package app.myliberty.easy_records.organizations.business.adjustment_reason

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.organizations.business.adjustment_reason.api.AdjustmentDirection
import app.myliberty.easy_records.organizations.business.adjustment_reason.api.AdjustmentDirectionConverter
import app.myliberty.easy_records.util.model.HasCreatorEntity
import app.myliberty.easy_records.util.model.TableNames
import org.hibernate.envers.Audited

@Audited
@Entity
@Table(name = TableNames.ADJUSTMENT_REASON)
class AdjustmentReasonEntity(

    @Column(name = "name", nullable = false, length = 100)
    var name: String,

    @Column(name = "code", length = 5, updatable = false)
    var code: String? = null,

    @Convert(converter = AdjustmentDirectionConverter::class)
    @Column(name = "direction", nullable = false, length = 5)
    var direction: AdjustmentDirection,

    @Column(name = "system_defined", nullable = false, updatable = false)
    var systemDefined: Boolean = false

) : HasCreatorEntity()
