package app.myliberty.easy_records.organizations.business.core.payment_method

import app.myliberty.easy_records.organizations.business.core.payment_method.api.PaymentMethodInsertDto
import app.myliberty.easy_records.organizations.business.core.payment_method.api.PaymentMethodResponseDto
import app.myliberty.easy_records.platform.business.sysuser.api.CreatorFullName
import app.myliberty.easy_records.util.business.mappers.RtsMapperConfig
import org.mapstruct.Context
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface PaymentMethodMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toEntity(paymentMethodInsertDto: PaymentMethodInsertDto): PaymentMethodEntity

    fun toDomainDto(paymentMethodEntity: PaymentMethodEntity): PaymentMethodDto

    fun toEntity(paymentMethodDto: PaymentMethodDto): PaymentMethodEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [CreatorFullName::class])
    @Mapping(target = "linkedAccount", expression = "java(linkedAccount)")
    fun toResponseDto(paymentMethodDto: PaymentMethodDto, @Context linkedAccount: String?): PaymentMethodResponseDto
}
