package server.controller

import server.service.SubmissionService
import server.service.FileUploader
import server.entity.Submission

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
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
                .body("There is no submissions.")

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(submissions)
    }

    @GetMapping("grading-system/submissions/submission/status")
    fun getSubmissionStatus(@RequestParam id: Long): ResponseEntity<String> {
        val submission = submissionService.getSubmissionOrNull(id)
            ?: return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body("Submission with id $id does not exist.")

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(submission.status.code)
    }

    @GetMapping("grading-system/submissions/submission/download")
    fun getSubmissionFile(@RequestParam id: Long): ResponseEntity<Any> {
        val submission = submissionService.getSubmissionOrNull(id)
            ?: return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body("Submission with id $id does not exist.")

        val stream = InputStreamResource(FileInputStream(File(submission.filePath)))

        return ResponseEntity.status(HttpStatus.OK).body(stream)
    }

    @PostMapping("grading-system/submissions/submission/upload")
    fun postSubmission(
        @RequestParam("task_name") taskName: String,
        @RequestParam file: MultipartFile
    ): ResponseEntity<Any> {
        val fileUploader = FileUploader(file, taskName)

        return try {
            if (fileUploader.upload()) {
                val submissionId =
                    submissionService.saveSubmission(Submission("./tasks/" + taskName + "/" + file.originalFilename))

                submissionService.testSubmission(submissionId)

                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(submissionId)
            } else
                ResponseEntity
                    .status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body("Uploading file ${file.originalFilename} if empty.")
        } catch (e: Exception) {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(e.message)
        }
    }
}