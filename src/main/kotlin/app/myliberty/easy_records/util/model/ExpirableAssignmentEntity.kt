package app.myliberty.easy_records.util.model

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import java.time.OffsetDateTime

@MappedSuperclass
class ExpirableAssignmentEntity(
    @Column(name = "end_on")
    var endOn: OffsetDateTime? = null

): HasReferenceEntity()
