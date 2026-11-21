package app.myliberty.easy_records.util.model

import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PreUpdate
import app.myliberty.easy_records.util.exceptions.LibertyGenericException

@MappedSuperclass
abstract class ImmutableEntity: HasReferenceEntity() {
    @PreUpdate
    fun onUpdate() {
        throw LibertyGenericException("Cannot update an immutable entity ${this::class.simpleName}")
    }
}
