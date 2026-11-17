package app.myliberty.easy_records.organizations.business.address.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.organizations.business.address.AddressCache
import app.myliberty.easy_records.organizations.business.address.AddressMapper
import app.myliberty.easy_records.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.Objects

@Service
@TransactionalOnLocationSchema
class AddressService(
    private val addressMapper: AddressMapper,
    private val addressCache: AddressCache
) {

    @TransactionalOnLocationSchema(readOnly = true)
    fun getAllAddresses(): Collection<AddressResponseDto> {
        return addressCache.getAllAddresses().map { addressMapper.toDto(it) }
    }

    fun createAddress(addressInsertDto: AddressInsertDto): AddressResponseDto {
        val dto = addressCache.create(addressInsertDto)
        return addressMapper.toDto(dto)
    }

    fun updateAddress(addressUpdateDto: AddressUpdateDto): AddressResponseDto {
        val existing = addressCache.getAllAddresses().find { Objects.equals(addressUpdateDto.id, it.id) }
            ?: throw UpdatingNonExistingRecordException()
        val updated = addressUpdateDto.applyTo(existing)
        val saved = addressCache.save(updated)
        return addressMapper.toDto(saved)
    }
}
