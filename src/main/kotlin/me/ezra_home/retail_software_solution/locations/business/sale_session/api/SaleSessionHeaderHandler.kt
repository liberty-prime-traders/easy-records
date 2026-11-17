package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleUpdater
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionUpdateFinalizer
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionStore
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionValidator
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.enums.SystemContact
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SaleSessionHeaderHandler(
    private val saleSessionStore: SaleSessionStore,
    private val saleSessionValidator: SaleSessionValidator,
    private val contactService: ContactService,
    private val saleSessionUpdateFinalizer: SaleSessionUpdateFinalizer,
    private val saleUpdater: SaleUpdater,
) {

    // A request that changes nothing is held to the notes rule, which every sale allows.
    private fun headerActions(headerUpdateDto: SaleSessionHeaderUpdateDto): List<SaleSessionAction> =
        listOfNotNull(
            SaleSessionAction.UPDATE_CONTACT.takeIf { headerUpdateDto.contactId != null },
            SaleSessionAction.UPDATE_SOLD_BY.takeIf { headerUpdateDto.soldById != null },
            SaleSessionAction.UPDATE_DATE_SOLD.takeIf { headerUpdateDto.dateSold != null },
            SaleSessionAction.UPDATE_NOTES.takeIf { headerUpdateDto.notes != null },
        ).ifEmpty { listOf(SaleSessionAction.UPDATE_NOTES) }

    fun update(sessionId: UUID, headerUpdateDto: SaleSessionHeaderUpdateDto): SaleSessionResponseDto {
        val session = saleSessionStore.load(sessionId)
        headerActions(headerUpdateDto).forEach { saleSessionValidator.guardAllowed(session, it) }
        val updatedHeader = session.header.copy(
            contactId = headerUpdateDto.contactId?.let {
                it.orElseThrow { RtsGenericException("contactId cannot be cleared") }
            } ?: session.header.contactId,
            soldById = headerUpdateDto.soldById?.orElse(null) ?: session.header.soldById,
            dateSold = headerUpdateDto.dateSold?.orElse(null) ?: session.header.dateSold,
            notes = StringUtils.useIfProvided(headerUpdateDto.notes, session.header.notes),
        )
        if (updatedHeader.contactId != SystemContact.WALK_IN.id) {
            contactService.guardExists(updatedHeader.contactId)
        }
        val updatedSaleVersion = persistNotesOfSavedSale(session, headerUpdateDto, updatedHeader.notes)
        return saleSessionUpdateFinalizer.finalize(
            session.copy(header = updatedHeader, saleVersion = updatedSaleVersion ?: session.saleVersion)
        )
    }

    // A draft keeps its notes in the session until it is saved; any other sale is not saved again, so they are
    // written to the sale at once.
    private fun persistNotesOfSavedSale(
        session: SaleSession,
        headerUpdateDto: SaleSessionHeaderUpdateDto,
        notes: String?,
    ): Long? {
        val saleId = session.saleId ?: return null
        if (headerUpdateDto.notes == null || session.mutable()) return null
        return saleUpdater.updateNotes(saleId, notes)
    }
}
