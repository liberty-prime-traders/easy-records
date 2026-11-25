# cache_admin

`POST /secured/cache-admin/clear` (platform admin) evicts the tenant caches of the selected organizations, or of every
organization when `organizationIds` is empty or omitted. It exists for databases changed outside the application
(restores, the migration scripts) and for the cucumber reset.

Tenant cache keys start with the schema name (`TenantCacheConfig`), so eviction is by key prefix: the organization
schema plus the schema of each of its locations, read through `LocationService`. Caches kept outside the cache manager
(the unit conversion graph) listen for `TenantCachesClearedEvent`, published once per organization with that
organization's session set; core cannot call into retail directly. The caller's
organization session is restored afterwards.
