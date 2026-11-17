package app.myliberty.easy_records.locations.business.retail.tax_entry

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.locations.business.retail.tax_entry.api.TaxDirection
import app.myliberty.easy_records.locations.business.retail.tax_entry.api.TaxSourceType
import app.myliberty.easy_records.platform.business.tax_type.api.TaxCalculationMethod
import app.myliberty.easy_records.platform.business.tax_type.api.TaxCalculationMethodConverter
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.ImmutableEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import org.hibernate.envers.Audited
import java.math.BigDecimal
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.TAX_ENTRY)
@HasReference(tableName = TableName.TAX_ENTRY)
class TaxEntryEntity(

    @Column(name = "source_reference_number", nullable = false, updatable = false)
    var sourceReferenceNumber: String,

    @Convert(converter = TaxSourceTypeConverter::class)
    @Column(name = "source_type", nullable = false, length = 5, updatable = false)
    var sourceType: TaxSourceType,

    @Convert(converter = TaxDirectionConverter::class)
    @Column(name = "direction", nullable = false, length = 5, updatable = false)
    var direction: TaxDirection,

    @Column(name = "tax_type_id", nullable = false, updatable = false)
    var taxTypeId: UUID,

    @Column(name = "tax_type_name", length = 100, nullable = false, updatable = false)
    var taxTypeName: String,

    @Column(name = "jurisdiction_name", length = 100, nullable = false, updatable = false)
    var jurisdictionName: String,

    @Column(name = "fiscal_period_id", nullable = false, updatable = false)
    var fiscalPeriodId: UUID,

    @Convert(converter = TaxCalculationMethodConverter::class)
    @Column(name = "calculation_method", nullable = false, length = 5, updatable = false)
    var calculationMethod: TaxCalculationMethod,

    @Column(name = "rate", nullable = false, precision = 19, scale = 4, updatable = false)
    var rate: BigDecimal,

    @Column(name = "tax_is_billed_to_customer_separately", nullable = false, updatable = false)
    var taxIsBilledToCustomerSeparately: Boolean,

    @Column(name = "tax_is_included_in_taxable_amount", nullable = false, updatable = false)
    var taxIsIncludedInTaxableAmount: Boolean,

    // The sale's pre-tax base; resolvedTaxableBase is that base with included taxes backed out, and what tax_amount is computed from.
    @Column(name = "taxable_amount", nullable = false, precision = 19, scale = 4, updatable = false)
    var taxableAmount: BigDecimal,

    @Column(name = "resolved_taxable_base", nullable = false, precision = 19, scale = 4, updatable = false)
    var resolvedTaxableBase: BigDecimal,

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4, updatable = false)
    var taxAmount: BigDecimal

) : ImmutableEntity()
