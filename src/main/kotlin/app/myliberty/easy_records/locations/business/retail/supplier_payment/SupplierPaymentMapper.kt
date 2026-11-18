package app.myliberty.easy_records.locations.business.retail.supplier_payment

import app.myliberty.easy_records.locations.business.retail.supplier_payment.api.SupplierPaymentCreateDto
import app.myliberty.easy_records.locations.business.retail.supplier_payment.api.SupplierPaymentDto
import app.myliberty.easy_records.util.business.mappers.LibertyMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = LibertyMapperConfig::class)
interface SupplierPaymentMapper {

    fun toDomainDto(supplierPaymentEntity: SupplierPaymentEntity): SupplierPaymentDto

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    fun toEntity(dto: SupplierPaymentCreateDto, paymentMethodAccountCode: String): SupplierPaymentEntity
}
