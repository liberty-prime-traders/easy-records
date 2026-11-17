package app.myliberty.easy_records.organizations.business.retail.unitconversion.api

import app.myliberty.easy_records.organizations.business.retail.unitconversion.UnitConversionGraphBuilder
import org.springframework.stereotype.Service

@Service
class UnitConversionGraphFacade(private val graphBuilder: UnitConversionGraphBuilder) {

    fun getOrLoad(): UnitConversionGraph = graphBuilder.getOrLoad()

    fun invalidate() = graphBuilder.invalidate()
}
