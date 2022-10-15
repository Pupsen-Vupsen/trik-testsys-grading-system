package server.controller

import com.beust.klaxon.JsonArray
import server.service.SubmissionService
import server.entity.Submission
import server.enum.*

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.FileInputStream

import com.beust.klaxon.JsonObject

@RestController
class SubmissionController {

    val logger: Logger = LoggerFactory.getLogger(SubmissionController::class.java)

    private val thereIsNoSubmissionJson = JsonObject(
        mapOf(
            "code" to 404,
            "error_type" to "client",
            "message" to "There is no submission with this id."
        )
    )

    private val submissionIsNotSuccessfulJson = JsonObject(
        mapOf(
            "code" to 400,
            "error_type" to "client",
            "message" to "Submission is not successful."
        )
    )

    private val serverErrorJson = JsonObject(
        mapOf(
            "code" to 500,
            "error_type" to "server",
            "message" to "Something on server went wrong."
        )
    )

    private val uploadingFileEmptyJson = JsonObject(
        mapOf(
            "code" to 400,
            "error_type" to "client",
            "message" to "Uploading file is empty."
        )
    )

    @Autowired
    private lateinit var submissionService: SubmissionService

    @GetMapping("grading-system/submissions")
    fun getAllSubmissions(): ResponseEntity<JsonArray<Any>> {
        logger.info("Client requested all submissions.")

        val submissions = submissionService.getAllSubmissionsOrNull()
            ?: run {
                logger.warn("There are no submissions.")

                return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(JsonArray(emptyList()))
            }

        logger.info("Returned all submissions.")
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(JsonArray(submissions))
    }

    @GetMapping("grading-system/submissions/submission/status")
    fun getSubmissionStatus(@RequestParam id: Long): ResponseEntity<JsonObject> {
        logger.info("[$id]: Client requested submission status.")

        val submission = submissionService.getSubmissionOrNull(id)
            ?: run {
                logger.warn("[$id]: There is no submission with this id.")
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(thereIsNoSubmissionJson)
            }

        logger.info("[$id]: Returned submission status.")

        val submissionStatusJson = JsonObject(
            mapOf(
                "id" to submission.id,
                "status" to submission.status
            )
        )
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(submissionStatusJson)
    }

    @GetMapping("grading-system/submissions/submission/download")
    fun getSubmissionFile(@RequestParam id: Long): ResponseEntity<Any> {
        logger.info("[$id]: Client requested submission file.")

        val submission = submissionService.getSubmissionOrNull(id)
            ?: run {
                logger.warn("[$id]: There is no submission with this id.")
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(thereIsNoSubmissionJson)
            }

        val submissionFilePath = Paths.SUBMISSIONS.text + "${submission.id}/submission" + FilePostfixes.QRS.text

        val stream = try {
            InputStreamResource(FileInputStream(File(submissionFilePath)))
        } catch (e: Exception) {
            logger.error("[$id]: Caught exception while returning file: ${e.stackTraceToString()}!")
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(serverErrorJson)
        }

        logger.info("[$id]: Returned submission file.")
        return ResponseEntity.status(HttpStatus.OK).body(stream)
    }

    @GetMapping("grading-system/submissions/submission/lectorium_info")
    fun getHashAndPin(@RequestParam id: Long): ResponseEntity<JsonObject> {
        logger.info("[$id]: Client requested submission hash and pin.")

        val submission = submissionService.getSubmissionOrNull(id)
            ?: run {
                logger.warn("[$id]: There is no submission with this id.")
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(thereIsNoSubmissionJson)
            }

        if (submission.status != Status.ACCEPTED)
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(submissionIsNotSuccessfulJson)

        val json = JsonObject(mapOf("hash" to submission.hash, "pin" to submission.pin))

        logger.info("[$id]: Returned submission hash and pin.")
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(json)
    }

    private var submissionId = Id.DEFAULT.value

    @PostMapping("grading-system/submissions/submission/upload")
    fun postSubmission(
        @RequestParam("task_name") taskName: String,
        @RequestParam file: MultipartFile
    ): ResponseEntity<JsonObject> {
        logger.info("Got file!")

        if (submissionId == Id.DEFAULT.value)
            submissionId = submissionService.getLastSubmissionIdOrNull() ?: Id.FIRST.value
        submissionId++

        logger.info("[$submissionId]: Set id to new file.")
        val fileUploader = FileUploader(file, submissionId)

        return try {
            if (fileUploader.upload()) {
                submissionService.saveSubmission(Submission(submissionId, taskName))
                submissionService.testSubmission(submissionId)

                logger.info("[$submissionId]: Saved submission.")

                val submissionJson = JsonObject(
                    mapOf(
                        "id" to submissionId,
                        "task_name" to taskName
                    )
                )
                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(submissionJson)
            } else {
                logger.warn("[$submissionId]: Uploading file is empty or not .qrs file.")

                ResponseEntity
                    .status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(uploadingFileEmptyJson)
            }
        } catch (e: Exception) {
            logger.error("[$submissionId]: Caught exception while uploading file: ${e.stackTraceToString()}!")

            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(serverErrorJson)
        }
    }
}