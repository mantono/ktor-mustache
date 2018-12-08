package com.mantono.ktor.mustache

data class MustacheContent(
	val file: String,
	val vars: Map<String, Any> = emptyMap()
)

inline fun mustache(file: String, substitution: () -> Map<String, Any>): MustacheContent =
	mustache(file, substitution())

fun mustache(file: String, substitution: Map<String, Any> = emptyMap()): MustacheContent {
	val withExtension = if(file.endsWith(".mustache")) file else "$file.mustache"
	return MustacheContent(withExtension, substitution)
}