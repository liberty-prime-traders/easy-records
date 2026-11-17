package app.myliberty.easy_records.util.model

import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PrePersist
import java.util.UUID

@MappedSuperclass
abstract class BaseEntity(
    @Id
    @Column(name = ColumnNames.Common.ID, nullable = false)
    var id: UUID? = null
) {

    @PrePersist
    fun prePersist() {
        if (id == null) id = UUID.randomUUID()
    }
}
