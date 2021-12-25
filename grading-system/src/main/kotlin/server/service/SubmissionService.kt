package server.service

import server.entity.Submission
import server.repository.SubmissionRepository

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SubmissionService {

    @Autowired
    lateinit var submissionRepository: SubmissionRepository

    fun getSubmission(id: Long) = submissionRepository.findSubmissionById(id)

    fun getAllSubmissions(): Iterable<Submission> = submissionRepository.findAll()

    fun saveSubmission(submission: Submission): Long? {
        submissionRepository.save(submission)
        return submission.id
    }

    fun acceptSubmission(id: Long) {
        submissionRepository.findSubmissionById(id)?.changeStatus("ok") ?: TODO()
    }

    fun denySubmission(id: Long) {
        submissionRepository.findSubmissionById(id)?.changeStatus("failed") ?: TODO()
    }
}