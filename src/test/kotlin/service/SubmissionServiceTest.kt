package service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean

import trik.testsys.Application
import trik.testsys.gradingsystem.entities.Submission
import trik.testsys.gradingsystem.repositories.SubmissionRepository
import trik.testsys.gradingsystem.services.SubmissionService

@SpringBootTest(classes = [Application::class])
class SubmissionServiceTest {

    @Autowired
    private lateinit var submissionService: SubmissionService

    @MockBean
    private lateinit var submissionRepository: SubmissionRepository

    @Test
    fun `getSubmissionById should return null if submission with such id doesn't exist`() {
        Mockito.`when`(submissionRepository.findSubmissionById(1)).thenReturn(null)

        assertNull(submissionService.getSubmissionOrNull(1))
    }

    @Test
    fun `getSubmissionById should return existing submission`() {
        val expectedSubmission = Submission("task_name", "id", "01.11.1212")

        Mockito.`when`(submissionRepository.findSubmissionById(1)).thenReturn(expectedSubmission)

        assertEquals(expectedSubmission, submissionService.getSubmissionOrNull(1))
    }
}