package app.myliberty.easy_records.organizations.business.account

import app.myliberty.easy_records.configuration.cache.CacheNames
import app.myliberty.easy_records.configuration.cache.CacheSchemaLevel
import app.myliberty.easy_records.organizations.business.account.api.AccountInsertDto
import app.myliberty.easy_records.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.ACCOUNT])
class AccountCache(
    private val accountRepository: AccountRepository,
    private val accountMapper: AccountMapper
) {

    @Cacheable
    fun getAll(): List<AccountDto> {
        return getAllFresh()
    }

    // Eviction runs before the writing transaction commits, so a reader can re-cache pre-commit state.
    // Callers holding an account lock must read committed state, not the cache.
    fun getAllFresh(): List<AccountDto> {
        return accountRepository.findAll().map { accountMapper.toDomainDto(it) }
    }

    @CacheEvict(allEntries = true)
    fun update(dto: AccountDto): AccountDto {
        val toSave = accountRepository.getReferenceById(dto.id)
        val updated = dto.applyTo(toSave)
        return accountMapper.toDomainDto(accountRepository.save(updated))
    }

    @CacheEvict(allEntries = true)
    fun saveAll(dtos: List<AccountInsertDto>): List<AccountDto> {
        val saved = accountRepository.saveAllAndFlush(dtos.map { accountMapper.toEntity(it) })
        return saved.map { accountMapper.toDomainDto(it) }
    }

    @CacheEvict(allEntries = true)
    fun create(dto: AccountInsertDto): AccountDto {
        return accountMapper.toDomainDto(accountRepository.save(accountMapper.toEntity(dto)))
    }
}
