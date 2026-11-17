package app.myliberty.easy_records

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.transaction.annotation.EnableTransactionManagement

@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement
@EnableMethodSecurity
class EasyRecordsApplication

fun main(args: Array<String>) {
	runApplication<EasyRecordsApplication>(*args)
}
