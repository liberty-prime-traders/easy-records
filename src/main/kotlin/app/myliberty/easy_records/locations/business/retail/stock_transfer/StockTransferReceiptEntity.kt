package app.myliberty.easy_records.locations.business.stock_transfer

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.HasReferenceEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import org.hibernate.envers.Audited
import java.time.OffsetDateTime
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.STOCK_TRANSFER_RECEIPT)
@HasReference(tableName = TableName.STOCK_TRANSFER_RECEIPT)
class StockTransferReceiptEntity(

    @Column(name = "stock_transfer_order_ref", nullable = false, updatable = false, length = 30)
    var stockTransferOrderRef: String,

    @Column(name = "received_by_id", nullable = false)
    var receivedById: UUID,

    @Column(name = "received_at", nullable = false)
    var receivedAt: OffsetDateTime,

    @Convert(converter = StockTransferReceiptStatusConverter::class)
    @Column(name = "status", nullable = false, length = 5)
    var status: StockTransferReceiptStatus = StockTransferReceiptStatus.PENDING,

    @Column(name = "notes")
    var notes: String? = null

) : HasReferenceEntity()
