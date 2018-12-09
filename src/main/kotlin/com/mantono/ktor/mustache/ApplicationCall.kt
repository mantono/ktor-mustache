package com.mantono.ktor.mustache

import io.ktor.application.ApplicationCall
import io.ktor.application.feature
import io.ktor.application.featureOrNull
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond

suspend inline fun ApplicationCall.respondTemplate(
	file: String,
	status: HttpStatusCode = HttpStatusCode.OK,
	substitution: Map<String, Any> = emptyMap()
) {
	checkNotNull(this.application.featureOrNull(Mustache.Feature)) {
		"Feature Mustache must be installed"
	}
	respond(status, mustache(file, substitution))
}

suspend inline fun ApplicationCall.respondTemplate(
	file: String,
	status: HttpStatusCode = HttpStatusCode.OK,
	substitution: () -> Map<String, Any>
) {
	respondTemplate(file, status, substitution())
}