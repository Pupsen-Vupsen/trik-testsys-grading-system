package server.service

import server.entity.Submission
import server.repository.SubmissionRepository
import server.constants.Constants.Status

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.stereotype.Service
import java.util.concurrent.Executor

@Service
@EnableAsync
class SubmissionService {

    @Autowired
    lateinit var submissionRepository: SubmissionRepository

    @Autowired
    lateinit var executor: Executor

    fun getSubmissionOrNull(id: Long) = submissionRepository.findSubmissionById(id)

    fun getAllSubmissionsOrNull(): List<Submission>? {
        val submissions = submissionRepository.findAll().toList()
        if (submissions.isEmpty()) return null
        return submissions
    }

    fun getLastSubmissionIdOrNull(): Long? {
        val submissions = getAllSubmissionsOrNull()
        return submissions?.maxByOrNull { it.id }?.id
    }

    fun getSameRunningSubmissionOrNull(filePath: String) =
        submissionRepository.findSubmissionByFilePathAndStatus(filePath, Status.RUNNING)

    fun saveSubmission(submission: Submission): Long {
        submissionRepository.save(submission)
        return submission.id
    }

    @Async
    fun testSubmission(id: Long) {
        executor.execute {
            val submission = submissionRepository.findSubmissionById(id)!!
            submission.test()
            submissionRepository.save(submission)
        }
    }
}