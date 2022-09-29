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
package com.gg.plugins.json.model

import com.gg.plugins.json.style.StyleAttributesProvider
import com.gg.plugins.json.utils.DateUtils
import com.gg.plugins.json.utils.StringUtils
import com.intellij.ui.ColoredTableCellRenderer
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import java.lang.Boolean.parseBoolean
import java.util.*

open class ValueDescriptor private constructor(
    private val index: Int,
    override var value: Any?,
    private val valueTextAttributes: SimpleTextAttributes?
) : NodeDescriptor {

    override fun renderValue(cellRenderer: ColoredTableCellRenderer, isNodeExpanded: Boolean) {
        if (!isNodeExpanded) {
            cellRenderer.append(formattedValue, valueTextAttributes!!)
        }
    }

    override fun renderNode(cellRenderer: ColoredTreeCellRenderer) {
        cellRenderer.append(key, StyleAttributesProvider.indexAttribute)
    }

    override val key: String
        get() = "[$index]"

    override val formattedValue: String
        get() = StringUtils.abbreviateInCenter(value.toString(), NodeDescriptor.MAX_LENGTH)

    override fun pretty(): String {
        return formattedValue
    }

    override fun toString(): String {
        return value.toString()
    }

    companion object {
        @JvmStatic
        fun createDescriptor(index: Int, value: Any?): ValueDescriptor {
            return when (value) {
                is String -> {
                    object : ValueDescriptor(index, value, StyleAttributesProvider.stringAttribute) {
                        override val formattedValue: String
                            get() = StringUtils.abbreviateInCenter(value.toString(), NodeDescriptor.MAX_LENGTH)
                    }
                }

                is Boolean -> {
                    object : ValueDescriptor(index, value, StyleAttributesProvider.booleanAttribute) {
                        override var value: Any? = value
                            set(value) {
                                field = parseBoolean(value as String?)
                            }
                    }
                }

                is Number -> {
                    object : ValueDescriptor(index, value, StyleAttributesProvider.numberAttribute) {
                        override var value: Any? = value
                            set(value) {
                                field = Integer.valueOf(value as String?)
                            }
                    }
                }

                is Date -> {
                    object : ValueDescriptor(index, value, StyleAttributesProvider.stringAttribute) {
                        override val formattedValue: String
                            get() = DateUtils.utcDateTime(Locale.getDefault()).format(value)

                        override fun toString(): String {
                            return String.format("\"$formattedValue\"")
                        }
                    }
                }

                is List<*> -> {
                    object : ValueDescriptor(index, value, StyleAttributesProvider.documentAttribute) {
                        override val formattedValue: String
                            get() = StringUtils.stringifyList(value)

                        override fun toString(): String {
                            return formattedValue
                        }
                    }
                }

                else -> {
                    ValueDescriptor(index, value, StyleAttributesProvider.stringAttribute)
                }
            }
        }
    }
}