package server.controller

import server.service.SubmissionService
import server.service.FileUploader
import server.entity.Submission
import server.enum.Requests

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import server.enum.Constants
import java.io.File
import java.io.FileInputStream

@RestController
class SubmissionController {

    @Autowired
    private lateinit var submissionService: SubmissionService

    @GetMapping("grading-system/submissions")
    fun getAllSubmissions(): ResponseEntity<Any> {
        val submissions = submissionService.getAllSubmissionsOrNull()
            ?: return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Requests.NO_SUBMISSIONS.text)

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(submissions)
    }

    @GetMapping("grading-system/submissions/submission/status")
    fun getSubmissionStatus(@RequestParam id: Long): ResponseEntity<String> {
        val submission = submissionService.getSubmissionOrNull(id)
            ?: return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Requests.SUBMISSION_NOT_FOUND.text)

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(submission.status)
    }

    @GetMapping("grading-system/submissions/submission/download")
    fun getSubmissionFile(@RequestParam id: Long): ResponseEntity<Any> {
        val submission = submissionService.getSubmissionOrNull(id)
            ?: return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Requests.SUBMISSION_NOT_FOUND.text)

        val stream = InputStreamResource(FileInputStream(File(submission.filePath)))

        return ResponseEntity.status(HttpStatus.OK).body(stream)
    }

    private var submissionId = Constants.DEFAULT_ID.value

    @PostMapping("grading-system/submissions/submission/upload")
    fun postSubmission(
        @RequestParam("task_name") taskName: String,
        @RequestParam file: MultipartFile
    ): ResponseEntity<Any> {
        if (submissionId == Constants.DEFAULT_ID.value)
            submissionId = submissionService.getLastSubmissionIdOrNull() ?: Constants.FIRST_ID.value
        submissionId++

        val fileName = "$submissionId.qrs"
        val fileUploader = FileUploader(file, "$taskName/$fileName")

        return try {
            if (fileUploader.upload()) {
                submissionService.saveSubmission(Submission(submissionId, taskName, fileName))
                submissionService.testSubmission(submissionId)

                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(submissionId)
            } else
                ResponseEntity
                    .status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Requests.EMPTY_FILE.text)
        } catch (e: Exception) {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(e.message)
        }
    }
}