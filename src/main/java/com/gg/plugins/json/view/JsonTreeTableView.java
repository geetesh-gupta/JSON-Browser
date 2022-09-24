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

package com.gg.plugins.json.view;

import com.gg.plugins.json.model.JsonTreeNode;
import com.gg.plugins.json.model.NodeDescriptor;
import com.gg.plugins.json.renderer.KeyCellRenderer;
import com.gg.plugins.json.renderer.ValueCellRenderer;
import com.gg.plugins.json.table.TreeNodeDatePickerCellEditor;
import com.gg.plugins.json.table.ValueCellEditor;
import com.intellij.ui.TreeTableSpeedSearch;
import com.intellij.ui.treeStructure.treetable.ListTreeTableModelOnColumns;
import com.intellij.ui.treeStructure.treetable.TreeTable;
import com.intellij.ui.treeStructure.treetable.TreeTableModel;
import com.intellij.ui.treeStructure.treetable.TreeTableTree;
import com.intellij.util.ui.ColumnInfo;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.Date;

public class JsonTreeTableView extends TreeTable {
	private static final ColumnInfo<JsonTreeNode, NodeDescriptor> KEY = new ColumnInfo<>("Key") {

		public NodeDescriptor valueOf(JsonTreeNode obj) {
			return obj.getDescriptor();
		}

		@Override
		public Class<TreeTableModel> getColumnClass() {
			return TreeTableModel.class;
		}

		@Override
		public boolean isCellEditable(JsonTreeNode o) {
			return false;
		}
	};

	private static final ColumnInfo<JsonTreeNode, NodeDescriptor> READONLY_VALUE = new ReadOnlyValueColumnInfo();

	public static final ColumnInfo[] COLUMNS_FOR_READING = new ColumnInfo[]{KEY, READONLY_VALUE};

	private static final ColumnInfo<JsonTreeNode, Object> WRITABLE_VALUE = new WritableColumnInfo();

	public static final ColumnInfo[] COLUMNS_FOR_WRITING = new ColumnInfo[]{KEY, WRITABLE_VALUE};

	private final ColumnInfo[] columns;

	public JsonTreeTableView(TreeNode rootNode, ColumnInfo[] columnInfos) {
		super(new ListTreeTableModelOnColumns(rootNode, columnInfos));
		this.columns = columnInfos;
		final TreeTableTree tree = getTree();

		tree.setShowsRootHandles(true);
		tree.setRootVisible(false);
		setTreeCellRenderer(new KeyCellRenderer());

		int keyColumnWidth =
				getFontMetrics(getFont()).stringWidth(StringUtils.repeat("*", getMaxKeyColumnWidth(rootNode)));
		getColumnModel().getColumn(0).setMaxWidth(keyColumnWidth);
		getColumnModel().getColumn(0).setPreferredWidth(keyColumnWidth);
		setAutoResizeMode(AUTO_RESIZE_LAST_COLUMN);

		new TreeTableSpeedSearch(this, path -> {
			final JsonTreeNode node = (JsonTreeNode) path.getLastPathComponent();
			NodeDescriptor descriptor = node.getDescriptor();
			return descriptor.getKey();
		});
	}

	private int getMaxKeyColumnWidth(TreeNode rootNode) {
		int length = 0;
		if (!rootNode.isLeaf()) {
			int maxChildLen = 0;
			for (int i = 0; i < rootNode.getChildCount(); i++) {
				maxChildLen = Math.max(getMaxKeyColumnWidth(rootNode.getChildAt(i)), maxChildLen);
			}
			length += maxChildLen;
		}
		NodeDescriptor userObject = (NodeDescriptor) ((JsonTreeNode) rootNode).getUserObject();
		length += userObject.getKey().length();
		return length;
	}

	@SuppressWarnings("unchecked")
	@Override
	public TableCellRenderer getCellRenderer(int row, int column) {
		TreePath treePath = getTree().getPathForRow(row);
		if (treePath == null)
			return super.getCellRenderer(row, column);

		JsonTreeNode node = (JsonTreeNode) treePath.getLastPathComponent();

		TableCellRenderer renderer = this.columns[column].getRenderer(node);
		return renderer == null ? super.getCellRenderer(row, column) : renderer;
	}

	@SuppressWarnings("unchecked")
	@Override
	public TableCellEditor getCellEditor(int row, int column) {
		TreePath treePath = getTree().getPathForRow(row);
		if (treePath == null)
			return super.getCellEditor(row, column);

		JsonTreeNode node = (JsonTreeNode) treePath.getLastPathComponent();
		TableCellEditor editor = columns[column].getEditor(node);
		return editor == null ? super.getCellEditor(row, column) : editor;
	}

	private static class ReadOnlyValueColumnInfo extends ColumnInfo<JsonTreeNode, NodeDescriptor> {
		private final TableCellRenderer myRenderer = new ValueCellRenderer();

		ReadOnlyValueColumnInfo() {
			super("Value");
		}

		public NodeDescriptor valueOf(JsonTreeNode treeNode) {
			return treeNode.getDescriptor();
		}

		@Override
		public boolean isCellEditable(JsonTreeNode o) {
			return false;
		}

		@Override
		public TableCellRenderer getRenderer(JsonTreeNode o) {
			return myRenderer;
		}
	}

	private static class WritableColumnInfo extends ColumnInfo<JsonTreeNode, Object> {

		private final TableCellRenderer myRenderer = new ValueCellRenderer();

		private final TableCellEditor defaultEditor = new ValueCellEditor();

		WritableColumnInfo() {
			super("Value");
		}

		public Object valueOf(JsonTreeNode treeNode) {
			return treeNode.getDescriptor();

		}

		@Override
		public boolean isCellEditable(JsonTreeNode treeNode) {
			return true;
		}

		@Override
		public void setValue(JsonTreeNode treeNode, Object value) {
			treeNode.getDescriptor().setValue(value);
		}

		@Override
		public TableCellRenderer getRenderer(JsonTreeNode o) {
			return myRenderer;
		}

		@Nullable
		@Override
		public TableCellEditor getEditor(final JsonTreeNode treeNode) {
			Object value = treeNode.getDescriptor().getValue();
			if (value instanceof Date) {
				return buildDateCellEditor(treeNode);
			}
			return defaultEditor;
		}

		private static TreeNodeDatePickerCellEditor buildDateCellEditor(final JsonTreeNode treeNode) {
			final TreeNodeDatePickerCellEditor dateEditor = new TreeNodeDatePickerCellEditor();

			//  Note from dev: Quite ugly because when clicking on the button to open popup calendar, stopCellEdition
			//  is invoked.
			//                 From that point, impossible to set the selected data in the node description
			dateEditor.addActionListener(actionEvent -> treeNode.getDescriptor()
			                                                    .setValue(dateEditor.getCellEditorValue()));
			return dateEditor;
		}
	}
}