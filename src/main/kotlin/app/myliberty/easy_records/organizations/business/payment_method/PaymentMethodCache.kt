package app.myliberty.easy_records.organizations.business.payment_method

import app.myliberty.easy_records.configuration.cache.CacheNames
import app.myliberty.easy_records.configuration.cache.CacheSchemaLevel
import app.myliberty.easy_records.organizations.business.payment_method.api.PaymentMethodInsertDto
import app.myliberty.easy_records.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.PAYMENT_METHOD])
class PaymentMethodCache(
    private val paymentMethodRepository: PaymentMethodRepository,
    private val paymentMethodMapper: PaymentMethodMapper
) {

    @Cacheable
    fun getAllPaymentMethods(): Collection<PaymentMethodDto> {
        return paymentMethodRepository.findAll().map { paymentMethodMapper.toDomainDto(it) }
    }

    @CacheEvict(allEntries = true)
    fun create(insertDto: PaymentMethodInsertDto): PaymentMethodDto {
        val saved = paymentMethodRepository.save(paymentMethodMapper.toEntity(insertDto))
        return paymentMethodMapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun save(paymentMethodDto: PaymentMethodDto): PaymentMethodDto {
        val saved = paymentMethodRepository.save(paymentMethodMapper.toEntity(paymentMethodDto))
        return paymentMethodMapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun deletePaymentMethod(id: UUID) {
        paymentMethodRepository.deleteById(id)
    }
}
