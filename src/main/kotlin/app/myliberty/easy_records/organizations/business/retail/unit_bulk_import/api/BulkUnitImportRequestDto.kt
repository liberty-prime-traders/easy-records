package app.myliberty.easy_records.organizations.business.unit_bulk_import.api

data class BulkUnitImportRequestDto(
    val unitGroups: List<UnitGroupBulkInsertDto> = emptyList()
)
