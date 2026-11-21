package app.myliberty.easy_records.configuration.serializer

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import app.myliberty.easy_records.util.business.Decimals
import java.math.BigDecimal

class StripZeroesAndRound : JsonSerializer<BigDecimal>() {
    override fun serialize(value: BigDecimal, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeNumber(Decimals.stripZeroesAndRoundNeverToZero(value))
    }
}
