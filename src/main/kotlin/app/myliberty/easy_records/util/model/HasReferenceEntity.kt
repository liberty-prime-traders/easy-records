package app.myliberty.easy_records.util.model

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import app.myliberty.easy_records.util.exceptions.RtsGenericException

@MappedSuperclass
@EntityListeners(ReferenceNumberEntityListener::class)
abstract class HasReferenceEntity(
    @Column(name = ColumnNames.Common.REFERENCE_NUMBER, unique = true)
    var referenceNumber: String? = null

): HasCreatorEntity() {

    fun requiredReference(): String = referenceNumber
        ?: throw RtsGenericException("${javaClass.simpleName} is missing its reference number")
}
