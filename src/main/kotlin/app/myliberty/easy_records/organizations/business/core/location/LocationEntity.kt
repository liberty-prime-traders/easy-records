package app.myliberty.easy_records.organizations.business.location

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.organizations.business.location.api.LocationType
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.HasReferenceEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import org.hibernate.envers.Audited

@Audited
@Entity
@Table(name = TableNames.LOCATION)
@HasReference(tableName = TableName.LOCATION)
class LocationEntity(

    @Column(name = "location_type", nullable = false)
    @Convert(converter = LocationTypeConverter::class)
    var locationType: LocationType,

    @Column(name = "name", length = 100)
    var name: String,

    @Column(name = "description", length = 1000)
    var description: String? = null,

    @Column(name = "schema_name", length = 100, nullable = false, updatable = false)
    var schemaName: String? = null,

    @Column(name = "timezone", length = 50)
    var timezone: String? = null

): HasReferenceEntity()
