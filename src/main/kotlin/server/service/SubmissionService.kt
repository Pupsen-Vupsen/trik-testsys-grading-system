package server.service

import server.entity.Submission
import server.repository.SubmissionRepository
import server.enum.FilePostfixes
import server.enum.Paths
import server.enum.TRIKLinux

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.Executor

@Service
@EnableAsync
class SubmissionService {

    val logger: Logger = LoggerFactory.getLogger(Submission::class.java)

    @Autowired
    lateinit var submissionRepository: SubmissionRepository

    @Autowired
    lateinit var testExecutor: Executor

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

    fun saveSubmission(submission: Submission): Long {
        submissionRepository.save(submission)

        val countOfTests = prepareForTesting(submission.id)
        submission.countOfTests = countOfTests

        submissionRepository.save(submission)
        return submission.id
    }

    private fun prepareForTesting(id: Long): Int {
        val submission = submissionRepository.findSubmissionById(id) ?: throw Exception("WTF?!")

        val taskPath = Paths.TASKS.text + submission.taskName
        val submissionDir = Paths.SUBMISSIONS.text + "${submission.id}"
        File(taskPath).copyRecursively(File(submissionDir))
        logger.info("Copied tests poles and pin.txt.")

        val submissionFilePath = submissionDir + "/submission" + FilePostfixes.QRS.text
        val testingFilePath = submissionDir + "/testing" + FilePostfixes.QRS.text
        File(submissionFilePath).copyTo(File(testingFilePath))
        logger.info("Copied submission ${submission.id} for testing.")

        return File(taskPath + "/" + Paths.TESTS.text).listFiles()!!.size
    }

    @Async("testExecutor")
    fun testSubmission(id: Long) = testExecutor.execute {
        val submission = submissionRepository.findSubmissionById(id) ?: throw Exception("WTF?!")

        logger.info("Started testing submission $id.")
//        submission.test()
        submissionRepository.save(submission)
    }

    private fun executePatcher(submissionId: Long, poleFilename: String) {
        val submissionDir = Paths.SUBMISSIONS.text + "$submissionId/"
        Runtime
            .getRuntime()
            .exec(
                "${TRIKLinux.PATCHER.command} $submissionDir${Paths.TESTS}/$poleFilename " +
                        "$submissionDir${FilePostfixes.TESTING.text}${FilePostfixes.QRS.text}"
            )
    }

    private fun execute2DModel(submissionId: Long) {
        val submissionDir = Paths.SUBMISSIONS.text + "$submissionId"
        Runtime
            .getRuntime()
            .exec(
                "${TRIKLinux.TWO_D_MODEL.command} $submissionDir${FilePostfixes.RESULT.text} " +
                        "$submissionDir${FilePostfixes.TESTING.text}${FilePostfixes.QRS.text}"
            )
        File("asdad").listFiles()
    }
}