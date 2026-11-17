package app.myliberty.easy_records.locations.business.retail.sale_session

import app.myliberty.easy_records.locations.business.retail.sale.api.SaleHeaderDto
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleLineDto
import app.myliberty.easy_records.locations.business.retail.sale_adjustment.api.SaleAdjustmentDto
import app.myliberty.easy_records.locations.business.retail.sale_payment.api.SalePaymentSnapshot
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionAdjustment
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionHeader
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionLine
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionPayment
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SessionIdentity
import app.myliberty.easy_records.util.business.mappers.RtsMapperConfig
import org.mapstruct.Context
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import java.util.UUID

@Mapper(config = RtsMapperConfig::class, uses = [SaleSessionQualifierUtil::class])
interface DomainToSessionMapper {

    fun toSaleSessionHeader(saleHeaderDto: SaleHeaderDto): SaleSessionHeader

    @Mapping(target = "storageQuantityOnHand", constant = "0")
    @Mapping(target = "storageQuantityReserved", constant = "0")
    @Mapping(target = "storageQuantityAvailable", constant = "0")
    @Mapping(target = "rank", constant = "0")
    @Mapping(source = "id", target = "identity", qualifiedBy = [PersistedSessionIdentity::class])
    fun toSaleSessionLine(saleLineDto: SaleLineDto): SaleSessionLine

    @Mapping(source = "id", target = "identity", qualifiedBy = [PersistedSessionIdentity::class])
    @Mapping(
        source = "saleLineId",
        target = "relatedSaleLineIdentity",
        qualifiedBy = [RelatedSaleLineSessionIdentity::class]
    )
    fun toSaleSessionAdjustment(
        saleAdjustmentDto: SaleAdjustmentDto,
        @Context relatedSaleLineIdentityBySaleLineId: Map<UUID, SessionIdentity>,
    ): SaleSessionAdjustment

    @Mapping(source = "id", target = "identity", qualifiedBy = [PersistedSessionIdentity::class])
    fun toSaleSessionPayment(salePaymentSnapshot: SalePaymentSnapshot): SaleSessionPayment
}
