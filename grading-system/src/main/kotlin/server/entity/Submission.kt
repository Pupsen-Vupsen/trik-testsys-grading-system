package server.entity

import javax.persistence.*

@Entity
@Table(name = "t_submission")
class Submission(val filePath: String? = null) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    var status: String = "running"
}