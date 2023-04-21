package trik.testsys.gradingsystem.controllers

import trik.testsys.gradingsystem.services.SubmissionService
import trik.testsys.gradingsystem.enums.*

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.FileInputStream

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import java.io.File
import java.io.FileWriter


@RequestMapping("/v3/grading-system/submissions")
@RestController
class SubmissionController {

    val logger: Logger = LoggerFactory.getLogger(SubmissionController::class.java)

    @Autowired
    private lateinit var submissionService: SubmissionService

    @GetMapping("/info/all")
    fun getAllSubmissions(): ResponseEntity<JsonArray<JsonObject>> {
        logger.info("Client requested all submissions.")

        val submissionsJson = JsonArray<JsonObject>()
        submissionService.getAllSubmissionsOrNull()?.forEach {
            submissionsJson.add(it.toJsonObject())
        } ?: run {
            logger.warn("There are no submissions.")

            return ResponseEntity
                .status(HttpStatus.OK)
                .body(submissionsJson)
        }

        logger.info("Returned all submissions.")
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(submissionsJson)
    }

    @GetMapping("/status")
    fun getArraySubmissionsStatus(@RequestParam("id_array") idArray: List<Long>): ResponseEntity<JsonArray<Any>> {
        logger.info("Client requested submissions statuses.")

        val submissionsStatuses = mutableMapOf<Long, Int?>()

        idArray.forEach {
            val submission = submissionService.getSubmissionOrNull(it) ?: run {
                logger.warn("There is no submission with id $it.")
                submissionsStatuses[it] = null
                return@forEach
            }

            submissionsStatuses[it] = submission.status.code
        }

        logger.info("Returned submissions statuses.")
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(convertFromMutableMapToJsonArray(submissionsStatuses))
    }

    private fun convertFromMutableMapToJsonArray(map: MutableMap<Long, Int?>): JsonArray<Any> {
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
                    .body(Jsons.thereIsNoSubmissionJson)
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
                    .body(Jsons.thereIsNoSubmissionJson)
            }


        logger.info("[$id]: Returned submission info.")
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(submission.toJsonObject())
    }

    @GetMapping("/info")
    fun getArraySubmissionsInfo(@RequestParam("id_array") idArray: List<Long>): ResponseEntity<JsonArray<JsonObject>> {
        logger.info("Client requested submissions info.")

        val submissionsInfo = JsonArray<JsonObject>()

        idArray.forEach {
            val submission = submissionService.getSubmissionOrNull(it) ?: run {
                logger.warn("There is no submission with id $it.")
                submissionsInfo.add(returnEmptySubmission(it))
                return@forEach
            }

            submissionsInfo.add(submission.toJsonObject())
        }

        logger.info("Returned submissions info.")
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(submissionsInfo)
    }

    private fun returnEmptySubmission(id: Long) = JsonObject(
        mapOf(
            "id" to id,
            "task_name" to null,
            "student_id" to null,
            "date" to null,
            "status" to null,
            "count_of_tests" to null,
            "count_of_successful_tests" to null,
            "hash" to null,
            "trik_message" to null
        )
    )

    @GetMapping("/submission/download")
    fun getSubmissionFile(@RequestParam id: Long): ResponseEntity<Any> {
        logger.info("[$id]: Client requested submission file.")

        val submissionFile = submissionService.getSubmissionFileOrNull(id)
            ?: run {
                logger.warn("[$id]: There is no submission with this id.")
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Jsons.thereIsNoSubmissionJson)
            }

        val stream = try {
            InputStreamResource(FileInputStream(submissionFile))
        } catch (e: Exception) {
            logger.error("[$id]: Caught exception while returning file: ${e.stackTraceToString()}!")
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Jsons.serverErrorJson)
        }

        logger.info("[$id]: Returned submission file.")
        return ResponseEntity
            .status(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header("Content-Disposition", "attachment; filename=\"${id}.qrs\"")
            .body(stream)

    }

    @PostMapping("/submission/upload")
    fun postSubmission(
        @RequestParam("task_name") taskName: String,
        @RequestParam("student_id") studentId: String,
        @RequestBody file: MultipartFile
    ): ResponseEntity<JsonObject> {
        logger.info("Got file!")

        val submission = submissionService.saveSubmission(taskName, studentId)
        val submissionId = submission.id!!
        val qrsUploader = QrsUploader(file, submissionId)
        return try {
            if (qrsUploader.upload()) {
                logger.info("[${submissionId}]: Set id to new file.")
                submissionService.prepareForTesting(submission)
                submissionService.testSubmission(submission)

                logger.info("[${submissionId}]: Saved submission.")

                val submissionJson = JsonObject(
                    mapOf(
                        "id" to submissionId,
                        "status" to submission.status,
                        "student_id" to submission.studentId,
                        "task_name" to taskName,
                        "date" to submission.date
                    )
                )
                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(submissionJson)
            } else {
                logger.warn("[$submissionId]: Uploading file is empty or not .qrs file.")

                submission.changeStatus(Status.FAILED)
                submissionService.saveSubmission(submission)
                ResponseEntity
                    .status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Jsons.uploadingFileEmptyOrNotQrsJson)
            }
        } catch (e: Exception) {
            logger.error("[${submissionId}]: Caught exception while uploading file: ${e.stackTraceToString()}!")

            submission.changeStatus(Status.ERROR)
            submissionService.saveSubmission(submission)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Jsons.serverErrorJson)
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
                    .body(Jsons.thereIsNoSubmissionJson)
            }

        if (submission.status == Status.QUEUED || submission.status == Status.RUNNING) {
            logger.warn("[$id]: Submission is queued or testing.")
            return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Jsons.submissionIsNotTestedYetJson)
        }

        submissionService.refreshSubmission(submission)
        submissionService.prepareForTesting(submission)
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

    // TODO: Add to documentation
    @PatchMapping("/recheck")
    fun recheckArraySubmissions(@RequestParam("idArray") idArray: List<Long>): ResponseEntity<JsonArray<Any>> {
        logger.info("Client requested recheck submissions.")

        val submissionStatuses = mutableMapOf<Long, Int?>()

        idArray.forEach { id ->
            val submission = submissionService.getSubmissionOrNull(id)
                ?: run {
                    logger.warn("[$id]: There is no submission with this id.")
                    submissionStatuses[id] = null
                    return@forEach
                }

            if (submission.status == Status.QUEUED || submission.status == Status.RUNNING) {
                logger.warn("[$id]: Submission is queued or testing.")
                submissionStatuses[id] = submission.status.code
                return@forEach
            }
            submissionService.refreshSubmission(submission)
            submissionService.prepareForTesting(submission)
            submissionService.testSubmission(submission)

            submissionStatuses[id] = submission.status.code

            logger.info("[$id]: Saved rechecking submission.")
        }

        val submissionsJson = convertFromMutableMapToJsonArray(submissionStatuses)

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(submissionsJson)
    }

    //  Returns csv file with header: id, student_id, task_name, date, status, using pure kotlin
    @GetMapping("/info/table")
    fun getCsvResultsFile(): ResponseEntity<InputStreamResource> {
        logger.info("Client requested csv results file.")

        val processedSubmissions = submissionService.getAllProcessedSubmissionsOrNull()

        val csvFile = File("results.csv")
        val csvWriter = FileWriter(csvFile)
        val header = arrayOf("id", "student_id", "task_name", "date", "status")

        header.forEach {
            csvWriter.write(it)
            csvWriter.write(",")
        }
        csvWriter.write("\n")

        processedSubmissions?.forEach { submission ->
            val row = arrayOf(
                submission.id,
                submission.studentId,
                submission.taskName,
                submission.date,
                submission.status
            )

            row.forEach {
                csvWriter.write(it.toString())
                csvWriter.write(",")
            }
            csvWriter.write("\n")
        }
        csvWriter.close()

        val stream = InputStreamResource(FileInputStream(csvFile))
        return ResponseEntity
            .status(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header("Content-Disposition", "attachment; filename=\"results.csv\"")
            .body(stream)
    }
}