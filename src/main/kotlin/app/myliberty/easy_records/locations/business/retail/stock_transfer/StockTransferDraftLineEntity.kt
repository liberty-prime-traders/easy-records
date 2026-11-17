package app.myliberty.easy_records.locations.business.retail.stock_transfer

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.HasReferenceEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import app.myliberty.easy_records.util.quantity.ExactQuantity
import org.hibernate.envers.Audited
import java.math.BigDecimal
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.STOCK_TRANSFER_DRAFT_LINE)
@HasReference(tableName = TableName.STOCK_TRANSFER_DRAFT_LINE)
class StockTransferDraftLineEntity(

    @Column(name = "stock_transfer_dispatch_id", nullable = false, updatable = false)
    var stockTransferDispatchId: UUID,

    @Column(name = "location_product_id", nullable = false)
    var locationProductId: UUID,

    @Column(name = "user_provided_quantity", nullable = false, precision = 19, scale = 4)
    var userProvidedQuantity: BigDecimal,

    @Column(name = "unit_id", nullable = false)
    var unitId: UUID,

    @Column(name = "storage_units_per_user_provided_unit", nullable = false)
    var storageUnitsPerUserProvidedUnit: Long

) : HasReferenceEntity() {

    fun storageQuantity(): ExactQuantity = ExactQuantity.ofStorageFromUserProvided(userProvidedQuantity, storageUnitsPerUserProvidedUnit)
}
