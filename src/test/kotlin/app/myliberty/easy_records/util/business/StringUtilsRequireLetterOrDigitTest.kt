package app.myliberty.easy_records.util.business

import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class StringUtilsRequireLetterOrDigitTest {

    @Test
    fun `a name with only symbols or spaces is rejected with the given message`() {
        listOf("***", "---", "_ _", "!?", "   ").forEach { name ->
            val exception = assertThrows(LibertyGenericException::class.java) { StringUtils.requireLetterOrDigit(name, "needs one") }

            assertEquals("needs one", exception.message)
        }
    }

    @Test
    fun `a single letter or digit is enough`() {
        assertDoesNotThrow { StringUtils.requireLetterOrDigit("!a!", "needs one") }
        assertDoesNotThrow { StringUtils.requireLetterOrDigit("#7", "needs one") }
    }
}
