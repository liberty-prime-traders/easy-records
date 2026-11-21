package app.myliberty.easy_records.organizations.business.core.loan.search

import app.myliberty.easy_records.organizations.business.core.loan.api.LoanSearchParameters
import app.myliberty.easy_records.organizations.business.core.loan.api.LoanStatus
import app.myliberty.easy_records.util.model.TableNames
import app.myliberty.easy_records.util.model.ViewNames
import app.myliberty.easy_records.util.queries.AnyListFilterStrategy
import app.myliberty.easy_records.util.queries.DateRangeFilterStrategy
import app.myliberty.easy_records.util.queries.KeysetSearchCursor
import app.myliberty.easy_records.util.queries.QueryBuilderContext
import app.myliberty.easy_records.util.queries.QueryParameterNames
import app.myliberty.easy_records.util.queries.SqlQuery

object LoanSearchQueryBuilder {

    private const val LOAN_ALIAS = "l"
    private const val LOAN_STATUS_ALIAS = "ls"
    private const val LENDER_ALIAS = "c"

    private const val FROM_AND_JOINS = """
        FROM ${TableNames.LOAN} $LOAN_ALIAS
        JOIN ${TableNames.CONTACT} $LENDER_ALIAS ON $LENDER_ALIAS.id = $LOAN_ALIAS.lender_contact_id
        JOIN ${ViewNames.LOAN_STATUS} $LOAN_STATUS_ALIAS ON $LOAN_STATUS_ALIAS.loan_id = $LOAN_ALIAS.id
    """

    fun buildPredicate(loanSearchParameters: LoanSearchParameters): QueryBuilderContext {
        val context = QueryBuilderContext()
        context.whereClauses.add("1=1")

        DateRangeFilterStrategy(
            "$LOAN_ALIAS.created_on",
            LoanSearchParameterNames.CREATED_FROM,
            loanSearchParameters.createdFrom,
            LoanSearchParameterNames.CREATED_BEFORE,
            loanSearchParameters.createdBefore
        ).apply(context)

        DateRangeFilterStrategy(
            "$LOAN_ALIAS.loan_date",
            LoanSearchParameterNames.LOAN_DATE_FROM,
            loanSearchParameters.loanDateFrom,
            LoanSearchParameterNames.LOAN_DATE_BEFORE,
            loanSearchParameters.loanDateBefore
        ).apply(context)

        AnyListFilterStrategy(
            "$LENDER_ALIAS.reference_number",
            LoanSearchParameterNames.LENDER_CONTACT_REFERENCES,
            loanSearchParameters.lenderContactReferences.toTypedArray()
        ).apply(context)

        AnyListFilterStrategy(
            "$LOAN_ALIAS.reference_number",
            LoanSearchParameterNames.REFERENCES,
            loanSearchParameters.references.toTypedArray()
        ).apply(context)

        if (loanSearchParameters.statuses.size < LoanStatus.entries.size) {
            AnyListFilterStrategy(
                "$LOAN_STATUS_ALIAS.status",
                LoanSearchParameterNames.STATUSES,
                loanSearchParameters.statuses.map { it.code }.toTypedArray()
            ).apply(context)
        }

        return context
    }

    fun buildListQuery(predicate: QueryBuilderContext, cursor: KeysetSearchCursor?): SqlQuery {
        val whereClauses = predicate.whereClauses.toMutableList()
        val params = predicate.params.toMutableMap()

        if (cursor != null) {
            whereClauses.add(
                "($LOAN_ALIAS.created_on, $LOAN_ALIAS.id) < (:${QueryParameterNames.CURSOR_CREATED_ON}, :${QueryParameterNames.CURSOR_ID})"
            )
            params[QueryParameterNames.CURSOR_CREATED_ON] = cursor.createdOn
            params[QueryParameterNames.CURSOR_ID] = cursor.id
        }

        val sql = """
            SELECT
              $LOAN_ALIAS.id AS id,
              $LOAN_ALIAS.created_on AS created_on
            $FROM_AND_JOINS
            WHERE ${whereClauses.joinToString(" AND ")}
            ORDER BY $LOAN_ALIAS.created_on DESC, $LOAN_ALIAS.id DESC
            LIMIT :${QueryParameterNames.PAGE_SIZE}
        """.trimIndent()

        return SqlQuery(sql, params, LoanSearchQueryMetadata("loan_search"))
    }
}
