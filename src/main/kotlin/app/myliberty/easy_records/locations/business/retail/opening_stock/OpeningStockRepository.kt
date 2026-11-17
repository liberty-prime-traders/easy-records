package app.myliberty.easy_records.locations.business.opening_stock

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OpeningStockRepository : JpaRepository<OpeningStockEntity, UUID> {

    fun findByLocationProductIdIn(locationProductIds: Collection<UUID>): List<OpeningStockEntity>
}
