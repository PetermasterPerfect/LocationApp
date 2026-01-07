package com.locationapp

import io.ktor.server.application.*
import java.sql.Connection
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager


fun Application.configureDatabases() {

    val dbUrl = "jdbc:postgresql://localhost:5432/test"
    val dbUser =  "postgres"
    val dbPassword = "postgres"

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