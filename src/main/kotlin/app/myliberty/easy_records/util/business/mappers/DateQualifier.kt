package app.myliberty.easy_records.util.business.mappers

import org.springframework.stereotype.Component
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Component
object DateQualifier {
    fun toOffsetDateTime(instant: Instant?): OffsetDateTime? {
        return instant?.atOffset(ZoneOffset.UTC)
    }
}
