package app.myliberty.easy_records.organizations.business.product_tag.api

import org.mapstruct.Qualifier

@Qualifier
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ActiveProductTags
