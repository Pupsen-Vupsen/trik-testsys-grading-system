package controller


import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

import trik.testsys.Application
import trik.testsys.gradingsystem.entities.Submission
import trik.testsys.gradingsystem.enums.Jsons
import trik.testsys.gradingsystem.enums.Status
import trik.testsys.gradingsystem.services.SubmissionService

import java.io.File


@SpringBootTest(classes = [Application::class])
@AutoConfigureMockMvc
@WithMockUser(username = "accessUser1", password = "super", roles = ["USER"])
class SubmissionControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var submissionService: SubmissionService

    @Test
    fun contextLoads() {
        assertNotNull(this)
    }

    @Nested
    inner class GetAllSubmissions {

        @Test
        fun `getAllSubmissions should return empty list if there are no submissions`() {
            mockMvc.perform(get("/v3/grading-system/submissions/info/all"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$").isArray)
                .andExpect(jsonPath("\$").isEmpty)
        }

        @Test
        fun `getAllSubmissions should return list of saved submissions`() {
            val submission1 = createSubmission(
                1,
                Status.ACCEPTED,
                "task1",
                "student1",
                "2020-01-01",
                2,
                2,
                "hash",
                "message"
            )
            val submission2 = createSubmission(
                2,
                Status.FAILED,
                "task2",
                "student1",
                "2020-01-01",
                2,
                0,
                "hash2",
                "message2"
            )

            val expectedSubmissionsList = listOf(submission1, submission2)

            Mockito.`when`(submissionService.getAllSubmissionsOrNull()).thenReturn(expectedSubmissionsList)

            mockMvc.perform(get("/v3/grading-system/submissions/info/all"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$").isArray)
                .andExpect(jsonPath("\$").isNotEmpty)

                .andExpect(jsonPath("\$[0].id").value(submission1.id))
                .andExpect(jsonPath("\$[0].task_name").value(submission1.taskName))
                .andExpect(jsonPath("\$[0].student_id").value(submission1.studentId))
                .andExpect(jsonPath("\$[0].date").value(submission1.date))
                .andExpect(jsonPath("\$[0].status").value(submission1.status.code))
                .andExpect(jsonPath("\$[0].count_of_tests").value(submission1.countOfTests))
                .andExpect(jsonPath("\$[0].count_of_successful_tests").value(submission1.countOfSuccessfulTests))
                .andExpect(jsonPath("\$[0].hash").value(submission1.hash))
                .andExpect(jsonPath("\$[0].trik_message").value(submission1.trikMessage))

                .andExpect(jsonPath("\$[1].id").value(submission2.id))
                .andExpect(jsonPath("\$[1].task_name").value(submission2.taskName))
                .andExpect(jsonPath("\$[1].student_id").value(submission2.studentId))
                .andExpect(jsonPath("\$[1].date").value(submission2.date))
                .andExpect(jsonPath("\$[1].status").value(submission2.status.code))
                .andExpect(jsonPath("\$[1].count_of_tests").value(submission2.countOfTests))
                .andExpect(jsonPath("\$[1].count_of_successful_tests").value(submission2.countOfSuccessfulTests))
                .andExpect(jsonPath("\$[1].hash").value(submission2.hash))
                .andExpect(jsonPath("\$[1].trik_message").value(submission2.trikMessage))
        }
    }

    @Test
    fun `getArraySubmissionsStatus should return list of submissions with status or with null`() {
        val submission1 = createSubmission(1, Status.RUNNING)
        val submission2 = createSubmission(2, Status.ACCEPTED)

        Mockito.`when`(submissionService.getSubmissionOrNull(1)).thenReturn(submission1)
        Mockito.`when`(submissionService.getSubmissionOrNull(2)).thenReturn(submission2)
        Mockito.`when`(submissionService.getSubmissionOrNull(3)).thenReturn(null)
        Mockito.`when`(submissionService.getSubmissionOrNull(4)).thenReturn(null)

        mockMvc.perform(
            get("/v3/grading-system/submissions/status")
                .param("id_array", "1", "2", "3", "4")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("\$").isArray)
            .andExpect(jsonPath("\$").isNotEmpty)
            .andExpect(jsonPath("\$[0].id").value(submission1.id))
            .andExpect(jsonPath("\$[0].status").value(submission1.status.code))

            .andExpect(jsonPath("\$[1].id").value(submission2.id))
            .andExpect(jsonPath("\$[1].status").value(submission2.status.code))

            .andExpect(jsonPath("\$[2].id").value(3))
            .andExpect(jsonPath("\$[2].status").value(null))

            .andExpect(jsonPath("\$[3].id").value(4))
            .andExpect(jsonPath("\$[3].status").value(null))
    }

    @Nested
    inner class GetSubmissionStatus {

        @Test
        fun `getSubmissionStatus should return json with info about not existing submission`() {
            Mockito.`when`(submissionService.getSubmissionOrNull(1)).thenReturn(null)

            mockMvc.perform(
                get("/v3/grading-system/submissions/submission/status")
                    .param("id", "1")
            )
                .andExpect(status().isNotFound)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$").isNotEmpty)
                .andExpect(jsonPath("\$.code").value(Jsons.thereIsNoSubmissionJson.getValue("code")))
                .andExpect(jsonPath("\$.error_type").value(Jsons.thereIsNoSubmissionJson.getValue("error_type")))
                .andExpect(jsonPath("\$.message").value(Jsons.thereIsNoSubmissionJson.getValue("message")))
        }

        @Test
        fun `getSubmissionStatus should return json with info about existing submission`() {
            val submission = createSubmission(1, Status.RUNNING)

            Mockito.`when`(submissionService.getSubmissionOrNull(1)).thenReturn(submission)

            mockMvc.perform(
                get("/v3/grading-system/submissions/submission/status")
                    .param("id", "1")
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$").isNotEmpty)
                .andExpect(jsonPath("\$.id").value(submission.id))
                .andExpect(jsonPath("\$.status").value(submission.status.code))
        }
    }

    @Nested
    inner class GetSubmissionInfo {

        @Test
        fun `getSubmissionInfo should return json with info about not found submission`() {
            Mockito.`when`(submissionService.getSubmissionOrNull(1)).thenReturn(null)

            mockMvc.perform(
                get("/v3/grading-system/submissions/submission/info")
                    .param("id", "1")
            )
                .andExpect(status().isNotFound)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$").isNotEmpty)

                .andExpect(jsonPath("\$.code").value(Jsons.thereIsNoSubmissionJson.getValue("code")))
                .andExpect(jsonPath("\$.error_type").value(Jsons.thereIsNoSubmissionJson.getValue("error_type")))
                .andExpect(jsonPath("\$.message").value(Jsons.thereIsNoSubmissionJson.getValue("message")))
        }

        @Test
        fun `getSubmissionInfo should return json with info about existing submission`() {
            val submission = createSubmission(
                1,
                Status.ACCEPTED,
                "task1",
                "student1",
                "2020-01-01",
                2,
                2,
                "hash",
                "message"
            )

            Mockito.`when`(submissionService.getSubmissionOrNull(1)).thenReturn(submission)

            mockMvc.perform(
                get("/v3/grading-system/submissions/submission/info")
                    .param("id", "1")
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$").isNotEmpty)

                .andExpect(jsonPath("\$.id").value(submission.id))
                .andExpect(jsonPath("\$.task_name").value(submission.taskName))
                .andExpect(jsonPath("\$.student_id").value(submission.studentId))
                .andExpect(jsonPath("\$.date").value(submission.date))
                .andExpect(jsonPath("\$.status").value(submission.status.code))
                .andExpect(jsonPath("\$.count_of_tests").value(submission.countOfTests))
                .andExpect(jsonPath("\$.count_of_successful_tests").value(submission.countOfSuccessfulTests))
                .andExpect(jsonPath("\$.trik_message").value(submission.trikMessage))
        }
    }

    @Test
    fun `getArraySubmissionsInfo should return list of submissions with info or with nulls`() {
        val submission1 = createSubmission(
            1,
            Status.ACCEPTED,
            "task1",
            "student1",
            "2020-01-01",
            2,
            2,
            "hash",
            "message"
        )
        val submission3 = createSubmission(
            2,
            Status.FAILED,
            "task2",
            "student1",
            "2020-01-01",
            2,
            0,
            "hash2",
            "message2"
        )

        Mockito.`when`(submissionService.getSubmissionOrNull(1)).thenReturn(submission1)
        Mockito.`when`(submissionService.getSubmissionOrNull(2)).thenReturn(null)
        Mockito.`when`(submissionService.getSubmissionOrNull(3)).thenReturn(submission3)
        Mockito.`when`(submissionService.getSubmissionOrNull(4)).thenReturn(null)

        mockMvc.perform(
            get("/v3/grading-system/submissions/info")
                .param("id_array", "1", "2", "3", "4")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("\$").isNotEmpty)

            .andExpect(jsonPath("\$[0].id").value(submission1.id))
            .andExpect(jsonPath("\$[0].task_name").value(submission1.taskName))
            .andExpect(jsonPath("\$[0].student_id").value(submission1.studentId))
            .andExpect(jsonPath("\$[0].date").value(submission1.date))
            .andExpect(jsonPath("\$[0].status").value(submission1.status.code))
            .andExpect(jsonPath("\$[0].count_of_tests").value(submission1.countOfTests))
            .andExpect(jsonPath("\$[0].count_of_successful_tests").value(submission1.countOfSuccessfulTests))
            .andExpect(jsonPath("\$[0].trik_message").value(submission1.trikMessage))

            .andExpect(jsonPath("\$[1].id").value(2))
            .andExpect(jsonPath("\$[1].task_name").value(null))
            .andExpect(jsonPath("\$[1].student_id").value(null))
            .andExpect(jsonPath("\$[1].date").value(null))
            .andExpect(jsonPath("\$[1].status").value(null))
            .andExpect(jsonPath("\$[1].count_of_tests").value(null))
            .andExpect(jsonPath("\$[1].count_of_successful_tests").value(null))
            .andExpect(jsonPath("\$[1].trik_message").value(null))

            .andExpect(jsonPath("\$[2].id").value(submission3.id))
            .andExpect(jsonPath("\$[2].task_name").value(submission3.taskName))
            .andExpect(jsonPath("\$[2].student_id").value(submission3.studentId))
            .andExpect(jsonPath("\$[2].date").value(submission3.date))
            .andExpect(jsonPath("\$[2].status").value(submission3.status.code))
            .andExpect(jsonPath("\$[2].count_of_tests").value(submission3.countOfTests))
            .andExpect(jsonPath("\$[2].count_of_successful_tests").value(submission3.countOfSuccessfulTests))
            .andExpect(jsonPath("\$[2].trik_message").value(submission3.trikMessage))

            .andExpect(jsonPath("\$[3].id").value(4))
            .andExpect(jsonPath("\$[3].task_name").value(null))
            .andExpect(jsonPath("\$[3].student_id").value(null))
            .andExpect(jsonPath("\$[3].date").value(null))
            .andExpect(jsonPath("\$[3].status").value(null))
            .andExpect(jsonPath("\$[3].count_of_tests").value(null))
            .andExpect(jsonPath("\$[3].count_of_successful_tests").value(null))
            .andExpect(jsonPath("\$[3].trik_message").value(null))
    }

    @Nested
    inner class GetSubmissionFile {

        @Test
        fun `getSubmissionFile should return json with info about not found submission`() {
            Mockito.`when`(submissionService.getSubmissionFileOrNull(1)).thenReturn(null)

            mockMvc.perform(
                get("/v3/grading-system/submissions/submission/download")
                    .param("id", "1")
            )
                .andExpect(status().isNotFound)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$").isNotEmpty)

                .andExpect(jsonPath("\$.code").value(Jsons.thereIsNoSubmissionJson.getValue("code")))
                .andExpect(jsonPath("\$.error_type").value(Jsons.thereIsNoSubmissionJson.getValue("error_type")))
                .andExpect(jsonPath("\$.message").value(Jsons.thereIsNoSubmissionJson.getValue("message")))
        }

        @Test
        fun `getSubmissionFile should return existing submission file`() {
            val file = File("src/test/resources/test_files/get_submission_test_files/some-file.qrs")
            Mockito.`when`(submissionService.getSubmissionFileOrNull(1)).thenReturn(file)

            mockMvc.perform(
                get("/v3/grading-system/submissions/submission/download")
                    .param("id", "1")
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(content().bytes(file.readBytes()))
        }

        @Test
        fun `getSubmissionFile should return json with info about error then caught exception`() {
            val nonExistingFile = File("src/test/resources/non-existing-file.qrs")
            Mockito.`when`(submissionService.getSubmissionFileOrNull(1)).thenReturn(nonExistingFile)

            mockMvc.perform(
                get("/v3/grading-system/submissions/submission/download")
                    .param("id", "1")
            )
                .andExpect(status().isInternalServerError)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$").isNotEmpty)

                .andExpect(jsonPath("\$.code").value(Jsons.serverErrorJson.getValue("code")))
                .andExpect(jsonPath("\$.error_type").value(Jsons.serverErrorJson.getValue("error_type")))
                .andExpect(jsonPath("\$.message").value(Jsons.serverErrorJson.getValue("message")))
        }
    }

    @Nested
    inner class PostSubmission {

        @Test
        fun `postSubmission should return json with info about getting not qrs file #1`() {
            val file = File("src/test/resources/test_files/post_submission_test_files/not-qrs-file.txt")
            val multipartFile = MockMultipartFile("file", file.name, "text/plain", file.readBytes())

            val submission = createSubmission(
                1,
                taskName = "task1",
                studentId = "student1",
            )

            Mockito.`when`(submissionService.saveSubmission(submission.taskName, submission.studentId))
                .thenReturn(submission)

            mockMvc.perform(
                multipart("/v3/grading-system/submissions/submission/upload")
                    .file(multipartFile)
                    .param("task_name", submission.taskName)
                    .param("student_id", submission.studentId)
            )
                .andExpect(status().isUnprocessableEntity)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$").isNotEmpty)

                .andExpect(jsonPath("\$.code").value(Jsons.uploadingFileEmptyOrNotQrsJson.getValue("code")))
                .andExpect(jsonPath("\$.error_type").value(Jsons.uploadingFileEmptyOrNotQrsJson.getValue("error_type")))
                .andExpect(jsonPath("\$.message").value(Jsons.uploadingFileEmptyOrNotQrsJson.getValue("message")))
        }

        @Test
        fun `postSubmission should return json with info about getting not qrs file #2`() {
            val file = File("src/test/resources/test_files/post_submission_test_files/not-qrs-file2.qrs")
            val multipartFile = MockMultipartFile("file", file.name, "text/plain", file.readBytes())

            val submission = createSubmission(
                1,
                taskName = "task1",
                studentId = "student1",
            )

            Mockito.`when`(submissionService.saveSubmission(submission.taskName, submission.studentId))
                .thenReturn(submission)

            mockMvc.perform(
                multipart("/v3/grading-system/submissions/submission/upload")
                    .file(multipartFile)
                    .param("task_name", submission.taskName)
                    .param("student_id", submission.studentId)
            )
                .andExpect(status().isUnprocessableEntity)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$").isNotEmpty)

                .andExpect(jsonPath("\$.code").value(Jsons.uploadingFileEmptyOrNotQrsJson.getValue("code")))
                .andExpect(jsonPath("\$.error_type").value(Jsons.uploadingFileEmptyOrNotQrsJson.getValue("error_type")))
                .andExpect(jsonPath("\$.message").value(Jsons.uploadingFileEmptyOrNotQrsJson.getValue("message")))
        }

        @Test
        fun `postSubmission should return json with info about getting not qrs file #3`() {
            val file = File("src/test/resources/test_files/post_submission_test_files/not-qrs-file3.qrs")
            val multipartFile = MockMultipartFile("file", file.name, "text/plain", file.readBytes())

            val submission = createSubmission(
                1,
                taskName = "task1",
                studentId = "student1",
            )

            Mockito.`when`(submissionService.saveSubmission(submission.taskName, submission.studentId))
                .thenReturn(submission)

            mockMvc.perform(
                multipart("/v3/grading-system/submissions/submission/upload")
                    .file(multipartFile)
                    .param("task_name", submission.taskName)
                    .param("student_id", submission.studentId)
            )
                .andExpect(status().isUnprocessableEntity)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$").isNotEmpty)

                .andExpect(jsonPath("\$.code").value(Jsons.uploadingFileEmptyOrNotQrsJson.getValue("code")))
                .andExpect(jsonPath("\$.error_type").value(Jsons.uploadingFileEmptyOrNotQrsJson.getValue("error_type")))
                .andExpect(jsonPath("\$.message").value(Jsons.uploadingFileEmptyOrNotQrsJson.getValue("message")))
        }


        @Test
        fun `postSubmission should return json with info about getting empty file`() {
            val file = File("src/test/resources/test_files/post_submission_test_files/empty-file.qrs")
            val multipartFile = MockMultipartFile("file", file.name, "text/plain", file.readBytes())

            val submission = createSubmission(
                1,
                taskName = "task1",
                studentId = "student1",
            )

            Mockito.`when`(submissionService.saveSubmission(submission.taskName, submission.studentId))
                .thenReturn(submission)

            mockMvc.perform(
                multipart("/v3/grading-system/submissions/submission/upload")
                    .file(multipartFile)
                    .param("task_name", submission.taskName)
                    .param("student_id", submission.studentId)
            )
                .andExpect(status().isUnprocessableEntity)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$").isNotEmpty)

                .andExpect(jsonPath("\$.code").value(Jsons.uploadingFileEmptyOrNotQrsJson.getValue("code")))
                .andExpect(jsonPath("\$.error_type").value(Jsons.uploadingFileEmptyOrNotQrsJson.getValue("error_type")))
                .andExpect(jsonPath("\$.message").value(Jsons.uploadingFileEmptyOrNotQrsJson.getValue("message")))
        }

        @Test
        fun `postSubmission should return json with info about uploaded submission`() {
            File("/submissions").mkdir()
            val file = File("src/test/resources/test_files/post_submission_test_files/qrs-file.qrs")
            val multipartFile = MockMultipartFile("file", file.name, "text/plain", file.readBytes())

            val submission = createSubmission(
                1,
                taskName = "task1",
                studentId = "student1",
            )

            Mockito.`when`(submissionService.saveSubmission(submission.taskName, submission.studentId))
                .thenReturn(submission)

            mockMvc.perform(
                multipart("/v3/grading-system/submissions/submission/upload")
                    .file(multipartFile)
                    .param("task_name", submission.taskName)
                    .param("student_id", submission.studentId)
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$").isNotEmpty)

                .andExpect(jsonPath("\$.id").value(submission.id))
                .andExpect(jsonPath("\$.status").value(submission.status.code))
                .andExpect(jsonPath("\$.student_id").value(submission.studentId))
                .andExpect(jsonPath("\$.task_name").value(submission.taskName))
                .andExpect(jsonPath("\$.date").value(submission.date))

            File("/submissions").deleteRecursively()
        }
    }

    @Nested
    inner class RecheckSubmission {

        @Test
        fun `recheckSubmission should return json with info about not found submission`() {
            Mockito.`when`(submissionService.getSubmissionOrNull(1)).thenReturn(null)

            mockMvc.perform(
                patch("/v3/grading-system/submissions/submission/recheck")
                    .param("id", "1")
            )
                .andExpect(status().isNotFound)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$").isNotEmpty)

                .andExpect(jsonPath("\$.code").value(Jsons.thereIsNoSubmissionJson.getValue("code")))
                .andExpect(jsonPath("\$.error_type").value(Jsons.thereIsNoSubmissionJson.getValue("error_type")))
                .andExpect(jsonPath("\$.message").value(Jsons.thereIsNoSubmissionJson.getValue("message")))
        }

        @Test
        fun `recheckSubmission should return json with info that submission is not tested yet is submission is queued`() {
            val submission = createSubmission(
                1,
                taskName = "task1",
                studentId = "student1",
                status = Status.QUEUED,
                date = "2021-01-01 00:00:00"
            )

            Mockito.`when`(submissionService.getSubmissionOrNull(submission.id!!)).thenReturn(submission)

            mockMvc.perform(
                patch("/v3/grading-system/submissions/submission/recheck")
                    .param("id", "1")
            )
                .andExpect(status().isMethodNotAllowed)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$").isNotEmpty)

                .andExpect(jsonPath("\$.code").value(Jsons.submissionIsNotTestedYetJson.getValue("code")))
                .andExpect(jsonPath("\$.error_type").value(Jsons.submissionIsNotTestedYetJson.getValue("error_type")))
                .andExpect(jsonPath("\$.message").value(Jsons.submissionIsNotTestedYetJson.getValue("message")))
        }

        @Test
        fun `recheckSubmission should return json with info that submission is not tested yet is submission is running`() {
            val submission = createSubmission(
                1,
                taskName = "task1",
                studentId = "student1",
                status = Status.RUNNING,
                date = "2021-01-01 00:00:00"
            )

            Mockito.`when`(submissionService.getSubmissionOrNull(submission.id!!)).thenReturn(submission)

            mockMvc.perform(
                patch("/v3/grading-system/submissions/submission/recheck")
                    .param("id", "1")
            )
                .andExpect(status().isMethodNotAllowed)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$").isNotEmpty)

                .andExpect(jsonPath("\$.code").value(Jsons.submissionIsNotTestedYetJson.getValue("code")))
                .andExpect(jsonPath("\$.error_type").value(Jsons.submissionIsNotTestedYetJson.getValue("error_type")))
                .andExpect(jsonPath("\$.message").value(Jsons.submissionIsNotTestedYetJson.getValue("message")))
        }

        @Test
        fun `recheckSubmission should return json with info about rechecking submission`() {
            val submission = createSubmission(
                1,
                taskName = "task1",
                studentId = "student1",
                status = Status.FAILED,
                date = "2021-01-01 00:00:00"
            )

            Mockito.`when`(submissionService.getSubmissionOrNull(submission.id!!)).thenReturn(submission)
            Mockito.`when`(submissionService.testSubmission(submission)).then { submission.changeStatus(Status.QUEUED) }

            mockMvc.perform(
                patch("/v3/grading-system/submissions/submission/recheck")
                    .param("id", "1")
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$").isNotEmpty)

                .andExpect(jsonPath("\$.id").value(submission.id))
                .andExpect(jsonPath("\$.status").value(submission.status.code))
                .andExpect(jsonPath("\$.student_id").value(submission.studentId))
                .andExpect(jsonPath("\$.task_name").value(submission.taskName))
                .andExpect(jsonPath("\$.date").value(submission.date))

        }

        @Test
        fun `recheckArraySubmissions should return json submission array with status or with null`() {
            val submission1 = createSubmission(
                1,
                status = Status.FAILED,
            )
            val submission2 = createSubmission(
                2,
                status = Status.QUEUED,
            )
            val submission3 = createSubmission(
                3,
                status = Status.RUNNING,
            )

            Mockito.`when`(submissionService.getSubmissionOrNull(submission1.id!!)).thenReturn(submission1)
            Mockito.`when`(submissionService.getSubmissionOrNull(submission2.id!!)).thenReturn(submission2)
            Mockito.`when`(submissionService.getSubmissionOrNull(submission3.id!!)).thenReturn(submission3)
            Mockito.`when`(submissionService.getSubmissionOrNull(4)).thenReturn(null)

            Mockito.`when`(submissionService.testSubmission(submission1))
                .then { submission1.changeStatus(Status.QUEUED) }

            mockMvc.perform(
                patch("/v3/grading-system/submissions/recheck")
                    .param("idArray", "1,2,3,4")
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$").isNotEmpty)

                .andExpect(jsonPath("\$[0].id").value(submission1.id))
                .andExpect(jsonPath("\$[0].status").value(submission1.status.code))

                .andExpect(jsonPath("\$[1].id").value(submission2.id))
                .andExpect(jsonPath("\$[1].status").value(submission2.status.code))

                .andExpect(jsonPath("\$[2].id").value(submission3.id))
                .andExpect(jsonPath("\$[2].status").value(submission3.status.code))

                .andExpect(jsonPath("\$[3].id").value(4))
                .andExpect(jsonPath("\$[3].status").value(null))
        }
    }

    private fun createSubmission(
        id: Long,
        status: Status = Status.ACCEPTED,
        taskName: String = "task_name",
        studentId: String = "id",
        date: String = "01.11.1212",
        countOfTests: Int? = null,
        countOfSuccessfulTests: Int = 0,
        hash: String? = null,
        trikMessage: String? = null
    ): Submission {
        val submission = Submission(taskName, studentId, date)

        submission.setId(id)
        submission.changeStatus(status)
        submission.countOfTests = countOfTests
        submission.countOfSuccessfulTests = countOfSuccessfulTests
        submission.hash = hash
        submission.trikMessage = trikMessage

        return submission
    }

    fun Submission.setId(id: Long) {
        val field = Submission::class.java.getDeclaredField("id")
        field.isAccessible = true
        field.set(this, id)
    }
}