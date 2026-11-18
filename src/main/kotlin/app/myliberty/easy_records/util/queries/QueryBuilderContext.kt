package app.myliberty.easy_records.util.queries

data class QueryBuilderContext(
  val whereClauses: MutableList<String> = mutableListOf(),
  val havingClauses: MutableList<String> = mutableListOf(),
  val params: MutableMap<String, Any> = mutableMapOf()
)
