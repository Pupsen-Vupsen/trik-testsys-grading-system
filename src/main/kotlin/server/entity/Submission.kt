package server.entity

import server.constants.Constants.*

import com.beust.klaxon.Klaxon
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import javax.persistence.*

@Entity
@Table(name = "SUBMISSIONS")
class Submission(
    @Id val id: Long = 0,
    private val taskName: String = "",
    private val fileName: String = "",
    private val testingFileName: String = ""
) {
    private val taskPath = Paths.TASKS + taskName
    private val testsPath = taskPath + Paths.TESTS
    private val pinRangeFile = "$taskPath/pin.txt"

    val filePath = "$taskPath/$fileName"
    private val testingFilePath = "$taskPath/$testingFileName"

    var pin: String? = null
        private set
    var hash: String? = null
        private set

    private val hashAndPinPath = "$taskPath/${id}_hash_pin.txt"

    var status = Status.RUNNING
        private set

    var message = ""
        private set

    private var countOfTests = 0

    private var countOfSuccessfulTests = 0

    fun test() {
        copyForTesting()
        val logger: Logger = LoggerFactory.getLogger(Submission::class.java)
        countOfTests = File(testsPath).listFiles()!!.size

        class TestingResults(val level: String, val message: String)

        logger.info("Started testing submission $id. Count of tests = $countOfTests.")
        try {
            File(testsPath).listFiles()!!.forEach { testFile ->
                executePatcher(testFile.absolutePath)
                logger.info("Submission $id patched with test ${testFile.name}.")

                execute2DModel()
                logger.info("Submission $id was executed on test ${testFile.name}.")

                val logFile = File("$filePath.info")
                while (!logFile.exists()) {
                    Thread.sleep(Time.WAIT_TIME)
                }

                Thread.sleep(Time.WAIT_TIME)
                if (logFile.readBytes().isEmpty()) {
                    logger.warn("Submission $id can't be tested on test ${testFile.name}.")

                    deny()
                    message = "Testing file is bad for testing!"
                    logFile.delete()
                    return
                }

                val log = Klaxon().parseArray<TestingResults>(logFile)

                if (log == null || log[0].level == "error") {
                    logger.info("Submission failed test ${testFile.name}.")
                    deny()
                } else {
                    logger.info("Submission $id passed test ${testFile.name}.")
                    countOfSuccessfulTests++
                }
                logFile.delete()
            }

            if (countOfTests == 0) {
                logger.warn("There are no tests for submission $id.")
                logger.warn("Executing submission on it's own test.")
                execute2DModel()

                val logFile = File("$filePath.info")
                while (!logFile.exists()) {
                    Thread.sleep(Time.WAIT_TIME)
                }

                if (logFile.readBytes().isEmpty()) {
                    logger.warn("Submission $id can't be executed on it's own tests.")
                    status = Status.FAILED
                    message = "Testing file is bad for testing!"
                    logFile.delete()
                    return
                }

                val log = Klaxon().parseArray<TestingResults>(logFile)

                message = if (log == null || log[0].level == "error") {
                    logger.warn("Submission $id failed it's own tests.")
                    deny()
                    "Task failed("
                } else {
                    logger.info("Submission $id passed it's own tests.")
                    accept()
                    log[0].message
                }
                logFile.delete()
            } else {
                if (countOfSuccessfulTests == countOfTests) {
                    accept()
                    logger.info("Generating hash and pin for submission $id.")
                    generateHashAndPin()
                    logger.info("Generating completed!")
                }

                logger.info("Submission $id passed $countOfSuccessfulTests/$countOfTests tests.")
                message = "Successful tests $countOfSuccessfulTests/$countOfTests"
            }
        } catch (e: Exception) {
            logger.error("Caught exception: $e!")
            deny()
            message = "Testing file is not .qrs!"
        }
    }

    private fun executePatcher(poleFilePath: String) =
        Runtime
            .getRuntime()
            .exec("${TRIKLinux.PATCHER} $poleFilePath $testingFilePath")

    private fun execute2DModel() =
        Runtime
            .getRuntime()
            .exec("${TRIKLinux.TWO_D_MODEL} $filePath.info $testingFilePath")

    private fun copyForTesting() =
        Runtime
            .getRuntime()
            .exec("cp $filePath $testingFilePath")

    private fun generateHashAndPin() {
        Runtime
            .getRuntime()
            .exec("./generate_hash.sh $filePath $hashAndPinPath")
        Thread.sleep(5_000)

        val hash = File(hashAndPinPath).readLines()[0].reversed()
        var newHash = ""
        for(i in 0..7) {
            newHash += hash[i * 4]
        }
        val range = getPinRange()
        val pin = newHash.toLong(16) % range.first + range.second

        Runtime
            .getRuntime()
            .exec("./echo_pin.sh $pin $hashAndPinPath")
        Thread.sleep(5_000)

        val strings = File(hashAndPinPath).readLines()
        this.hash = strings[0]
        this.pin = strings[1]
    }
    private fun getPinRange(): Pair<Int, Int> {
        val strings = File(pinRangeFile).readLines()

        val pinRange = strings[0].toInt()
        val firstPin = strings[1].toInt()

        return pinRange to firstPin
    }

    private fun accept() {
        status = Status.OK
    }

    private fun deny() {
        status = Status.FAILED
    }
}