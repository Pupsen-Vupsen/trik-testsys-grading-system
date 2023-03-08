package trik.testsys.security.basicauth.services

import trik.testsys.security.basicauth.entities.TrikUser
import trik.testsys.security.basicauth.repositories.TrikUserRepository

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
@EnableAsync
class TrikUserService : UserDetailsService {

    @Autowired
    private lateinit var trikUserRepository: TrikUserRepository

    fun getUserOrNull(username: String): TrikUser? {
        return trikUserRepository.getByUsername(username)
    }

    override fun loadUserByUsername(username: String): UserDetails {
        val user = trikUserRepository.getByUsername(username)
            ?: run {
                throw UsernameNotFoundException("User $username doesn't exist")
            }

        return User
            .withUsername(user.username)
            .password(user.password)
            .authorities("USER").build()
    }
}