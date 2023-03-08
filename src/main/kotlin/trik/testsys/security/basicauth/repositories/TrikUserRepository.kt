package trik.testsys.security.basicauth.repositories

import trik.testsys.security.basicauth.entities.TrikUser

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.data.relational.core.mapping.Table


@Repository
@Table(name = "TRIK_USERS")
interface TrikUserRepository: CrudRepository<TrikUser, String> {

    fun getByUsername(username: String): TrikUser?
}