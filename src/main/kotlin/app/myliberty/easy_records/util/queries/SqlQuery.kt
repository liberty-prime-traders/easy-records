package app.myliberty.easy_records.util.queries

data class SqlQuery(
  val sql: String,
  val params: Map<String, Any>,
  val metadata: QueryLogMetadata
)
