package app.myliberty.easy_records.util.enums

enum class ReferenceType(override val code: String) : HasCode {
    SALE("SAL"),
    TRANSFER("TRF"),
    SUPPLIER_RETURN("SR"),
    MANUAL("MAN")
}
