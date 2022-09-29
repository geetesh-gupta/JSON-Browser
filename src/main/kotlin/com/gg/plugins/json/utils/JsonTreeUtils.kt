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
import com.gg.plugins.json.model.ValueDescriptor
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import javax.swing.tree.TreeNode

object JsonTreeUtils {
    fun buildJsonTree(documents: JsonArray, startIndex: Int): TreeNode {
        val rootNode = JsonTreeNode(KeyValueDescriptor.createDescriptor("<root>", ""))
        var i = startIndex
        for (document in documents) {
            val currentNode = JsonTreeNode(ValueDescriptor.createDescriptor(i++, document))
            processJsonElement(currentNode, document)
            rootNode.add(currentNode)
        }
        return rootNode
    }

    @JvmStatic
    fun buildJsonTree(document: JsonElement?): TreeNode {
        val rootNode = JsonTreeNode(KeyValueDescriptor.createDescriptor("<root>", ""))
        if (document!!.isJsonArray) {
            return buildJsonTree(document.asJsonArray, 0)
        } else if (document.isJsonObject) {
            processJsonElement(rootNode, document)
        }
        return rootNode
    }

    @JvmStatic
    fun processJsonElement(parentNode: JsonTreeNode, document: JsonElement?) {
        if (document!!.isJsonArray) {
            processJsonArray(parentNode, document.asJsonArray)
        } else if (document.isJsonObject) {
            processJsonObject(parentNode, document.asJsonObject)
        }
    }

    @JvmStatic
    fun processJsonObject(parentNode: JsonTreeNode, document: JsonObject) {
        for (key in document.keySet()) {
            val value = document[key]
            val currentNode = JsonTreeNode(KeyValueDescriptor.createDescriptor(key, value))
            processJsonElement(currentNode, value)
            parentNode.add(currentNode)
        }
    }

    @JvmStatic
    fun processJsonArray(parentNode: JsonTreeNode, documents: JsonArray) {
        for (i in 0 until documents.size()) {
            val value = documents[i]
            val currentNode = JsonTreeNode(ValueDescriptor.createDescriptor(i, value))
            processJsonElement(currentNode, value)
            parentNode.add(currentNode)
        }
    }
}