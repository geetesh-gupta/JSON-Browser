/*
 * Copyright (c) 2018 David Boissier.
 * Modifications Copyright (c) 2022 Geetesh Gupta.
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

package com.gg.plugins.json.model;

import com.gg.plugins.json.style.StyleAttributesProvider;
import com.gg.plugins.json.utils.DateUtils;
import com.gg.plugins.json.utils.StringUtils;
import com.intellij.ui.ColoredTableCellRenderer;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class KeyValueDescriptor implements NodeDescriptor {

	final String key;

	private final SimpleTextAttributes valueTextAttributes;

	Object value;

	private KeyValueDescriptor(String key, Object value, SimpleTextAttributes valueTextAttributes) {
		this.key = key;
		this.value = value;
		this.valueTextAttributes = valueTextAttributes;
	}

	public static KeyValueDescriptor createDescriptor(String key, Object value) { //TODO refactor this
		if (value == null) {
			return new KeyNullValueDescriptor(key);
		}

		if (value instanceof Boolean) {
			return new KeyValueDescriptor(key, value, StyleAttributesProvider.getBooleanAttribute()) {
				@Override
				public void setValue(Object value) {
					this.value = Boolean.valueOf((String) value);
				}
			};
		} else if (value instanceof Integer) {
			return new KeyValueDescriptor(key, value, StyleAttributesProvider.getNumberAttribute()) {
				@Override
				public void setValue(Object value) {
					this.value = Integer.valueOf((String) value);
				}
			};
		} else if (value instanceof Double) {
			return new KeyValueDescriptor(key, value, StyleAttributesProvider.getNumberAttribute()) {
				@Override
				public void setValue(Object value) {
					this.value = Double.valueOf((String) value);
				}
			};
		} else if (value instanceof Long) {
			return new KeyValueDescriptor(key, value, StyleAttributesProvider.getNumberAttribute()) {
				@Override
				public void setValue(Object value) {
					this.value = Long.valueOf((String) value);
				}
			};
		} else if (value instanceof String) {
			return new KeyStringValueDescriptor(key, (String) value);
		} else if (value instanceof Date) {
			return new KeyDateValueDescriptor(key, (Date) value);
		} else if (value instanceof List) {
			return new KeyListValueDescriptor(key, value);
		} else {
			return new KeyValueDescriptor(key, value, StyleAttributesProvider.getStringAttribute());
		}
	}

	public void renderValue(ColoredTableCellRenderer cellRenderer, boolean isNodeExpanded) {
		if (!isNodeExpanded) {
			cellRenderer.append(getFormattedValue(), valueTextAttributes);
		}
	}

	public void renderNode(ColoredTreeCellRenderer cellRenderer) {
		cellRenderer.append(getFormattedKey(), StyleAttributesProvider.getKeyValueAttribute());
	}

	public String getFormattedKey() {
		return key;
	}

	public String getKey() {
		return key;
	}

	public String getFormattedValue() {
		return StringUtils.abbreviateInCenter(value.toString(), MAX_LENGTH);
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	@Override
	public String pretty() {
		return getFormattedValue();
	}

	@Override
	public String toString() {
		return value.toString();
	}

	private static class KeyNullValueDescriptor extends KeyValueDescriptor {

		private KeyNullValueDescriptor(String key) {
			super(key, null, StyleAttributesProvider.getNullAttribute());
		}

		@Override
		public String getFormattedValue() {
			return "null";
		}

		@Override
		public String toString() {
			return getFormattedValue();
		}
	}

	private static class KeyStringValueDescriptor extends KeyValueDescriptor {

		private KeyStringValueDescriptor(String key, String value) {
			super(key, value, StyleAttributesProvider.getStringAttribute());
		}

		@Override
		public String getFormattedValue() {
			return value.toString();
		}

		@Override
		public String toString() {
			return getFormattedValue();
		}
	}

	private static class KeyDateValueDescriptor extends KeyValueDescriptor {

		private static final DateFormat DATE_FORMAT = DateUtils.utcDateTime(Locale.getDefault());

		private KeyDateValueDescriptor(String key, Date value) {
			super(key, value, StyleAttributesProvider.getStringAttribute());
		}

		@Override
		public String getFormattedValue() {
			return getFormattedDate();
		}

		@Override
		public String toString() {
			return getFormattedDate();
		}

		private String getFormattedDate() {
			return DATE_FORMAT.format(value);
		}
	}

	private static class KeyListValueDescriptor extends KeyValueDescriptor {

		private static final String TO_STRING_TEMPLATE = "\"%s\" : %s";

		KeyListValueDescriptor(String key, Object value) {
			super(key, value, StyleAttributesProvider.getDocumentAttribute());
		}

		@Override
		public String getFormattedValue() {
			return StringUtils.abbreviateInCenter(getFormattedList(), MAX_LENGTH);
		}

		@Override
		public String toString() {
			return String.format(TO_STRING_TEMPLATE, key, getFormattedList());
		}

		private String getFormattedList() {
			return StringUtils.stringifyList((List<?>) value);
		}
	}

}
