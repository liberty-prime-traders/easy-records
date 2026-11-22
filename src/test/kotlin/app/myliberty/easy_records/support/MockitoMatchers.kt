package app.myliberty.easy_records.support

import org.mockito.ArgumentMatchers.any

// Mockito's any() returns null, which Kotlin rejects for non-null parameters. The cast is erased, so the call site never sees the null.
@Suppress("UNCHECKED_CAST")
fun <T> anyNonNull(type: Class<T>): T {
    any(type)
    return null as T
}

@Suppress("UNCHECKED_CAST")
fun <T> anyNonNull(): T {
    any<T>()
    return null as T
}
