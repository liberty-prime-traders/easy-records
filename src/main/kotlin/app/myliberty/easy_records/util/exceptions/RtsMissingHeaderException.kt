package app.myliberty.easy_records.util.exceptions

class RtsMissingHeaderException(headerName: String): RtsGenericException(
    "Header $headerName is required to access this resource"
)
