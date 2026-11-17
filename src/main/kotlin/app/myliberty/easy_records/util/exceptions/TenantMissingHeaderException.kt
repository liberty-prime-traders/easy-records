package app.myliberty.easy_records.util.exceptions

class TenantMissingHeaderException(headerName: String): LibertyGenericException(
    "Header $headerName is required to access this resource"
)
