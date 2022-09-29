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
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonArray
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorSettings
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.LexerEditorHighlighter
import com.intellij.openapi.editor.highlighter.EditorHighlighter
import com.intellij.openapi.fileTypes.PlainTextSyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.ui.JBCardLayout
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.tree.TreeUtil
import java.awt.BorderLayout
import java.util.stream.Collectors
import java.util.stream.IntStream
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.tree.DefaultMutableTreeNode

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
                        pagination.startIndex
                    ), JsonTreeTableView.COLUMNS_FOR_READING
                )
            } else {
                JsonTreeTableView(
                    JsonTreeUtils.buildJsonTree(`object`),
                    JsonTreeTableView.COLUMNS_FOR_READING
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
        editor = createEditor(`object`)
        displayResult(editor!!.component)
    }

    private fun displayResult(tableView: JComponent) {
        resultTreePanel.invalidate()
        resultTreePanel.removeAll()
        resultTreePanel.add(JBScrollPane(tableView))
        resultTreePanel.validate()
    }

    private fun createEditor(data: JsonElement?): Editor {
        val editorFactory = EditorFactory.getInstance()
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonOutput = gson.toJson(data)
        val editorDocument = editorFactory.createDocument(jsonOutput)
        val editor = editorFactory.createEditor(editorDocument, project)
        fillEditorSettings(editor.settings)
        val editorEx = editor as EditorEx
        attachHighlighter(editorEx)
        return editor
    }

    private fun attachHighlighter(editor: EditorEx) {
        val scheme = editor.colorsScheme
        scheme.setColor(EditorColors.CARET_ROW_COLOR, null)
        editor.highlighter = createHighlighter(scheme)
    }

    private fun createHighlighter(settings: EditorColorsScheme): EditorHighlighter {
        var language = Language.findLanguageByID("JSON")
        if (language == null) {
            language = Language.ANY
        }
        return LexerEditorHighlighter(
            PlainTextSyntaxHighlighterFactory.getSyntaxHighlighter(language!!, null, null),
            settings
        )
    }

    //    public void editSelectedDocument() {
    //        Document mongoDocument = getSelectedDocument();
    //        if (mongoDocument == null) {
    //            return;
    //        }
    //
    //        EditionDialog
    //                .create(project, mongoDocumentOperations, actionCallback)
    //                .initDocument(mongoDocument)
    //                .show();
    //    }
    //
    //
    //    public void addDocument() {
    //        EditionDialog
    //                .create(project, mongoDocumentOperations, actionCallback)
    //                .initDocument(null)
    //                .show();
    //    }
    fun expandAll() {
        resultTreeTableView?.let { TreeUtil.expandAll(it.tree) }
    }

    fun collapseAll() {
        resultTreeTableView?.let { TreeUtil.collapseAll(it.tree, 1) }
    }

    val stringifiedResult: String
        get() {
            when (currentViewMode) {
                ViewMode.INPUT -> {
                    return editorValue.toString()
                }

                ViewMode.TABLE -> {
                    return resultTableView!!.listTableModel.items.toString()
                }

                else -> {
                    val rootNode = resultTreeTableView!!.tree.model.root as JsonTreeNode
                    if (rootNode.childCount == 0) return ""
                    return if ((rootNode.getChildAt(0) as JsonTreeNode).descriptor is KeyValueDescriptor) IntStream.range(
                        0,
                        rootNode.childCount
                    ).mapToObj { i: Int ->
                        val childNode = getDescriptor(i, rootNode)
                        String.format(TO_STRING_TEMPLATE, childNode.key, childNode)
                    }
                        .collect(Collectors.joining(",", "{", "}")) else IntStream.range(0, rootNode.childCount)
                        .mapToObj { i: Int -> rootNode.getChildAt(i).toString() }
                        .collect(Collectors.joining(",", "[", "]"))
                }
            }
        }

    val editorValue: JsonElement
        get() {
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
        private const val TO_STRING_TEMPLATE = "\"%s\" : %s"
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

        private fun fillEditorSettings(editorSettings: EditorSettings) {
            editorSettings.isWhitespacesShown = true
            editorSettings.isLineMarkerAreaShown = false
            editorSettings.isIndentGuidesShown = false
            editorSettings.isLineNumbersShown = false
            editorSettings.isAllowSingleLogicalLineFolding = true
            editorSettings.additionalColumnsCount = 0
            editorSettings.additionalLinesCount = 1
            editorSettings.isUseSoftWraps = true
            editorSettings.setUseTabCharacter(false)
            editorSettings.isCaretInsideTabs = false
            editorSettings.isVirtualSpace = false
        }

        private fun getDescriptor(i: Int, parentNode: DefaultMutableTreeNode): NodeDescriptor {
            val childNode = parentNode.getChildAt(i) as DefaultMutableTreeNode
            return childNode.userObject as NodeDescriptor
        }
    }
}