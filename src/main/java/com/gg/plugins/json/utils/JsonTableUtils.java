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

import com.gg.plugins.json.renderer.TableCellRenderer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

public class JsonTableUtils {

	public static ListTableModel<JsonObject> buildJsonTable(JsonElement document) {

		ColumnInfo<?, ?>[] columnInfos = extractColumnNames(document);

		if (document.isJsonArray()) {
			List<JsonObject> columnInfoList = new ArrayList<>();
			document.getAsJsonArray().forEach(d -> columnInfoList.add(d.getAsJsonObject()));
			return new ListTableModel<>(columnInfos, columnInfoList);
		} else {
			return new ListTableModel<>(columnInfos, Collections.singletonList(document.getAsJsonObject()));
		}
	}

	private static ColumnInfo<?, ?>[] extractColumnNames(final JsonElement document) {
		if (document.isJsonArray()) {
			Set<ColumnInfo<?, ?>> columnInfos = new HashSet<>();
			document.getAsJsonArray().forEach(d -> {
				ColumnInfo<?, ?>[] columnInfos1 = extractColumnNames(d);
				columnInfos.addAll(Arrays.asList(columnInfos1));
			});
			ColumnInfo<?, ?>[] columnInfosArr = new ColumnInfo[columnInfos.size()];
			columnInfos.toArray(columnInfosArr);
			return columnInfosArr;
		} else {
			return extractColumnNames(document.getAsJsonObject());
		}
	}

	private static ColumnInfo<?, ?>[] extractColumnNames(final JsonObject document) {
		Set<String> keys = document.keySet();
		ColumnInfo<?, ?>[] columnInfos = new ColumnInfo[keys.size()];

		int index = 0;
		for (final String key : keys) {
			columnInfos[index++] = new TableColumnInfo(key);
		}
		return columnInfos;
	}

	private static class TableColumnInfo extends ColumnInfo<Object, Object> {
		private static final TableCellRenderer TABLE_CELL_RENDERER = new TableCellRenderer();

		private final String key;

		TableColumnInfo(String key) {
			super(key);
			this.key = key;
		}

		@Nullable
		@Override
		public Object valueOf(Object o) {
			JsonObject document = (JsonObject) o;
			return document.get(key);
		}

		@Nullable
		@Override
		public TableCellRenderer getRenderer(Object o) {
			return TABLE_CELL_RENDERER;
		}
	}
}
