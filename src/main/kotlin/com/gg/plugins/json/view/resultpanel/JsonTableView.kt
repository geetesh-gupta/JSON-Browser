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
package com.gg.plugins.json.view.resultpanel

import com.gg.plugins.json.model.JsonTreeNode
import com.gg.plugins.json.utils.JsonTableUtils
import com.gg.plugins.json.utils.JsonTreeUtils
import com.intellij.ui.TableSpeedSearch
import com.intellij.ui.table.TableView
import java.awt.Component

class JsonTableView(private val rootNode: JsonTreeNode) : TableView<JsonTreeNode>(), IResultPanelState {
    init {
        TableSpeedSearch(this)
        model = JsonTableUtils.buildJsonTable(rootNode)
    }

    override fun getSelected(): JsonTreeNode? {
        if (selectedColumn == -1) return selectedObject
        return selectedObject?.getChildByKey(getSelectedColumnName())
    }

    override fun getAll(): JsonTreeNode {
//        val currentNode = JsonTreeNode(KeyValueDescriptor.createDescriptor("<root>", ""))
//        IntStream.range(0, listTableModel.items.size)
//            .forEach { index: Int -> currentNode.add(listTableModel.items[index] as JsonTreeNode) }
        return rootNode
    }

    override fun getSelectedColumnName(): String {
        if (selectedColumn == -1) return ""
        val selectedColumnNum = selectedColumn
        return columnModel.getColumn(selectedColumnNum).headerValue.toString()
    }

    override fun getView(): Component {
        return this
    }

    override fun expandAll() {
    }

    override fun collapseAll() {
    }

    override fun dispose() {
    }

    override fun getAllStringified(): String {
        return listTableModel.items.toString()
    }

    override fun refresh() {
        JsonTreeUtils.updateNode(rootNode)
    }

}




