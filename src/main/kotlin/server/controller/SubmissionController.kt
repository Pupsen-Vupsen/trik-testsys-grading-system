package server.controller

import server.service.SubmissionService
import server.service.FileUploader
import server.entity.Submission
import server.constants.Constants.*

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.FileInputStream
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@RestController
class SubmissionController {

    val logger: Logger = LoggerFactory.getLogger(SubmissionController::class.java)

    @Autowired
    private lateinit var submissionService: SubmissionService

    @GetMapping("grading-system/submissions")
    fun getAllSubmissions(): ResponseEntity<Any> {
        logger.info("Client requested all submissions.")

        val submissions = submissionService.getAllSubmissionsOrNull()
            ?: run {
                logger.warn("There are no submissions.")
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Requests.NO_SUBMISSIONS)
            }

        logger.info("Returned all submissions.")
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(submissions)
    }

    @GetMapping("grading-system/submissions/submission/status")
    fun getSubmissionStatus(@RequestParam id: Long): ResponseEntity<String> {
        logger.info("Client requested submission $id status.")

        val submission = submissionService.getSubmissionOrNull(id)
            ?: run {
                logger.warn("There is no submission with id $id")
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Requests.SUBMISSION_NOT_FOUND)
            }

        logger.info("Returned submission $id status.")
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(submission.status)
    }

    @GetMapping("grading-system/submissions/submission/download")
    fun getSubmissionFile(@RequestParam id: Long): ResponseEntity<Any> {
        logger.info("Client requested submission $id file.")
        val submission = submissionService.getSubmissionOrNull(id)
            ?: run {
                logger.warn("There is no submission with id $id")
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Requests.SUBMISSION_NOT_FOUND)
            }

        val stream = try {
            InputStreamResource(FileInputStream(File(submission.filePath)))
        } catch (e: Exception) {
            logger.error("Caught exception: $e!")
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.toString())
        }

        logger.info("Returned submission $id file.")
        return ResponseEntity.status(HttpStatus.OK).body(stream)
    }

    private var submissionId = ID.DEFAULT_ID

    @PostMapping("grading-system/submissions/submission/upload")
    fun postSubmission(
        @RequestParam("task_name") taskName: String,
        @RequestParam file: MultipartFile
    ): ResponseEntity<Any> {
        logger.info("Got file!")

        if (submissionId == ID.DEFAULT_ID)
            submissionId = submissionService.getLastSubmissionIdOrNull() ?: ID.FIRST_ID
        submissionId++

        logger.info("Set $submissionId to new file.")

        val fileName = "$submissionId.qrs"
        val testingFileName = "${submissionId}_testing.qrs"
        val fileUploader = FileUploader(file, "$taskName/$fileName")

        return try {
            if (fileUploader.upload()) {
                submissionService.saveSubmission(Submission(submissionId, taskName, fileName, testingFileName))
                submissionService.testSubmission(submissionId)

                logger.info("Saved submission $submissionId.")
                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(submissionId)
            } else {
                logger.warn("Uploading file is empty.")

                ResponseEntity
                    .status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Requests.EMPTY_FILE)
            }
        } catch (e: Exception) {
            logger.error("Caught exception: $e!")

            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(e.message)
        }
    }
}