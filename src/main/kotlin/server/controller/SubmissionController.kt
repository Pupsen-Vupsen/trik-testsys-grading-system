package server.controller

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
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.LocalDate

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject

@RequestMapping("/v2/grading-system/submissions")
@RestController
class SubmissionController {

    val logger: Logger = LoggerFactory.getLogger(SubmissionController::class.java)

    @Autowired
    private lateinit var submissionService: SubmissionService

    @GetMapping("/all")
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

    @GetMapping("/status")
    fun getArraySubmissionsStatus(@RequestParam("id_array") idArray: List<Long>): ResponseEntity<JsonArray<Any>> {
        logger.info("Client requested submissions statuses.")

        val submissionsStatuses = mutableMapOf<Long, String?>()

        idArray.forEach {
            val submission = submissionService.getSubmissionOrNull(it) ?: run {
                logger.warn("There is no submission with id $it.")
                submissionsStatuses[it] = null
                return@forEach
            }

            submissionsStatuses[it] = submission.status.name
        }

        logger.info("Returned submissions statuses.")
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(convertFromMutableMapToJsonArray(submissionsStatuses))
    }

    private fun convertFromMutableMapToJsonArray(map: MutableMap<Long, String?>): JsonArray<Any> {
        val jsonArray = JsonArray<Any>()

        map.forEach {
            val jsonObject = JsonObject()
            jsonObject["id"] = it.key
            jsonObject["status"] = it.value
            jsonArray.add(jsonObject)
        }

        return jsonArray
    }

    @GetMapping("/submission/status")
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

    @GetMapping("/submission/info")
    fun getSubmissionInfo(@RequestParam id: Long): ResponseEntity<JsonObject> {
        logger.info("[$id]: Client requested submission info.")

        val submission = submissionService.getSubmissionOrNull(id)
            ?: run {
                logger.warn("[$id]: There is no submission with this id.")
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(thereIsNoSubmissionJson)
            }


        logger.info("[$id]: Returned submission info.")
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(submission.toJsonObject())
    }

    @GetMapping("/submission/download")
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

    private var submissionId = Id.DEFAULT.value

    @PostMapping("/submission/upload")
    fun postSubmission(
        @RequestParam("task_name") taskName: String,
        @RequestParam("student_id") studentId: String,
        @RequestParam file: MultipartFile
    ): ResponseEntity<JsonObject> {
        logger.info("Got file!")
        val date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) +
                "-" +
                LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))

        if (submissionId == Id.DEFAULT.value)
            submissionId = submissionService.getLastSubmissionIdOrNull() ?: Id.FIRST.value
        submissionId++

        logger.info("[$submissionId]: Set id to new file.")
        val fileUploader = FileUploader(file, submissionId)

        return try {
            if (fileUploader.upload()) {
                val submission = submissionService.saveSubmission(Submission(submissionId, taskName, studentId, date))
                submissionService.testSubmission(submission)

                logger.info("[$submissionId]: Saved submission.")

                val submissionJson = JsonObject(
                    mapOf(
                        "id" to submissionId,
                        "status" to submission.status,
                        "student_id" to submission.studentId,
                        "task_name" to taskName,
                        "date" to date
                    )
                )
                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(submissionJson)
            } else {
                logger.warn("[$submissionId]: Uploading file is empty or not .qrs file.")

                ResponseEntity
                    .status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(uploadingFileEmptyOrNotQrsJson)
            }
        } catch (e: Exception) {
            logger.error("[$submissionId]: Caught exception while uploading file: ${e.stackTraceToString()}!")

            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(serverErrorJson)
        }
    }

    @PatchMapping("/submission/recheck")
    fun recheckSubmission(@RequestParam id: Long): ResponseEntity<JsonObject> {
        logger.info("[$id]: Client requested recheck submission.")

        val submission = submissionService.getSubmissionOrNull(id)
            ?: run {
                logger.warn("[$id]: There is no submission with this id.")
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(thereIsNoSubmissionJson)
            }

        if (submission.status == Status.ACCEPTED) {
            logger.warn("[$id]: Submission is already accepted.")
            return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(submissionAlreadyAcceptedJson)
        }

        submissionService.saveSubmission(submission)
        submissionService.testSubmission(submission)

        logger.info("[$id]: Saved rechecking submission.")

        val submissionJson = JsonObject(
            mapOf(
                "id" to submission.id,
                "status" to submission.status,
                "student_id" to submission.studentId,
                "task_name" to submission.taskName,
                "date" to submission.date
            )
        )
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(submissionJson)
    }

    private val thereIsNoSubmissionJson = JsonObject(
        mapOf(
            "code" to 404,
            "error_type" to "client",
            "message" to "There is no submission with this id."
        )
    )

    private val serverErrorJson = JsonObject(
        mapOf(
            "code" to 500,
            "error_type" to "server",
            "message" to "Something on server went wrong."
        )
    )

    private val uploadingFileEmptyOrNotQrsJson = JsonObject(
        mapOf(
            "code" to 400,
            "error_type" to "client",
            "message" to "Uploading file is empty or not .qrs."
        )
    )

    private val submissionAlreadyAcceptedJson = JsonObject(
        mapOf(
            "code" to 422,
            "error_type" to "client",
            "message" to "Submission is already accepted."
        )
    )
}