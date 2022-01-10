package server.service

import server.entity.Submission
import server.repository.SubmissionRepository
import server.enum.Status

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.stereotype.Service

@Service
@EnableAsync
class SubmissionService {

    @Autowired
    lateinit var submissionRepository: SubmissionRepository

    fun getSubmissionOrNull(id: Long) = submissionRepository.findSubmissionById(id)

    fun getAllSubmissionsOrNull(): List<Submission>? {
        val submissions = submissionRepository.findAll().toList()
        if (submissions.isEmpty()) return null
        return submissions
    }

    fun getSameRunningSubmissionOrNull(filePath: String) =
        submissionRepository.findSubmissionByFilePathAndStatus(filePath, Status.RUNNING.code)

    fun saveSubmission(submission: Submission): Long {
        submissionRepository.save(submission)
        return submission.id
    }

    @Async
    fun testSubmission(id: Long) {
        val submission = submissionRepository.findSubmissionById(id)!!
        submission.test()
        submissionRepository.save(submission)
    }
}