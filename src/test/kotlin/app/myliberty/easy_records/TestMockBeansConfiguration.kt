package app.myliberty.easy_records

import org.mockito.Mockito.mock
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.oauth2.jwt.JwtDecoder

@TestConfiguration
class TestMockBeansConfiguration {

  @Bean
  @Primary
  fun jwtDecoder(): JwtDecoder = mock()

}
