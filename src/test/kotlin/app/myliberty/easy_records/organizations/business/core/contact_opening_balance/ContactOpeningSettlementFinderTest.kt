package app.myliberty.easy_records.organizations.business.core.contact_opening_balance

import app.myliberty.easy_records.organizations.business.core.contact_opening_balance.api.ContactSubledgerDirection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy
import java.math.BigDecimal

/** A settlement reduces only the direction it was recorded against; the other side keeps its declared amount. */
class ContactOpeningSettlementFinderTest {

    private val contactReferenceNumber = "CON000001"
    private val declaredAmountsByDirection = mapOf(
        ContactSubledgerDirection.RECEIVABLE to BigDecimal("100.00"),
        ContactSubledgerDirection.PAYABLE to BigDecimal("50.00")
    )
    private val activeSettledAmountsByDirection = mapOf(
        ContactSubledgerDirection.RECEIVABLE to BigDecimal("40.00"),
        ContactSubledgerDirection.PAYABLE to BigDecimal.ZERO
    )

    private val contactOpeningBalanceRepository = Proxy.newProxyInstance(
        ContactOpeningBalanceRepository::class.java.classLoader,
        arrayOf(ContactOpeningBalanceRepository::class.java)
    ) { _, method, arguments ->
        check(method.name == "findLatestRevision") { method.name }
        val subledgerDirection = arguments[1] as ContactSubledgerDirection
        ContactOpeningBalanceEntity(
            contactReferenceNumber,
            subledgerDirection,
            declaredAmountsByDirection.getValue(subledgerDirection)
        )
    } as ContactOpeningBalanceRepository

    private val activeContactOpeningSettlementRepository = Proxy.newProxyInstance(
        ActiveContactOpeningSettlementRepository::class.java.classLoader,
        arrayOf(ActiveContactOpeningSettlementRepository::class.java)
    ) { _, method, arguments ->
        check(method.name == "sumAmount") { method.name }
        val subledgerDirection = arguments[1] as ContactSubledgerDirection
        activeSettledAmountsByDirection.getValue(subledgerDirection)
    } as ActiveContactOpeningSettlementRepository

    private val contactOpeningSettlementFinder = ContactOpeningSettlementFinder(
        contactOpeningBalanceRepository,
        activeContactOpeningSettlementRepository
    )

    @Test
    fun `outstanding receivable is the declared amount less the receivable settlements`() {
        val outstandingReceivable = contactOpeningSettlementFinder.outstandingAmount(
            contactReferenceNumber,
            ContactSubledgerDirection.RECEIVABLE
        )

        assertEquals(0, BigDecimal("60.00").compareTo(outstandingReceivable))
    }

    @Test
    fun `outstanding payable is not reduced by a receivable settlement`() {
        val outstandingPayable = contactOpeningSettlementFinder.outstandingAmount(
            contactReferenceNumber,
            ContactSubledgerDirection.PAYABLE
        )

        assertEquals(0, BigDecimal("50.00").compareTo(outstandingPayable))
    }
}
