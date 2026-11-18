package app.myliberty.easy_records.organizations.business.core.contact.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.core.contact.ContactCache
import app.myliberty.easy_records.organizations.business.core.contact.ContactResponseMapper
import app.myliberty.easy_records.organizations.business.core.contact.ContactValidator
import app.myliberty.easy_records.organizations.business.core.ledger.api.SubledgerBalanceDto
import app.myliberty.easy_records.organizations.business.core.ledger.api.SubledgerService
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.springframework.stereotype.Service

@Service
@TransactionalOnOrganizationSchema
class ContactBulkInsertService(
    private val contactValidator: ContactValidator,
    private val contactCache: ContactCache,
    private val contactResponseMapper: ContactResponseMapper,
    private val subledgerService: SubledgerService
) {

    fun bulkCreateContacts(contactBulkInsertDtos: List<ContactBulkInsertDto>): List<ContactResponseDto> {
        val errors = mutableListOf<String>()
        val validatedInsertDtos = mutableListOf<ContactInsertDto>()
        val identityKeysInRequest = mutableSetOf<String>()
        val existingNormalizedKeys = contactCache.getAllContacts().map { it.identity.normalizedKey }.toSet()
        contactBulkInsertDtos.forEachIndexed { index, contactBulkInsertDto ->
            try {
                val contactInsertDto = toValidatedInsertDto(contactBulkInsertDto)
                contactValidator.validateUniquenessAgainst(contactInsertDto.identity, existingNormalizedKeys)
                val identityKey = contactInsertDto.identity.normalizedKey
                if (identityKey.isNotBlank() && !identityKeysInRequest.add(identityKey)) {
                    throw LibertyGenericException("A Contact with ${contactInsertDto.identity.description()} appears more than once in the request")
                }
                validatedInsertDtos.add(contactInsertDto)
            } catch (libertyGenericException: LibertyGenericException) {
                errors.add("Row ${index + 1}: ${libertyGenericException.message}")
            }
        }
        if (errors.isNotEmpty()) {
            throw LibertyGenericException("Bulk contact import validation failed", errors)
        }
        val createdContactDtos = contactCache.createAll(validatedInsertDtos)
        subledgerService.openBalances(createdContactDtos.map { it.referenceNumber })
        return createdContactDtos.map {
            contactResponseMapper.toResponseDto(it, SubledgerBalanceDto.ZERO, SubledgerBalanceDto.ZERO)
        }
    }

    private fun toValidatedInsertDto(contactBulkInsertDto: ContactBulkInsertDto): ContactInsertDto {
        val contactTypes = parseContactTypes(contactBulkInsertDto.contactTypes)
        contactValidator.validateContactTypes(contactTypes)
        val identityType = IdentityType.forCompanyName(contactBulkInsertDto.companyName)
        contactValidator.validateIdentity(identityType, contactBulkInsertDto.firstName, contactBulkInsertDto.companyName)
        val isOrganization = identityType == IdentityType.ORGANIZATION
        return ContactInsertDto(
            contactTypes = contactTypes,
            identityType = identityType,
            firstName = if (isOrganization) null else StringUtils.getValueOrNull(contactBulkInsertDto.firstName),
            lastName = if (isOrganization) null else StringUtils.getValueOrNull(contactBulkInsertDto.lastName),
            companyName = StringUtils.getValueOrNull(contactBulkInsertDto.companyName),
            email = StringUtils.getValueOrNull(contactBulkInsertDto.email),
            phone = StringUtils.getValueOrNull(contactBulkInsertDto.phone),
            address = StringUtils.getValueOrNull(contactBulkInsertDto.address),
            creditLimit = contactBulkInsertDto.creditLimit,
            notes = StringUtils.getValueOrNull(contactBulkInsertDto.notes)
        )
    }

    private fun parseContactTypes(commaSeparatedContactTypeCodes: String?): Set<ContactType> {
        val contactTypesByCode = ContactType.entries.associateBy { it.code }
        return commaSeparatedContactTypeCodes.orEmpty()
            .split(",")
            .map { it.trim().uppercase() }
            .filter { it.isNotEmpty() }
            .map { code ->
                contactTypesByCode[code] ?: throw LibertyGenericException("Unknown contact type '$code'")
            }
            .toSet()
    }
}
