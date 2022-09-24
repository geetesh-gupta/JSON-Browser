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

package com.gg.plugins.json.utils;

import com.gg.plugins.json.model.JsonTreeNode;
import com.gg.plugins.json.model.KeyValueDescriptor;
import com.gg.plugins.json.model.ValueDescriptor;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.swing.tree.TreeNode;

public class JsonTreeUtils {

	public static TreeNode buildJsonTree(JsonArray documents, int startIndex) {

		JsonTreeNode rootNode = new JsonTreeNode(KeyValueDescriptor.createDescriptor("<root>", ""));
		int i = startIndex;
		for (JsonElement document : documents) {
			JsonTreeNode currentNode = new JsonTreeNode(ValueDescriptor.createDescriptor(i++, document));
			processJsonElement(currentNode, document);
			rootNode.add(currentNode);
		}
		return rootNode;
	}

	public static TreeNode buildJsonTree(JsonElement document) {
		JsonTreeNode rootNode = new JsonTreeNode(KeyValueDescriptor.createDescriptor("<root>", ""));
		if (document.isJsonArray()) {
			return buildJsonTree(document.getAsJsonArray(), 0);
		} else if (document.isJsonObject()) {
			processJsonElement(rootNode, document);
		}
		return rootNode;
	}

	public static void processJsonElement(JsonTreeNode parentNode, JsonElement document) {
		if (document.isJsonArray()) {
			processJsonArray(parentNode, document.getAsJsonArray());
		} else if (document.isJsonObject()) {
			processJsonObject(parentNode, document.getAsJsonObject());
		}
	}

	public static void processJsonObject(JsonTreeNode parentNode, JsonObject document) {
		for (String key : document.keySet()) {
			JsonElement value = document.get(key);
			JsonTreeNode currentNode = new JsonTreeNode(KeyValueDescriptor.createDescriptor(key, value));
			processJsonElement(currentNode, value);
			parentNode.add(currentNode);
		}
	}

	public static void processJsonArray(JsonTreeNode parentNode, JsonArray documents) {
		for (int i = 0; i < documents.size(); i++) {
			JsonElement value = documents.get(i);
			JsonTreeNode currentNode = new JsonTreeNode(ValueDescriptor.createDescriptor(i, value));
			processJsonElement(currentNode, value);
			parentNode.add(currentNode);
		}
	}

}
