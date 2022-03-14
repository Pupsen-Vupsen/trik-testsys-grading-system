package server.entity

import server.constants.Constants.*

import com.beust.klaxon.Klaxon
import java.io.File
import javax.persistence.*

@Entity
@Table(name = "SUBMISSIONS")
class Submission(
    @Id val id: Long = 0,
    private val taskName: String = "",
    private val fileName: String = ""
) {

    private var taskPath = Paths.TASKS + taskName
    private var testsPath = taskPath + Paths.TESTS
    var filePath = "$taskPath/$fileName"

    var status = Status.RUNNING
        private set

    var message = ""
        private set

    private var countOfTests = 0

    private var countOfSuccessfulTests = 0

    fun test() {
        class TestingResults(val level: String, val message: String)
        countOfTests = File(testsPath).listFiles()!!.size
        File(testsPath).listFiles()!!.forEach { testFile ->
            executePatcher(testFile.absolutePath)
            execute2DModel()

            val logFile = File("$filePath.info")
            while (!logFile.exists()) {
                Thread.sleep(Time.WAIT_TIME)
            }

            if (logFile.readBytes().isEmpty()) {
                status = Status.FAILED
                message = "Testing file is bad for testing!"
                logFile.delete()
                return
            }

            val log = Klaxon().parseArray<TestingResults>(logFile)

            if (log == null || log[0].level == "error") deny()
            else countOfSuccessfulTests++
            logFile.delete()
        }

        if (countOfTests == 0) {
            execute2DModel()
            val logFile = File("$filePath.info")
            while (!logFile.exists()) {
                Thread.sleep(Time.WAIT_TIME)
            }

            if (logFile.readBytes().isEmpty()) {
                status = Status.FAILED
                message = "Testing file is bad for testing!"
                logFile.delete()
                return
            }

            val log = Klaxon().parseArray<TestingResults>(logFile)

            message = if (log == null || log[0].level == "error") {
                deny()
                "Task failed("
            } else {
                accept()
                log[0].message
            }
            logFile.delete()
        } else {
            if (countOfSuccessfulTests == countOfTests) accept()
            message = "Successful tests $countOfSuccessfulTests/$countOfTests"
        }
    }

    private fun executePatcher(poleFilePath: String) {
//        if (System.getProperty("os.name") == "Windows 10")
//            Runtime
//                .getRuntime()
//                .exec("${TRIKWindows.PATCHER} $poleFilePath $filePath")
//        else
        Runtime
            .getRuntime()
            .exec("${TRIKLinux.PATCHER} $poleFilePath $filePath")
    }

    private fun execute2DModel() {
//        if (System.getProperty("os.name") == "Windows 10")
//            Runtime
//                .getRuntime()
//                .exec("${TRIKWindows.TWO_D_MODEL} $filePath.info  $filePath")
//        else
        Runtime
            .getRuntime()
            .exec("${TRIKLinux.TWO_D_MODEL} $filePath.info  $filePath")
    }

    private fun accept() {
        status = Status.OK
    }

    private fun deny() {
        status = Status.FAILED
    }
}