package app.myliberty.easy_records.locations.business.retail.stock_revision

import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.messaging.kafka.transaction.EventReissueHandler
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import java.time.LocalDate
import java.util.UUID

abstract class StockRevisionEventHandler(
    private val stockRevisionRepository: StockRevisionRepository
) : EventReissueHandler {

    override fun reissue(sourceDocumentId: UUID) {
        republish(
            stockRevisionRepository.findById(sourceDocumentId)
                .orElseThrow { RtsGenericException("Stock revision $sourceDocumentId not found") }
        )
    }

    protected abstract fun republish(stockRevision: StockRevisionEntity)

    protected fun sourceContext() = EventSourceContext.LocationLevel(
        orgSchema = SessionContextProvider.getOrganizationSchema(),
        locationSchema = SessionContextProvider.getLocationSchema()
    )

    protected fun postingDate(): LocalDate = DateTimes.Local.Now.organization()
}
