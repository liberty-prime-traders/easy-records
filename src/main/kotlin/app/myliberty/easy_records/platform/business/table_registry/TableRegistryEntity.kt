package app.myliberty.easy_records.platform.business.table_registry

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.enums.SchemaLevel
import app.myliberty.easy_records.util.enums.SchemaLevelConverter
import app.myliberty.easy_records.util.model.BaseEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import java.util.UUID

@Entity
@Table(name = TableNames.TABLE_REGISTRY)
@HasReference(tableName = TableName.TABLE_REGISTRY)
class TableRegistryEntity(
    @Column(name = "table_name", insertable = false, updatable = false)
    var tableName: String,

    @Column(name = "default_prefix", nullable = false)
    var defaultPrefix: String,

    @Column(name = "minimum_version_id", insertable = false, updatable = false)
    var minimumVersionId: UUID,

    @Column(name = "schema_level", insertable = false, updatable = false)
    @Convert(converter = SchemaLevelConverter::class)
    var schemaLevel: SchemaLevel,

    @Column(name = "display_name", nullable = false)
    var displayName: String,

    @Column(name = "description", nullable = false)
    var description: String,

    @Column(name = "user_facing", nullable = false)
    var userFacing: Boolean = false,

    @Column(name = "validated", nullable = false)
    var validated: Boolean = false
): BaseEntity()
