package server.entity

import com.beust.klaxon.Klaxon
import server.enum.Constants
import server.enum.Paths
import server.enum.Status
import server.enum.TRIK

import java.io.File
import javax.persistence.*

@Entity
@Table(name = "SUBMISSIONS")
class Submission(
    @Id val id: Long = 0,
    private val taskName: String = "",
    private val fileName: String = ""
) {

    private var taskPath = Paths.TASKS.text + taskName
    private var testsPath = taskPath + Paths.TESTS.text
    var filePath = "$taskPath/$fileName"

    var status = Status.RUNNING.code
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
                Thread.sleep(Constants.WAIT_TIME.value)
            }

            val log = Klaxon().parseArray<TestingResults>(logFile)

            if (log == null || log[0].level == "error") deny()
            else countOfSuccessfulTests++
            logFile.delete()
        }

        if (countOfSuccessfulTests == countOfTests) accept()
        message = "Successful tests $countOfSuccessfulTests/$countOfTests"
    }

    private fun executePatcher(poleFilePath: String) =
        Runtime.getRuntime().exec("${TRIK.TWO_D_MODEL.text} $poleFilePath $filePath")

    private fun execute2DModel() =
        Runtime.getRuntime().exec("${TRIK.PATCHER.text} $filePath.info  $filePath")

    private fun accept() {
        status = Status.OK.code
    }

    private fun deny() {
        status = Status.FAILED.code
    }
}