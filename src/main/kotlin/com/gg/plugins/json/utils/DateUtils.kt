/*
 * Copyright (c) 2018 David Boissier.
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

import java.text.DateFormat
import java.util.*

object DateUtils {
    @JvmStatic
    fun utcDateTime(locale: Locale?): DateFormat {
        val format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG, locale)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format
    }

    @JvmStatic
    fun utcTime(locale: Locale?): DateFormat {
        val format = DateFormat.getTimeInstance(DateFormat.MEDIUM, locale)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format
    }
}