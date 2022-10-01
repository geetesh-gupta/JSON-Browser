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
package com.gg.plugins.json.view

import com.gg.plugins.json.model.JsonTreeNode
import com.gg.plugins.json.model.NodeDescriptor
import com.gg.plugins.json.renderer.KeyCellRenderer
import com.gg.plugins.json.renderer.ValueCellRenderer
import com.gg.plugins.json.table.TreeNodeDatePickerCellEditor
import com.gg.plugins.json.table.ValueCellEditor
import com.gg.plugins.json.utils.JsonTreeUtils
import com.intellij.ui.TreeTableSpeedSearch
import com.intellij.ui.treeStructure.treetable.ListTreeTableModelOnColumns
import com.intellij.ui.treeStructure.treetable.TreeTable
import com.intellij.ui.treeStructure.treetable.TreeTableModel
import com.intellij.util.ui.ColumnInfo
import org.apache.commons.lang.StringUtils
import java.util.Date
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath
import kotlin.math.max

class JsonTreeTableView(rootNode: TreeNode, private val columns: Array<ColumnInfo<JsonTreeNode, out Any>>) :
    TreeTable(ListTreeTableModelOnColumns(rootNode, columns)) {
    init {
        val tree = tree
        tree.showsRootHandles = true
        tree.isRootVisible = false
        setTreeCellRenderer(KeyCellRenderer())
        val keyColumnWidth = getFontMetrics(font).stringWidth(StringUtils.repeat("*", getMaxKeyColumnWidth(rootNode)))
        getColumnModel().getColumn(0).maxWidth = keyColumnWidth
        getColumnModel().getColumn(0).preferredWidth = keyColumnWidth
        setAutoResizeMode(AUTO_RESIZE_LAST_COLUMN)
        TreeTableSpeedSearch(this) { path: TreePath ->
            val node = path.lastPathComponent as JsonTreeNode
            val descriptor = node.descriptor
            descriptor.key
        }
    }

    private fun getMaxKeyColumnWidth(rootNode: TreeNode): Int {
        var length = 0
        if (!rootNode.isLeaf) {
            var maxChildLen = 0
            for (i in 0 until rootNode.childCount) {
                maxChildLen = max(getMaxKeyColumnWidth(rootNode.getChildAt(i)), maxChildLen)
            }
            length += maxChildLen
        }
        val userObject = (rootNode as JsonTreeNode).userObject as NodeDescriptor
        length += userObject.key.length
        return length
    }

    override fun getCellRenderer(row: Int, column: Int): TableCellRenderer {
        val treePath = tree.getPathForRow(row) ?: return super.getCellRenderer(row, column)
        val node = treePath.lastPathComponent as JsonTreeNode
        val renderer = columns[column].getRenderer(node)
        return renderer ?: super.getCellRenderer(row, column)
    }

    override fun getCellEditor(row: Int, column: Int): TableCellEditor {
        val treePath = tree.getPathForRow(row) ?: return super.getCellEditor(row, column)
        val node = treePath.lastPathComponent as JsonTreeNode
        val editor = columns[column].getEditor(node)
        return editor ?: super.getCellEditor(row, column)
    }

    private class ReadOnlyValueColumnInfo : ColumnInfo<JsonTreeNode, NodeDescriptor>("Value") {
        private val myRenderer: TableCellRenderer = ValueCellRenderer()
        override fun valueOf(treeNode: JsonTreeNode): NodeDescriptor {
            return treeNode.descriptor
        }

        override fun isCellEditable(o: JsonTreeNode): Boolean {
            return false
        }

        override fun getRenderer(o: JsonTreeNode): TableCellRenderer {
            return myRenderer
        }
    }

    private class WritableColumnInfo : ColumnInfo<JsonTreeNode, Any>("Value") {
        private val myRenderer: TableCellRenderer = ValueCellRenderer()
        private val defaultEditor: TableCellEditor = ValueCellEditor()
        override fun valueOf(treeNode: JsonTreeNode): NodeDescriptor {
            return treeNode.descriptor
        }

        override fun isCellEditable(treeNode: JsonTreeNode): Boolean {
            return true
        }

        override fun setValue(treeNode: JsonTreeNode, value: Any) {
            JsonTreeUtils.updateNode(treeNode, value)
        }

        override fun getRenderer(o: JsonTreeNode): TableCellRenderer {
            return myRenderer
        }

        override fun getEditor(treeNode: JsonTreeNode): TableCellEditor {
            val value = treeNode.descriptor.value
            return if (value is Date) {
                buildDateCellEditor(treeNode)
            } else defaultEditor
        }

        companion object {
            private fun buildDateCellEditor(treeNode: JsonTreeNode): TreeNodeDatePickerCellEditor {
                val dateEditor = TreeNodeDatePickerCellEditor()

                //  Note from dev: Quite ugly because when clicking on the button to open popup calendar, stopCellEdition
                //  is invoked.
                //                 From that point, impossible to set the selected data in the node description
                dateEditor.addActionListener {
                    treeNode.descriptor.value = dateEditor.cellEditorValue
                }
                return dateEditor
            }
        }
    }

    companion object {
        private val KEY: ColumnInfo<JsonTreeNode, NodeDescriptor> =
            object : ColumnInfo<JsonTreeNode, NodeDescriptor>("Key") {
                override fun valueOf(obj: JsonTreeNode): NodeDescriptor {
                    return obj.descriptor
                }

                override fun getColumnClass(): Class<TreeTableModel> {
                    return TreeTableModel::class.java
                }

                override fun isCellEditable(o: JsonTreeNode): Boolean {
                    return false
                }
            }
        private val READONLY_VALUE: ReadOnlyValueColumnInfo = ReadOnlyValueColumnInfo()

        @JvmField
        val COLUMNS_FOR_READING = arrayOf(KEY, READONLY_VALUE)
        private val WRITABLE_VALUE: WritableColumnInfo = WritableColumnInfo()
        val COLUMNS_FOR_WRITING = arrayOf(KEY, WRITABLE_VALUE)
    }
}