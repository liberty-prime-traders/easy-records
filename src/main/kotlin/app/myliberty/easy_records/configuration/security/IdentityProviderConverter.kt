package app.myliberty.easy_records.configuration.security

import jakarta.persistence.Converter
import app.myliberty.easy_records.platform.business.identity.api.IdentityProvider
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class IdentityProviderConverter : EnumConverter<IdentityProvider>(IdentityProvider::class.java)
