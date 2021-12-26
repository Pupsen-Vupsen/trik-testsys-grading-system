package server.service

import server.entity.Submission
import server.repository.SubmissionRepository

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

    fun getAllSubmissions() = submissionRepository.findAll().toList()

    fun getSameRunningSubmissionOrNull(filePath: String) =
        submissionRepository.findSubmissionByFilePathAndStatus(filePath, "running")

    fun saveSubmission(submission: Submission): Long {
        submissionRepository.save(submission)
        return submission.id
    }

    @Async
    fun changeSubmissionStatus(id: Long) {
        val submission = submissionRepository.findSubmissionById(id)!!
        submission.changeStatus()
        submissionRepository.save(submission)
    }
}