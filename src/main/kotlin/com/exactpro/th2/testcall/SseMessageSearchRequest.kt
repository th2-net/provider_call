package com.exactpro.th2.testcall

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

import com.exactpro.cradle.Direction
import com.exactpro.cradle.TimeRelation
import com.exactpro.cradle.messages.StoredMessageId
import java.time.Instant

data class StreamPointer(
    val sequence: Long,
    val streamName: String,
    val direction: Direction,
    val hasStarted: Boolean
)


data class SseMessageSearchRequest(
    val startTimestamp: Instant?,
    val stream: List<String>,
    val searchDirection: TimeRelation,
    val endTimestamp: Instant?,
    val resultCountLimit: Int?,
    val attachedEvents: Boolean,
    val lookupLimitDays: Int?,
    val resumeFromIdsList: List<StreamPointer>,
    val includeProtocols: List<String>?,
    val excludeProtocols: List<String>?
) {

    companion object {
        private fun asCradleTimeRelation(value: String): TimeRelation {
            if (value == "next") return TimeRelation.AFTER
            if (value == "previous") return TimeRelation.BEFORE

            throw Exception("'$value' is not a valid timeline direction. Use 'next' or 'previous'")
        }
    }

    constructor(parameters: Map<String, List<String>>) : this(
        startTimestamp = parameters["startTimestamp"]?.firstOrNull()?.let { Instant.ofEpochMilli(it.toLong()) },
        stream = parameters["stream"] ?: emptyList(),
        searchDirection = parameters["searchDirection"]?.firstOrNull()?.let {
            asCradleTimeRelation(
                it
            )
        } ?: TimeRelation.AFTER,
        endTimestamp = parameters["endTimestamp"]?.firstOrNull()?.let { Instant.ofEpochMilli(it.toLong()) },

        //FIXME: negative value is used to mark a stream that has not yet started. This needs to be replaced with an explicit flag
        resumeFromIdsList = parameters["messageId"]
            ?.map {
                StoredMessageId.fromString(it).let { id ->
                    StreamPointer(
                        id.index, id.streamName, id.direction, id.index > 0
                    )
                }
            }
            ?: emptyList(),

        resultCountLimit = parameters["resultCountLimit"]?.firstOrNull()?.toInt(),
        attachedEvents = parameters["attachedEvents"]?.firstOrNull()?.toBoolean() ?: false,
        lookupLimitDays = parameters["lookupLimitDays"]?.firstOrNull()?.toInt(),

        includeProtocols = parameters["includeProtocols"],
        excludeProtocols = parameters["excludeProtocols"]
    )



    private fun checkEndTimestamp() {
        if (endTimestamp == null || startTimestamp == null) return

        if (searchDirection == TimeRelation.AFTER) {
            if (startTimestamp.isAfter(endTimestamp))
                throw Exception("startTimestamp: $startTimestamp > endTimestamp: $endTimestamp")
        } else {
            if (startTimestamp.isBefore(endTimestamp))
                throw Exception("startTimestamp: $startTimestamp < endTimestamp: $endTimestamp")
        }
    }

    private fun checkStartPoint() {
        if (startTimestamp == null && resumeFromIdsList.isEmpty())
            throw Exception("One of the 'startTimestamp' or 'messageId' must not be null")
    }

    private fun checkStreamList() {
        if (stream.isEmpty()) {
            throw Exception("Streams list can not be empty")
        }
    }

    fun checkRequest() {
        checkStartPoint()
        checkEndTimestamp()
        checkStreamList()
    }
}

