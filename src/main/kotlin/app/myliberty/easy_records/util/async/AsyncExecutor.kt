package app.myliberty.easy_records.util.async

interface AsyncExecutor {
  fun execute(block: () -> Unit)
}
