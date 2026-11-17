package app.myliberty.easy_records.locations.business.retail.location_product

import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductResponseDto
import app.myliberty.easy_records.util.business.mappers.LibertyMapperConfig
import org.mapstruct.Context
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = LibertyMapperConfig::class)
interface LocationProductMapper {

    fun toDomainDto(locationProductEntity: LocationProductEntity): LocationProductDto

    fun toEntity(dto: LocationProductDto): LocationProductEntity

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "defaultSalePrice", ignore = true)
    @Mapping(target = "minStockLevel", ignore = true)
    @Mapping(target = "lastPurchasePrice", ignore = true)
    fun toEntity(insertDto: LocationProductInsertDto): LocationProductEntity

    @Mapping(target = "baseUnit", expression = "java(ctx.getUnitName())")
    @Mapping(target = "stockBalance", expression = "java(ctx.getBalance())")
    @Mapping(target = "openingStockQuantity", expression = "java(ctx.getOpeningStockQuantity())")
    @Mapping(target = "openingStockUnitCost", expression = "java(ctx.getOpeningStockUnitCost())")
    @Mapping(target = "stockBalanceBreakdown", expression = "java(ctx.getBalanceBreakdown())")
    @Mapping(target = "openingStockBreakdown", expression = "java(ctx.getOpeningStockBreakdown())")
    fun toResponseDto(dto: LocationProductDto, @Context ctx: LocationProductContext): LocationProductResponseDto
}
