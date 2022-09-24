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

public class ValueDescriptor implements NodeDescriptor {

	private final int index;

	private final SimpleTextAttributes valueTextAttributes;

	Object value;

	private ValueDescriptor(int index, Object value, SimpleTextAttributes valueTextAttributes) {
		this.index = index;
		this.value = value;
		this.valueTextAttributes = valueTextAttributes;
	}

	public static ValueDescriptor createDescriptor(int index, Object value) {
		if (value == null) {
			return new NullValueDescriptor(index);
		}

		if (value instanceof String) {
			return new StringValueDescriptor(index, (String) value);
		} else if (value instanceof Boolean) {
			return new ValueDescriptor(index, value, StyleAttributesProvider.getBooleanAttribute()) {
				@Override
				public void setValue(Object value) {
					this.value = Boolean.parseBoolean((String) value);
				}
			};
		} else if (value instanceof Number) {
			return new ValueDescriptor(index, value, StyleAttributesProvider.getNumberAttribute()) {
				@Override
				public void setValue(Object value) {
					this.value = Integer.parseInt((String) value);
				}
			};
		} else if (value instanceof Date) {
			return new DateValueDescriptor(index, (Date) value);
		} else if (value instanceof List) {
			return new ListValueDescriptor(index, value);
		} else {
			return new ValueDescriptor(index, value, StyleAttributesProvider.getStringAttribute());
		}
	}

	public void renderValue(ColoredTableCellRenderer cellRenderer, boolean isNodeExpanded) {
		if (!isNodeExpanded) {
			cellRenderer.append(getFormattedValue(), valueTextAttributes);
		}
	}

	public void renderNode(ColoredTreeCellRenderer cellRenderer) {
		cellRenderer.append(getKey(), StyleAttributesProvider.getIndexAttribute());
	}

	public String getKey() {
		return String.format("[%s]", index);
	}

	public String getFormattedValue() {
		return String.format("%s", StringUtils.abbreviateInCenter(value.toString(), MAX_LENGTH));
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

	private static class StringValueDescriptor extends ValueDescriptor {

		private StringValueDescriptor(int index, String value) {
			super(index, value, StyleAttributesProvider.getStringAttribute());
		}

		@Override
		public String getFormattedValue() {
			return StringUtils.abbreviateInCenter(value.toString(), MAX_LENGTH);
		}
	}

	private static class NullValueDescriptor extends ValueDescriptor {

		private NullValueDescriptor(int index) {
			super(index, null, StyleAttributesProvider.getNullAttribute());
		}

		@Override
		public String getFormattedValue() {
			return "null";
		}

		@Override
		public String toString() {
			return "null";
		}
	}

	private static class DateValueDescriptor extends ValueDescriptor {

		private static final DateFormat DATE_FORMAT = DateUtils.utcDateTime(Locale.getDefault());

		private DateValueDescriptor(int index, Date value) {
			super(index, value, StyleAttributesProvider.getStringAttribute());
		}

		@Override
		public String getFormattedValue() {
			return getFormattedDate();
		}

		@Override
		public String toString() {
			return String.format("\"%s\"", getFormattedDate());
		}

		private String getFormattedDate() {
			return DATE_FORMAT.format(value);
		}
	}

	private static class ListValueDescriptor extends ValueDescriptor {
		ListValueDescriptor(int index, Object value) {
			super(index, value, StyleAttributesProvider.getDocumentAttribute());
		}

		@Override
		public String getFormattedValue() {
			return getFormattedList();
		}

		@Override
		public String toString() {
			return getFormattedList();
		}

		private String getFormattedList() {
			return StringUtils.stringifyList((List<?>) value);
		}
	}
}
