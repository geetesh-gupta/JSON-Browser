/*
 * Copyright (c) 2022 Geetesh Gupta.
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

import com.gg.plugins.json.renderer.TableCellRenderer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import java.util.*
import java.util.function.Consumer

object JsonTableUtils {
    @JvmStatic
    fun buildJsonTable(document: JsonElement?): ListTableModel<JsonObject> {
        val columnInfos = extractColumnNames(document)
        return if (document!!.isJsonArray) {
            val columnInfoList: MutableList<JsonObject> = ArrayList()
            document.asJsonArray.forEach(Consumer { d: JsonElement -> columnInfoList.add(d.asJsonObject) })
            ListTableModel(columnInfos, columnInfoList)
        } else {
            ListTableModel(columnInfos, listOf(document.asJsonObject))
        }
    }

    private fun extractColumnNames(document: JsonElement?): Array<ColumnInfo<*, *>> {
        return when {
            document == null -> {
                return arrayOf()
            }

            document.isJsonArray -> {
                val columnInfos: MutableSet<ColumnInfo<*, *>> = HashSet()
                document.asJsonArray.forEach(Consumer { d: JsonElement? ->
                    val columnInfos1 = extractColumnNames(d)
                    columnInfos.addAll(listOf(*columnInfos1))
                })
                val columnInfosArr: Array<ColumnInfo<*, *>> = columnInfos.toTypedArray()
                columnInfosArr
            }

            else -> {
                extractColumnNames(document.asJsonObject)
            }
        }
    }

    private fun extractColumnNames(document: JsonObject): Array<ColumnInfo<*, *>> {
        val keys = document.keySet().toList()
        return Array(keys.size) { TableColumnInfo(keys[it]) }
    }

    private class TableColumnInfo(private val key: String) : ColumnInfo<Any, Any?>(key) {
        override fun valueOf(o: Any): Any? {
            val document = o as JsonObject
            return document[key]
        }

        override fun getRenderer(o: Any): TableCellRenderer {
            return TABLE_CELL_RENDERER
        }

        companion object {
            private val TABLE_CELL_RENDERER = TableCellRenderer()
        }
    }
}

inline fun <reified T> Collection<T>.toTypedArray(): Array<T> {
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    val thisCollection = this as java.util.Collection<*>
    return thisCollection.toArray(arrayOfNulls<T>(0))
}
