package app.myliberty.easy_records.util.enums

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import java.util.EnumSet

@Converter
open class EnumConverter<ENUM>(private val enumClass: Class<ENUM>) : AttributeConverter<ENUM, String?>
        where ENUM: Enum<ENUM>, ENUM: HasCode {

    override fun convertToDatabaseColumn(enumValue: ENUM?): String? {
        return enumValue?.code
    }

    override fun convertToEntityAttribute(code: String?): ENUM? {
        if (code == null) return null
        return EnumSet.allOf(enumClass)
            .firstOrNull { enumValue -> enumValue.code.contentEquals(code) }
            ?: throw LibertyGenericException("No enum constant '${code}' found for enum class '${enumClass.simpleName}'")
    }
}
