package app.myliberty.easy_records.locations.business.sale_payment

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID
import app.myliberty.easy_records.util.model.ViewNames
import org.hibernate.annotations.Immutable

@Entity
@Immutable
@Table(name = ViewNames.ACTIVE_SALE_PAYMENT)
class ActiveSalePaymentEntity(

    @Id
    @Column(name = "id")
    var id: UUID,

    @Column(name = "reference_number")
    var referenceNumber: String,

    @Column(name = "sale_id")
    var saleId: UUID,

    @Column(name = "amount")
    var amount: BigDecimal,

    @Column(name = "created_on")
    var createdOn: OffsetDateTime
)
