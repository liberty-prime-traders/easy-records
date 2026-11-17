package app.myliberty.easy_records.organizations.business.address

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AddressRepository: JpaRepository<AddressEntity, UUID>
