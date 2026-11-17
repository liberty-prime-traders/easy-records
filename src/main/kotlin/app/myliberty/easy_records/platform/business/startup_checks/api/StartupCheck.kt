package app.myliberty.easy_records.platform.business.startup_checks.api

interface StartupCheck {
    val name: String
    fun check()
}
