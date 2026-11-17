package app.myliberty.easy_records.organizations.business.location

import jakarta.persistence.Converter
import app.myliberty.easy_records.organizations.business.location.api.LocationType
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class LocationTypeConverter : EnumConverter<LocationType>(LocationType::class.java)
