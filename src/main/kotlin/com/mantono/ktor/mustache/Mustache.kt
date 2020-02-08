package com.mantono.ktor.mustache

import com.github.mustachejava.DefaultMustacheFactory
import com.github.mustachejava.MustacheFactory
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.http.withCharset
import io.ktor.response.ApplicationSendPipeline
import io.ktor.util.AttributeKey
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeAvailable
import mu.KotlinLogging
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStreamWriter

private val log = KotlinLogging.logger(Mustache::class.qualifiedName.toString())

class Mustache(configuration: Configuration) {

	private val mustacheFactory: MustacheFactory = DefaultMustacheFactory(configuration.resources)

	class Configuration {
		var resources: File = File("src/main/resources/templates")
			set(value)
			{
				require(value.exists()) {
					"Default resources directory does not exist: $value"
				}
				field = value
			}

		var bufferSize: Int = 128
			set(value)
			{
				require(value > 0) {
					"Buffer must be > 0, was $value"
				}
				field = value
			}

		var defaultContentType: ContentType = ContentType.Text.Html.withCharset(Charsets.UTF_8)
	}

	companion object Feature : ApplicationFeature<ApplicationCallPipeline, Mustache.Configuration, Mustache> {
		override val key: AttributeKey<Mustache> = AttributeKey("Mustache")

		override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): Mustache {
			val configuration = Mustache.Configuration().apply(configure)
			val feature = Mustache(configuration)
			log(configuration)

			pipeline.sendPipeline.intercept(ApplicationSendPipeline.Transform) { content ->
				if(content is MustacheContent) {
					val transformed: OutgoingContent = transform(content, configuration, feature)
					proceedWith(transformed)
				}
			}
			return feature
		}

		private fun transform(
			content: MustacheContent,
			configuration: Configuration,
			feature: Mustache
		): OutgoingContent {
			val compiledFile: com.github.mustachejava.Mustache = feature.mustacheFactory.compile(content.file)
				?: error("Unable to compile file ${content.file}")

			val byteStream = ByteArrayOutputStream(configuration.bufferSize)
			val stream = BufferedOutputStream(byteStream)
			val writer = OutputStreamWriter(stream)
			compiledFile.execute(writer, content.vars)
			writer.flush()

			return ContentPipe(byteStream, configuration.defaultContentType)
		}

		private fun log(configuration: Configuration) {
			val found: List<File> = configuration.resources
				.listFiles { _, name -> name.endsWith(".mustache") }
				?.toList() ?: emptyList()
			if(found.isEmpty()) {
				log.warn { "Found no mustache templates in ${configuration.resources}" }
			} else {
				log.info { "Found templates: ${found.joinToString(separator = "\n") { it.canonicalPath }}" }
			}
		}
	}
}

class ContentPipe(
	private val buffer: ByteArray,
	override val contentType: ContentType
): OutgoingContent.WriteChannelContent() {

	constructor(stream: ByteArrayOutputStream, contentType: ContentType): this(stream.toByteArray(), contentType)

	override suspend fun writeTo(channel: ByteWriteChannel) {
		assert(!channel.isClosedForWrite) {
			val message = channel.closedCause?.message ?: "Unknown closed cause"
			"Trying to write to channel when it is closed: $message"
		}
		while(channel.availableForWrite > 0) {
			channel.writeAvailable(buffer)
			if(!channel.autoFlush) {
				channel.flush()
			}
		}
	}
}