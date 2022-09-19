/*******************************************************************************
 * Copyright 2020-2021 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.th2.testcall.configuration

import mu.KotlinLogging
import java.time.Instant

class CustomConfigurationClass {
    val hostname: String = "localhost"
    val port: Int = 8080

    val targetUrl: String = "http://localhost:8082"

    val responseTimeout: Int = 60000

    val statisticsTimeout: Long = 6000

    val streams: Set<String> = emptySet()

    val startTimestamp: String = Instant.now().toString()

    val endTimestamp: String = Instant.now().toString()

    val limit: Int = -1

    val direction: String = "next"
}

class Configuration(customConfiguration: CustomConfigurationClass) {

    companion object {
        private val logger = KotlinLogging.logger { }
    }

    val hostname: Variable =
        Variable("hostname", customConfiguration.hostname, "localhost")

    val port: Variable =
        Variable("port", customConfiguration.port.toString(), "8080")

    val responseTimeout: Variable =
        Variable("responseTimeout", customConfiguration.responseTimeout.toString(), "60000")

    val targetUrl = Variable("targetUrl", customConfiguration.targetUrl.toString(), "")

    val statisticsTimeout = Variable("statisticsTimeout", customConfiguration.statisticsTimeout.toString(), "6000")

    val streams = customConfiguration.streams.also {
        if (it.isEmpty()) {
            throw IllegalArgumentException("streams must contains one or more values")
        }
    }

    val startTimestamp = Variable("startTimestamp", customConfiguration.startTimestamp, Instant.now().toString())

    val endTimestamp = Variable("endTimestamp", customConfiguration.endTimestamp, Instant.now().toString())

    val limit = Variable("limit", customConfiguration.limit.toString(), "-1")

    val direction = Variable("direction", customConfiguration.direction, "next")
}
