package app.myliberty.easy_records.locations.business.sale_session

import app.myliberty.easy_records.locations.business.sale.api.SaleAdjustmentSaveRequest
import app.myliberty.easy_records.locations.business.sale.api.SaleLineSaveRequest
import app.myliberty.easy_records.locations.business.sale.api.SalePaymentSaveRequest
import app.myliberty.easy_records.locations.business.sale.api.SaleSaveRequest
import app.myliberty.easy_records.locations.business.sale_session.api.SaleSession
import app.myliberty.easy_records.locations.business.sale_session.api.SaleSessionAdjustment
import app.myliberty.easy_records.locations.business.sale_session.api.SaleSessionLine
import app.myliberty.easy_records.locations.business.sale_session.api.SaleSessionPayment
import app.myliberty.easy_records.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class, uses = [SaleSessionQualifierUtil::class])
interface SessionToSaveRequestMapper {

    @Mapping(source = "saleVersion", target = "expectedVersion")
    @Mapping(source = "header.contactId", target = "contactId")
    @Mapping(source = "header.soldById", target = "soldById")
    @Mapping(source = "header.dateSold", target = "dateSold")
    @Mapping(source = "header.notes", target = "notes")
    @Mapping(target = "preAllocatedReferenceNumber", ignore = true)
    fun toSaleSaveRequest(saleSession: SaleSession): SaleSaveRequest

    @Mapping(source = "identity", target = "clientKey", qualifiedBy = [SessionIdentityKey::class])
    @Mapping(source = "identity", target = "existingId", qualifiedBy = [SessionIdentityExistingId::class])
    fun toSaleLineSaveRequest(saleSessionLine: SaleSessionLine): SaleLineSaveRequest

    @Mapping(source = "identity", target = "clientKey", qualifiedBy = [SessionIdentityKey::class])
    @Mapping(source = "identity", target = "existingId", qualifiedBy = [SessionIdentityExistingId::class])
    @Mapping(source = "relatedSaleLineIdentity", target = "relatedSaleLineClientKey", qualifiedBy = [SessionIdentityKey::class])
    fun toSaleAdjustmentSaveRequest(saleSessionAdjustment: SaleSessionAdjustment): SaleAdjustmentSaveRequest

    @Mapping(source = "identity", target = "clientKey", qualifiedBy = [SessionIdentityKey::class])
    @Mapping(source = "identity", target = "existingId", qualifiedBy = [SessionIdentityExistingId::class])
    fun toSalePaymentSaveRequest(saleSessionPayment: SaleSessionPayment): SalePaymentSaveRequest

}
