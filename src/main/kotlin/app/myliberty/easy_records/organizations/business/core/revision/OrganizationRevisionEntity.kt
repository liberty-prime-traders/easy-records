package app.myliberty.easy_records.organizations.business.revision

import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.util.model.BaseRevisionEntity
import org.hibernate.envers.RevisionEntity

@Entity
@RevisionEntity
@Table(name = "revinfo")
class OrganizationRevisionEntity : BaseRevisionEntity()
