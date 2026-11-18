package app.myliberty.easy_records.cucumber.support.context

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.stereotype.Component

@Component
class KafkaContext {
  var lastCatalogEvent: JsonNode? = null

  fun reset() {
    lastCatalogEvent = null
  }
}
