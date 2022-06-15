package server.entity

import server.enum.Status

import javax.persistence.*

@Entity
@Table(name = "SUBMISSIONS")
class Submission(
    @Id val id: Long,
    val taskName: String
) {

    var status = Status.RUNNING.symbol
        private set

    var countOfTests: Int? = null
    var countOfSuccessfulTests: Int = 0

    var pin: String? = null
    var hash: String? = null

fun accept() {
        status = Status.OK.symbol
    }

    fun deny() {
        status = Status.FAILED.symbol
    }
}