package app.myliberty.easy_records.platform.business.tax_type.api

import app.myliberty.easy_records.util.enums.HasCode

enum class TaxTrigger(override val code: String) : HasCode {
    SALE("SAL"),
    SALE_VOID("SALV"),
    SALE_RETURN("SALR"),
    CREDIT_NOTE("CRN"),
    DELIVERY("DEL"),
    PURCHASE_RETURN("PURR"),
    WRITE_OFF("WTO"),
    STOCK_ADJUSTMENT("STKAD");
}
