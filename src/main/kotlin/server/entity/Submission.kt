package server.entity

import com.beust.klaxon.Klaxon
import server.enum.Status

import java.io.File
import javax.persistence.*

@Entity
@Table(name = "SUBMISSIONS")
class Submission(val filePath: String = "") {

    @Id
    @SequenceGenerator(name = "seq", initialValue = 1_000_000, allocationSize = 10_000)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq")
    val id: Long = 0

    var status = Status.RUNNING.code
        private set

    var message = ""
        private set

    fun test() {
        class TestingResults(val level: String, val message: String)

        Runtime.getRuntime().exec("C:\\TRIKStudio\\2D-model.exe -b -r $filePath.info  $filePath")
        Thread.sleep(10_000)

        val logFile = File("$filePath.info")

        /*while(!logFile.exists()) {
            logFile = File("$filePath.log")
        }*/

        val log = Klaxon().parseArray<TestingResults>(logFile)!!

        if(log[0].level == "error") deny()
        else accept()

        message = log[0].message
     //Runtime.getRuntime().exec("del ${logFile.absolutePath}")
    }

    private fun accept() {
        status = Status.OK.code
    }

    private fun deny() {
        status = Status.FAILED.code
    }
}