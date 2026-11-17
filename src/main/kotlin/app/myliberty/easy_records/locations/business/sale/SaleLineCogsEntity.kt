package app.myliberty.easy_records.locations.business.sale

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.locations.business.sale.api.SaleLineCogsStatus
import app.myliberty.easy_records.locations.business.sale.api.SaleLineCogsStatusConverter
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.HasReferenceEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import org.hibernate.envers.Audited
import java.math.BigDecimal
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.SALE_LINE_COGS)
@HasReference(tableName = TableName.SALE_LINE_COGS)
class SaleLineCogsEntity(

    @Column(name = "sale_line_id", nullable = false, updatable = false)
    var saleLineId: UUID,

    @Column(name = "cogs_total", nullable = false, precision = 19, scale = 4)
    var cogsTotal: BigDecimal,

    @Convert(converter = SaleLineCogsStatusConverter::class)
    @Column(name = "status", nullable = false, length = 5)
    var status: SaleLineCogsStatus = SaleLineCogsStatus.ACTIVE

) : HasReferenceEntity()
