package trik.testsys.gradingsystem.entities

import com.beust.klaxon.JsonObject

import org.springframework.boot.context.properties.bind.Name

import trik.testsys.gradingsystem.enums.Status

import javax.persistence.*


@Entity
@Table(name = "SUBMISSIONS")
class Submission(
    @Column(nullable = false)
    @Name("task_name")
    val taskName: String,

    @Column(nullable = false)
    @Name("student_id")
    val studentId: String,

    val date: String
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        private set

    @Enumerated(EnumType.STRING)
    var status = Status.QUEUED
        private set

    @setparam:Name("count_of_tests")
    var countOfTests: Int? = null

    @setparam:Name("count_of_successful_tests")
    var countOfSuccessfulTests: Int = 0

    var hash: String? = null

    @setparam:Name("trik_message")
    var trikMessage: String? = null

    fun accept() {
        changeStatus(Status.ACCEPTED)
    }

    fun deny() {
        changeStatus(Status.FAILED)
    }

    fun changeStatus(status: Status) {
        this.status = status
    }

    fun toJsonObject(): JsonObject {
        return JsonObject(
            mapOf(
                "id" to id,
                "task_name" to taskName,
                "student_id" to studentId,
                "date" to date,
                "status" to status,
                "count_of_tests" to countOfTests,
                "count_of_successful_tests" to countOfSuccessfulTests,
                "hash" to hash,
                "trik_message" to trikMessage
            )
        )
    }
}