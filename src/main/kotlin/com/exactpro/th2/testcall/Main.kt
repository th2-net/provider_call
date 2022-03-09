package com.exactpro.th2.testcall

/*******************************************************************************
 * Copyright 2022-2022 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

import com.exactpro.cradle.TimeRelation
import com.exactpro.th2.common.schema.factory.CommonFactory
import com.exactpro.th2.dataprovider.grpc.*
import com.google.protobuf.Int32Value
import com.google.protobuf.Timestamp
import com.exactpro.th2.testcall.configuration.Configuration
import com.exactpro.th2.testcall.configuration.CustomConfigurationClass
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.cio.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import java.nio.channels.ClosedChannelException
import kotlin.coroutines.coroutineContext


private class Timeouts {
    class Config(var requestTimeout: Long = 5000L, var excludes: List<String> = listOf("sse"))

    companion object : ApplicationFeature<ApplicationCallPipeline, Config, Unit> {
        override val key: AttributeKey<Unit> = AttributeKey("com.exactpro.th2.testcall.Timeouts")

        override fun install(pipeline: ApplicationCallPipeline, configure: Config.() -> Unit) {
            val config = Config().apply(configure)
            val timeout = config.requestTimeout
            val excludes = config.excludes

            if (timeout <= 0) return

            pipeline.intercept(ApplicationCallPipeline.Features) {
                if (excludes.any { call.request.uri.contains(it) }) return@intercept
                withTimeout(timeout) {
                    proceed()
                }
            }
        }
    }
}

@EngineAPI
@InternalAPI
suspend fun checkContext(context: ApplicationCall) {
    context.javaClass.getDeclaredField("call").also {
        it.trySetAccessible()
        val nettyApplicationRequest = it.get(context) as NettyApplicationCall

        while (coroutineContext.isActive) {
            if (nettyApplicationRequest.context.isRemoved)
                throw ClosedChannelException()

            delay(100)
        }
    }
}


@InternalCoroutinesApi
@FlowPreview
@ExperimentalCoroutinesApi
@EngineAPI
@InternalAPI
fun main(args: Array<String>) {
    val factory = CommonFactory.createFromArguments(*args)
    val applicationContext = Context(
        Configuration(factory.getCustomConfiguration(CustomConfigurationClass::class.java))
    )

    val targetUrl = applicationContext.configuration.targetUrl.value

    val configuration = applicationContext.configuration

    val grpc = factory.grpcRouter.getService(DataProviderService::class.java)

    embeddedServer(Netty, configuration.port.value.toInt()) {

        install(Compression)
        install(Timeouts) {
            requestTimeout = applicationContext.timeout
        }
        install(CallLogging) {
            level = org.slf4j.event.Level.DEBUG
        }

        routing {


            get("/http") {
                launch {
                    checkContext(call)
                }
                call.response.headers.append(HttpHeaders.CacheControl, "no-cache, no-store, no-transform")

                val client = HttpClient { expectSuccess = false }
                val targetUri = "${targetUrl}/search/sse/messages/${call.request.uri.let {
                    it.substring(it.indexOf("?"))
                }}"

                val request = SseMessageSearchRequest(call.parameters.toMap())
                client.request<HttpStatement>(targetUri) {
                    header("Accept-Encoding", "")
                    header("Accept", "text/event-stream")
                    header("Cache-Control", "no-cache")
                    method = HttpMethod.Get

                }.execute { response: HttpResponse ->
                    val channel = response.receive<ByteReadChannel>()
                    val builder = StringBuilder()
                    var counter = 0
                    call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                        while (isActive) {
                            var needSend = false
                            do {
                                val line = channel.readUTF8Line()
                                if (line != null && line.isNotBlank()) {
                                    val key = line.substring(0 until line.indexOf(": "))

                                    if (key == "event" && line.substring(line.indexOf(": ") + 2, line.length) == "message") {
                                        needSend = true
                                    }

                                    if (key == "data" && needSend && counter % request.frequency == 0) {
                                        builder.append(line)
                                    }
                                    counter++
                                }
                            } while (line?.isBlank() != true)

                            if (builder.isNotBlank()) {
                                write(builder.toString())
                                write("\n\n")
                                flush()
                                builder.setLength(0)
                            }
                        }
                    }
                }
            }


            get("/grpc") {
                val queryParametersMap = call.parameters.toMap()
                val request = SseMessageSearchRequest(queryParametersMap)
                request.checkRequest()
                launch {
                    checkContext(call)
                }

                val filters = FilterBuilder(queryParametersMap).buildFilters()

                val grpcRequest = MessageSearchRequest.newBuilder()
                request.startTimestamp?.let { grpcRequest.setStartTimestamp(Timestamp.newBuilder().setSeconds(it.epochSecond).setNanos(it.nano)) }
                request.endTimestamp?.let { grpcRequest.setEndTimestamp(Timestamp.newBuilder().setSeconds(it.epochSecond).setNanos(it.nano)) }
                grpcRequest.setStream(StringList.newBuilder().addAllListString(request.stream))
                grpcRequest.setSearchDirection(if (request.searchDirection == TimeRelation.AFTER) com.exactpro.th2.dataprovider.grpc.TimeRelation.NEXT else com.exactpro.th2.dataprovider.grpc.TimeRelation.PREVIOUS)
                request.resultCountLimit?.let { grpcRequest.setResultCountLimit(Int32Value.of(it)) }

                grpcRequest.addAllFilters(filters)

                var counter = 0
                call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                    val iter = grpc.searchMessages(grpcRequest.build())

                    for (response in iter) {

                        if (counter % request.frequency == 0) {
                            write("data: ${response.message.message.metadata}\n")

                            write("\n")
                            flush()
                        }
                        counter++
                    }
                }
            }
        }
    }.start(false)
    println("serving on: http://${configuration.hostname.value}:${configuration.port.value}")
}
