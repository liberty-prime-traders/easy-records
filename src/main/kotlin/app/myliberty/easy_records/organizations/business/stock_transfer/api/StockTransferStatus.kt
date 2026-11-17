package app.myliberty.easy_records.organizations.business.stock_transfer.api

import app.myliberty.easy_records.util.enums.HasCode

enum class StockTransferStatus(override val code: String) : HasCode {
    DRAFT("DRAFT"),
    DISPATCHED("DSPCH"),
    COMPLETED("COMPD"),
    CANCELLED("CNCLD")
}
