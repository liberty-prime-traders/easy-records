package app.myliberty.easy_records.organizations.business.contact

import app.myliberty.easy_records.configuration.cache.CacheNames
import app.myliberty.easy_records.configuration.cache.CacheSchemaLevel
import app.myliberty.easy_records.organizations.business.contact.api.ContactDto
import app.myliberty.easy_records.organizations.business.contact.api.ContactInsertDto
import app.myliberty.easy_records.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.CONTACT])
class ContactCache(
    private val contactRepository: ContactRepository,
    private val contactMapper: ContactMapper
) {

    @Cacheable
    fun getAllContacts(): Collection<ContactDto> {
        return contactRepository.findAll().map { contactMapper.toDomainDto(it) }
    }

    @Cacheable
    fun getContactsByReferenceNumber(): Map<String, ContactDto> {
        return contactRepository.findAll().map { contactMapper.toDomainDto(it) }.associateBy { it.referenceNumber }
    }

    @CacheEvict(allEntries = true)
    fun create(insertDto: ContactInsertDto): ContactDto {
        val saved = contactRepository.save(contactMapper.toEntity(insertDto))
        return contactMapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun createAll(insertDtos: List<ContactInsertDto>): List<ContactDto> {
        val saved = contactRepository.saveAll(insertDtos.map { contactMapper.toEntity(it) })
        return saved.map { contactMapper.toDomainDto(it) }
    }

    @CacheEvict(allEntries = true)
    fun save(contactDto: ContactDto): ContactDto {
        val saved = contactRepository.save(contactMapper.toEntity(contactDto))
        return contactMapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun save(contactEntity: ContactEntity): ContactDto {
        val saved = contactRepository.save(contactEntity)
        return contactMapper.toDomainDto(saved)
    }
}
