package app.myliberty.easy_records.organizations.business.core.loan

import app.myliberty.easy_records.util.business.mappers.LibertyMapperConfig
import org.mapstruct.Mapper

@Mapper(config = LibertyMapperConfig::class)
interface LoanMapper {

    fun toDto(loanEntity: LoanEntity): LoanDto

    fun toRepaymentDto(loanRepaymentEntity: LoanRepaymentEntity): LoanRepaymentDto

    fun toVoidDto(loanVoidEntity: LoanVoidEntity): LoanVoidDto

    fun toRepaymentVoidDto(loanRepaymentVoidEntity: LoanRepaymentVoidEntity): LoanRepaymentVoidDto

    fun toStatusFigures(loanStatusEntity: LoanStatusEntity): LoanStatusFigures
}
