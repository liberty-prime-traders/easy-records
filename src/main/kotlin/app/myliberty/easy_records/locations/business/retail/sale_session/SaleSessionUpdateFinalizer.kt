package app.myliberty.easy_records.locations.business.sale_session

import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.locations.business.sale_session.api.SaleSession
import app.myliberty.easy_records.locations.business.sale_session.api.SaleSessionResponseDto
import org.springframework.stereotype.Component

@Component
class SaleSessionUpdateFinalizer(
    private val saleSessionStore: SaleSessionStore,
    private val saleSessionAssembler: SaleSessionAssembler,
    private val saleSessionValidator: SaleSessionValidator,
) {

    fun finalize(updated: SaleSession): SaleSessionResponseDto {
        val withTotals = prepare(updated)
        saleSessionStore.save(withTotals)
        return saleSessionAssembler.buildResponse(withTotals)
    }

    fun prepare(updated: SaleSession): SaleSession {
        val touched = updated.markTouched(SessionContextProvider.getUserId())
        val rankedSession = touched.copy(saleLines = touched.saleLines.sortedBy { it.rank })
        val withTotals = SaleSessionTotalsCalculator.recompute(rankedSession)
        saleSessionValidator.validate(withTotals)
        return withTotals
    }
}
