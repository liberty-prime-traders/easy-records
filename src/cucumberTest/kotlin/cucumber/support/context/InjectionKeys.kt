package app.myliberty.easy_records.cucumber.support.context

sealed interface ContextKey {
  val key: String
}

enum class PersistentKey(override val key: String) : ContextKey {
  ORGANIZATION("organization"),
  LOCATION("location")
}

enum class TransientKey(override val key: String) : ContextKey {
  PRODUCT("product"),
  CATEGORY("category"),
  PRODUCT_GROUP("productGroup"),
  UNIT_GROUP("unitGroup"),
  UNIT_VALUE("unitValue"),
  ROOT_UNIT("rootUnit"),
  CONTACT("contact"),
  CONTACT_REFERENCE("contactReference"),
  LOAN("loan"),
  LOAN_REFERENCE("loanReference"),
  REPAYMENT("repayment"),
  REPAYMENT_REFERENCE("repaymentReference"),
  JURISDICTION_TYPE("jurisdictionType"),
  JURISDICTION("jurisdiction"),
  TAX_TYPE("taxType"),
  TAX_RATE("taxRate"),
  JURISDICTION_TAX_TYPE("jurisdictionTaxType"),
  ORG_TAX_TYPE("orgTaxType"),
  TAX_ACCOUNT("taxAccount"),
  MANUAL_JOURNAL("manualJournal"),
  MANUAL_JOURNAL_REVERSAL("manualJournalReversal"),
  SELECTED_EXPENSE_TYPE("selectedExpenseType"),
}
