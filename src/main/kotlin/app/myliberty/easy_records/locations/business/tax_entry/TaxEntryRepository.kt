package app.myliberty.easy_records.locations.business.tax_entry

import app.myliberty.easy_records.locations.business.tax_entry.api.TaxSourceType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TaxEntryRepository : JpaRepository<TaxEntryEntity, UUID> {

    fun findBySourceReferenceNumberInAndSourceType(
        sourceReferenceNumbers: Collection<String>,
        sourceType: TaxSourceType
    ): List<TaxEntryEntity>
}
