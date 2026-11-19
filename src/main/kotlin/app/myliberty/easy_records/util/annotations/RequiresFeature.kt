package app.myliberty.easy_records.util.annotations

import app.myliberty.easy_records.platform.business.feature.api.Feature

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresFeature(val value: Feature)
