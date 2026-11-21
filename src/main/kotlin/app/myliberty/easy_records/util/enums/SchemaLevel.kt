package app.myliberty.easy_records.util.enums

enum class SchemaLevel(override val code: String) : HasCode {
    PLATFORM("PLT"),
    ORGANIZATION("ORG"),
    LOCATION("LOC")
}
