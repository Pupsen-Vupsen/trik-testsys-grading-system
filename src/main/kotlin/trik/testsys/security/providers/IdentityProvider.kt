package trik.testsys.security.providers

import trik.testsys.security.basicauth.services.TrikUserService
import trik.testsys.security.encoders.SHA256PasswordEncoder

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component


@Component
class IdentityProvider : AuthenticationProvider {

    @Autowired
    private lateinit var trikUserService: TrikUserService

    private val passwordEncoder: PasswordEncoder = SHA256PasswordEncoder()

    fun isValidUser(username: String, password: String): UserDetails? {
        val trikUser = trikUserService.loadUserByUsername(username)

        return if (passwordEncoder.matches(password, trikUser.password))
            User
                .withUsername(username)
                .password("NOT_DISCLOSED")
                .roles("USER")
                .build()
        else
            null
    }

    override fun authenticate(authentication: Authentication): Authentication {
        val username = authentication.name
        val password = authentication.credentials.toString()
        val userDetails = isValidUser(username, password)

        return if (userDetails != null) {
            UsernamePasswordAuthenticationToken(
                username,
                password,
                userDetails.authorities
            )
        } else {
            throw BadCredentialsException("Incorrect user credentials!!")
        }
    }

    override fun supports(authenticationType: Class<*>): Boolean {
        return (authenticationType == UsernamePasswordAuthenticationToken::class.java)
    }
}