/*
 * Copyright (c) 2018 David Boissier.
 * Modifications Copyright (c) 2022 Geetesh Gupta.
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
 */
package com.gg.plugins.json.utils

import org.apache.commons.lang.StringUtils
import java.util.*

object StringUtils {
    private const val ELLIPSIS = "..."

    @JvmStatic
    fun abbreviateInCenter(stringToAbbreviate: String, length: Int): String {
        if (stringToAbbreviate.length <= length) {
            return stringToAbbreviate
        }
        val halfLength = length / 2
        val firstPartLastIndex = halfLength - ELLIPSIS.length
        val stringLength = stringToAbbreviate.length
        return String.format(
            "%s%s%s",
            stringToAbbreviate.substring(0, firstPartLastIndex),
            ELLIPSIS,
            stringToAbbreviate.substring(stringLength - halfLength, stringLength)
        )
    }

    fun parseNumber(number: String): Number {
        try {
            return number.toInt()
        } catch (ignored: NumberFormatException) {
        }
        try {
            return number.toLong()
        } catch (ignored: NumberFormatException) {
        }
        return number.toDouble()
    }

    @JvmStatic
    fun stringifyList(list: List<*>): String {
        val stringifiedObjects: MutableList<String?> = LinkedList()
        for (`object` in list) {
            when (`object`) {
                is String -> {
                    stringifiedObjects.add("\"$`object`\"")
                }

                is List<*> -> {
                    stringifiedObjects.add(stringifyList(`object`))
                }

                else -> {
                    stringifiedObjects.add(`object`.toString())
                }
            }
        }
        return "[${StringUtils.join(stringifiedObjects, ", ")}]"
    }
}