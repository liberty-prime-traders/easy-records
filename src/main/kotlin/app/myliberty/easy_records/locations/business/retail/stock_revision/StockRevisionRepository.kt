package app.myliberty.easy_records.locations.business.retail.stock_revision

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface StockRevisionRepository : JpaRepository<StockRevisionEntity, UUID>
