package com.locationapp

import io.ktor.server.application.*
fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureDatabases()
    configureRouting()
}
//$2a$12$cGcF9TZSvhsQidGOtk26BOTZBxtufnC/pqqBWDM2/ZdrfR8viUGHW