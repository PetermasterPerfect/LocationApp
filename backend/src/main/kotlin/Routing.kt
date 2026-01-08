package com.locationapp

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.http.content.staticFiles
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import java.io.File
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.time.LocalDateTime
import java.util.Base64
import java.util.Date
import java.util.concurrent.TimeUnit

object Users : IntIdTable("users") {
    val email = varchar("email", 255)
    val passwordHash = varchar("password_hash", 255)
    val createdAt = datetime("created_at")
}

object Devices : IntIdTable("devices") {
    val userId = integer("user_id")
    val deviceUuid = varchar("device_uuid", 255)
    val deviceName = varchar("device_name", 255)
    val platform = varchar("platform", 255)
    val createdAt = datetime("created_at")
}


fun hashPassword(password: String): String = BCrypt.withDefaults().hashToString(12, password.toCharArray());

fun verifyPassword(password: CharArray, hashed: CharArray): Boolean = BCrypt.verifyer().verify(password, hashed).verified

fun getUserByEmail(email: String): ResultRow? = transaction {
    Users.select { Users.email eq email }.singleOrNull()
}

fun getDevicesByUser(userid: Int): List<ResultRow> = transaction {
    Devices.select { Devices.userId eq userid }.toList()
}


fun Application.loadPublicKey(): RSAPublicKey {
    val publicKeyString = environment.config.property("jwt.publicKey").getString().replace("\\s".toRegex(), "")
    val keySpecX509 = X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyString))
    return KeyFactory.getInstance("RSA").generatePublic(keySpecX509) as RSAPublicKey
}

fun Application.loadPrivateKey(): RSAPrivateKey {
    val privateKeyString = environment.config.property("jwt.privateKey").getString().replace("\\s".toRegex(), "")
    val keySpecPKCS8 = PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyString))
    return KeyFactory.getInstance("RSA").generatePrivate(keySpecPKCS8) as RSAPrivateKey
}

fun Application.configureRouting() {
    val issuer = environment.config.property("jwt.issuer").getString()
    val audience = environment.config.property("jwt.audience").getString()
    val myRealm = environment.config.property("jwt.realm").getString()
    val jwkProvider = JwkProviderBuilder(issuer)
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()
    install(ContentNegotiation) {
        json()
    }
    install(Authentication) {
        jwt("auth-jwt") {
            realm = myRealm
            verifier(jwkProvider, issuer) {
                withAudience(audience)
                acceptLeeway(3)
            }
            validate { credential ->
                println("JWT payload = ${credential.payload}")
                JWTPrincipal(credential.payload)
            }
            challenge { defaultScheme, realm ->
                println("jwt challenge")
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
            }
        }
    }


    routing {
        post("/signup") {

            val email = call.request.queryParameters["email"] ?: ""
            val password = call.request.queryParameters["password"] ?: ""

            if (email.isNullOrBlank() || password.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Email or password cannot be empty")
                return@post
            }

            if (getUserByEmail(email) != null) {
                call.respond(HttpStatusCode.Conflict, "Email already exists")
                return@post
            }

            transaction {
                Users.insert {
                    it[Users.email] = email
                    it[Users.passwordHash] = hashPassword(password)
                    it[Users.createdAt] = LocalDateTime.now()
                }
            }

            call.respond(HttpStatusCode.Created, "User created successfully")
        }

        post("/login") {
            val email = call.request.queryParameters["email"] ?: ""
            val password = call.request.queryParameters["password"] ?: ""

            if (email.isNullOrBlank() || password.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Email or password cannot be empty")
                return@post
            }

            val user = getUserByEmail(email)

            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, "Incorrect login or password")
                return@post
            }

            val hash = user[Users.passwordHash]

            if (!verifyPassword(password.toCharArray(), hash.toCharArray())) {
                call.respond(HttpStatusCode.Unauthorized, "Incorrect login or password")
                return@post
            }

            val publicKey = jwkProvider.get("6f8856ed-9189-488f-9011-0ff4b6c08edc").publicKey as RSAPublicKey
            val privateKey = loadPrivateKey()
            val token = JWT.create()
                .withAudience(audience)
                .withIssuer(issuer)
                .withClaim("userid", user[Users.id].toString())
                .withKeyId("6f8856ed-9189-488f-9011-0ff4b6c08edc")
                .withExpiresAt(Date(System.currentTimeMillis() + 60000))
                .sign(Algorithm.RSA256(publicKey, privateKey))

            println("token = $token")
            call.respond(HttpStatusCode.OK,hashMapOf("token" to token))

            //call.respond(HttpStatusCode.OK,"Login successful")
        }

        authenticate("auth-jwt") {
            get("/devices") {
                    call.respond("DEVICES")
                }

            post("/devices") {
                val uuid = call.request.queryParameters["uuid"] ?: ""
                val name = call.request.queryParameters["name"] ?: ""
                val platform = call.request.queryParameters["platform"] ?: ""

                if (uuid.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "Cannot register device");
                    return@post
                }
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userid").asString().toInt()
                val devices = getDevicesByUser(userId)
                if(devices != null) {
                    for (device in devices) {
                        if (device[Devices.deviceUuid] == uuid) {
                            call.respond(HttpStatusCode.Conflict, "Uuid already exists")
                            return@post
                        }
                    }
                }

                transaction {
                    Devices.insert {
                        it[Devices.userId] = userId
                        it[Devices.deviceUuid] = uuid
                        it[Devices.deviceName] = name
                        it[Devices.platform] = platform
                        it[Devices.createdAt] = LocalDateTime.now()
                    }
                }
                call.respond(HttpStatusCode.OK)
            }
        }
        staticFiles("/.well-known", File("certs")) {
            default("jwks.json")
        }
    }
}
