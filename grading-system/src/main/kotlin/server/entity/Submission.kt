package server.entity

import server.enum.Status

import javax.persistence.*

@Entity
@Table(name = "SUBMISSIONS")
class Submission(val filePath: String = "") {

    @Id
    @SequenceGenerator(name = "seq", initialValue = 1000000, allocationSize = 10000)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq")
    val id: Long = 0

    var status = Status.RUNNING
        private set

    fun test() {
        Runtime.getRuntime().exec("powershell.exe -File ./test_submission.ps1")

        /*val n = (0..1).random()
            TimeUnit.SECONDS.sleep(60)

            if (n == 0) accept()
            else deny()*/
    }

    private fun accept() {
        status = Status.OK
    }

    private fun deny() {
        status = Status.FAILED
    }
}