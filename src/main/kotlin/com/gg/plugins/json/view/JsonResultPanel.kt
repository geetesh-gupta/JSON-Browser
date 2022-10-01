/*
 * Copyright (c) 2018 David Boissier.
 * Modifications Copyright (c) 2022 David Boissier.
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

import com.gg.plugins.json.model.*
import com.gg.plugins.json.service.Notifier
import com.gg.plugins.json.utils.JsonTableUtils
import com.gg.plugins.json.utils.JsonTreeUtils
import com.gg.plugins.json.utils.createEditor
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.ui.JBCardLayout
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.tree.TreeUtil
import java.awt.BorderLayout
import java.util.stream.IntStream
import javax.swing.JComponent
import javax.swing.JPanel

class JsonResultPanel(private val project: Project, jsonElement: JsonElement?) : JPanel(), Disposable {
    private val notifier: Notifier?
    private val resultTreePanel: JPanel
    private val actionCallback: ActionCallback
    private lateinit var mainPanel: JPanel
    private lateinit var containerPanel: JPanel
    private var editor: Editor? = null

    var resultTreeTableView: JsonTreeTableView? = null
    var resultTableView: JsonTableView<JsonObject>? = null
    var currentViewMode = ViewMode.INPUT

    init {
        notifier = Notifier.getInstance(project)
        layout = BorderLayout()
        add(mainPanel, BorderLayout.CENTER)
        resultTreePanel = JPanel(BorderLayout())
        containerPanel.layout = JBCardLayout()
        containerPanel.add(resultTreePanel)
        updateResultView(jsonElement, Pagination())
        actionCallback = object : ActionCallback {
            override fun onOperationSuccess(label: String?, message: String?) {
                notifier.notifyInfo(message)
            }

            override fun onOperationFailure(exception: Exception?) {
                notifier.notifyError(exception?.message)
            }
        }
    }

    fun updateResultView(`object`: JsonElement?, pagination: Pagination?) {
        if (ViewMode.TREE == currentViewMode) {
            updateResultTreeTable(`object`, pagination)
        } else if (ViewMode.TABLE == currentViewMode) {
            updateResultTable(`object`)
        } else {
            updateInputPane(`object`)
        }
    }

    private fun updateResultTreeTable(`object`: JsonElement?, pagination: Pagination?) {
        if (`object` != null && pagination != null) {
            resultTreeTableView = if (`object`.isJsonArray) {
                JsonTreeTableView(
                    JsonTreeUtils.buildJsonTree(
                        extractDocuments(pagination, `object`.asJsonArray),
                        null,
                        pagination.startIndex
                    ), JsonTreeTableView.COLUMNS_FOR_WRITING
                )
            } else {
                JsonTreeTableView(
                    JsonTreeUtils.buildJsonTree(`object`),
                    JsonTreeTableView.COLUMNS_FOR_WRITING
                )
            }
        }

        resultTreeTableView?.let {
            it.name = "resultTreeTable"
            displayResult(it)
            UIUtil.invokeAndWaitIfNeeded(Runnable { TreeUtil.expand(it.tree, 2) })
        }
    }

    private fun updateResultTable(`object`: JsonElement?) {
        resultTableView = JsonTableView(JsonTableUtils.buildJsonTable(`object`))
        resultTableView?.let {
            it.name = "resultTable"
            displayResult(it)
        }
    }

    private fun updateInputPane(`object`: JsonElement?) {
        editor = createEditor(`object`, project)
        displayResult(editor!!.component)
    }

    private fun displayResult(tableView: JComponent) {
        resultTreePanel.invalidate()
        resultTreePanel.removeAll()
        resultTreePanel.add(JBScrollPane(tableView))
        resultTreePanel.validate()
    }

    fun expandAll() {
        resultTreeTableView?.let { TreeUtil.expandAll(it.tree) }
    }

    fun collapseAll() {
        resultTreeTableView?.let { TreeUtil.collapseAll(it.tree, 1) }
    }

    fun stringifiedResult(): String {
        return when (currentViewMode) {
            ViewMode.INPUT -> {
                editorValue().toString()
            }

            ViewMode.TABLE -> {
                resultTableView!!.listTableModel.items.toString()
            }

            else -> {
                JsonTreeUtils.stringifyTree(resultTreeTableView!!.tree.model.root as JsonTreeNode)
            }
        }
    }

    fun stringifiedResult(viewMode: ViewMode): String {
        return when (viewMode) {
            ViewMode.INPUT -> {
                editorValue().toString()
            }

            ViewMode.TABLE -> {
                resultTableView!!.listTableModel.items.toString()
            }

            else -> {
                JsonTreeUtils.stringifyTree(resultTreeTableView!!.tree.model.root as JsonTreeNode)
            }
        }
    }

    fun editorValue(): JsonElement {
        val document = editor!!.document
        return JsonParser.parseString(document.text)
    }

    fun getSelectedNodeStringifiedValue(): String {
        val result = getSelectedCell().toString()
        return result.replace("^\"|\"$".toRegex(), "")
    }

    fun getSelectedCell(): JsonElement? {
        resultTableView?.let {
            if (it.selectedColumn == -1) return it.selectedObject
            val selectedColumnNum = it.selectedColumn
            val selectedColumnName =
                it.columnModel.getColumn(selectedColumnNum).headerValue.toString()
            return it.selectedObject?.get(selectedColumnName)
        }
        resultTreeTableView?.let {
            val treeNode = it.tree.lastSelectedPathComponent as JsonTreeNode
            return treeNode.descriptor.value as JsonElement
        }
        return null
    }

    override fun dispose() {
        resultTreeTableView = null
    }

    enum class ViewMode {
        TREE, TABLE, INPUT
    }

    interface ActionCallback {
        fun onOperationSuccess(label: String?, message: String?)
        fun onOperationFailure(exception: Exception?)
    }

    companion object {
        private fun extractDocuments(pagination: Pagination?, documents: JsonArray): JsonArray {
            if (ResultsPerPage.ALL == pagination!!.resultsPerPage) {
                return documents
            }
            if (pagination.countPerPage >= documents.size()) {
                return documents
            }
            val startIndex = pagination.startIndex
            val endIndex = startIndex + pagination.countPerPage
            val jsonArray = JsonArray()
            IntStream.range(startIndex, endIndex).forEach { number: Int -> jsonArray.add(number) }
            return jsonArray
        }
    }
}