package app.myliberty.easy_records.util.queries

interface FilterStrategy {
  fun apply(context: QueryBuilderContext)
}
