package app.myliberty.easy_records.configuration.cache

import app.myliberty.easy_records.util.enums.SchemaLevel

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheSchemaLevel(
    val schemaLevel: SchemaLevel = SchemaLevel.PLATFORM
)
