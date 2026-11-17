package app.myliberty.easy_records.platform.business.feature

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.platform.business.feature.api.Feature
import app.myliberty.easy_records.platform.business.feature.api.FeatureConverter
import app.myliberty.easy_records.util.model.BaseEntity
import app.myliberty.easy_records.util.model.TableNames

@Entity
@Table(name = TableNames.FEATURE)
class FeatureEntity(

    @Convert(converter = FeatureConverter::class)
    @Column(name = "feature", nullable = false, updatable = false)
    var feature: Feature,

    @Column(name = "description", nullable = false)
    var description: String

) : BaseEntity()
