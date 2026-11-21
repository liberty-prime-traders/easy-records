package app.myliberty.easy_records.platform.business

import app.myliberty.easy_records.platform.business.sysuser.api.ServiceAccountInserter
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class PlatformBootstrap(private val serviceAccountInserter: ServiceAccountInserter) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        serviceAccountInserter.seed()
    }
}
