package com.exactpro.th2.testcall

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking

class HttpLoader(
    private val targetUri: String
) {

    private val client = HttpClient { expectSuccess = false }

    private fun getEventType(line: String): String {
        return line.substring(line.indexOf(": ") + 2, line.length)
    }

    suspend fun request(): Flow<String> {
        val request = client.request<HttpStatement>(targetUri) {
            header("Accept-Encoding", "")
            header("Accept", "text/event-stream")
            header("Cache-Control", "no-cache")
            method = HttpMethod.Get
        }

        return flow {
            request.execute { response: HttpResponse ->
                val channel = response.receive<ByteReadChannel>()
                val builder = StringBuilder()
                var counter = 0
                var isEmpty = false
                while (!isEmpty) {
                    var needSend = false
                    do {
                        val line = runBlocking { channel.readUTF8Line() }
                        if (line != null && line.isNotBlank()) {
                            val key = line.substring(0 until line.indexOf(": "))

                            if (key == "event" && getEventType(line) == "message") {
                                needSend = true
                            }

                            if (key == "event" && getEventType(line) == "close") {
                                isEmpty = true
                            }

                            if (key == "data" && needSend) {
                                builder.append(line)
                            }
                            counter++
                        }
                    } while (line?.isBlank() != true)

                    if (builder.isNotBlank()) {
                        emit(builder.toString())
                        builder.setLength(0)
                    }
                }
            }
        }
    }
}