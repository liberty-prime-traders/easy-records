package app.myliberty.easy_records.organizations.business.core.account.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.core.account.AccountCache
import app.myliberty.easy_records.organizations.business.core.account.AccountAssertions
import app.myliberty.easy_records.organizations.business.core.account.AccountCodeGenerator
import app.myliberty.easy_records.organizations.business.core.account.AccountDto
import app.myliberty.easy_records.organizations.business.core.account.AccountRepository
import app.myliberty.easy_records.organizations.business.core.account.AccountResponseBuilder
import app.myliberty.easy_records.organizations.business.core.account.ChildAccountCreator
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class AccountService(
    private val accountCache: AccountCache,
    private val accountRepository: AccountRepository,
    private val accountResponseBuilder: AccountResponseBuilder,
    private val childAccountCreator: ChildAccountCreator,
    private val accountStructureLock: AccountStructureLock,
    private val accountUsagesFinder: AccountUsagesFinder
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAll(): List<AccountResponseDto> {
        return accountResponseBuilder.buildResponse(accountCache.getAll())
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAccountNamesByCode(): Map<String, String> {
        return accountCache.getAll().associate { it.code to it.label }
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAccountTree(): List<AccountTreeNode> {
        val accounts = accountCache.getAll()
        val parentAccountCodes = accounts.mapNotNull { account -> account.parentAccountCode }.toSet()
        return accounts.map { account ->
            AccountTreeNode(
                code = account.code,
                name = account.label,
                parentAccountCode = account.parentAccountCode,
                category = account.accountType.category(),
                isContra = account.accountType.isContra(),
                normalBalance = account.accountType.normalBalance,
                isLeaf = account.code !in parentAccountCodes
            )
        }
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getFreshBalancesByCodes(accountCodes: Set<String>): Map<String, BigDecimal> {
        return accountRepository.findBalancesByCodes(accountCodes).associate { accountBalance -> accountBalance.code to accountBalance.currentBalance }
    }

    fun createRoot(dto: AccountRootCreateRequest): AccountResponseDto {
        val accountType = dto.accountType
        if (accountType.canBeRoot().not()) {
            throw RtsGenericException("Only certain account types can be created as root accounts and $accountType is not one of them")
        }
        val accounts = accountCache.getAll()
        accounts.find { it.parentAccountCode == null && StringUtils.isEquivalent(it.name, dto.name) }
            ?.let { throw RtsGenericException("A root account with the same name already exists") }

        val code = AccountCodeGenerator.generateRootCode(accounts)
        val insertDto = AccountInsertDto(
            code = code,
            name = dto.name,
            accountType = accountType
        )
        val saved = accountCache.create(insertDto)
        return accountResponseBuilder.buildResponse(saved)
    }

    fun createChild(dto: AccountChildCreateRequest): AccountResponseDto {
        accountStructureLock.acquire(dto.parentAccountCode)
        val accounts = accountCache.getAllFresh()
        val accountsByCode = accounts.associateBy { it.code }
        val newAccount = childAccountCreator.createChild(dto, accountsByCode)
        return accountResponseBuilder.buildResponse(newAccount)
    }

    fun rename(dto: AccountUpdateDto): AccountResponseDto {
        val existing = lockAndGetFresh(dto.id)
        if (existing.accountIsSystemMaintained) {
            throw RtsGenericException("System accounts cannot be renamed")
        }
        val saved = accountCache.update(dto.applyTo(existing))
        return accountResponseBuilder.buildResponse(saved)
    }

    fun toggleActive(id: UUID, setActive: Boolean): AccountResponseDto {
        val existing = lockAndGetFresh(id)
        if (existing.accountIsSystemMaintained) {
            throw RtsGenericException("System accounts cannot be activated or deactivated")
        }
        if (existing.accountIsActive == setActive) {
            return accountResponseBuilder.buildResponse(existing)
        }
        if (!setActive) {
            accountUsagesFinder.failOnUsagesForCode(existing.code, "be deactivated")
        }
        val saved = accountCache.update(existing.copy(accountIsActive = setActive))
        return accountResponseBuilder.buildResponse(saved)
    }

    // The code is immutable per id, so the cached lookup is safe; the row itself is re-read once the lock is held.
    private fun lockAndGetFresh(id: UUID): AccountDto {
        val accountCode = accountCache.getAll().firstOrNull { it.id == id }?.code
            ?: throw RtsGenericException("Account not found")
        accountStructureLock.acquire(accountCode)
        return accountCache.getAllFresh().first { it.id == id }
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun assertPostable(accountPostings: List<AccountPosting>) {
        AccountAssertions.assertPostable(accountCache.getAllFresh(), accountPostings)
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun assertPostableReversal(accountPostings: List<AccountPosting>) {
        AccountAssertions.assertPostable(accountCache.getAllFresh(), accountPostings, inactiveAccountsMayIncrease = true)
    }

    fun patchBalances(accountPostings: List<AccountPosting>) {
        val accountsByCode = accountCache.getAll().associateBy { it.code }
        val balanceUpdatedAt = Instant.now()
        accountPostings.forEach { accountPosting ->
            val account = accountsByCode.getValue(accountPosting.accountCode)
            val delta = account.accountType.amountInNormalDirection(accountPosting.entryType, accountPosting.amount)
            accountRepository.incrementBalance(account.code, delta, balanceUpdatedAt)
        }
    }
}
