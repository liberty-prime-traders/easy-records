package app.myliberty.easy_records.cucumber.support

import app.myliberty.easy_records.cucumber.support.database.SchemaScopedRunner
import app.myliberty.easy_records.organizations.business.core.account.api.EntryType
import app.myliberty.easy_records.organizations.business.core.ledger.LedgerEntryGroupEntity
import app.myliberty.easy_records.organizations.business.core.ledger.LedgerEntryGroupRepository
import app.myliberty.easy_records.organizations.business.core.ledger.LedgerEntryRepository
import app.myliberty.easy_records.organizations.business.core.ledger.SubledgerEntryRepository
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerSourceType
import org.springframework.stereotype.Component
import java.math.BigDecimal

data class LedgerGroupRow(val referenceNumber: String, val sourceType: LedgerSourceType)

data class LedgerEntryRow(val accountCode: String, val entryType: EntryType, val amount: BigDecimal)

data class SubledgerEntryRow(
  val contactReferenceNumber: String,
  val payableAmount: BigDecimal,
  val receivableAmount: BigDecimal,
  val runningPayable: BigDecimal,
  val runningReceivable: BigDecimal
)

@Component
class LedgerRowReader(
  private val ledgerEntryGroupRepository: LedgerEntryGroupRepository,
  private val ledgerEntryRepository: LedgerEntryRepository,
  private val subledgerEntryRepository: SubledgerEntryRepository,
  private val schemaScopedRunner: SchemaScopedRunner
) {

  fun groupsFor(sourceReferenceNumber: String): List<LedgerGroupRow> =
    schemaScopedRunner.run(LedgerEntryGroupEntity::class.java) {
      groupEntitiesFor(sourceReferenceNumber).map { LedgerGroupRow(it.requiredReference(), it.sourceType) }
    }

  fun entriesFor(sourceReferenceNumber: String): List<LedgerEntryRow> =
    schemaScopedRunner.run(LedgerEntryGroupEntity::class.java) {
      val groupReferences = groupReferencesFor(sourceReferenceNumber)
      ledgerEntryRepository.findByGroupReferenceNumberIn(groupReferences).map { entry ->
        LedgerEntryRow(entry.accountCode, entry.entryType, entry.amount)
      }
    }

  fun subledgerEntriesFor(sourceReferenceNumber: String): List<SubledgerEntryRow> =
    schemaScopedRunner.run(LedgerEntryGroupEntity::class.java) {
      val groupReferences = groupReferencesFor(sourceReferenceNumber).toSet()
      subledgerEntryRepository.findAll()
        .filter { it.groupReferenceNumber in groupReferences }
        .map { entry ->
          SubledgerEntryRow(
            entry.contactReferenceNumber,
            entry.payableAmount,
            entry.receivableAmount,
            entry.runningPayable,
            entry.runningReceivable
          )
        }
    }

  private fun groupReferencesFor(sourceReferenceNumber: String): List<String> =
    groupEntitiesFor(sourceReferenceNumber).map { it.requiredReference() }

  private fun groupEntitiesFor(sourceReferenceNumber: String): List<LedgerEntryGroupEntity> =
    ledgerEntryGroupRepository.findAll().filter { it.sourceReferenceNumber == sourceReferenceNumber }
}
