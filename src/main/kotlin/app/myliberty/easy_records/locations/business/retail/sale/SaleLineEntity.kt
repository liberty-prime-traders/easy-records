package app.myliberty.easy_records.locations.business.retail.sale

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.locations.business.retail.location_product.api.ProductLineWithPrice
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
@Table(name = TableNames.SALE_LINE)
@HasReference(tableName = TableName.SALE_LINE)
class SaleLineEntity(

    @Column(name = "sale_id", nullable = false)
    var saleId: UUID,

    @Column(name = "location_product_id", nullable = false)
    override var locationProductId: UUID,

    @Column(name = "user_provided_quantity", nullable = false, precision = 19, scale = 4)
    override var userProvidedQuantity: BigDecimal,

    @Column(name = "unit_id", nullable = false)
    var unitId: UUID,

    // Gross, before any price override
    @Column(name = "line_total", nullable = false, precision = 19, scale = 4)
    override var lineTotal: BigDecimal,

    @Column(name = "default_sale_price", nullable = false, precision = 28, scale = 10)
    var defaultSalePrice: BigDecimal,

    @Column(name = "storage_units_per_user_provided_unit", nullable = false)
    var storageUnitsPerUserProvidedUnit: Long,

    @Column(name = "cogs_total", precision = 19, scale = 4)
    var cogsTotal: BigDecimal? = null

) : HasReferenceEntity(), ProductLineWithPrice {

    override val lineKey: UUID
        get() = requireNotNull(id) { "Sale line has no id yet" }

    fun storageQuantity(): ExactQuantity = ExactQuantity.ofStorageFromUserProvided(userProvidedQuantity, storageUnitsPerUserProvidedUnit)
}
