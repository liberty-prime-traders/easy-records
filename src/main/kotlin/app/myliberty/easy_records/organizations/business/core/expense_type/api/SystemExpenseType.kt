package app.myliberty.easy_records.organizations.business.core.expense_type.api

import app.myliberty.easy_records.cross_tier.core.expense.ExpenseSourceType
import app.myliberty.easy_records.organizations.business.core.account.api.SystemAccount
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactType
import app.myliberty.easy_records.util.enums.HasCode

enum class SystemExpenseType(
    override val code: String,
    val displayName: String,
    val expenseAccount: SystemAccount,
    val eligiblePayeeTypes: Set<ContactType>,
    val eligibleSourceTypes: Set<ExpenseSourceType>
) : HasCode {
    WAGES(
        "WGS", "Wages", SystemAccount.WAGES_EXPENSE,
        setOf(ContactType.EMPLOYEE),
        setOf(ExpenseSourceType.WAGES)
    ),
    RENT(
        "RNT", "Rent", SystemAccount.RENT_EXPENSE,
        setOf(ContactType.SERVICE_PROVIDER, ContactType.OTHER),
        setOf(ExpenseSourceType.ADHOC)
    ),
    UTILITIES(
        "UTIL", "Utilities", SystemAccount.UTILITIES_EXPENSE,
        setOf(ContactType.SERVICE_PROVIDER, ContactType.GOVERNMENT, ContactType.OTHER),
        setOf(ExpenseSourceType.ADHOC)
    ),
    INBOUND_FREIGHT(
        "INFRT", "Inbound freight", SystemAccount.INBOUND_FREIGHT,
        setOf(ContactType.SUPPLIER, ContactType.SERVICE_PROVIDER, ContactType.OTHER),
        setOf(
            ExpenseSourceType.ADHOC,
            ExpenseSourceType.PURCHASE,
            ExpenseSourceType.STOCK_TRANSFER
        )
    ),
    OUTBOUND_FREIGHT(
        "OTFRT", "Outbound freight", SystemAccount.OUTBOUND_FREIGHT,
        setOf(ContactType.SUPPLIER, ContactType.SERVICE_PROVIDER, ContactType.OTHER),
        setOf(ExpenseSourceType.ADHOC, ExpenseSourceType.SALE)
    ),
    OFFICE_SUPPLIES(
        "OFFSP", "Office supplies", SystemAccount.OFFICE_SUPPLIES,
        setOf(ContactType.SUPPLIER, ContactType.SERVICE_PROVIDER, ContactType.OTHER),
        setOf(ExpenseSourceType.ADHOC)
    ),
    STAFF_WELFARE(
        "WELF", "Staff welfare", SystemAccount.STAFF_WELFARE,
        setOf(ContactType.EMPLOYEE, ContactType.SUPPLIER, ContactType.SERVICE_PROVIDER, ContactType.OTHER),
        setOf(ExpenseSourceType.ADHOC)
    ),
    REPAIRS_AND_MAINTENANCE(
        "RPR", "Repairs and maintenance", SystemAccount.REPAIRS_AND_MAINTENANCE,
        setOf(ContactType.SERVICE_PROVIDER, ContactType.OTHER),
        setOf(ExpenseSourceType.ADHOC)
    ),
    OTHER_OPERATING(
        "OTH", "Other operating expense", SystemAccount.OTHER_OPERATING_EXPENSES,
        ContactType.entries.toSet(),
        ExpenseSourceType.entries.filter { it != ExpenseSourceType.WAGES }.toSet()
    )
}
