package com.mantono.ktor.mustache

import com.github.mustachejava.DefaultMustacheFactory
import com.github.mustachejava.MustacheFactory
import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.response.ApplicationSendPipeline
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.Writer

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
	}

	companion object Feature : ApplicationFeature<ApplicationCallPipeline, Mustache.Configuration, Mustache> {
		override val key: AttributeKey<Mustache> = AttributeKey("Mustache")

		override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): Mustache {
			val configuration = Mustache.Configuration().apply(configure)
			val feature = Mustache(configuration)

			pipeline.sendPipeline.intercept(ApplicationSendPipeline.Transform) { content ->
				if(content is MustacheContent) {
					this.transform(content, configuration, feature)
				}
			}
			return feature
		}

		private suspend fun PipelineContext<Any, ApplicationCall>.transform(
			content: MustacheContent,
			configuration: Configuration,
			feature: Mustache
		) {
			val file: File = configuration.resources.resolve(content.file)

			ByteArrayOutputStream(configuration.bufferSize).use { stream: OutputStream ->
				val writer: Writer = OutputStreamWriter(stream)
				feature.mustacheFactory.compile(file.canonicalPath).execute(writer, content.vars)
				proceedWith(stream)
			}
		}
	}
}