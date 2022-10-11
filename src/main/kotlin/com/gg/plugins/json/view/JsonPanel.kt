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

import com.gg.plugins.json.action.*
import com.gg.plugins.json.model.JsonTreeNode
import com.gg.plugins.json.model.pagination.Pagination
import com.gg.plugins.json.model.pagination.ResultsPerPage
import com.gg.plugins.json.view.resultpanel.JsonResultPanel
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.intellij.ide.CommonActionsManager
import com.intellij.ide.TreeExpander
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.LoadingDecorator
import com.intellij.openapi.ui.Splitter
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.GuiUtils
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.util.stream.IntStream
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener

class JsonPanel(project: Project, var node: JsonTreeNode, titleText: String = "<root>") : JPanel(),
    Disposable, TreeModelListener {
    private val rowCountLabel = JBLabel()
    private val pageNumberLabel = JBLabel()

    val resultPanel: JsonResultPanel
    private val pagination: Pagination = Pagination()

    lateinit var rootPanel: JPanel
    lateinit var splitter: Splitter
    lateinit var toolBar: JPanel
    lateinit var errorPanel: JPanel
    lateinit var paginationPanel: JPanel
    lateinit var title: JLabel

    init {
        title.text = titleText
        errorPanel.layout = BorderLayout()

        resultPanel = createResultPanel(project)
//        resultPanel.addDataChangeListener(this)
        val loadingDecorator = LoadingDecorator(resultPanel, this, 0)

        splitter.orientation = true
        splitter.proportion = 0.2f
        splitter.firstComponent = loadingDecorator.component
        layout = BorderLayout()
        add(rootPanel)

        updateToolBar()

        initPaginationPanel()
//        pagination.addSetPageListener { updateJsonElementPagination() }
//        pagination.addSetPageListener {
//            pagination.setTotalDocuments(if (node.isJsonArray) node.asJsonArray.size() else 1)
//            if (ResultsPerPage.ALL == pagination.resultsPerPage) {
//                pageNumberLabel.isVisible = false
//            } else {
//                pageNumberLabel.text = String.format(
//                    "Page %d/%d",
//                    pagination.pageNumber,
//                    pagination.totalPageNumber
//                )
//                pageNumberLabel.isVisible = true
//            }
//        }
    }

    private fun createResultPanel(project: Project): JsonResultPanel {
        return JsonResultPanel(project, node)
    }

    private fun updateToolBar() {
        toolBar.invalidate()
        toolBar.removeAll()
        toolBar.layout = BorderLayout()
        val actionToolBarComponent = createActions()
        toolBar.add(actionToolBarComponent, BorderLayout.EAST)
        toolBar.add(title, BorderLayout.CENTER)
        toolBar.validate()
    }

    private fun initPaginationPanel() {
        paginationPanel.layout = BorderLayout()
        val actionToolbarComponent = createPaginationActionsComponent()
        paginationPanel.add(actionToolbarComponent, BorderLayout.CENTER)
        val panel = JPanel()
        panel.add(pageNumberLabel)
        panel.add(GuiUtils.createVerticalStrut())
        panel.add(rowCountLabel)
        paginationPanel.add(panel, BorderLayout.EAST)
    }

//    private fun updateJsonElementPagination() {
//        val jsonArray = jsonElement.asJsonArray
//        val newArray = JsonArray()
//        for (i in pagination.countPerPage * (pagination.pageNumber - 1) until min(
//            pagination.countPerPage * pagination.pageNumber,
//            jsonArray.size()
//        )) {
//            newArray.add(jsonArray[i])
//        }
//        resultPanel.updateResultView(newArray, pagination.startIndex)
//    }

    private fun createActions(): JComponent {
        val actionResultGroup = DefaultActionGroup("JsonResultGroup", true)
        actionResultGroup.add(FormatJsonAction(resultPanel))
        addBasicTreeActions(actionResultGroup)
        actionResultGroup.addSeparator()
        actionResultGroup.add(CopyAllAction(resultPanel))
        actionResultGroup.addSeparator()
        actionResultGroup.add(ViewAsInputAction(this))
        actionResultGroup.add(ViewAsTreeAction(this))
        actionResultGroup.add(ViewAsTableAction(this))
        actionResultGroup.addSeparator()
        actionResultGroup.add(CloseJsonPanelAction(this))
        return createToolbarComponent(actionResultGroup, "JsonResultGroupActions")
    }

    private fun createPaginationActionsComponent(): JComponent {
        val actionResultGroup = DefaultActionGroup("PaginationGroup", false)
        actionResultGroup.add(ChangeResultsPerPageActionComponent { PaginationPopupComponent(pagination).initUi() })
        actionResultGroup.add(PaginationAction.Previous(pagination))
        actionResultGroup.add(PaginationAction.Next(pagination))
        return createToolbarComponent(actionResultGroup, "PaginationGroupActions")
    }

    private fun addBasicTreeActions(actionResultGroup: DefaultActionGroup) {
        val treeExpander: TreeExpander = object : TreeExpander {
            override fun expandAll() {
                resultPanel.activeView.expandAll()
            }

            override fun canExpand(): Boolean {
                return resultPanel.currentViewMode == JsonResultPanel.ViewMode.TREE
            }

            override fun collapseAll() {
                resultPanel.activeView.collapseAll()
            }

            override fun canCollapse(): Boolean {
                return resultPanel.currentViewMode == JsonResultPanel.ViewMode.TREE
            }
        }
        val actionsManager = CommonActionsManager.getInstance()
        val expandAllAction = actionsManager.createExpandAllAction(treeExpander, resultPanel)
        val collapseAllAction = actionsManager.createCollapseAllAction(treeExpander, resultPanel)
        Disposer.register(this) {
            collapseAllAction.unregisterCustomShortcutSet(resultPanel)
            expandAllAction.unregisterCustomShortcutSet(resultPanel)
        }
        actionResultGroup.addSeparator()
        actionResultGroup.add(expandAllAction)
        actionResultGroup.add(collapseAllAction)
    }

    private fun createToolbarComponent(actionGroup: DefaultActionGroup, name: String): JComponent {
        val viewToolbar = ActionManager.getInstance().createActionToolbar(name, actionGroup, true)
        viewToolbar.targetComponent = viewToolbar.component
        viewToolbar.layoutPolicy = ActionToolbar.AUTO_LAYOUT_POLICY
        val viewToolbarComponent = viewToolbar.component
        viewToolbarComponent.border = null
        viewToolbarComponent.isOpaque = false
        return viewToolbarComponent
    }

    val isCloseActionActive: Boolean
        get() = parent != null && parent is Splitter

    fun close() {
        Disposer.dispose(((parent as Splitter).secondComponent as Disposable))
        (parent as Splitter).secondComponent = null
    }

    override fun dispose() {
        resultPanel.dispose()
    }

    fun setViewMode(viewMode: JsonResultPanel.ViewMode) {
//        val prevViewMode = resultPanel.currentViewMode
        if (resultPanel.currentViewMode == viewMode) {
            return
        }
        resultPanel.changeViewMode(viewMode)
        UIUtil.invokeLaterIfNeeded {
//            jsonElement = JsonParser.parseString(resultPanel.stringifiedResult(prevViewMode))
//            updateToolBar()

//            resultPanel.updateResultView(jsonElement, pagination.startIndex)
//            rowCountLabel.text = String.format(
//                "%s documents",
//                if (node.isJsonArray) node.asJsonArray.size() else 1
//            )
        }
    }

    val content: JComponent
        get() = rootPanel

    private class ChangeResultsPerPageActionComponent(private val myComponentCreator: Computable<JComponent>) :
        DumbAwareAction(), CustomComponentAction {
        override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
            return myComponentCreator.compute()
        }

        override fun actionPerformed(e: AnActionEvent) {}
    }

    companion object {
        private fun extractDocuments(documents: JsonElement?, pagination: Pagination?): JsonElement? {
            if (documents != null && documents.isJsonArray && pagination != null) {
                documents as JsonArray
                if (ResultsPerPage.ALL == pagination.resultsPerPage) {
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
            return documents
        }
    }

    override fun treeNodesChanged(e: TreeModelEvent?) {
//        resultPanel.refresh()
    }

    override fun treeNodesInserted(e: TreeModelEvent?) {
        TODO("Not yet implemented")
    }

    override fun treeNodesRemoved(e: TreeModelEvent?) {
        TODO("Not yet implemented")
    }

    override fun treeStructureChanged(e: TreeModelEvent?) {
        TODO("Not yet implemented")
    }
}