package app.myliberty.easy_records.organizations.business.retail.product_group

import app.myliberty.easy_records.organizations.business.retail.product_group.api.ProductGroupDto
import app.myliberty.easy_records.organizations.business.retail.product_group.api.ProductGroupInsertDto
import app.myliberty.easy_records.organizations.business.retail.product_group.api.ProductGroupResponseDto
import app.myliberty.easy_records.platform.business.sysuser.api.CreatorFullName
import app.myliberty.easy_records.util.business.mappers.LibertyMapperConfig
import org.mapstruct.Context
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = LibertyMapperConfig::class)
interface ProductGroupMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toEntity(productGroupInsertDto: ProductGroupInsertDto): ProductGroupEntity

    fun toDomainDto(productGroupEntity: ProductGroupEntity): ProductGroupDto

    fun toEntity(productGroupDto: ProductGroupDto): ProductGroupEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [CreatorFullName::class])
    @Mapping(target = "categoryName", expression = "java(categoryName)")
    @Mapping(source = "categoryId", target = "categoryId")
    fun toResponseDto(productGroupDto: ProductGroupDto, @Context categoryName: String?): ProductGroupResponseDto
}
