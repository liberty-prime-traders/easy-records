package app.myliberty.easy_records.util.annotations.interceptors

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import app.myliberty.easy_records.organizations.business.feature.api.OrganizationFeatureService
import app.myliberty.easy_records.util.annotations.RequiresFeature
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

@Component
class FeatureGateInterceptor(private val organizationFeatureService: OrganizationFeatureService) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (handler !is HandlerMethod) return true

        val feature = handler.getMethodAnnotation(RequiresFeature::class.java)?.value
            ?: handler.beanType.getAnnotation(RequiresFeature::class.java)?.value
            ?: return true

        if (!organizationFeatureService.isActive(feature)) {
            throw RtsGenericException("Feature ${feature.code} is not enabled for this organization")
        }

        return true
    }
}
