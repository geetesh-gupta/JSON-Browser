package com.gg.plugins.json.view.resultpanel

import com.gg.plugins.json.model.JsonTreeNode
import com.gg.plugins.json.utils.JsonTreeUtils
import com.gg.plugins.json.utils.createEditor
import com.gg.plugins.json.utils.formatJson
import com.google.gson.JsonParser
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import java.awt.Component

class JsonView(private val rootNode: JsonTreeNode, var project: Project) : IResultPanelState {
    private var editor: Editor = createEditorObject(rootNode)

    override fun getSelected(): JsonTreeNode {
        return JsonTreeUtils.buildJsonTree(JsonParser.parseString(editor.selectionModel.getSelectedText(true)))
    }

    override fun getAll(): JsonTreeNode {
//        JsonTreeUtils.updateNode(rootNode, JsonParser.parseString(editor.document.text))
        rootNode.setValue(JsonParser.parseString(editor.document.text))
        rootNode.refreshNodeViaValue()
        return rootNode
    }

    override fun refresh() {
//        JsonTreeUtils.updateNode(rootNode)
        rootNode.refreshNodeViaChildren()
        val r = Runnable { editor.document.setText(formatJson(JsonTreeUtils.convertTreeNodeToJsonElement(rootNode))) }
        WriteCommandAction.runWriteCommandAction(project, r)
    }

    private fun createEditorObject(node: JsonTreeNode?): Editor {
        return createEditor(JsonTreeUtils.convertTreeNodeToJsonElement(node), project)
    }

    override fun getView(): Component {
        return editor.component
    }

    override fun expandAll() {
    }

    override fun collapseAll() {
    }

    override fun dispose() {
    }

    override fun getSelectedColumnName(): String {
        return getSelected().userObject.key
    }

}