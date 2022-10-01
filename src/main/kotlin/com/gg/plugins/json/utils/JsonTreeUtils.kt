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
package com.gg.plugins.json.utils

import com.gg.plugins.json.model.JsonTreeNode
import com.gg.plugins.json.model.KeyValueDescriptor
import com.gg.plugins.json.model.NodeDescriptor
import com.gg.plugins.json.model.ValueDescriptor
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.stream.Collectors
import java.util.stream.IntStream
import javax.swing.tree.TreeNode

object JsonTreeUtils {

    @JvmStatic
    fun buildJsonTree(obj: JsonElement?, parent: JsonTreeNode? = null, startIndex: Int = 0): TreeNode {
        val parentNode: JsonTreeNode = parent ?: JsonTreeNode(KeyValueDescriptor.createDescriptor("<root>", ""))

        when {
            obj == null -> {
                return parentNode
            }

            obj.isJsonArray -> {
                return processJsonArray(obj.asJsonArray, parentNode, startIndex)
            }

            obj.isJsonObject -> {
                return processJsonObject(obj.asJsonObject, parentNode)
            }

            else -> {
                return parentNode
            }
        }
    }

    @JvmStatic
    fun processJsonObject(obj: JsonObject, parentNode: JsonTreeNode): JsonTreeNode {
        for (key in obj.keySet()) {
            val value = obj[key]
            val currentNode = JsonTreeNode(KeyValueDescriptor.createDescriptor(key, value))
            buildJsonTree(value, currentNode)
            parentNode.add(currentNode)
        }
        return parentNode
    }

    @JvmStatic
    fun processJsonArray(objArr: JsonArray, parentNode: JsonTreeNode, startIndex: Int = 0): JsonTreeNode {
        for (i in startIndex until objArr.size()) {
            val value = objArr[i]
            val currentNode = JsonTreeNode(ValueDescriptor.createDescriptor(i, value))
            buildJsonTree(value, currentNode)
            parentNode.add(currentNode)
        }
        return parentNode
    }

    fun stringifyTree(treeNode: JsonTreeNode): String {
        if (treeNode.descriptor.key == "<root>") {
            if (treeNode.childCount == 0) return ""
            return if ((treeNode.getChildAt(0) as JsonTreeNode).descriptor is KeyValueDescriptor) IntStream.range(
                0,
                treeNode.childCount
            ).mapToObj { i: Int ->
                val childNode = (treeNode.getChildAt(i) as JsonTreeNode).userObject as NodeDescriptor
                "\"${childNode.key}\" : $childNode"
            }
                .collect(Collectors.joining(",", "{", "}")) else IntStream.range(0, treeNode.childCount)
                .mapToObj { i: Int -> treeNode.getChildAt(i).toString() }
                .collect(Collectors.joining(",", "[", "]"))
        }
        return treeNode.descriptor.toString()
    }

    fun updateNode(node: JsonTreeNode, value: Any) {
        updateChildren(node, value)
        updateParents(node.parent as JsonTreeNode, node)
    }

    private fun isRootNode(node: JsonTreeNode): Boolean {
        return node.descriptor.key == "<root>"
    }

    private fun updateParents(node: JsonTreeNode, childNode: JsonTreeNode) {
        if (!isRootNode(node)) {
            val childIndex = node.getIndex(childNode)
            when (node.descriptor.value) {
                is JsonObject -> {
                    (node.descriptor.value as JsonObject).add(
                        childNode.descriptor.key,
                        JsonParser.parseString(childNode.descriptor.value as String)
                    )
                }

                is JsonArray -> {
                    (node.descriptor.value as JsonArray).set(
                        childIndex,
                        JsonParser.parseString(childNode.descriptor.value.toString())
                    )
                }
            }
            updateParents(node.parent as JsonTreeNode, node)
        }
    }

    private fun updateChildren(node: JsonTreeNode, value: Any) {
        node.descriptor.value = value
        node.removeAllChildren()
        val elem: JsonElement = JsonParser.parseString(node.descriptor.toString())
        buildJsonTree(elem, node)
    }
}