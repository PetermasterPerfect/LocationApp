package com.locationapp

import io.ktor.server.application.*
import java.sql.Connection
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager


fun Application.configureDatabases() {

    val dbUrl = environment.config.property("postgres.url").getString()
    val dbUser =  environment.config.property("postgres.user").getString()
    val dbPassword = environment.config.property("postgres.password").getString()

    Database.connect(
        url = dbUrl,
        driver = "org.postgresql.Driver",
        user = dbUser,
        password = dbPassword
    )

    TransactionManager.manager.defaultIsolationLevel =
        Connection.TRANSACTION_REPEATABLE_READ

    log.info("Connected to PostgreSQL database")
}