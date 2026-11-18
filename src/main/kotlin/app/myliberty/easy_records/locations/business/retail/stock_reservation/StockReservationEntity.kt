package app.myliberty.easy_records.locations.business.retail.stock_reservation

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.util.model.HasCreatorEntity
import app.myliberty.easy_records.util.model.TableNames
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = TableNames.SALE_LINE_STOCK_RESERVATION)
class StockReservationEntity(

    @Column(name = "sale_id", nullable = false)
    var saleId: UUID,

    @Column(name = "sale_line_id", nullable = false)
    var saleLineId: UUID,

    @Column(name = "location_product_id", nullable = false)
    var locationProductId: UUID,

    @Column(name = "storage_quantity_reserved", nullable = false, precision = 28, scale = 4)
    var storageQuantityReserved: BigDecimal,

) : HasCreatorEntity()
