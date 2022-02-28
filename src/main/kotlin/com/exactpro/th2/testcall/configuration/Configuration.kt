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

class CustomConfigurationClass {
    val hostname: String = "localhost"
    val port: Int = 8080

    val targetPort: Int = 8082

    val responseTimeout: Int = 60000
    val serverCacheTimeout: Int = 60000

    val eventCacheSize: Int = 1

    val checkRequestsAliveDelay: Long = 2000

    val enableCaching: Boolean = true
    val notModifiedObjectsLifetime: Int = 3600
    val rarelyModifiedObjects: Int = 500

    val sseEventSearchStep: Long = 200

    val keepAliveTimeout: Long = 5000

    val httpProviderLink = ""
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

    val serverCacheTimeout: Variable =
        Variable("serverCacheTimeout", customConfiguration.serverCacheTimeout.toString(), "60000")

    val eventCacheSize: Variable =
        Variable("eventCacheSize", customConfiguration.eventCacheSize.toString(), "1")

    val checkRequestsAliveDelay: Variable =
        Variable("checkRequestsAliveDelay", customConfiguration.checkRequestsAliveDelay.toString(), "2000")

    val enableCaching: Variable = Variable("enableCaching", customConfiguration.enableCaching.toString(), "true")

    val notModifiedObjectsLifetime: Variable =
        Variable("notModifiedObjectsLifetime", customConfiguration.notModifiedObjectsLifetime.toString(), "3600")

    val rarelyModifiedObjects: Variable =
        Variable("rarelyModifiedObjects", customConfiguration.rarelyModifiedObjects.toString(), "500")

    val sseEventSearchStep: Variable =
        Variable("sseEventSearchStep", customConfiguration.sseEventSearchStep.toString(), "200")

    val keepAliveTimeout: Variable =
        Variable("keepAliveTimeout", customConfiguration.keepAliveTimeout.toString(), "5000")

    val httpProviderLink = Variable("httpProviderLink", customConfiguration.httpProviderLink, "")

    val targetPort = Variable("targetPort", customConfiguration.targetPort.toString(), "8082")
}
