package server.controller


import server.entity.Submission
import server.service.SubmissionService

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class SubmissionController {

    @Autowired
    private lateinit var submissionService: SubmissionService

    @GetMapping
    fun getAllSubmission(): ResponseEntity<Iterable<Submission>> {
        val submissions = submissionService.getAllSubmissions()
        return ResponseEntity.status(HttpStatus.CREATED).body(submissions)
    }

    @PostMapping
    fun postSubmission(@RequestBody filePath: String): ResponseEntity<Long> {
        val submissionId = submissionService.saveSubmission(Submission(filePath))
        return ResponseEntity.status(HttpStatus.OK).body(submissionId)
    }
}