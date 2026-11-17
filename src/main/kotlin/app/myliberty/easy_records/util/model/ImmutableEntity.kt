package app.myliberty.easy_records.util.model

import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PreUpdate
import app.myliberty.easy_records.util.exceptions.RtsGenericException

@MappedSuperclass
abstract class ImmutableEntity: HasReferenceEntity() {
    @PreUpdate
    fun onUpdate() {
        throw RtsGenericException("Cannot update an immutable entity ${this::class.simpleName}")
    }
}
