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

import com.gg.plugins.json.action.CopyNodeAction
import com.gg.plugins.json.action.OpenInJsonPanelAction
import com.gg.plugins.json.action.SaveAction
import com.gg.plugins.json.model.JsonTreeNode
import com.gg.plugins.json.service.Notifier
import com.gg.plugins.json.view.JsonPanel
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Splitter
import com.intellij.ui.JBCardLayout
import com.intellij.ui.PopupHandler
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener

class JsonResultPanel(
    private val project: Project,
    var node: JsonTreeNode,
    var currentViewMode: ViewMode = ViewMode.INPUT,
    var startIndex: Int = 0
) : JPanel(), Disposable, TreeModelListener {
    var activeView: IResultPanelState = JsonView(node, project)
    private val notifier: Notifier = Notifier.getInstance(project)
    private val resultPanel = JPanel(BorderLayout())
    private val actionCallback: ActionCallback

    lateinit var mainPanel: JPanel
    lateinit var containerPanel: JPanel

    var subPanel: JsonPanel? = null
    var parentPanel: JsonResultPanel? = null

    init {
        layout = BorderLayout()
        add(mainPanel, BorderLayout.CENTER)
        containerPanel.layout = JBCardLayout()
        containerPanel.add(resultPanel)

        changeViewMode(currentViewMode)

        actionCallback = object : ActionCallback {
            override fun onOperationSuccess(label: String?, message: String?) {
                notifier.notifyInfo(message)
            }

            override fun onOperationFailure(exception: Exception?) {
                notifier.notifyError(exception?.message)
            }
        }

        initActions()
    }

    private fun initActions() {
        val actionPopupGroup = DefaultActionGroup("JsonPanelPopupGroup", true)
        if (ApplicationManager.getApplication() != null) {
            actionPopupGroup.add(CopyNodeAction(this))
            actionPopupGroup.add(OpenInJsonPanelAction(this))
            actionPopupGroup.add(SaveAction(this))
            PopupHandler.installPopupMenu(activeView.getView() as JComponent, actionPopupGroup, ActionPlaces.POPUP)
        }
    }

    fun addDataChangeListener(listener: TreeModelListener) {
        listenerList.add(TreeModelListener::class.java, listener)
    }

    fun updateResultView(node: JsonTreeNode, startIndex: Int = 0) {
        this.node = node
        this.startIndex = startIndex

        refresh()
        refreshParents(false)
        refreshChildren(false)
    }

    private fun refresh() {
        this.activeView.refresh()
    }

    private fun refreshParents(refreshSelf: Boolean = true) {
        if (refreshSelf) refresh()
        parentPanel?.refreshParents()
    }

    private fun refreshChildren(refreshSelf: Boolean = true) {
        if (refreshSelf) refresh()
        subPanel?.resultPanel?.refreshChildren()
    }

    private fun fireDataChanged(source: Any?) {
        val listeners = listenerList.listenerList
        var e: TreeModelEvent? = null
        var i = listeners.size - 2
        while (i >= 0) {
            if (listeners[i] === TreeModelListener::class.java) {
                if (e == null) e = TreeModelEvent(source, node.path)
                (listeners[i + 1] as TreeModelListener).treeNodesChanged(e)
            }
            i -= 2
        }
    }

    fun changeViewMode(newViewMode: ViewMode) {
        when (newViewMode) {
            ViewMode.TREE -> {
                this.currentViewMode = ViewMode.TREE
                this.activeView = JsonTreeView(node)
            }

            ViewMode.TABLE -> {
                this.currentViewMode = ViewMode.TABLE
                this.activeView = JsonTableView(node)
            }

            else -> {
                this.currentViewMode = ViewMode.INPUT
                this.activeView = JsonView(node, project)
            }
        }
        displayResult()
    }

    private fun displayResult() {
        resultPanel.invalidate()
        resultPanel.removeAll()
        resultPanel.add(JBScrollPane(this.activeView.getView()))
        resultPanel.validate()
    }

    override fun dispose() {
        activeView.dispose()
    }

    enum class ViewMode {
        TREE, TABLE, INPUT
    }

    interface ActionCallback {
        fun onOperationSuccess(label: String?, message: String?)
        fun onOperationFailure(exception: Exception?)
    }

    fun openInSubJsonPanel() {
        val selected = activeView.getSelected() ?: return
        subPanel = JsonPanel(
            project,
            selected,
            activeView.getSelectedColumnName()
        )
        subPanel!!.let {
            it.resultPanel.parentPanel = this
            it.resultPanel.addDataChangeListener(this)
//            addDataChangeListener(it.resultPanel)
            (parent.parent as Splitter).secondComponent = it
        }
    }

    override fun treeNodesChanged(e: TreeModelEvent?) {
        refresh()
        refreshParents()
        refreshChildren()
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