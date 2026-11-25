package app.myliberty.easy_records.cucumber.support.initialization

import app.myliberty.easy_records.configuration.datasource.DataSourceBeanNames
import app.myliberty.easy_records.configuration.session.ServiceAccountContext
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.TestUserRegistry
import app.myliberty.easy_records.cucumber.support.context.AuthContext
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.PersistentKey
import app.myliberty.easy_records.cucumber.support.context.ResponseContext
import app.myliberty.easy_records.organizations.business.core.location.LocationCache
import app.myliberty.easy_records.organizations.business.core.location.api.LocationInsertDto
import app.myliberty.easy_records.organizations.business.core.location.api.LocationType
import app.myliberty.easy_records.platform.business.authorization_pass.api.AuthorizationPassInsertDto
import app.myliberty.easy_records.platform.business.authorization_pass.api.PassType
import app.myliberty.easy_records.platform.business.organization.OrganizationCache
import app.myliberty.easy_records.platform.business.organization.api.OrganizationDto
import app.myliberty.easy_records.platform.business.organization.api.OrganizationInsertDto
import app.myliberty.easy_records.platform.business.role_assignment.api.PlatformRoleAssignmentService
import app.myliberty.easy_records.platform.business.sysuser.api.SysUserService
import app.myliberty.easy_records.platform.business.table_registry.TableRegistryCache
import app.myliberty.easy_records.support.TestConstants
import app.myliberty.easy_records.util.enums.EsrRole
import app.myliberty.easy_records.util.enums.ServiceAccount
import app.myliberty.easy_records.util.model.ReferenceNumberEntityListener
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.DependsOn
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@DependsOn(
  DataSourceBeanNames.PLATFORM_SCHEMA_LIQUIBASE,
  ReferenceNumberEntityListener.BEAN_NAME
)
class BoilerPlateDataInitializer(
  private val apiClient: ApiClient,
  private val authContext: AuthContext,
  private val injectContext: InjectContext,
  private val organizationCache: OrganizationCache,
  private val locationCache: LocationCache,
  private val tableRegistryCache: TableRegistryCache,
  private val sysUserService: SysUserService,
  private val platformRoleAssignmentService: PlatformRoleAssignmentService,
  private val organizationMembershipSeeder: OrganizationMembershipSeeder
) {

  companion object {
    const val SUBDOMAIN = "test"
    private const val PLATFORM_ADMIN_EMAIL = "platform-admin@test.local"
    private const val ORGANIZATION_USER_EMAIL = "org-user@test.local"
  }

  @EventListener(ApplicationReadyEvent::class)
  fun initialize() {
    val existingOrganization = organizationCache.getAllOrganizations()
    if (existingOrganization.isEmpty()) {
      validateAllTables()
      authContext.authToken = TestConstants.Tokens.PLATFORM_ADMIN
      val platformUserId = createPlatformUser()
      grantPlatformAdminRole(platformUserId)
      val passRecordId = issuePass(platformUserId)
      reserveSubdomain()
      createOrganization(passRecordId)
      createLocation()
      createOrgUser()
      ensureDefaultMemberships()
    } else {
      registerExistingUsers()
      TestUserRegistry.platformAdminUserId?.let { grantPlatformAdminRole(it) }
      existingOrganization.find { it.subdomain == SUBDOMAIN }?.let { organization ->
        injectContext.persist(PersistentKey.ORGANIZATION, organization.id)
        SessionContextProvider.initOrganization(organization)
        locationCache.getAllLocations().firstOrNull()?.let { location ->
          injectContext.persist(PersistentKey.LOCATION, location.id)
        }
        SessionContextProvider.clear()
      }
      ensureDefaultMemberships()
    }
  }

  // Memberships are protected from the per-scenario reset, so the default users are re-asserted as members before each
  // scenario and can call member-gated endpoints even after a scenario has ended their membership.
  fun ensureDefaultMemberships() {
    val organization: OrganizationDto = checkNotNull(
      organizationCache.getAllOrganizations().find { it.subdomain == SUBDOMAIN }
    ) { "Test organization was not created" }
    val userIds = listOfNotNull(TestUserRegistry.platformAdminUserId, TestUserRegistry.organizationUserId)
    ServiceAccountContext.runWithServiceAccount<Unit>(ServiceAccount.RECORD_INITIALIZER) {
      SessionContextProvider.initOrganization(organization)
      try {
        userIds.forEach { userId -> organizationMembershipSeeder.ensureMember(userId) }
      } finally {
        SessionContextProvider.clear()
      }
    }
  }

  private fun createPlatformUser(): UUID {
    val platformUserId = findOrCreateUser(
      email = PLATFORM_ADMIN_EMAIL,
      localFirstName = "Fake",
      localLastName = "Platform Admin"
    )
    TestUserRegistry.platformAdminUserId = platformUserId
    return platformUserId
  }

  private fun createOrgUser() {
    TestUserRegistry.organizationUserId = findOrCreateUser(
      email = ORGANIZATION_USER_EMAIL,
      localFirstName = "Fake",
      localLastName = "Organization User"
    )
  }

  // TenantFilter replaces the request's authorities with the roles stored for the user, so the role must exist in the
  // database. assignAll skips a user who already holds it.
  private fun grantPlatformAdminRole(platformUserId: UUID) {
    ServiceAccountContext.runWithServiceAccount<Unit>(ServiceAccount.RECORD_INITIALIZER) {
      platformRoleAssignmentService.assignAll(listOf(platformUserId), EsrRole.PLATFORM_ADMIN, platformUserId)
    }
  }

  private fun registerExistingUsers() {
    TestUserRegistry.platformAdminUserId = sysUserService.findByEmail(PLATFORM_ADMIN_EMAIL)?.id
    TestUserRegistry.organizationUserId = sysUserService.findByEmail(ORGANIZATION_USER_EMAIL)?.id
  }

  private fun findOrCreateUser(email: String, localFirstName: String, localLastName: String): UUID {
    val existingUser = sysUserService.findByEmail(email)
    if (existingUser != null) return existingUser.id
    return sysUserService.createUser(email = email, localFirstName = localFirstName, localLastName = localLastName)
  }

  private fun reserveSubdomain() {
    val response = apiClient.get("/secured/reserved-subdomains/verify?suggestedSubdomain=$SUBDOMAIN")
    check(response.statusCode() == 200) { "Failed to reserve subdomain: ${response.asString()}" }
  }

  private fun issuePass(userId: UUID): UUID {
    val response = apiClient.post(
      "/secured/authorization-passes",
      AuthorizationPassInsertDto(
        passType = PassType.CREATE_ORGANIZATION,
        maxUseCount = 1,
        assignedToId = userId
      )
    )
    check(response.statusCode() == 200) { "Failed to issue authorization pass: ${response.asString()}" }
    return ResponseContext.idFromResponse(response)
  }

  private fun createOrganization(passRecordId: UUID) {
    val secretCodeResponse = apiClient.get("/secured/authorization-passes/$passRecordId/secret-code")
    check(secretCodeResponse.statusCode() == 200) { "Failed to get secret code: ${secretCodeResponse.asString()}" }
    val secretCode = UUID.fromString(secretCodeResponse.jsonPath().getString("code"))

    val response = apiClient.post(
      "/secured/organizations",
      OrganizationInsertDto(name = "Test Organization", subdomain = SUBDOMAIN, passCode = secretCode)
    )
    check(response.statusCode() == 200) { "Failed to create organization: ${response.asString()}" }
    injectContext.persist(PersistentKey.ORGANIZATION, ResponseContext.idFromResponse(response))
  }

  private fun createLocation() {
    val response = apiClient.post(
      "/secured/locations",
      LocationInsertDto(locationType = LocationType.SHOP, name = "Test Location")
    )
    check(response.statusCode() == 200) { "Failed to create location: ${response.asString()}" }
    injectContext.persist(PersistentKey.LOCATION, ResponseContext.idFromResponse(response))
  }

  private fun validateAllTables() {
    tableRegistryCache.getAllTables()
      .filter { !it.validated }
      .forEach {
        it.copy(validated = true).also { validated ->
          tableRegistryCache.save(validated)
        }
      }
  }
}
