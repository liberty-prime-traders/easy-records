package app.myliberty.easy_records.organizations.business.retail.product_tag

import app.myliberty.easy_records.util.business.mappers.LibertyMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = LibertyMapperConfig::class)
interface ProductTagMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "endOn", ignore = true)
    fun toEntity(insertDto: ProductTagInsertDto): ProductTagEntity

    fun toDomainDto(productTagEntity: ProductTagEntity): ProductTagDto

    fun toEntity(productTagDto: ProductTagDto): ProductTagEntity
}
