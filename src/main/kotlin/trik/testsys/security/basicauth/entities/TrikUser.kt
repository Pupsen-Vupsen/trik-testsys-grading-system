package trik.testsys.security.basicauth.entities

import javax.persistence.*

@Entity
@Table(name = "TRIK_USERS")
class TrikUser(
    @Column(nullable = false) val username: String,
    @Column(nullable = false) val password: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    override fun toString(): String {
        return "TrikUser(id=$id, username='$username', password='$password')"
    }
}