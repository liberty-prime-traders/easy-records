package app.myliberty.easy_records.organizations.business.core.jobtitle.api

import app.myliberty.easy_records.organizations.business.core.jobtitle.JobTitleCache
import app.myliberty.easy_records.organizations.business.core.jobtitle.JobTitleDto
import app.myliberty.easy_records.organizations.business.core.jobtitle.JobTitleMapper
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class JobTitleServiceTest {

    private val jobTitleCache = mock(JobTitleCache::class.java)
    private val jobTitleMapper = mock(JobTitleMapper::class.java)
    private val jobTitleService = JobTitleService(jobTitleMapper, jobTitleCache)

    @Test
    fun `create rejects a value that matches an existing title once case and spacing are ignored`() {
        `when`(jobTitleCache.getAllJobTitles()).thenReturn(listOf(existingJobTitle("Cashier")))

        val exception = assertThrows(LibertyGenericException::class.java) {
            jobTitleService.createJobTitle(JobTitleInsertDto(value = "  Cash ier  "))
        }

        assertEquals("A job title with the value Cash ier already exists.", exception.message)
    }

    @Test
    fun `create rejects a blank or missing value`() {
        `when`(jobTitleCache.getAllJobTitles()).thenReturn(emptyList())

        val blankException = assertThrows(LibertyGenericException::class.java) {
            jobTitleService.createJobTitle(JobTitleInsertDto(value = "   "))
        }
        val missingException = assertThrows(LibertyGenericException::class.java) {
            jobTitleService.createJobTitle(JobTitleInsertDto(value = null))
        }

        assertEquals(JobTitleService.VALUE_IS_REQUIRED, blankException.message)
        assertEquals(JobTitleService.VALUE_IS_REQUIRED, missingException.message)
    }

    @Test
    fun `update rejects a value that matches another title ignoring case`() {
        val cashierId = UUID.randomUUID()
        val clerkId = UUID.randomUUID()
        `when`(jobTitleCache.getAllJobTitles()).thenReturn(
            listOf(existingJobTitle("Cashier", cashierId), existingJobTitle("Clerk", clerkId))
        )

        val exception = assertThrows(LibertyGenericException::class.java) {
            jobTitleService.updateJobTitle(JobTitleUpdateDto(id = clerkId, value = Optional.of("CASHIER")))
        }

        assertEquals("A job title with the value CASHIER already exists.", exception.message)
    }

    private fun existingJobTitle(value: String, id: UUID = UUID.randomUUID()) = JobTitleDto(
        id = id,
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.now(),
        referenceNumber = "JT000001",
        value = value
    )
}
