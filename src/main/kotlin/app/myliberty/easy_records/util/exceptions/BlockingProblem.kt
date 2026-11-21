package app.myliberty.easy_records.util.exceptions

data class BlockingProblem(
    val documentReference: String,
    val description: String,
    val effect: String,
)
