package app.myliberty.easy_records.util.enums

enum class PaymentStatus(override val code: String) : HasCode {
  UNPAID("UNP"),
  PARTIALLY_SETTLED("PST"),
  FULLY_SETTLED("FST"),
  OVERPAID("OVP");
}
