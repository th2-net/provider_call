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

package com.exactpro.th2.testcall

import com.exactpro.th2.dataprovider.grpc.Filter
import com.exactpro.th2.dataprovider.grpc.FilterName
import com.google.protobuf.BoolValue


class FilterBuilder(private val requestMap: Map<String, List<String>>) {

    private data class HttpFilter(val filterName: String, val requestMap: Map<String, List<String>>) {
        fun getName(): String {
            return filterName
        }

        fun isNegative(): Boolean {
            return requestMap["$filterName-negative"]?.first()?.toBoolean() ?: false
        }

        fun isConjunct(): Boolean {
            return requestMap["$filterName-conjunct"]?.first()?.toBoolean() ?: false
        }

        fun getValues(): List<String>? {
            return requestMap["$filterName-values"] ?: requestMap["$filterName-value"]
        }
    }

    fun buildFilters(): List<Filter> {
        return requestMap["filters"]?.map { filterName ->
            val filter = HttpFilter(filterName, requestMap)
            Filter.newBuilder()
                .setName(FilterName.newBuilder().setFilterName(filterName))
                .setNegative(BoolValue.of(filter.isNegative()))
                .setConjunct(BoolValue.of(filter.isConjunct()))
                .addAllValues(filter.getValues())
                .build()
        } ?: emptyList()
    }
}