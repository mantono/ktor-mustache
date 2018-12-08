package com.mantono.ktor.mustache

import io.ktor.application.ApplicationCall
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond

suspend inline fun ApplicationCall.respondTemplate(
	file: String,
	status: HttpStatusCode = HttpStatusCode.OK,
	substitution: Map<String, Any> = emptyMap()
) {
	respond(status, mustache(file, substitution))
}

suspend inline fun ApplicationCall.respondTemplate(
	file: String,
	status: HttpStatusCode = HttpStatusCode.OK,
	substitution: () -> Map<String, Any>
) {
	respond(status, mustache(file, substitution))
}