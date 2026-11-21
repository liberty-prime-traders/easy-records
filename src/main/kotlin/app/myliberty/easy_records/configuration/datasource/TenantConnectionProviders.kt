package app.myliberty.easy_records.configuration.datasource

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class TenantConnectionProviders {

    @Bean(value = [DataSourceBeanNames.ORGANIZATION_SCHEMA_CONNECTION_PROVIDER])
    fun organizationConnectionProvider(
        @Qualifier(DataSourceBeanNames.ORGANIZATION_SCHEMA_DATA_SOURCE)
        dataSource: DataSource
    ): TenantConnectionProvider {

        return TenantConnectionProvider(dataSource)
    }

    @Bean(value = [DataSourceBeanNames.LOCATION_SCHEMA_CONNECTION_PROVIDER])
    fun locationConnectionProvider(
        @Qualifier(DataSourceBeanNames.LOCATION_SCHEMA_DATA_SOURCE)
        dataSource: DataSource
    ): TenantConnectionProvider {

        return TenantConnectionProvider(dataSource)
    }
}
