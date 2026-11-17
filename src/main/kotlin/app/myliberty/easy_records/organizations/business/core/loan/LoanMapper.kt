package app.myliberty.easy_records.organizations.business.loan

import app.myliberty.easy_records.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper

@Mapper(config = RtsMapperConfig::class)
interface LoanMapper {

    fun toDto(loanEntity: LoanEntity): LoanDto

    fun toRepaymentDto(loanRepaymentEntity: LoanRepaymentEntity): LoanRepaymentDto

    fun toVoidDto(loanVoidEntity: LoanVoidEntity): LoanVoidDto

    fun toRepaymentVoidDto(loanRepaymentVoidEntity: LoanRepaymentVoidEntity): LoanRepaymentVoidDto

    fun toStatusFigures(loanStatusEntity: LoanStatusEntity): LoanStatusFigures
}
