package app.myliberty.easy_records.organizations.business.core.loan.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactService
import app.myliberty.easy_records.organizations.business.core.loan.LoanMapper
import app.myliberty.easy_records.organizations.business.core.loan.LoanRepository
import app.myliberty.easy_records.organizations.business.core.loan.LoanResponseBuilder
import app.myliberty.easy_records.organizations.business.core.loan.search.LoanSearchExecutor
import app.myliberty.easy_records.organizations.business.core.loan.search.LoanSearchQueryBuilder
import app.myliberty.easy_records.organizations.business.core.loan.search.LoanSearchRowMapper
import app.myliberty.easy_records.organizations.business.core.loan.search.LoanSearchValidator
import app.myliberty.easy_records.util.paging.PageRequest
import app.myliberty.easy_records.util.paging.PageResponse
import app.myliberty.easy_records.util.queries.KeysetSearchCursor
import org.springframework.stereotype.Service

@Service
@TransactionalOnOrganizationSchema(readOnly = true)
class LoanSearchService(
    private val loanSearchExecutor: LoanSearchExecutor,
    private val loanRepository: LoanRepository,
    private val loanMapper: LoanMapper,
    private val loanResponseBuilder: LoanResponseBuilder,
    private val contactService: ContactService
) {

    fun search(pageRequest: PageRequest<LoanSearchParameters, String>): PageResponse<LoanResponse, String> {
        val loanSearchParameters = pageRequest.parameters.sanitized()
        LoanSearchValidator.guardValidParameters(loanSearchParameters)
        LoanSearchValidator.guardValidPageSize(pageRequest.requestedSize)
        val cursor = KeysetSearchCursor.decode(pageRequest.previousCursor)

        val predicate = LoanSearchQueryBuilder.buildPredicate(loanSearchParameters)
        val sqlQuery = LoanSearchQueryBuilder.buildListQuery(predicate, cursor)
        val rawRows = loanSearchExecutor.execute(sqlQuery, pageRequest.requestedSize + 1, setTimeout = true)
            .map { LoanSearchRowMapper.fromTuple(it) }
        val hasMore = rawRows.size > pageRequest.requestedSize
        val pageRows = if (hasMore) rawRows.take(pageRequest.requestedSize) else rawRows

        val loanDtosById = loanRepository.findAllById(pageRows.map { it.id })
            .map { loanMapper.toDto(it) }
            .associateBy { it.id }
        val contents = loanResponseBuilder.buildResponses(pageRows.map { loanDtosById.getValue(it.id) }, contactService.getContactsById())

        val currentCursor = pageRows.lastOrNull()
            ?.let { KeysetSearchCursor(it.createdOn, it.id).encode() }
            ?: pageRequest.previousCursor

        return PageResponse(currentCursor = currentCursor, hasMore = hasMore, contents = contents)
    }
}
