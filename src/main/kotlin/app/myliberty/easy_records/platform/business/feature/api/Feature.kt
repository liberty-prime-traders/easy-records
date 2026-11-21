package app.myliberty.easy_records.platform.business.feature.api

import app.myliberty.easy_records.util.enums.HasCode

enum class Feature(override val code: String) : HasCode {
    CHART_OF_ACCOUNTS("CHART_OF_ACCOUNTS"),
    TAX_CONFIGURATION("TAX_CONFIGURATION");
}
