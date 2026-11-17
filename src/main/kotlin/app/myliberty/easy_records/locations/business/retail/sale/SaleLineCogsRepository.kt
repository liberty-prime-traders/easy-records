package app.myliberty.easy_records.locations.business.retail.sale

import app.myliberty.easy_records.locations.business.retail.sale.api.SaleLineCogsStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SaleLineCogsRepository : JpaRepository<SaleLineCogsEntity, UUID> {

    fun findBySaleLineIdInAndStatus(saleLineIds: Collection<UUID>, status: SaleLineCogsStatus): List<SaleLineCogsEntity>

    @Query(
        """
        SELECT saleLine.saleId AS saleId, SUM(saleLineCogs.cogsTotal) AS cogsTotal
        FROM SaleLineCogsEntity saleLineCogs
        JOIN SaleLineEntity saleLine ON saleLine.id = saleLineCogs.saleLineId
        WHERE saleLine.saleId IN :saleIds AND saleLineCogs.status = :status
        GROUP BY saleLine.saleId
        """
    )
    fun sumCogsTotalsBySaleIdsAndStatus(
        @Param("saleIds") saleIds: Collection<UUID>,
        @Param("status") status: SaleLineCogsStatus,
    ): List<SaleCogsTotalRow>
}
