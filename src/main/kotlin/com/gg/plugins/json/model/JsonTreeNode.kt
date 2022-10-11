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

import com.gg.plugins.json.view.nodedescriptor.NodeDescriptor
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.MutableTreeNode

class JsonTreeNode(val descriptor: NodeDescriptor) : DefaultMutableTreeNode(descriptor) {
    private val keyToIndexMap: HashMap<String, Int> = HashMap()
    override fun getChildAt(index: Int): JsonTreeNode? {
        return when {
            childCount > 0 && index < childCount -> super.getChildAt(index) as JsonTreeNode
            else -> null
        }
    }

    override fun getUserObject(): NodeDescriptor {
        return super.getUserObject() as NodeDescriptor
    }

    override fun add(newChild: MutableTreeNode?) {
        keyToIndexMap[((newChild as JsonTreeNode).userObject as NodeDescriptor).key] = childCount
        super.add(newChild)
    }

    fun getChildByKey(key: String): JsonTreeNode? {
        if (!keyToIndexMap.containsKey(key)) return null
        return getChildAt(keyToIndexMap[key]!!)
    }
}