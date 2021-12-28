package server.controller

import server.service.SubmissionService
import server.service.FileUploader

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

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

    @GetMapping("grading-system/submissions/submission")
    fun getSubmissionStatus(@RequestParam id: Long): ResponseEntity<String> {
        val submission = submissionService.getSubmissionOrNull(id)
            ?: return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body("Submission with id $id does not exist.")

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(submission.status.name)
    }

    /*@PostMapping("grading-system/submissions/submission")
    fun postSubmission(@RequestParam("file_path") filePath: String): ResponseEntity<Any> {
        submissionService.getSameRunningSubmissionOrNull(filePath)?.let {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body("You have already running submission on this task.")
        }

        val submissionId = submissionService.saveSubmission(Submission(filePath))
        submissionService.changeSubmissionStatus(submissionId)
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(submissionId)
    }*/

    @PostMapping("grading-system/submissions/submission")
    fun postSubmission(
        @RequestParam("task_name") taskName: String,
        @RequestParam("file_name") fileName: String,
        @RequestParam file: MultipartFile
    ): ResponseEntity<String> {
        val fileUploader = FileUploader(file, fileName)

        return try {
            if (fileUploader.upload(""))
                ResponseEntity
                    .status(HttpStatus.OK)
                    .body("You have successfully uploaded file $fileName to task $taskName.")
            else
                ResponseEntity
                    .status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body("Uploading file $fileName if empty.")
        } catch (e: Exception) {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(e.message)
        }
    }
}