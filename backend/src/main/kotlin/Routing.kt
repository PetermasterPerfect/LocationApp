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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Base64
import java.util.Date
import java.util.concurrent.TimeUnit

object Users : IntIdTable("users") {
    val login = varchar("login", 255)
    val passwordHash = varchar("password_hash", 255)
    val createdAt = datetime("created_at")
}

object Devices : IntIdTable("devices") {
    val userId = integer("user_id")
    val deviceUuid = varchar("device_uuid", 255)
    val deviceName = varchar("device_name", 255)
    val createdAt = datetime("created_at")
}

object Locations : IntIdTable("locations") {
    val userId = integer("user_id")
    val deviceId = integer("device_id")
    val latitude = double("latitude")
    val longitude = double("longitude")
    val recordedAt = datetime("recorded_at")
}


fun hashPassword(password: String): String = BCrypt.withDefaults().hashToString(12, password.toCharArray())

fun verifyPassword(password: CharArray, hashed: CharArray): Boolean = BCrypt.verifyer().verify(password, hashed).verified

fun getUserByEmail(login: String): ResultRow? = transaction {
    Users.select { Users.login eq login }.singleOrNull()
}

fun getDevicesByUser(userId: Int): List<ResultRow> = transaction {
    Devices.select { Devices.userId eq userId }.toList()
}

fun getLocationDataByTime(userId: Int, deviceId: Int, start: LocalDateTime, end: LocalDateTime): List<ResultRow> = transaction {
    Locations.select { (Locations.userId eq userId) and (Locations.deviceId eq deviceId) and
            (Locations.recordedAt greaterEq start) and (Locations.recordedAt lessEq end)}.toList()
}

fun getLocationDataAll(userId: Int, deviceId: Int): List<ResultRow> = transaction {
    Locations.select { (Locations.userId eq userId) and (Locations.deviceId eq deviceId) }.toList()
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
            challenge { _, _ ->
                println("jwt challenge")
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
            }
        }
    }


    routing {
	get("/") {
                call.respond(HttpStatusCode.OK)
	}
        post("/signup") {

            val login = call.request.queryParameters["login"] ?: ""
            val password = call.request.queryParameters["password"] ?: ""

            if (login.isNullOrBlank() || password.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Email or password cannot be empty")
                return@post
            }

            if (getUserByEmail(login) != null) {
                call.respond(HttpStatusCode.Conflict, "Email already exists")
                return@post
            }

            transaction {
                Users.insert {
                    it[Users.login] = login
                    it[Users.passwordHash] = hashPassword(password)
                    it[Users.createdAt] = LocalDateTime.now()
                }
            }

            call.respond(HttpStatusCode.Created, "User created successfully")
        }

        post("/login") {
            val login = call.request.queryParameters["login"] ?: ""
            val password = call.request.queryParameters["password"] ?: ""

            if (login.isNullOrBlank() || password.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Email or password cannot be empty")
                return@post
            }

            val user = getUserByEmail(login)

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
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userid").asString().toInt()

                val devices = getDevicesByUser(userId)
                if (devices == null) {
                    call.respond(HttpStatusCode.NotFound, "No devices found")
                }

                val result = devices.map { row ->
                    mapOf(
                        "uuid" to row[Devices.deviceUuid],
                        "name" to row[Devices.deviceName]
                    )
                }
                call.respond(HttpStatusCode.OK, result)
            }

            post("/devices") {
                val uuid = call.request.queryParameters["uuid"] ?: ""
                val name = call.request.queryParameters["name"] ?: ""

                if (uuid.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "Cannot register device")
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
                        it[Devices.createdAt] = LocalDateTime.now()
                    }
                }
                call.respond(HttpStatusCode.OK)
            }

            get("/gpsall") {
                val uuid = call.request.queryParameters["uuid"]
                if (uuid.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "Missing uuid, start or end")
                    return@get
                }

                val principal = call.principal<JWTPrincipal>()
                val userId = principal
                    ?.payload
                    ?.getClaim("userid")
                    ?.asString()
                    ?.toIntOrNull()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    return@get
                }

                val devices = getDevicesByUser(userId)
                var curDevice: String? = null
                if(devices != null) {
                    for (device in devices)
                    {
                        if (device[Devices.deviceUuid] == uuid) {
                            curDevice = device[Devices.deviceUuid]
                        }
                    }
                }

                if(curDevice == null) {
                    call.respond(HttpStatusCode.NotFound, "No such device")
                    return@get
                }
                var deviceId: Int = 0
                transaction {
                    deviceId =
                        Devices.select { (Devices.userId eq userId) and (Devices.deviceUuid eq curDevice) }.single()[Devices.id].toString().toInt()

                }
                val data = getLocationDataAll(userId, deviceId)
                val result = data.map {
                    mapOf(
                        "longitude" to it[Locations.longitude].toString(),
                        "latitude" to it[Locations.latitude].toString(),
                        "timestamp" to it[Locations.recordedAt].toString()
                    )
                }

                call.respond(HttpStatusCode.OK, result)

            }

            get("/gps") {
                val uuid = call.request.queryParameters["uuid"]
                val start = call.request.queryParameters["start"]
                val end = call.request.queryParameters["end"]

                if (uuid.isNullOrBlank() || start.isNullOrBlank() || end.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "Missing uuid, start or end")
                    return@get
                }

                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

                val startTime = try {
                    LocalDateTime.parse(start, formatter)
                } catch (e: DateTimeParseException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        "Invalid start date format. Expected yyyy-MM-dd HH:mm:ss"
                    )
                    return@get
                }

                val endTime = try {
                    LocalDateTime.parse(end, formatter)
                } catch (e: DateTimeParseException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        "Invalid end date format. Expected yyyy-MM-dd HH:mm:ss"
                    )
                    return@get
                }

                if (endTime.isBefore(startTime)) {
                    call.respond(HttpStatusCode.BadRequest, "End date must be after start date")
                    return@get
                }


                val principal = call.principal<JWTPrincipal>()
                val userId = principal
                    ?.payload
                    ?.getClaim("userid")
                    ?.asString()
                    ?.toIntOrNull()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    return@get
                }

                val devices = getDevicesByUser(userId)
                var curDevice: String? = null
                if(devices != null) {
                    for (device in devices)
                    {
                        if (device[Devices.deviceUuid] == uuid) {
                            curDevice = device[Devices.deviceUuid]
                        }
                    }
                }

                if(curDevice == null) {
                    call.respond(HttpStatusCode.NotFound, "No such device")
                    return@get
                }
                var deviceId: Int = 0
                transaction {
                    deviceId =
                        Devices.select { (Devices.userId eq userId) and (Devices.deviceUuid eq curDevice) }.single()[Devices.id].toString().toInt()

                }
                    val data = getLocationDataByTime(userId, deviceId, startTime, endTime)
                    val result = data.map {
                            mapOf(
                                "longitude" to it[Locations.longitude].toString(),
                                "latitude" to it[Locations.latitude].toString(),
                                "timestamp" to it[Locations.recordedAt].toString()
                            )
                        }

                call.respond(HttpStatusCode.OK, result)
            }

            post("/gps") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)

                val userId = principal.payload
                    .getClaim("userid")
                    .asString()
                    .toInt()

                val uuid = call.request.queryParameters["uuid"] ?: ""
                val time = call.request.queryParameters["time"] ?: ""
                val longitude = call.request.queryParameters["longitude"]?.toDoubleOrNull()
                val latitude = call.request.queryParameters["latitude"]?.toDoubleOrNull()

                if (uuid.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "Device UUID is required")
                    return@post
                }

                if(longitude == null || latitude == null) {
                    call.respond(HttpStatusCode.BadRequest, "Latitude/Longitude aren't in proper format")
                    return@post
                }

                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                val recordedAt = try {
                    LocalDateTime.parse(time, formatter)
                } catch (_: DateTimeParseException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        "Invalid date format. Expected yyyy-MM-dd HH:mm:ss"
                    )
                    return@post
                }

                val deviceId = transaction {
                    Devices.select {
                        (Devices.userId eq userId) and
                                (Devices.deviceUuid eq uuid)
                    }.map { it[Devices.id].value }.singleOrNull()
                }

                if (deviceId == null) {
                    call.respond(HttpStatusCode.NotFound, "No such device")
                    return@post
                }

                transaction {
                    Locations.insert {
                        it[Locations.userId] = userId
                        it[Locations.deviceId] = deviceId
                        it[Locations.latitude] = latitude
                        it[Locations.longitude] = longitude
                        it[Locations.recordedAt] = recordedAt
                    }
                }
                call.respond(HttpStatusCode.Created, "Location saved")
            }
        }
        staticFiles("/.well-known", File("certs")) {
            default("jwks.json")
        }
    }
}
