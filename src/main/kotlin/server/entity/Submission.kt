package server.entity

import server.enum.Status
import server.service.StatusConverter

import javax.persistence.*

@Entity
@Table(name = "SUBMISSIONS")
class Submission(
    @Id val id: Long,
    val taskName: String,
    val date: String
) {

    @Convert(converter = StatusConverter::class)
    var status = Status.QUEUED
        private set

    var countOfTests: Int? = null
    var countOfSuccessfulTests: Int = 0

    var pin: String? = null
    var hash: String? = null

    fun accept() {
        changeStatus(Status.ACCEPTED)
    }

    fun deny() {
        changeStatus(Status.FAILED)
    }

    fun changeStatus(status: Status) {
        this.status = status
    }
}