package trik.testsys.gradingsystem.services

import com.beust.klaxon.Klaxon

import trik.testsys.gradingsystem.entities.Submission
import trik.testsys.gradingsystem.repositories.SubmissionRepository
import trik.testsys.gradingsystem.enums.*

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.stereotype.Service

import java.io.File
import java.security.MessageDigest
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
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
        val queuedSubmissions = submissionRepository.findSubmissionsByStatus(Status.QUEUED)?: emptyList()
        val runningSubmissions = submissionRepository.findSubmissionsByStatus(Status.RUNNING)?: emptyList()

        val submissions = queuedSubmissions + runningSubmissions

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
    }

    fun prepareForTesting(submission: Submission) {
        val submissionId = submission.id
        logger.info("[$submissionId]: Started preparing for testing.")

        val submissionDir = Paths.SUBMISSIONS.text + "${submission.id}/"

        try {
            File(Paths.TASKS.text + findTaskName(submission.taskName)).copyRecursively(File(submissionDir), true)
        } catch (e: Exception) {
            logger.error("[$submissionId]: Error while copying task files. Can't find task starts with name ${submission.taskName}: ${e.message}")
            submission.changeStatus(Status.ERROR)
            submission.countOfTests = null
            return
        }
        logger.info("[$submissionId]: Copied tests poles.")

        File(submissionDir + "results/").mkdir()
        logger.info("[$submissionId]: Created dir for results.")

        val submissionFilePath = submissionDir + "submission" + FilePostfixes.QRS.text
        val testingFilePath = submissionDir + "testing" + FilePostfixes.QRS.text

        if (!File(submissionFilePath).exists()) {
            logger.error("[$submissionId]: Can't find submission file.")
            submission.changeStatus(Status.ERROR)
            submission.countOfTests = null
            saveSubmission(submission)
            return
        }
        File(submissionFilePath).copyTo(File(testingFilePath), true)
        logger.info("[$submissionId]: Copied submission for testing.")

        submission.countOfTests =
            File(Paths.SUBMISSIONS.text + "$submissionId/" + Paths.TESTS.text).listFiles()?.size ?: run {
                logger.error("[$submissionId]: Tests not found. Something went wrong.")
                submission.changeStatus(Status.ERROR)
                saveSubmission(submission)
                null
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
        val submissionId = submission.id!!
        if (submission.countOfTests == null) {
            logger.warn("[$submissionId]: Tests wont start cause the error while preparing.")
            return@execute
        }
        val submissionDir = Paths.SUBMISSIONS.text + "${submissionId}/"

        val testsDir = submissionDir + Paths.TESTS.text
        val resultFilesPath = submissionDir + Paths.RESULTS.text + FilePostfixes.RESULT.text

        logger.info("[$submissionId]: Started testing. Count of tests ${submission.countOfTests}.")
        submission.changeStatus(Status.RUNNING)
        var trikMessage = "[ "
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
                    val log = Klaxon().parseArray<TestingResults>(logFile) ?: run {
                        submission.deny()
                        logger.warn("[$submissionId]: Cannot parse log file.")
                        return@forEach
                    }

                    var message = "{ "
                    log.forEach {
                        message += "\"${it.level}\": \"${it.message}\", "
                    }
                    message = message.dropLast(2) + "},"
                    trikMessage += message

                    if (log.any { it.level == "error" }) {
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
                logger.info("[$submissionId]: Started generating hash.")
                generateHash(submission)
                logger.info("[$submissionId]: Successfully generated hash.")
            }

            submission.trikMessage = trikMessage.dropLast(1) + "]"
            logger.info("[$submissionId]: Successful tests ${submission.countOfSuccessfulTests}/${submission.countOfTests}.")
        } catch (e: Exception) {
            logger.error("[$submissionId]: Caught exception while testing file: ${e.message}")
            submission.changeStatus(Status.ERROR)
        }
        submissionRepository.save(submission)
        deleteTestingFiles(submissionId)
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

        return waitOrKillProcess(twoDModelProcess, 180)
    }

    private fun waitOrKillProcess(process: Process, timeout: Long): Boolean {
        if (!process.waitFor(timeout, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return false
        }
        return true
    }

    private fun generateHash(submission: Submission) {
        val submissionFilePath = Paths.SUBMISSIONS.text + "${submission.id}/submission" + FilePostfixes.QRS.text

        val hash = MessageDigest
            .getInstance("SHA-256")
            .digest(File(submissionFilePath).readBytes())
            .fold("") { str, it -> str + "%02x".format(it) }

        submission.hash = hash
    }
}
