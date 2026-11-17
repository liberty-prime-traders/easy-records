package app.myliberty.easy_records.platform.business.sysuser.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnPlatformSchema
import app.myliberty.easy_records.platform.business.sysuser.SysUserEntity
import app.myliberty.easy_records.platform.business.sysuser.SysUserRepository
import app.myliberty.easy_records.util.enums.ServiceAccount
import org.springframework.stereotype.Component

@Component
@TransactionalOnPlatformSchema
class ServiceAccountInserter(private val sysUserRepository: SysUserRepository) {

    fun seed() {
        val serviceAccountEntries = ServiceAccount.entries
        val serviceAccountIds = serviceAccountEntries.map { it.uniqueId }.toSet()
        val existingAccounts = sysUserRepository.findAllById(serviceAccountIds).associateBy { it.id }
        val newAccounts =  serviceAccountEntries
            .filter { it.uniqueId !in existingAccounts }
            .map { account ->
                SysUserEntity(userType = UserType.SERVICE_ACCOUNT, localFirstName = account.displayName)
                    .also { it.id = account.uniqueId }
            }
        sysUserRepository.saveAll(newAccounts)
    }
}
