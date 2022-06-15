package server.service

import com.beust.klaxon.Klaxon

import server.entity.Submission
import server.repository.SubmissionRepository
import server.enum.*

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
        logger.info("[$id]: Copied tests poles and pin.txt.")

        val submissionFilePath = submissionDir + "/submission" + FilePostfixes.QRS.text
        val testingFilePath = submissionDir + "/testing" + FilePostfixes.QRS.text
        File(submissionFilePath).copyTo(File(testingFilePath))
        logger.info("[$id]: Copied submission for testing.")

        return File(taskPath + "/" + Paths.TESTS.text).listFiles()!!.size
    }

    data class TestingResults(val level: String, val message: String)

    @Async("testExecutor")
    fun testSubmission(id: Long) = testExecutor.execute {
        val submission = submissionRepository.findSubmissionById(id) ?: throw Exception("WTF?!")

        val testsDir = Paths.SUBMISSIONS.text + "${submission.id}/" + Paths.TESTS.text
        val logFilePath = Paths.SUBMISSIONS.text + "${submission.id}/" + FilePostfixes.RESULT.text

        logger.info("[$id]: Started testing. Count of tests ${submission.countOfTests}.")
        try {
            File(testsDir).listFiles()!!.forEach { poleFile ->
                executePatcher(submission.id, poleFile.name)
                logger.info("[$id]: Patched with ${poleFile.name}.")

                execute2DModel(submission.id)
                logger.info("[$id]: Executed on test ${poleFile.name}.")

                val logFile = File(logFilePath)

                if (logFile.readBytes().isEmpty()) {
                    submission.deny()
                    logger.warn("[$id]: Cannot generate correct log file.")
                    logFile.delete()
                } else {
                    val log = Klaxon().parseArray<TestingResults>(logFile)
                    if (log == null || log[0].level == "error") {
                        logger.info("[$id]: Submission failed test ${poleFile.name}.")
                        submission.deny()
                    } else {
                        submission.countOfSuccessfulTests++
                        logger.info("[$id]: Submission passed test ${poleFile.name}.")
                    }
                }
            }

            if (submission.countOfSuccessfulTests == submission.countOfTests) {
                submission.accept()
                logger.info("[$id]: Started generating hash and pin.")
                generateHashAndPin(submission)
                logger.info("[$id]: Successfully generated hash and pin.")
            }
            logger.info("[$id]: Successful tests ${submission.countOfSuccessfulTests}/${submission.countOfTests}.")
        } catch (e: Exception) {
            logger.error("[$id]: Caught exception while testing file: ${e.stackTraceToString()}!")
            submission.deny()
        }
        submissionRepository.save(submission)
    }

    private fun executePatcher(submissionId: Long, poleFilename: String) {
        val submissionDir = Paths.SUBMISSIONS.text + "$submissionId/"
        Runtime
            .getRuntime()
            .exec(
                "${TRIKLinux.PATCHER.command} $submissionDir${Paths.TESTS}/$poleFilename " +
                        "$submissionDir${FilePostfixes.TESTING.text}${FilePostfixes.QRS.text}"
            ).waitFor()
        Thread.sleep(10_000)
    }

    private fun execute2DModel(submissionId: Long) {
        val submissionDir = Paths.SUBMISSIONS.text + "$submissionId/"
        Runtime
            .getRuntime()
            .exec(
                "${TRIKLinux.TWO_D_MODEL.command} $submissionDir${FilePostfixes.RESULT.text} " +
                        "$submissionDir${FilePostfixes.TESTING.text}${FilePostfixes.QRS.text}"
            ).waitFor()
    }

    private fun generateHashAndPin(submission: Submission) {
        val submissionFilePath = Paths.SUBMISSIONS.text + "${submission.id}/submission" + FilePostfixes.QRS.text
        val hashAndPinFilePath = Paths.SUBMISSIONS.text + "${submission.id}/" + FilePostfixes.HASH_PIN_TXT.text
        Runtime
            .getRuntime()
            .exec("./generate_hash.sh $submissionFilePath $hashAndPinFilePath")
            .waitFor()

        val hash = File(hashAndPinFilePath).readLines()[0].reversed()
        var newHash = ""
        for (i in 0..7) {
            newHash += hash[i * 4]
        }
        val range = getPinRange(submission.id)
        val pin = newHash.toLong(16) % range.first + range.second

        Runtime
            .getRuntime()
            .exec("./echo_pin.sh $pin $hashAndPinFilePath")
            .waitFor()

        val strings = File(hashAndPinFilePath).readLines()
        submission.hash = strings[0]
        submission.pin = strings[1]
    }

    private fun getPinRange(submissionId: Long): Pair<Long, Long> {
        val pinRangeFilePath = Paths.SUBMISSIONS.text + "$submissionId/" + Paths.PIN.text
        val strings = File(pinRangeFilePath).readLines()

        val pinRange = strings[0].toLong()
        val firstPin = strings[1].toLong()

        return pinRange to firstPin
    }
}
