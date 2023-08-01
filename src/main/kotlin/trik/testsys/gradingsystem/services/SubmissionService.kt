package trik.testsys.gradingsystem.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import trik.testsys.grading.Grader
import trik.testsys.gradingsystem.entities.Submission
import trik.testsys.gradingsystem.enums.FilePostfixes
import trik.testsys.gradingsystem.enums.Paths
import trik.testsys.gradingsystem.enums.Status
import trik.testsys.gradingsystem.repositories.SubmissionRepository
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter


@Service
@EnableAsync
class SubmissionService {

    val logger: Logger = LoggerFactory.getLogger(Submission::class.java)

    @Autowired
    lateinit var submissionRepository: SubmissionRepository

    private final val grader = Grader(Paths.GRADER_CONFIG.text)

    init {
        grader.gradingFinishedEvent += ::saveSubmission
    }

    fun getSubmissionOrNull(id: Long) = submissionRepository.findSubmissionById(id)

    fun getSubmissionFileOrNull(id: Long): File? {
        val submission = getSubmissionOrNull(id)

        return if (submission != null) {
            File(Paths.SUBMISSIONS.text + "${submission.id}/submission" + FilePostfixes.QRS.text)
        } else {
            null
        }
    }

    fun getAllSubmissionsOrNull(): List<Submission>? {
        val submissions = submissionRepository.findAll().toList()
        if (submissions.isEmpty()) return null
        return submissions
    }

    fun getAllUnprocessedSubmissionsOrNull(): List<Submission>? {
        val submissions = submissionRepository.findAll().toList().filter {
            it.status == Status.QUEUED || it.status == Status.RUNNING
        }

        if (submissions.isEmpty()) return null
        return submissions
    }

    fun getAllProcessedSubmissionsOrNull(): List<Submission>? {
        val submissions = submissionRepository.findAll().toList().filter {
            it.status == Status.ACCEPTED || it.status == Status.FAILED || it.status == Status.ERROR
        }

        if (submissions.isEmpty()) return null
        return submissions
    }

    fun saveSubmission(taskName: String, studentId: String): Submission {
        val date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) +
                "-" +
                LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        val submission = Submission(taskName, studentId, date)

        return submissionRepository.save(submission)
    }

    fun saveSubmission(submission: Submission): Submission {
        return submissionRepository.save(submission)
    }

    fun refreshSubmission(submission: Submission) {
        submission.changeStatus(Status.QUEUED)
        submission.countOfSuccessfulTests = 0
        saveSubmission(submission)
        testSubmission(submission)
    }

    fun testSubmission(submission: Submission) {
        grader.grade(submission)
    }

    @Scheduled(fixedDelay = 1000)
    private fun poll() {
        grader.poll()
    }
}
