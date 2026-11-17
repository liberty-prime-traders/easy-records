package app.myliberty.easy_records.util.enums

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class EsrRoleNamesTest {

    @Test
    fun `every EsrRoleNames constant matches an EsrRole entry name, and vice versa`() {
        val declaredNames = EsrRoleNames::class.java.fields
            .filter { it.name != "INSTANCE" }
            .map { it.get(null) as String }
            .toSet()
        val enumNames = EsrRole.entries.map { it.name }.toSet()

        assertEquals(enumNames, declaredNames)
    }
}
