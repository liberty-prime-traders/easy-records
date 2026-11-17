package app.myliberty.easy_records.organizations.business

import app.myliberty.easy_records.support.WithDefaultOrganizationSession
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductUpdateDto
import app.myliberty.easy_records.locations.business.retail.sale_payment.api.SalePaymentCreateDto
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionAdjustmentAddDto
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionLineUpdateDto
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionPaymentAddDto
import app.myliberty.easy_records.locations.business.retail.supplier_payment.api.SupplierPaymentCreateDto
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactInsertDto
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactUpdateDto
import app.myliberty.easy_records.organizations.business.core.fund_movement.api.FundMovementCapitalRequest
import app.myliberty.easy_records.organizations.business.core.fund_movement.api.FundMovementDrawRequest
import app.myliberty.easy_records.organizations.business.core.fund_movement.api.FundMovementTransferRequest
import app.myliberty.easy_records.organizations.business.core.loan.api.LoanCreateRequest
import app.myliberty.easy_records.organizations.business.core.loan.api.LoanRepaymentCreateRequest
import app.myliberty.easy_records.organizations.business.core.opening_balance.api.OpeningBalanceUpsertDto
import app.myliberty.easy_records.organizations.business.core.tax_rate.api.TaxRateInsertDto
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

@WithDefaultOrganizationSession
class RequestMoneyFieldPrecisionTest {

    private val objectMapper = jacksonObjectMapper().findAndRegisterModules()
    private val id = UUID.randomUUID()

    private fun everyRequestBuiltWith(amount: String): List<() -> Any> = listOf(
        { read<LocationProductUpdateDto>("""{"id":"$id","defaultSalePrice":$amount}""") },
        { read<ContactInsertDto>("""{"contactTypes":[],"identityType":"INDIVIDUAL","firstName":"A","creditLimit":$amount}""") },
        { read<ContactUpdateDto>("""{"id":"$id","creditLimit":$amount}""") },
        {
            read<TaxRateInsertDto>(
                """{"orgJurisdictionTaxTypeId":"$id","name":"Levy","rateFlatAmount":$amount,""" +
                    """"taxIsBilledToCustomerSeparately":false,"taxIsIncludedInTaxableAmount":false,"startDate":"2026-01-01"}"""
            )
        },
        { read<LoanCreateRequest>("""{"lenderContactReference":"C1","assetAccountCode":"1000","amount":$amount,"loanDate":"2026-01-01"}""") },
        { read<LoanRepaymentCreateRequest>("""{"loanId":"$id","assetAccountCode":"1000","amount":$amount,"repaymentDate":"2026-01-01"}""") },
        { read<FundMovementTransferRequest>("""{"fromAccountCode":"1","toAccountCode":"2","amount":$amount,"movementDate":"2026-01-01"}""") },
        { read<FundMovementCapitalRequest>("""{"accountCode":"1","amount":$amount,"movementDate":"2026-01-01"}""") },
        { read<FundMovementDrawRequest>("""{"accountCode":"1","amount":$amount,"movementDate":"2026-01-01"}""") },
        { read<SalePaymentCreateDto>("""{"paymentMethodId":"$id","amount":$amount}""") },
        { read<SupplierPaymentCreateDto>("""{"purchaseId":"$id","deliveryId":null,"paymentMethodId":"$id","amount":$amount,"paymentDate":"2026-01-01T00:00:00Z","notes":null}""") },
        { read<SaleSessionPaymentAddDto>("""{"paymentMethodId":"$id","amount":$amount}""") },
        { read<OpeningBalanceUpsertDto>("""{"accountCode":"1","newAmount":$amount}""") },
        { read<SaleSessionLineUpdateDto>("""{"identity":{"id":"$id"},"quantity":1,"unitId":"$id","unitPriceOverride":$amount}""") },
    )

    private inline fun <reified RequestType> read(json: String): RequestType = objectMapper.readValue(json, RequestType::class.java)

    @Test
    fun `a typed money field with three decimals is a business error in every request that carries one`() {
        everyRequestBuiltWith("1.255").forEach { build ->
            val thrown = assertThrows(Exception::class.java) { build() }
            assertTrue(generateSequence<Throwable>(thrown) { it.cause }.any { it is RtsGenericException })
        }
    }

    @Test
    fun `a money field written with trailing zeros beyond two decimals is accepted everywhere`() {
        everyRequestBuiltWith("1.2500").forEach { build -> build() }
    }

    @Test
    fun `an explicit null still clears an optional money field`() {
        val update = read<LocationProductUpdateDto>("""{"id":"$id","defaultSalePrice":null}""")
        assertTrue(update.defaultSalePrice!!.isEmpty)
    }

    @Test
    fun `an adjustment percentage keeps two decimals and a fixed amount follows the money scale`() {
        val addPercentageWithThreeDecimals = """{"adjustmentReasonId":"$id","direction":"DISCOUNT","percentage":7.125}"""
        val thrown = assertThrows(Exception::class.java) { read<SaleSessionAdjustmentAddDto>(addPercentageWithThreeDecimals) }
        assertTrue(generateSequence<Throwable>(thrown) { it.cause }.any { it is RtsGenericException })
        read<SaleSessionAdjustmentAddDto>(addPercentageWithThreeDecimals.replace("7.125", "7.12"))
    }

    @Test
    fun `a missing price override means the price is unchanged`() {
        val update = read<SaleSessionLineUpdateDto>("""{"identity":{"id":"$id"},"quantity":1,"unitId":"$id"}""")
        assertTrue(update.unitPriceOverride == null)
    }
}
