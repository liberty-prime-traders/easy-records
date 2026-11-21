package app.myliberty.easy_records.cucumber.config

import app.myliberty.easy_records.configuration.filters.UserDataExtractionFilter
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter
import org.springframework.security.web.SecurityFilterChain

@TestConfiguration
class TestSecurityConfiguration {

  @Bean
  @Primary
  fun testSecurityFilterChain(
    http: HttpSecurity,
    testAuthenticationFilter: TestAuthenticationFilter,
    userDataExtractionFilter: UserDataExtractionFilter
  ): SecurityFilterChain {
    return http.authorizeHttpRequests { it.requestMatchers("/secured/**").authenticated().anyRequest().permitAll() }
      .addFilterBefore(userDataExtractionFilter, BearerTokenAuthenticationFilter::class.java)
      .addFilterBefore(testAuthenticationFilter, UserDataExtractionFilter::class.java)
      .anonymous { it.disable() }
      .csrf { it.disable() }
      .cors { it.disable() }
      .build()
  }

  @Bean
  fun testAuthenticationFilter(): TestAuthenticationFilter = TestAuthenticationFilter()

  // Both filters are Filter beans, so Boot would also register them as servlet filters outside the security chain.
  @Bean
  fun testAuthenticationFilterRegistration(
    testAuthenticationFilter: TestAuthenticationFilter
  ): FilterRegistrationBean<TestAuthenticationFilter> {
    return FilterRegistrationBean(testAuthenticationFilter).apply { isEnabled = false }
  }

  @Bean
  fun userDataExtractionFilterRegistration(
    userDataExtractionFilter: UserDataExtractionFilter
  ): FilterRegistrationBean<UserDataExtractionFilter> {
    return FilterRegistrationBean(userDataExtractionFilter).apply { isEnabled = false }
  }
}
