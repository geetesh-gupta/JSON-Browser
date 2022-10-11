package com.gg.plugins.json.view.resultpanel

import com.gg.plugins.json.model.JsonTreeNode
import com.intellij.openapi.Disposable
import java.awt.Component

interface IResultPanelState : Disposable {
    fun getSelected(): JsonTreeNode?
    fun getAll(): JsonTreeNode

    fun getSelectedStringified(): String {
        val result = getSelected().toString()
        return result.replace("^\"|\"$".toRegex(), "")
    }

    fun getAllStringified(): String {
        return getAll().toString()
    }

    fun getSelectedColumnName(): String

//    fun updateView(node: JsonTreeNode?, startIndex: Int = 0)

    fun getView(): Component

    fun expandAll()
    fun collapseAll()
    fun refresh()
}