package app.myliberty.easy_records.organizations.business.core.loan.search

import app.myliberty.easy_records.organizations.business.core.loan.api.LoanSearchParameters
import app.myliberty.easy_records.util.queries.SearchGuards

object LoanSearchValidator {

    private const val MAX_LENDER_CONTACT_REFERENCES = 50
    private const val MAX_REFERENCES = 20
    private const val MIN_PAGE_SIZE = 1
    private const val MAX_PAGE_SIZE = 500

    fun guardValidParameters(loanSearchParameters: LoanSearchParameters) {
        SearchGuards.guardBoundedRange(
            loanSearchParameters.createdFrom,
            loanSearchParameters.createdBefore,
            LoanSearchParameterNames.CREATED_FROM,
            LoanSearchParameterNames.CREATED_BEFORE
        )
        SearchGuards.guardMaxSize(loanSearchParameters.lenderContactReferences.size, MAX_LENDER_CONTACT_REFERENCES, "lender contact references")
        SearchGuards.guardMaxSize(loanSearchParameters.references.size, MAX_REFERENCES, "references")

        SearchGuards.guardRangeOrder(
            loanSearchParameters.createdFrom,
            loanSearchParameters.createdBefore,
            "createdFrom must not be after createdBefore"
        ) { from, before -> from.isAfter(before) }

        SearchGuards.guardRangeOrder(
            loanSearchParameters.loanDateFrom,
            loanSearchParameters.loanDateBefore,
            "loanDateFrom must not be after loanDateBefore"
        ) { from, before -> from.isAfter(before) }
    }

    fun guardValidPageSize(requestedSize: Int) {
        SearchGuards.guardPageSize(requestedSize, MIN_PAGE_SIZE, MAX_PAGE_SIZE)
    }
}
