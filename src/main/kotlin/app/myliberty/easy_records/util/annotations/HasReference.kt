package app.myliberty.easy_records.util.annotations

import app.myliberty.easy_records.util.model.TableName

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class HasReference(
    val tableName: TableName
)
