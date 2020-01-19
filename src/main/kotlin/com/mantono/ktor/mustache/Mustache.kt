package com.mantono.ktor.mustache

import com.github.mustachejava.DefaultMustacheFactory
import com.github.mustachejava.MustacheFactory
import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.http.ContentType
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.content.OutgoingContent
import io.ktor.http.withCharset
import io.ktor.response.ApplicationSendPipeline
import io.ktor.util.AttributeKey
import io.ktor.util.cio.use
import io.ktor.util.pipeline.PipelineContext
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeAvailable
import mu.KotlinLogging
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.Writer

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

		var bufferSize: Int = 64
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

			ByteArrayOutputStream(configuration.bufferSize).use { bytesStream ->
				val writer: Writer = OutputStreamWriter(bytesStream)
				feature.mustacheFactory.compile(content.file).execute(writer, content.vars).flush()
				return object : OutgoingContent.WriteChannelContent() {
					override val contentType: ContentType = content.contentType ?: configuration.defaultContentType
					override suspend fun writeTo(channel: ByteWriteChannel) {
						channel.writeAvailable(bytesStream.toByteArray())
					}

					override val contentLength: Long = bytesStream.toByteArray().size.toLong()
				}
			}
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