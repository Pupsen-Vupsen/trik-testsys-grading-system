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
    fun getAllSubmission(): ResponseEntity<Iterable<Submission>> {
        val submissions = submissionService.getAllSubmissions()
        return ResponseEntity.status(HttpStatus.OK).body(submissions)
    }

    @GetMapping("grading-system/submission")
    fun getSubmission(id: Long): ResponseEntity<String> {
        val submission = submissionService.getSubmission(id)
        return ResponseEntity.status(HttpStatus.OK).body(submission?.status)
    }

    @PutMapping("grading-system/submission")
    fun postSubmission(@RequestParam filePath: String): ResponseEntity<Long> {
        val submissionId = submissionService.saveSubmission(Submission(filePath))
        return ResponseEntity.status(HttpStatus.CREATED).body(submissionId)
    }
}