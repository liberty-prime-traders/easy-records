package me.ezra_home.retail_software_solution.locations.business.sale_payment

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ActiveSalePaymentRepository : JpaRepository<ActiveSalePaymentEntity, UUID> {

    fun findBySaleIdIn(saleIds: Collection<UUID>): List<ActiveSalePaymentEntity>

    @Query(
        "SELECT DISTINCT activePayment.saleId FROM ActiveSalePaymentEntity activePayment " +
            "WHERE activePayment.saleId IN :saleIds"
    )
    fun findSaleIdsWithActivePayments(@Param("saleIds") saleIds: Collection<UUID>): Set<UUID>

}
