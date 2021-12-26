package server.controller

import server.entity.Submission
import server.service.SubmissionService

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class SubmissionController {

    @Autowired
    private lateinit var submissionService: SubmissionService

    @GetMapping("grading-system/submissions")
    fun getAllSubmission(): ResponseEntity<Any> {
        val submissions = submissionService.getAllSubmissions()

        if (submissions.isEmpty())
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body("There is no submissions.")

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(submissions)
    }

    @GetMapping("grading-system/submissions/submission")
    fun getSubmission(@RequestParam id: Int): ResponseEntity<String> {
        val submission = submissionService.getSubmissionOrNull(id)
            ?: return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body("Submission with id $id does not exist.")

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(submission.status)
    }

    @PostMapping("grading-system/submissions/submission")
    fun postSubmission(@RequestParam filePath: String): ResponseEntity<Any> {
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
    }
}