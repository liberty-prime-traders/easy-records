package app.myliberty.easy_records.cucumber.config

import io.cucumber.spring.CucumberContextConfiguration
import app.myliberty.easy_records.TestMockBeansConfiguration
import app.myliberty.easy_records.cucumber.support.initialization.ContainerInitializer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration


@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test", "cucumber")
@Import(TestMockBeansConfiguration::class, TestSecurityConfiguration::class)
@ContextConfiguration(initializers = [ContainerInitializer::class])
class CucumberSpringConfiguration
