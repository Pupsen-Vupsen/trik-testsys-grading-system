package server.entity

import org.springframework.scheduling.annotation.Async
import java.util.concurrent.TimeUnit
import javax.persistence.*

@Entity
@Table(name = "SUBMISSIONS")
class Submission(val filePath: String? = null) {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private val tableId: Long = 0

    val id: Int = this.hashCode()

    var status: String = "running"
        private set

    fun changeStatus() {
        val n = (0..1).random()
        TimeUnit.SECONDS.sleep(15)

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