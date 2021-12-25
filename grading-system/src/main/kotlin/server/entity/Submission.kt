package server.entity

import javax.persistence.*

@Entity
@Table(name = "t_submission")
class Submission(val filePath: String? = null) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
        private set

    var status: String = "running"
        private set

    fun changeStatus(newStatus: String) {
        status = newStatus
    }
}