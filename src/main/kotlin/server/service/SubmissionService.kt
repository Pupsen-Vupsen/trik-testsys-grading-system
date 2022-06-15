package server.service

import com.beust.klaxon.Klaxon
import org.apache.tomcat.util.json.JSONParser
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
        logger.info("Copied tests poles and pin.txt.")

        val submissionFilePath = submissionDir + "/submission" + FilePostfixes.QRS.text
        val testingFilePath = submissionDir + "/testing" + FilePostfixes.QRS.text
        File(submissionFilePath).copyTo(File(testingFilePath))
        logger.info("Copied submission ${submission.id} for testing.")

        return File(taskPath + "/" + Paths.TESTS.text).listFiles()!!.size
    }

    data class TestingResults(val level: String, val message: String)

    @Async("testExecutor")
    fun testSubmission(id: Long) = testExecutor.execute {
        val submission = submissionRepository.findSubmissionById(id) ?: throw Exception("WTF?!")

        val testsDir = Paths.SUBMISSIONS.text + "${submission.id}/" + Paths.TESTS.text
        val logFilePath = Paths.SUBMISSIONS.text + "${submission.id}/" + FilePostfixes.RESULT.text

        logger.info("${submission.id}: Started testing. Count of tests ${submission.countOfTests}")
        try {
            File(testsDir).listFiles()!!.forEach { poleFile ->
                executePatcher(submission.id, poleFile.name)
                logger.info("${submission.id}: Patched with ${poleFile.name}.")

                execute2DModel(submission.id)
                logger.info("${submission.id}: Executed on test ${poleFile.name}.")

                val logFile = File(logFilePath)

                if (logFile.readBytes().isEmpty()) {
                    submission.deny()
                    logger.warn("${submission.id}: Cannot generate correct log file.")
                    logFile.delete()
                } else {
                    val log = Klaxon().parseArray<TestingResults>(logFile)
                    if (log == null || log[0].level == "error") {
                        logger.info("${submission.id}: Submission failed test ${poleFile.name}.")
                        submission.deny()
                    } else logger.info("${submission.id}: Submission passed test ${poleFile.name}.")
                }
            }

            if (submission.countOfSuccessfulTests == submission.countOfTests) {
                submission.accept()
                logger.info("${submission.id}: Successful test ${submission.countOfSuccessfulTests}/${submission.countOfTests}.")
            }

        } catch (e: Exception) {
            logger.error("${submission.id}: Caught exception! ${e.stackTraceToString()}")
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

    private fun generateHashAndPin() {
//        Runtime
//            .getRuntime()
//            .exec("./generate_hash.sh $filePath $hashAndPinPath")
//        Thread.sleep(5_000)
//
//        val hash = File(hashAndPinPath).readLines()[0].reversed()
//        var newHash = ""
//        for(i in 0..7) {
//            newHash += hash[i * 4]
//        }
//        val range = getPinRange()
//        val pin = newHash.toLong(16) % range.first + range.second
//
//        Runtime
//            .getRuntime()
//            .exec("./echo_pin.sh $pin $hashAndPinPath")
//        Thread.sleep(5_000)
//
//        val strings = File(hashAndPinPath).readLines()
//        this.hash = strings[0]
//        this.pin = strings[1]
//    }
//    private fun getPinRange(): Pair<Int, Int> {
//        val strings = File(pinRangeFile).readLines()
//
//        val pinRange = strings[0].toInt()
//        val firstPin = strings[1].toInt()
//
//        return pinRange to firstPin
//    }
    }
}