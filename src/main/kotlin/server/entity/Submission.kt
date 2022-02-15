package server.entity

import com.beust.klaxon.Klaxon
import server.enum.Status

import java.io.File
import javax.persistence.*

@Entity
@Table(name = "SUBMISSIONS")
class Submission(
    private val taskName: String = "",
    private val fileName: String = ""
) {

    @Id
    @SequenceGenerator(name = "seq", initialValue = 1_000_000, allocationSize = 10_000)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq")
    val id: Long = 0

    private var taskPath = "./tasks/$taskName/"
    var testsPath = taskPath + "tests"
    var filePath = taskPath + fileName

    var status = Status.RUNNING.code
        private set

    var message = ""
        private set

    private var countOfTests = 0

    private var countOfSuccessfulTests = 0

    fun test() {
        class TestingResults(val level: String, val message: String)

        countOfTests = File(testsPath).listFiles()!!.size
        File(testsPath).listFiles()!!.forEach {testFile ->
            patchWithPole(testFile.absolutePath)
            run2DModelTest()

            val logFile = File("$filePath.info")

            while (!logFile.exists()) {
                Thread.sleep(10_000)
            }

            val log = Klaxon().parseArray<TestingResults>(logFile) ?: throw Exception("Log file doesn't exist")

            if (log[0].level == "error") deny()
            else countOfSuccessfulTests++

            logFile.delete()
        }

        accept()
        message = "Successful tests $countOfSuccessfulTests/$countOfTests"
    }

    private fun patchWithPole(poleFilePath: String) =
        Runtime.getRuntime().exec("C:\\TRIKStudio\\patcher.exe -f $poleFilePath $filePath")

    private fun run2DModelTest() =
        Runtime.getRuntime().exec("C:\\TRIKStudio\\2D-model.exe -b -r $filePath.info  $filePath")

    private fun accept() {
        status = Status.OK.code
    }

    private fun deny() {
        status = Status.FAILED.code
    }
}