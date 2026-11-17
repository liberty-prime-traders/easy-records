package app.myliberty.easy_records.platform.business.db_migration

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.platform.business.db_migration.api.MigrationStatus
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.enums.SchemaOwnerType
import app.myliberty.easy_records.util.enums.SchemaOwnerTypeConverter
import app.myliberty.easy_records.util.model.HasReferenceEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = TableNames.DB_MIGRATION)
@HasReference(tableName = TableName.DB_MIGRATION)
class DbMigrationEntity(

    @Column(name = "db_version_id", nullable = false, updatable = false)
    var dbVersionId: UUID,

    @Column(name = "schema_owner_id", nullable = false, updatable = false)
    var schemaOwnerId: UUID,

    @Column(name = "schema_owner_type", nullable = false, length = 5)
    @Convert(converter = SchemaOwnerTypeConverter::class)
    var schemaOwnerType: SchemaOwnerType,

    @Column(name = "start_on", nullable = false)
    var startOn: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "end_on")
    var endOn: OffsetDateTime? = null,

    @Column(name = "status", nullable = false, length = 10)
    @Convert(converter = MigrationStatusConverter::class)
    var status: MigrationStatus,

    @Column(name = "type", length = 20)
    @Convert(converter = MigrationTypeConverter::class)
    var migrationType: MigrationType? = null,

    @Column(name = "message", length = 100)
    var message: String? = null,

    @Column(name = "migration_parent_id", updatable = false)
    var migrationParentId: UUID? = null

) : HasReferenceEntity()
