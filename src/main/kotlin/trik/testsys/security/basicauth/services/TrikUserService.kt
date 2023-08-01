package trik.testsys.security.basicauth.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import trik.testsys.security.basicauth.entities.TrikUser
import trik.testsys.security.basicauth.repositories.TrikUserRepository
import javax.annotation.PostConstruct

@Service
@EnableAsync
class TrikUserService : UserDetailsService {

    @Autowired
    private lateinit var trikUserRepository: TrikUserRepository

    @PostConstruct
    fun init() {
        if (getUserOrNull("test") == null)
            trikUserRepository.save(TrikUser("test", "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08"))
    }

    fun getUserOrNull(username: String): TrikUser? {
        return trikUserRepository.getByUsername(username)
    }

    fun hasUsers(): Boolean {
        return trikUserRepository.count() > 0
    }

    fun countUsers(): Long {
        return trikUserRepository.count()
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