package app.myliberty.easy_records.platform.business.reserved_subdomain.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnPlatformSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.platform.business.reserved_subdomain.ReservedSubdomainEntity
import app.myliberty.easy_records.platform.business.reserved_subdomain.ReservedSubdomainMaper
import app.myliberty.easy_records.platform.business.reserved_subdomain.SubdomainRepository
import app.myliberty.easy_records.util.business.SchemaNameGenerator
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@TransactionalOnPlatformSchema
class ReservedSubdomainService(
    private val subdomainRepository: SubdomainRepository,
    private val reservedSubdomainMapper: ReservedSubdomainMaper
) {

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getReservedSubdomains(): Collection<ReservedSubdomainDto> {
        return subdomainRepository.findAll().map { reservedSubdomainMapper.toDto(it) }
    }

    fun sanitizeThenReserveSubdomain(suggestedSubdomain: String?): ReservedSubdomainDto {
        if (!StringUtils.hasValue(suggestedSubdomain)) {
            throw LibertyGenericException("An empty subdomain cannot be verified")
        }
        val subdomain = SchemaNameGenerator.generateSubDomain(suggestedSubdomain!!)
        val userId = SessionContextProvider.getUserId()
        subdomainRepository.findByStatusNotAndSubdomain(ReservedDomainStatus.ABANDONED, subdomain)
            .find { it.status == ReservedDomainStatus.USED || it.createdById != userId }
            ?.let { throw LibertyGenericException("Subdomain '$subdomain' is taken") }
        return reservedSubdomainMapper.toDto(reserveSubdomain(subdomain))
    }

    private fun reserveSubdomain(sanitizedSubdomain: String): ReservedSubdomainEntity {
        val sysUserId = SessionContextProvider.getUserId()
        subdomainRepository.abandonSubdomainsForUser(sysUserId)
        val reservedSubdomain = ReservedSubdomainEntity(sanitizedSubdomain, ReservedDomainStatus.UNUSED)
        return subdomainRepository.save(reservedSubdomain)
    }

    fun releaseSubdomain(id: UUID?) {
        id?.let{
            subdomainRepository.findByIdOrNull(id)?.let {
                it.status = ReservedDomainStatus.ABANDONED
                subdomainRepository.save(it)
            }
        }
    }

    fun markSubdomainAsUsed(id: UUID?) {
        id?.let{
            subdomainRepository.findByIdOrNull(id)?.let {
                it.status = ReservedDomainStatus.USED
                subdomainRepository.save(it)
            }
        }
    }

}
