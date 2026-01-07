package com.locationapp

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import at.favre.lib.crypto.bcrypt.BCrypt
import java.time.LocalDateTime

fun hashPassword(password: String): String = BCrypt.withDefaults().hashToString(12, password.toCharArray());

fun verifyPassword(password: CharArray, hashed: CharArray): Boolean = BCrypt.verifyer().verify(password, hashed).verified
fun getUserByEmail(email: String): ResultRow? = transaction {
    Users.select { Users.email eq email }.singleOrNull()
}
fun isEmailAvailable(email: String): Boolean = getUserByEmail(email) == null

object Users : IntIdTable("users") {
    val email = varchar("email", 255)
    val passwordHash = varchar("password_hash", 255)
    val createdAt = datetime("created_at")
}

fun Application.configureRouting() {

    routing {
        post("/signup") {

            val email = call.request.queryParameters["email"] ?: ""
            val password = call.request.queryParameters["password"] ?: ""

            if (email.isNullOrBlank() || password.isNullOrBlank()) {
                call.respond("Email or password cannot be empty")
                return@post
            }

            if (!isEmailAvailable(email)) {
                call.respond("Email already exists")
                return@post
            }

            transaction {
                Users.insert {
                    it[Users.email] = email
                    it[Users.passwordHash] = hashPassword(password)
                    it[Users.createdAt] = LocalDateTime.now()
                }
            }

            call.respond("User created successfully")
        }

        post("/login") {
            val email = call.request.queryParameters["email"] ?: ""
            val password = call.request.queryParameters["password"] ?: ""

            if (email.isNullOrBlank() || password.isNullOrBlank()) {
                call.respond("Email or password cannot be empty")
                return@post
            }

            val user = getUserByEmail(email)

            if (user == null) {
                call.respond("Incorrect login or password")
                return@post
            }

            val hash = user[Users.passwordHash]

            if (!verifyPassword(password.toCharArray(), hash.toCharArray())) {
                call.respond("Incorrect login or password")
                return@post
            }

            call.respond("Login successful")
        }

    }
}
