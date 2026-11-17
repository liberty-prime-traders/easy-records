package app.myliberty.easy_records.util.enums

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class EsrPermissionNamesTest {

    @Test
    fun `every EsrPermissionNames constant matches an EsrPermission entry name, and vice versa`() {
        val declaredNames = EsrPermissionNames::class.java.fields
            .filter { it.name != "INSTANCE" }
            .map { it.get(null) as String }
            .toSet()
        val enumNames = EsrPermission.entries.map { it.name }.toSet()

        assertEquals(enumNames, declaredNames)
    }
}
