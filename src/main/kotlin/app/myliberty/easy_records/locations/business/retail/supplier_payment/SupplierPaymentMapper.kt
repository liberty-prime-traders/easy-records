package app.myliberty.easy_records.locations.business.supplier_payment

import app.myliberty.easy_records.locations.business.supplier_payment.api.SupplierPaymentCreateDto
import app.myliberty.easy_records.locations.business.supplier_payment.api.SupplierPaymentDto
import app.myliberty.easy_records.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface SupplierPaymentMapper {

    fun toDomainDto(supplierPaymentEntity: SupplierPaymentEntity): SupplierPaymentDto

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    fun toEntity(dto: SupplierPaymentCreateDto, paymentMethodAccountCode: String): SupplierPaymentEntity
}
