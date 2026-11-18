package app.myliberty.easy_records.util.model

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import java.util.UUID

@MappedSuperclass
abstract class ExpirableUserAssignmentEntity(
    @Column(name = "user_id", updatable = false)
    var userId: UUID? = null

): ExpirableAssignmentEntity()
