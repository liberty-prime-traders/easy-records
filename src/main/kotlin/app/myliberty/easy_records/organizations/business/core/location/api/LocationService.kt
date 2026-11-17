package app.myliberty.easy_records.organizations.business.core.location.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.locations.business.core.location_user.api.LocationUserService
import app.myliberty.easy_records.organizations.business.core.location.LocationCache
import app.myliberty.easy_records.organizations.business.core.location.LocationMapper
import app.myliberty.easy_records.organizations.business.core.location.LocationSchemaService
import app.myliberty.easy_records.organizations.business.core.location.LocationValidator
import app.myliberty.easy_records.util.business.SchemaNameGenerator
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import app.myliberty.easy_records.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.Objects
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class LocationService(
    private val locationCache: LocationCache,
    private val locationMapper: LocationMapper,
    private val locationValidator: LocationValidator,
    private val locationSchemaService: LocationSchemaService,
    private val locationUserService: LocationUserService,
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAllLocations(): Collection<LocationResponseDto> {
        return locationCache.getAllLocations().map { locationMapper.toResponseDto(it) }
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAllLocationDtos(): Collection<LocationDto> = locationCache.getAllLocations()

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getBySchema(schema: String): LocationDto =
        locationCache.getAllLocations().find { it.schemaName == schema }
            ?: throw LibertyGenericException("No location found for schema $schema.")

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getById(locationId: UUID): LocationDto =
        locationCache.getAllLocations().find { it.id == locationId }
            ?: throw LibertyGenericException("No location found for $locationId.")

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getSchemaByLocationId(locationId: UUID): String =
        locationCache.getAllLocations().find { it.id == locationId }?.schemaName
            ?: throw LibertyGenericException("Location schema not found for $locationId")

    fun createLocation(locationInsertDto: LocationInsertDto): LocationResponseDto {
        locationValidator.validateLocationInsert(locationInsertDto)
        val schemaName = createLocationSchema(locationInsertDto.name!!)
        try {
            val locationDto = locationCache.create(locationInsertDto, schemaName)
            SessionContextProvider.initLocation(locationDto)
            locationUserService.registerFounder(locationDto.createdById)
            return locationMapper.toResponseDto(locationDto)
        } catch (e: Exception) {
            locationSchemaService.dropSchema(schemaName)
            throw e
        }
    }

    private fun createLocationSchema(locationName: String): String {
        val schemaName = SchemaNameGenerator.generateSchemaName(locationName, "loc")
        locationSchemaService.createSchema(schemaName)
        return schemaName
    }

    fun updateLocation(locationUpdateDto: LocationUpdateDto): LocationResponseDto {
        val locationId = SessionContextProvider.getLocationId()
        locationValidator.validateLocationUpdate(locationUpdateDto)
        val existing = locationCache.getAllLocations().find { Objects.equals(it.id, locationId) }
            ?: throw UpdatingNonExistingRecordException()
        val updated = locationUpdateDto.applyTo(existing)
        val saved = locationCache.save(updated)
        return locationMapper.toResponseDto(saved)
    }
}
