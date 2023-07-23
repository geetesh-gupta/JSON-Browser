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
package com.gg.plugins.json.model

import com.gg.plugins.json.view.nodedescriptor.KeyValueDescriptor
import com.gg.plugins.json.view.nodedescriptor.NodeDescriptor
import com.gg.plugins.json.view.nodedescriptor.ValueDescriptor
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreeNode

class JsonTreeNode(val descriptor: NodeDescriptor) : DefaultMutableTreeNode(descriptor) {
    private val keyToIndexMap: HashMap<String, Int> = HashMap()

    companion object {
        fun createRootNode(): JsonTreeNode {
            return JsonTreeNode(KeyValueDescriptor.createDescriptor("<root>", "<root>"))
        }
    }

    override fun getChildAt(index: Int): JsonTreeNode {
        return when {
            !isLeaf && index < childCount -> super.getChildAt(index) as JsonTreeNode
            else -> throw ArrayIndexOutOfBoundsException(index)
        }
    }

    private fun getChildType(): Class<out NodeDescriptor>? {
        if (isLeaf) return null
        when (getChildAt(0).userObject) {
            is KeyValueDescriptor -> {
                return KeyValueDescriptor::class.java
            }

            is ValueDescriptor -> {
                return ValueDescriptor::class.java
            }
        }
        throw Exception("Invalid child node: " + this.userObject)
    }

    override fun getParent(): JsonTreeNode? {
        return super.getParent() as? JsonTreeNode
    }

    override fun getUserObject(): NodeDescriptor {
        return super.getUserObject() as NodeDescriptor
    }

    override fun add(newChild: MutableTreeNode?) {
        keyToIndexMap[((newChild as JsonTreeNode).userObject as NodeDescriptor).key] = childCount
        super.add(newChild)
    }

    fun containsChildWithKey(key: String): Boolean {
        return keyToIndexMap.containsKey(key)
    }

    fun getChildByKey(key: String): JsonTreeNode {
        if (!containsChildWithKey(key)) throw Exception("$key not present in child")
        return getChildAt(keyToIndexMap[key]!!)
    }

    override fun getPath(): Array<JsonTreeNode>? {
        return getPathToRoot(this, 0)
    }

    override fun getPathToRoot(aNode: TreeNode?, depth: Int): Array<JsonTreeNode>? {
        val retNodes: Array<JsonTreeNode>?

        when (aNode) {
            null -> {
                retNodes = if (depth == 0) null else emptyArray()
            }

            else -> {
                val newDepth = depth + 1

                retNodes = getPathToRoot(aNode.parent, newDepth)
                if (retNodes != null)
                    retNodes[retNodes.size - newDepth] = aNode as JsonTreeNode
            }
        }
        return retNodes
    }

    override fun toString(): String {
        return getKey()
    }

    fun getValue(): Any? {
        return getUserObject().value
    }

    fun getKey(): String {
        return getUserObject().key
    }

    fun setValue(value: Any?) {
        getUserObject().value = value
    }

    fun refreshNodeViaValue() {
        updateChildrenFromValue()
    }

    fun refreshNodeViaChildren() {
        updateValueFromChildren()
        updateParentValue()
    }

    @Suppress("UNCHECKED_CAST")
    private fun updateParentValue() {
        val parentNode = getParent() ?: return
        if (parentNode.isRoot) return
        when (parentNode.getChildType()) {
            KeyValueDescriptor::class.java -> {
                (parentNode.getValue() as HashMap<String, Any?>)[getKey()] = getValue()
            }

            ValueDescriptor::class.java -> {
                (parentNode.getValue() as MutableList<Any?>)[parentNode.getIndex(this)] = getValue()
            }
        }
        updateParentValue()
    }

    private fun updateValueFromChildren() {
        setValue(createValueFromChildren())
    }

    fun createValueFromChildren(): Any? {
        if (isLeaf) return null
        var childValue: Any? = null
        when (getChildType()) {
            KeyValueDescriptor::class.java -> {
                val childKeyValueMap = HashMap<String, Any?>()
                children().iterator().forEach {
                    it as JsonTreeNode
                    childKeyValueMap[it.getKey()] = it.getValue()
                }
                childValue = childKeyValueMap
            }

            ValueDescriptor::class.java -> {
                val childList = ArrayList<Any?>()
                children().iterator().forEach {
                    it as JsonTreeNode
                    childList.add(it)
                }
                childValue = childList
            }
        }
        return childValue
    }

    private fun updateChildrenFromValue() {
        removeAllChildren()
        when (getValue()) {
            is Map<*, *> -> {
                (getValue() as Map<*, *>).entries.forEach {
                    add(JsonTreeNode(KeyValueDescriptor.createDescriptor(it.key as String, it.value)))
                }
            }

            is List<*> -> {
                (getValue() as List<Any?>).forEachIndexed { index, it ->4
                    add(JsonTreeNode(ValueDescriptor.createDescriptor(index, it)))
                }
            }
        }
    }

    fun stringify(): String {
        if (isRoot) {
            return createValueFromChildren().toString()
        }
        return getValue().toString()
    }
}