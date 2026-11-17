package app.myliberty.easy_records.locations.business.retail.sale

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Version
import app.myliberty.easy_records.util.enums.PaymentStatusConverter
import app.myliberty.easy_records.util.enums.PaymentStatus
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleStatus
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.ColumnNames
import app.myliberty.easy_records.util.model.HasReferenceEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import org.hibernate.envers.Audited
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.SALE)
@HasReference(tableName = TableName.SALE)
class SaleEntity(

    @Column(name = ColumnNames.Sale.CONTACT_ID, nullable = false)
    var contactId: UUID,

    @Column(name = "sold_by_id")
    var soldById: UUID? = null,

    @Column(name = "date_sold")
    var dateSold: OffsetDateTime? = null,

    @Column(name = "notes")
    var notes: String? = null,

    @Convert(converter = SaleStatusConverter::class)
    @Column(name = "status", nullable = false, length = 5)
    var status: SaleStatus,

    @Convert(converter = PaymentStatusConverter::class)
    @Column(name = "payment_status", nullable = false, length = 5)
    var paymentStatus: PaymentStatus = PaymentStatus.UNPAID,

    @Column(name = "subtotal", nullable = false, precision = 19, scale = 4)
    var subtotal: BigDecimal = BigDecimal.ZERO,

    @Column(name = "line_level_discount_total", nullable = false, precision = 19, scale = 4)
    var lineLevelDiscountTotal: BigDecimal = BigDecimal.ZERO,

    @Column(name = "order_level_discount_total", nullable = false, precision = 19, scale = 4)
    var orderLevelDiscountTotal: BigDecimal = BigDecimal.ZERO,

    @Column(name = "line_level_surcharge_total", nullable = false, precision = 19, scale = 4)
    var lineLevelSurchargeTotal: BigDecimal = BigDecimal.ZERO,

    @Column(name = "order_level_surcharge_total", nullable = false, precision = 19, scale = 4)
    var orderLevelSurchargeTotal: BigDecimal = BigDecimal.ZERO,

    // Null until tax finalization has run; SaleValidator.guardCanVoid relies on that to detect a pending finalization.
    @Column(name = "tax_total", precision = 19, scale = 4)
    var taxTotal: BigDecimal? = null,

    @Column(name = "tax_billed", nullable = false, precision = 19, scale = 4)
    var taxBilled: BigDecimal = BigDecimal.ZERO,

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0

) : HasReferenceEntity() {

    fun discountTotal(): BigDecimal =
        lineLevelDiscountTotal + orderLevelDiscountTotal

    fun surchargeTotal(): BigDecimal =
        lineLevelSurchargeTotal + orderLevelSurchargeTotal

    fun displaySubtotal(): BigDecimal =
        subtotal + surchargeTotal()

    fun taxableAmount(): BigDecimal =
        displaySubtotal() - discountTotal()

    fun receivableTotal(): BigDecimal =
        taxableAmount() + taxBilled
}
