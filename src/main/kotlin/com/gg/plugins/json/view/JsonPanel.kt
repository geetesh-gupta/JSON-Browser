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
import com.gg.plugins.json.model.Pagination
import com.gg.plugins.json.model.ResultsPerPage
import com.gg.plugins.json.utils.JsonTreeUtils
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.ide.CommonActionsManager
import com.intellij.ide.TreeExpander
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.LoadingDecorator
import com.intellij.openapi.ui.Splitter
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.GuiUtils
import com.intellij.ui.PopupHandler
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import kotlin.math.min

class JsonPanel(private val project: Project, jsonElement: JsonElement) : JPanel(), Disposable {
    private val rowCountLabel = JBLabel()
    private val pageNumberLabel = JBLabel()
    private val resultPanel: JsonResultPanel
    private val pagination: Pagination = Pagination()
    private var jsonElement: JsonElement
    private var rootPanel: JPanel? = null
    private var splitter: Splitter? = null
    private var toolBar: JPanel? = null
    private var errorPanel: JPanel? = null
    private var paginationPanel: JPanel? = null
    private var title: JLabel? = null

    constructor(project: Project, jsonElement: JsonElement, titleText: String?) : this(project, jsonElement) {
        title!!.text = titleText
    }

    init {
        this.jsonElement = jsonElement
        errorPanel!!.layout = BorderLayout()
        resultPanel = createResultPanel(project)
        val loadingDecorator = LoadingDecorator(resultPanel, this, 0)
        splitter!!.orientation = true
        splitter!!.proportion = 0.2f
        splitter!!.firstComponent = loadingDecorator.component
        layout = BorderLayout()
        add(rootPanel)
        updateToolBar()
        initPaginationPanel()
        pagination.addSetPageListener { updateJsonElementPagination() }
        pagination.addSetPageListener {
            pagination.setTotalDocuments(if (jsonElement.isJsonArray) jsonElement.asJsonArray.size() else 1)
            if (ResultsPerPage.ALL == pagination.resultsPerPage) {
                pageNumberLabel.isVisible = false
            } else {
                pageNumberLabel.text = String.format(
                    "Page %d/%d",
                    pagination.pageNumber,
                    pagination.totalPageNumber
                )
                pageNumberLabel.isVisible = true
            }
        }
    }

    private fun createResultPanel(project: Project): JsonResultPanel {
        return JsonResultPanel(project, jsonElement)
    }

    private fun updateToolBar() {
        toolBar?.let {
            it.invalidate()
            it.removeAll()
            it.layout = BorderLayout()
            val actionToolBarComponent = createActions()
            it.add(actionToolBarComponent, BorderLayout.EAST)
            title?.let { title -> it.add(title, BorderLayout.CENTER) }
            it.validate()
        }
    }

    private fun initPaginationPanel() {
        paginationPanel?.let {
            it.layout = BorderLayout()
            val actionToolbarComponent = createPaginationActionsComponent()
            it.add(actionToolbarComponent, BorderLayout.CENTER)
            val panel = JPanel()
            panel.add(pageNumberLabel)
            panel.add(GuiUtils.createVerticalStrut())
            panel.add(rowCountLabel)
            it.add(panel, BorderLayout.EAST)
        }
    }

    private fun updateJsonElementPagination() {
        val jsonArray = jsonElement.asJsonArray
        val newArray = JsonArray()
        for (i in pagination.countPerPage * (pagination.pageNumber - 1) until min(
            pagination.countPerPage * pagination.pageNumber,
            jsonArray.size()
        )) {
            newArray.add(jsonArray[i])
        }
        resultPanel.updateResultView(newArray, pagination)
    }

    private fun createActions(): JComponent {
        val actionResultGroup = DefaultActionGroup("JsonResultGroup", true)
        if (resultPanel.currentViewMode == JsonResultPanel.ViewMode.INPUT) {
            actionResultGroup.add(FormatJsonAction(resultPanel))
        }
        if (resultPanel.currentViewMode == JsonResultPanel.ViewMode.TREE) {
            addBasicTreeActions(actionResultGroup)
        }
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
                resultPanel.expandAll()
            }

            override fun canExpand(): Boolean {
                return true
            }

            override fun collapseAll() {
                resultPanel.collapseAll()
            }

            override fun canCollapse(): Boolean {
                return true
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
        val prevViewMode = resultPanel.currentViewMode
        if (resultPanel.currentViewMode == viewMode) {
            return
        }
        resultPanel.currentViewMode = viewMode
        UIUtil.invokeLaterIfNeeded {
            jsonElement = JsonParser.parseString(resultPanel.stringifiedResult(prevViewMode))

            updateToolBar()
            resultPanel.updateResultView(jsonElement, pagination)
            rowCountLabel.text = String.format(
                "%s documents",
                if (jsonElement.isJsonArray) jsonElement.asJsonArray.size() else 1
            )
            initActions(resultPanel)
        }
    }

    private fun initActions(jsonResultPanel: JsonResultPanel) {
        val actionPopupGroup = DefaultActionGroup("JsonPanelPopupGroup", true)
        if (ApplicationManager.getApplication() != null) {
            actionPopupGroup.add(CopyNodeAction(resultPanel))
        }

        jsonResultPanel.resultTableView?.let { tableView ->
            tableView.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(mouseEvent: MouseEvent) {
                    if (mouseEvent.clickCount == 2) {
                        val selectedColumnNum = tableView.selectedColumn
                        val selectedColumnName = tableView.columnModel
                            .getColumn(selectedColumnNum)
                            .headerValue
                            .toString()
                        val childJsonElement =
                            tableView.selectedObject?.get(selectedColumnName)
                        if (childJsonElement != null && (childJsonElement.isJsonArray || childJsonElement.isJsonObject)) {
                            val subPanel = JsonPanel(
                                project, childJsonElement, selectedColumnName
                            )
                            subPanel.addPropertyChangeListener("ancestor") { event ->
                                if (event.newValue == null) {
                                    UIUtil.invokeLaterIfNeeded {
                                        val parentPanel: JsonResultPanel =
                                            ((event.oldValue as Splitter).firstComponent.getComponent(0) as JsonResultPanel)
                                        val subPanelNewVal =
                                            ((event.oldValue as Splitter).secondComponent as JsonPanel).resultPanel.stringifiedResult()
                                        when (parentPanel.currentViewMode) {
                                            JsonResultPanel.ViewMode.INPUT -> {
                                                val activeJson = (parentPanel.editorValue() as JsonArray)
                                                val activeObject =
                                                    activeJson.get(tableView.selectedRow) as JsonObject
                                                activeObject.add(
                                                    selectedColumnName,
                                                    JsonParser.parseString(subPanelNewVal)
                                                )
                                                parentPanel.updateResultView(activeJson, pagination)
                                            }

                                            JsonResultPanel.ViewMode.TREE -> {
                                                val activeNode =
                                                    (parentPanel.resultTreeTableView!!.tableModel.root as JsonTreeNode).getChildAt(
                                                        tableView.selectedRow
                                                    ) as JsonTreeNode
                                                val activeObject = activeNode.descriptor.value as JsonObject
                                                activeObject.add(
                                                    selectedColumnName,
                                                    JsonParser.parseString(subPanelNewVal)
                                                )
                                                JsonTreeUtils.updateNode(activeNode, activeObject)
                                            }

                                            JsonResultPanel.ViewMode.TABLE -> {
                                                val parentPanelTableView = parentPanel.resultTableView
                                                parentPanelTableView?.selectedObject?.add(
                                                    selectedColumnName,
                                                    JsonParser.parseString(subPanelNewVal)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            splitter!!.secondComponent = subPanel
                        }
                    }
                }
            })

            PopupHandler.installPopupMenu(tableView, actionPopupGroup, ActionPlaces.POPUP)
        }

        jsonResultPanel.resultTreeTableView?.let {
            PopupHandler.installPopupMenu(it, actionPopupGroup, ActionPlaces.POPUP)
        }
    }

    val content: JComponent?
        get() = rootPanel

    private class ChangeResultsPerPageActionComponent(private val myComponentCreator: Computable<JComponent>) :
        DumbAwareAction(), CustomComponentAction {
        override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
            return myComponentCreator.compute()
        }

        override fun actionPerformed(e: AnActionEvent) {}
    }
}