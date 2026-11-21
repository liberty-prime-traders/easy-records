package app.myliberty.easy_records.organizations.business.core.feature

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.platform.business.feature.api.Feature
import app.myliberty.easy_records.platform.business.feature.api.FeatureConverter
import app.myliberty.easy_records.util.model.HasCreatorEntity
import app.myliberty.easy_records.util.model.TableNames
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = TableNames.ORGANIZATION_FEATURE)
class OrganizationFeatureEntity(

    @Column(name = "feature_code", nullable = false, updatable = false)
    @Convert(converter = FeatureConverter::class)
    var feature: Feature,

    @Column(name = "status", nullable = false)
    @Convert(converter = OrganizationFeatureStatusConverter::class)
    var status: OrganizationFeatureStatus,

    @Column(name = "enabled_on")
    var enabledOn: OffsetDateTime? = null,

    @Column(name = "enabled_by")
    var enabledBy: UUID? = null,

    @Column(name = "disabled_on")
    var disabledOn: OffsetDateTime? = null,

    @Column(name = "disabled_by")
    var disabledBy: UUID? = null

) : HasCreatorEntity()
