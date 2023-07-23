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
import com.gg.plugins.json.view.nodedescriptor.KeyValueDescriptor
import com.gg.plugins.json.view.nodedescriptor.ValueDescriptor
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.stream.Collectors
import java.util.stream.IntStream

object JsonTreeUtils {

    @JvmStatic
    fun buildJsonTree(obj: JsonElement?, parent: JsonTreeNode? = null, startIndex: Int = 0): JsonTreeNode {
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
        parentNode.removeAllChildren()
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
        parentNode.removeAllChildren()
        for (i in startIndex until objArr.size()) {
            val value = objArr[i]
            val currentNode = JsonTreeNode(ValueDescriptor.createDescriptor(i, value))
            buildJsonTree(value, currentNode)
            parentNode.add(currentNode)
        }
        return parentNode
    }

    fun stringifyTree(treeNode: JsonTreeNode): String {
        if (treeNode.getKey() == "<root>") {
            val firstChildNode = treeNode.getChildAt(0) ?: return ""
            when (firstChildNode.userObject) {
                is KeyValueDescriptor -> {
                    return IntStream.range(
                        0,
                        treeNode.childCount
                    ).mapToObj {
                        val childNode = treeNode.getChildAt(it)
                        "\"${childNode.userObject.key}\" : $childNode"
                    }
                        .collect(Collectors.joining(",", "{", "}"))
                }

                else -> {
                    return IntStream.range(0, treeNode.childCount)
                        .mapToObj { i: Int -> treeNode.getChildAt(i).getValue().toString() }
                        .collect(Collectors.joining(",", "[", "]"))
                }
            }
        }
        return treeNode.getValue().toString()
    }
//
//    fun updateNode(node: JsonTreeNode, value: Any) {
//        updateChildren(node, value)
//        if (node.parent != null)
//            updateParents(node.parent as JsonTreeNode, node)
//    }
//
//    fun updateNode(node: JsonTreeNode) {
//        updateChildren(node, stringifyTree(node))
//        if (node.parent != null)
//            updateParents(node.parent as JsonTreeNode, node)
//    }
//
//    private fun isRootNode(node: JsonTreeNode): Boolean {
//        return node.getKey() == "<root>"
//    }
//
//    private fun updateParents(node: JsonTreeNode, childNode: JsonTreeNode) {
//        if (!isRootNode(node)) {
//            val childIndex = node.getIndex(childNode)
//            // TODO: Update this based on TreeNode type
//            when (node.getValue()) {
//
//                is JsonObject -> {
//                    (node.getValue() as JsonObject).add(
//                        childNode.getKey(),
//                        JsonParser.parseString(childNode.getValue().toString())
//                    )
//                }
//
//                is JsonArray -> {
//                    (node.getValue() as JsonArray).set(
//                        childIndex,
//                        JsonParser.parseString(childNode.getValue().toString())
//                    )
//                }
//            }
//            if (node.parent != null)
//                updateParents(node.parent as JsonTreeNode, node)
//        }
//    }
//
//    private fun updateChildren(node: JsonTreeNode, value: Any) {
//        node.setValue(value)
//        node.removeAllChildren()
//        val elem: JsonElement = JsonParser.parseString(node.getValue().toString())
//        buildJsonTree(elem, node)
//    }

    fun convertTreeNodeToJsonElement(treeNode: JsonTreeNode?): JsonElement {
        if (treeNode == null) return JsonParser.parseString("null")
        return JsonParser.parseString(treeNode.stringify())
    }
}