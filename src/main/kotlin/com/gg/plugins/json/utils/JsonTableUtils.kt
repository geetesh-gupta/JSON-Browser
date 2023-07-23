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

import com.gg.plugins.json.model.JsonTreeNode
import com.gg.plugins.json.view.nodedescriptor.KeyValueDescriptor
import com.gg.plugins.json.view.nodedescriptor.ValueDescriptor
import com.gg.plugins.json.view.renderer.TableCellRenderer
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import java.util.stream.IntStream

object JsonTableUtils {
    @JvmStatic
    fun buildJsonTable(node: JsonTreeNode?): ListTableModel<JsonTreeNode> {
        val columnInfos = extractColumnNames(node)
        return when {
            node == null -> {
                ListTableModel()
            }


            node.childCount > 0 -> {
                val firstChildNode = node.getChildAt(0) ?: return ListTableModel()
                when (firstChildNode.userObject) {

                    is KeyValueDescriptor -> {
                        return ListTableModel(columnInfos, listOf(node))
                    }

                    is ValueDescriptor -> {
                        val columnInfoList: MutableList<JsonTreeNode> = ArrayList()
                        node.children().iterator().forEach { columnInfoList.add(it as JsonTreeNode) }
                        return ListTableModel(columnInfos, columnInfoList)
                    }

                    else -> {
                        return ListTableModel()
                    }
                }
            }

            else -> {
                ListTableModel()
            }
        }
    }

    private fun extractColumnNames(node: JsonTreeNode?): Array<ColumnInfo<JsonTreeNode, *>> {
        return when {
            node == null -> {
                arrayOf()
            }

            node.childCount > 0 -> {
                val firstChildNode = node.getChildAt(0) ?: return arrayOf()
                when (firstChildNode.userObject) {
                    is KeyValueDescriptor -> {
                        return Array(node.childCount) { TableColumnInfo(node.getChildAt(it).getKey()) }
                    }

                    is ValueDescriptor -> {
                        val columnInfos: MutableSet<ColumnInfo<JsonTreeNode, *>> = HashSet()
                        IntStream.range(0, node.childCount)
                            .forEach { columnInfos.addAll(extractColumnNames(node.getChildAt(it))) }
                        return columnInfos.stream().map { it }.toArray { arrayOfNulls(it) }
                    }

                    else -> {
                        return arrayOf()
                    }
                }
            }

            else -> {
                arrayOf()
            }
        }
    }

    private class TableColumnInfo(val key: String) : ColumnInfo<JsonTreeNode, Any?>(key) {
        override fun valueOf(o: JsonTreeNode): Any {
            return o.getChildByKey(this.key)
        }

        override fun getRenderer(o: JsonTreeNode): TableCellRenderer {
            return TABLE_CELL_RENDERER
        }

        companion object {
            private val TABLE_CELL_RENDERER = TableCellRenderer()
        }
    }
}