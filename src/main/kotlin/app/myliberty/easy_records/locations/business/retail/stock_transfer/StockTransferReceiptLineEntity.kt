package app.myliberty.easy_records.locations.business.retail.stock_transfer

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.HasReferenceEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import org.hibernate.envers.Audited
import java.math.BigDecimal
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.STOCK_TRANSFER_RECEIPT_LINE)
@HasReference(tableName = TableName.STOCK_TRANSFER_RECEIPT_LINE)
class StockTransferReceiptLineEntity(

    @Column(name = "stock_transfer_receipt_id", nullable = false, updatable = false)
    var stockTransferReceiptId: UUID,

    @Column(name = "stock_transfer_dispatch_line_ref", nullable = false, updatable = false, length = 30)
    var stockTransferDispatchLineRef: String,

    @Column(name = "location_product_id", nullable = false, updatable = false)
    var locationProductId: UUID,

    @Column(name = "storage_quantity_received", nullable = false, precision = 28, scale = 4)
    var storageQuantityReceived: BigDecimal

) : HasReferenceEntity()
