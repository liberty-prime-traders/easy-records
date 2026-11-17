package app.myliberty.easy_records.organizations.business.core.fiscal_period.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.organizations.business.core.accounting_config.api.OrgAccountingConfigDto
import app.myliberty.easy_records.organizations.business.core.accounting_config.api.OrgAccountingConfigService
import app.myliberty.easy_records.organizations.business.core.fiscal_period.FiscalPeriodDto
import app.myliberty.easy_records.organizations.business.core.fiscal_period.FiscalPeriodGenerationService
import app.myliberty.easy_records.organizations.business.core.fiscal_period.FiscalPeriodMapper
import app.myliberty.easy_records.organizations.business.core.fiscal_period.FiscalPeriodRepository
import app.myliberty.easy_records.organizations.business.core.fiscal_period.FiscalPeriodUtils
import app.myliberty.easy_records.organizations.business.core.fiscal_period.PeriodCloseValidator
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.business.mappers.UserQualifier
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class FiscalPeriodService(
    private val repository: FiscalPeriodRepository,
    private val mapper: FiscalPeriodMapper,
    private val configService: OrgAccountingConfigService,
    private val fiscalPeriodGenerationService: FiscalPeriodGenerationService,
    private val validator: PeriodCloseValidator,
    private val userFullNameQualifier: UserQualifier
) {


    fun nudgePeriodGeneration() {
        fiscalPeriodGenerationService.generatePeriods()
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun findOpenForDate(date: LocalDate): UUID? =
        repository.findOpenContaining(date)

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun requireOpenForDate(date: LocalDate): UUID {
        return findOpenForDate(date) ?: throw RtsGenericException("No open fiscal period for date $date")
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAll(): List<FiscalPeriodResponseDto> {
        val config = requireConfig()
        val dtos = repository.findAll().map { mapper.toDomainDto(it) }
        val periodsByYearEnd = dtos.groupBy { FiscalPeriodUtils.yearEnd(it.endDate, config.fiscalYearEndMonth) }
        return dtos.map { dto ->
            val yearEnd = FiscalPeriodUtils.yearEnd(dto.endDate, config.fiscalYearEndMonth)
            val yearPeers = periodsByYearEnd[yearEnd].orEmpty()
            toResponseDto(dto, config, validator.isClosable(dto, yearPeers))
        }
    }

    fun close(ids: Set<UUID>): List<FiscalPeriodResponseDto> {
        val now = Instant.now()
        val periods = repository.findByIdIn(ids)
        val missingIds = ids - periods.mapNotNull { it.id }.toSet()
        if (missingIds.isNotEmpty()) throw RtsGenericException("Fiscal periods not found: $missingIds")
        periods.forEach { period ->
            if (period.yearEnd) {
                throw RtsGenericException("Period '${period.name}' is a year-end period — use the year-end close endpoint")
            }
            validator.assertCanClose(period)
            period.closedAt = now
            period.closedBy = SessionContextProvider.getUserId()
        }
        val config = requireConfig()
        return repository.saveAll(periods).map {
            dto -> toResponseDto(mapper.toDomainDto(dto), config, false)
        }
    }

    fun rename(fiscalPeriodRenameDto: FiscalPeriodRenameDto): FiscalPeriodResponseDto {
        val id = fiscalPeriodRenameDto.id
        val name = fiscalPeriodRenameDto.name
        val period = repository.findById(id).orElseThrow { RtsGenericException("Fiscal period not found: $id") }
        val config = requireConfig()
        if (StringUtils.isEquivalent(period.name, name)) {
            val dto = mapper.toDomainDto(period)
            return toResponseDto(dto, config, validator.isClosable(dto))
        }
        val yearEnd = FiscalPeriodUtils.yearEnd(period.startDate, config.fiscalYearEndMonth)
        val yearStart = FiscalPeriodUtils.yearStart(yearEnd)
        val nameConflict = repository.findPeriodsInGivenYear(yearStart, yearEnd)
            .any { StringUtils.isEquivalent(it.name, name) }
        if (nameConflict) throw RtsGenericException("A period named '$name' already exists in this fiscal year")
        period.name = name
        val savedDto = mapper.toDomainDto(repository.save(period))
        return toResponseDto(savedDto, config, validator.isClosable(savedDto))
    }

    private fun requireConfig(): OrgAccountingConfigDto =
        configService.getConfig() ?: throw RtsGenericException("Accounting configuration has not been initialized")

    private fun toResponseDto(
        dto: FiscalPeriodDto,
        config: OrgAccountingConfigDto,
        closable: Boolean
    ): FiscalPeriodResponseDto {
        return FiscalPeriodResponseDto(
            id = dto.id,
            name = dto.name,
            fiscalYear = FiscalPeriodUtils.fiscalYearLabel(dto.endDate, config.fiscalYearEndMonth),
            startDate = dto.startDate,
            endDate = dto.endDate,
            yearEnd = dto.yearEnd,
            stub = dto.stub,
            closedAt = dto.closedAt,
            closedBy = userFullNameQualifier.getUserFullName(dto.closedBy),
            closable = closable,
            current = DateTimes.todayIsInRange(DateTimes.DateRange(dto.startDate, dto.endDate))
        )
    }
}
