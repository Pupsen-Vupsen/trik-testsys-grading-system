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
import java.util.concurrent.TimeUnit

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

    fun saveSubmission(submission: Submission): Submission {
        submissionRepository.save(submission)
        logger.info("[${submission.id}]: Saved to database.")

        try {
            val countOfTests = prepareForTesting(submission)
            submission.countOfTests = countOfTests
        } catch (e: Exception) {
            logger.error("[${submission.id}]: Error while preparing for testing: ${e.message}")
            submission.changeStatus(Status.ERROR)
            return submission
        }

        submissionRepository.save(submission)
        return submission
    }

    private fun prepareForTesting(submission: Submission): Int {
        val submissionId = submission.id
        logger.info("[$submissionId]: Started preparing for testing.")

        val submissionDir = Paths.SUBMISSIONS.text + "${submission.id}/"

        try {
            File(Paths.TASKS.text + findTaskName(submission.taskName)).copyRecursively(File(submissionDir))
        } catch (e: Exception) {
            logger.error("[$submissionId]: Error while copying task files. Can't find task starts with name ${submission.taskName}: ${e.message}")
            submission.changeStatus(Status.ERROR)
            return 0
        }
        logger.info("[$submissionId]: Copied tests poles and pin.txt.")

        File(submissionDir + "results/").mkdir()
        logger.info("[$submissionId]: Created dir for results.")

        val submissionFilePath = submissionDir + "submission" + FilePostfixes.QRS.text
        val testingFilePath = submissionDir + "testing" + FilePostfixes.QRS.text
        File(submissionFilePath).copyTo(File(testingFilePath))
        logger.info("[$submissionId]: Copied submission for testing.")

        return File(Paths.SUBMISSIONS.text + "$submissionId/" + Paths.TESTS.text).listFiles()?.size ?: run {
            logger.error("[$submissionId]: Tests not found. Something went wrong.")
            submission.changeStatus(Status.ERROR)
            return 0
        }
    }

    private fun findTaskName(taskPrefix: String): String {
        val tasksDir = File(Paths.TASKS.text)
        val taskName = tasksDir.listFiles()!!.find {
            it.name.startsWith(taskPrefix)
        }?.name ?: throw Exception("Can't find task, which starts with $taskPrefix.")
        return String(taskName.toByteArray(), charset("utf-8"))
    }

    data class TestingResults(val level: String, val message: String)

    @Async("testExecutor")
    fun testSubmission(submission: Submission) = testExecutor.execute {
        val submissionId = submission.id
        val submissionDir = Paths.SUBMISSIONS.text + "${submissionId}/"

        val testsDir = submissionDir + Paths.TESTS.text
        val resultFilesPath = submissionDir + Paths.RESULTS.text + FilePostfixes.RESULT.text

        logger.info("[$submissionId]: Started testing. Count of tests ${submission.countOfTests}.")
        submission.changeStatus(Status.ON_TESTING)
        try {
            File(testsDir).listFiles()?.forEach { poleFile ->
                val resultFilePath = resultFilesPath + "_" + poleFile.nameWithoutExtension + FilePostfixes.INFO.text

                logger.info("[$submissionId]: Trying to patch with test ${poleFile.name}.")
                val isSuccessfullyPatched = executePatcher(submissionId, poleFile.name)
                if (!isSuccessfullyPatched) {
                    logger.error("[$submissionId]: Error while patching with test ${poleFile.name}.")
                    submission.changeStatus(Status.ERROR)
                    return@forEach
                }
                logger.info("[$submissionId]: Patched with test ${poleFile.nameWithoutExtension}.")

                logger.info("[$submissionId]: Trying to execute on test ${poleFile.nameWithoutExtension}.")
                val isSuccessfullyExecuted = execute2DModel(submissionId, resultFilePath)
                if (!isSuccessfullyExecuted) {
                    logger.error("[$submissionId]: Error while executing on test ${poleFile.nameWithoutExtension}.")
                    submission.changeStatus(Status.ERROR)
                    return@forEach
                }
                logger.info("[$submissionId]: Executed on test ${poleFile.nameWithoutExtension}.")

                val logFile = File(resultFilePath)

                if (logFile.readBytes().isEmpty()) {
                    submission.deny()
                    logger.warn("[$submissionId]: Cannot generate correct log file.")
                } else {
                    val log = Klaxon().parseArray<TestingResults>(logFile)

                    if (log == null || log.last().level == "error") {
                        submission.deny()
                        logger.info("[$submissionId]: Failed test ${poleFile.name}.")
                    } else {
                        submission.countOfSuccessfulTests++
                        logger.info("[$submissionId]: Passed test ${poleFile.name}.")
                    }
                }
            } ?: run {
                logger.error("[$submissionId]: Tests not found. Something went wrong.")
                logger.error("[$submissionId]: Stopped testing.")

                submission.changeStatus(Status.ERROR)
                submissionRepository.save(submission)
            }

            if (submission.countOfSuccessfulTests == submission.countOfTests) {
                submission.accept()
                logger.info("[$submissionId]: Started generating hash and pin.")
                generateHashAndPin(submission)
                logger.info("[$submissionId]: Successfully generated hash and pin.")
            }
            logger.info("[$submissionId]: Successful tests ${submission.countOfSuccessfulTests}/${submission.countOfTests}.")
        } catch (e: Exception) {
            logger.error("[$submissionId]: Caught exception while testing file: ${e.message}")
            submission.changeStatus(Status.ERROR)
        }
        submissionRepository.save(submission)
        deleteTestingFiles(submissionId)
    }

    fun changeSubmissionStatus(id: Long, newStatus: Char) {
        val submission = getSubmissionOrNull(id)!!

        if (newStatus == '+') {
            logger.info("[$id]: Changed status to accepted.")
            submission.accept()

            logger.info("[$id]: Started generating hash and pin.")
            generateHashAndPin(submission)
        } else {
            logger.info("[$id]: Changed status to denied.")
            submission.deny()
        }

        submissionRepository.save(submission)
    }

    private fun deleteTestingFiles(id: Long) {
        val submissionDirPath = Paths.SUBMISSIONS.text + "$id/"

        File(submissionDirPath + Paths.TESTS.text).deleteRecursively()
        logger.info("[$id]: Deleted tests.")

        File(submissionDirPath + FilePostfixes.TESTING.text + FilePostfixes.QRS.text).delete()
        logger.info("[$id]: Deleted testing file.")
    }

    private fun executePatcher(submissionId: Long, poleFilename: String): Boolean {
        val submissionDir = Paths.SUBMISSIONS.text + "$submissionId/"
        val patcherProcess =
            Runtime
                .getRuntime()
                .exec(
                    "${TRIKCommands.PATCHER.command} $submissionDir${Paths.TESTS.text}/$poleFilename " +
                            "$submissionDir${FilePostfixes.TESTING.text}${FilePostfixes.QRS.text}"
                )
        return waitOrKillProcess(patcherProcess, 10)
    }

    private fun execute2DModel(submissionId: Long, resultPath: String): Boolean {
        val submissionDir = Paths.SUBMISSIONS.text + "$submissionId/"
        val twoDModelProcess =
            Runtime
                .getRuntime()
                .exec(
                    "${TRIKCommands.TWO_D_MODEL.command} $resultPath " +
                            "$submissionDir${FilePostfixes.TESTING.text}${FilePostfixes.QRS.text}"
                )

        return waitOrKillProcess(twoDModelProcess, 120)
    }

    private fun waitOrKillProcess(process: Process, timeout: Long): Boolean {
        if (!process.waitFor(timeout, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return false
        }
        return true
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
