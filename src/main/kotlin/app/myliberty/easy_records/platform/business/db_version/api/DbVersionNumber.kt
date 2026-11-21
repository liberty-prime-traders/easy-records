package app.myliberty.easy_records.platform.business.db_version.api

import org.mapstruct.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class DbVersionNumber
