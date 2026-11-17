package app.myliberty.easy_records.organizations.business.retail.product_category

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ProductCategoryRepository: JpaRepository<ProductCategoryEntity, UUID>
