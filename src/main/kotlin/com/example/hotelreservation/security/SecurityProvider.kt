package com.example.hotelreservation.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.core.env.Environment
import org.springframework.core.env.getProperty
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import java.util.*

@Component
class SecurityProvider(val env : Environment) {

    private val jwtSecret = env.getProperty<String>("spring.security.jwt.secret")
    private val expiration = env.getProperty<Long>("spring.security.jwt.expiration")

    companion object {
        val SIGNATURE_ALG: SignatureAlgorithm = SignatureAlgorithm.HS512
    }

    suspend fun generateToken(userId: Long): String {
        val claims: Claims = Jwts.claims()
        claims["user_id"] = userId
        return Jwts.builder().setClaims(claims).setExpiration(Date(System.currentTimeMillis() + expiration!!))
            .signWith(SIGNATURE_ALG, jwtSecret)
            .compact()
    }

    suspend fun validateToken(token: String): Boolean {
        val claims: Claims = getAllClaims(token)
        val exp: Date = claims.expiration
        return exp.after(Date())
    }

    suspend fun parseUserId(token: String): Long {
        val claims: Claims = getAllClaims(token)
        return claims["user_id"] as Long
    }

    suspend fun hashPassword(password: String): String {
        val round = env.getProperty<Int>("spring.security.password.hash.round")
        return BCrypt.hashpw(password, BCrypt.gensalt(round!!))
    }

    suspend fun validatePassword(plainPassword : String, hashedPassword : String): Boolean{
        return BCrypt.checkpw(plainPassword, hashedPassword)
    }

    fun getAllClaims(token: String): Claims {
        return Jwts.parser().setSigningKey(jwtSecret).parseClaimsJwt(token).body
    }

    suspend fun getAuthToken(request: ServerRequest): String {
        return request.headers().firstHeader("Authorization") ?: throw Error("NO_TOKEN")
    }



}