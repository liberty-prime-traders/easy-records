package app.myliberty.easy_records.organizations.business.org_table_registry

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.util.model.BaseEntity
import app.myliberty.easy_records.util.model.TableNames
import java.util.UUID

@Entity
@Table(name = TableNames.ORG_TABLE_REGISTRY)
class OrgTableRegistryEntity(
    @Column(name = "registry_id", nullable = false, insertable = false, updatable = false)
    var registryId: UUID,

    @Column(name = "default_prefix", nullable = false, unique = true)
    var defaultPrefix: String,

    @Column(name = "display_name", nullable = false, unique = true)
    var displayName: String
): BaseEntity()
