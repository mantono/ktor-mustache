package com.mantono.ktor.mustache

import io.ktor.application.call
import io.ktor.application.install
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.io.File

fun main(args: Array<String>) {
	embeddedServer(Netty, 1234) {
		install(Mustache) {
			this.resources = File("src/main/resources/templates")
		}
		routing {
			get("/{user}") {
				call.respondTemplate("test") {
					val name = call.parameters["user"]!!
					mapOf("name" to name)
				}
			}
		}
	}.start()
}