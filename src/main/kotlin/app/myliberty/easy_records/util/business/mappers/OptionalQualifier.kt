package app.myliberty.easy_records.util.business.mappers

import org.springframework.stereotype.Component
import java.util.Optional

@Component
object OptionalQualifier {
    fun <T, EXTENDER: T> fromOptional(optional: Optional<EXTENDER>): T? = optional.orElse(null)
}
