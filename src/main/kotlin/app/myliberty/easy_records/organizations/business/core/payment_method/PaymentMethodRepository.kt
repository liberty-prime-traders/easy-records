package app.myliberty.easy_records.organizations.business.payment_method

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PaymentMethodRepository: JpaRepository<PaymentMethodEntity, UUID>
