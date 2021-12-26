package server.entity

import java.util.concurrent.TimeUnit
import javax.persistence.*

@Entity
@Table(name = "SUBMISSIONS")
class Submission(val filePath: String? = null) {

    @Id
    @SequenceGenerator(name = "seq", initialValue = 1000000, allocationSize = 10000)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator="seq")
    val id: Long = 0

    var status: String = "running"
        private set

    fun changeStatus() {
        val n = (0..1).random()
        TimeUnit.SECONDS.sleep(60)

        if (n == 0) accept()
        else deny()
    }

    private fun accept() {
        status = "ok"
    }

    private fun deny() {
        status = "failed"
    }
}