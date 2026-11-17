package app.myliberty.easy_records.util.business.mappers

import app.myliberty.easy_records.util.business.StringUtils
import org.springframework.stereotype.Component
import java.util.Optional

@Component
object StringQualifier {
    fun mapString(value: String?): String? {
        return StringUtils.getValueOrNull(value)
    }

    fun mapString(value: Optional<String>): String? {
        return mapString(OptionalQualifier.fromOptional(value))
    }
}
