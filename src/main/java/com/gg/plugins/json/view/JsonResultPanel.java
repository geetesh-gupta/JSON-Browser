/*
 * Copyright (c) 2018 David Boissier.
 * Modifications Copyright (c) 2022 David Boissier.
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

import com.gg.plugins.json.model.*;
import com.gg.plugins.json.service.Notifier;
import com.gg.plugins.json.utils.JsonTableUtils;
import com.gg.plugins.json.utils.JsonTreeUtils;
import com.google.gson.*;
import com.intellij.lang.Language;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.util.LexerEditorHighlighter;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.fileTypes.PlainTextSyntaxHighlighterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBCardLayout;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.treetable.TreeTableTree;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class JsonResultPanel extends JPanel implements Disposable {

	private final Project project;

	private final Notifier notifier;

	private final JPanel resultTreePanel;

	private final ActionCallback actionCallback;

	JsonTreeTableView resultTreeTableView;

	JsonTableView<JsonObject> resultTableView;

	private JPanel mainPanel;

	private JPanel containerPanel;

	private ViewMode currentViewMode = ViewMode.INPUT;

	private Editor editor;

	public JsonResultPanel(Project project, JsonElement jsonElement) {
		this.project = project;
		this.notifier = Notifier.getInstance(project);

		setLayout(new BorderLayout());
		add(mainPanel, BorderLayout.CENTER);

		resultTreePanel = new JPanel(new BorderLayout());

		containerPanel.setLayout(new JBCardLayout());
		containerPanel.add(resultTreePanel);

		updateResultView(jsonElement, new Pagination());

		actionCallback = new ActionCallback() {
			@Override
			public void onOperationSuccess(String shortMessage, String detailedMessage) {
				notifier.notifyInfo(detailedMessage);
			}

			@Override
			public void onOperationFailure(Exception exception) {
				notifier.notifyError(exception.getMessage());
			}
		};
	}

	public void updateResultView(JsonElement object, @Nullable Pagination pagination) {
		if (ViewMode.TREE.equals(currentViewMode)) {
			updateResultTreeTable(object, pagination);
		} else if (ViewMode.TABLE.equals(currentViewMode)) {
			updateResultTable(object);
		} else {
			updateInputPane(object);
		}
	}

	private void updateResultTreeTable(JsonElement object, Pagination pagination) {
		if (object.isJsonArray()) {
			resultTreeTableView = new JsonTreeTableView(JsonTreeUtils.buildJsonTree(extractDocuments(pagination,
					object.getAsJsonArray()), pagination.getStartIndex()), JsonTreeTableView.COLUMNS_FOR_READING);
		} else {
			resultTreeTableView =
					new JsonTreeTableView(JsonTreeUtils.buildJsonTree(object), JsonTreeTableView.COLUMNS_FOR_READING);
		}
		resultTreeTableView.setName("resultTreeTable");

		displayResult(resultTreeTableView);

		UIUtil.invokeAndWaitIfNeeded((Runnable) () -> TreeUtil.expand(resultTreeTableView.getTree(), 2));
	}

	private void updateResultTable(JsonElement object) {
		resultTableView = new JsonTableView<>(JsonTableUtils.buildJsonTable(object));
		resultTableView.setName("resultTable");
		displayResult(resultTableView);
	}

	private void updateInputPane(JsonElement object) {
		this.editor = createEditor(object);
		displayResult(this.editor.getComponent());
	}

	private static JsonArray extractDocuments(Pagination pagination, JsonArray documents) {
		if (ResultsPerPage.ALL.equals(pagination.getResultsPerPage())) {
			return documents;
		}
		if (pagination.getCountPerPage() >= documents.size()) {
			return documents;
		}

		int startIndex = pagination.getStartIndex();
		int endIndex = startIndex + pagination.getCountPerPage();

		JsonArray jsonArray = new JsonArray();
		IntStream.range(startIndex, endIndex).forEach(jsonArray::add);
		return jsonArray;
	}

	private void displayResult(JComponent tableView) {
		resultTreePanel.invalidate();
		resultTreePanel.removeAll();
		resultTreePanel.add(new JBScrollPane(tableView));
		resultTreePanel.validate();
	}

	public Editor createEditor(JsonElement data) {
		EditorFactory editorFactory = EditorFactory.getInstance();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String jsonOutput = gson.toJson(data);
		Document editorDocument = editorFactory.createDocument(jsonOutput);
		Editor editor = editorFactory.createEditor(editorDocument, project);
		fillEditorSettings(editor.getSettings());
		EditorEx editorEx = (EditorEx) editor;
		attachHighlighter(editorEx);

		return editor;
	}

	private static void fillEditorSettings(final EditorSettings editorSettings) {
		editorSettings.setWhitespacesShown(true);
		editorSettings.setLineMarkerAreaShown(false);
		editorSettings.setIndentGuidesShown(false);
		editorSettings.setLineNumbersShown(false);
		editorSettings.setAllowSingleLogicalLineFolding(true);
		editorSettings.setAdditionalColumnsCount(0);
		editorSettings.setAdditionalLinesCount(1);
		editorSettings.setUseSoftWraps(true);
		editorSettings.setUseTabCharacter(false);
		editorSettings.setCaretInsideTabs(false);
		editorSettings.setVirtualSpace(false);
	}

	private void attachHighlighter(final EditorEx editor) {
		EditorColorsScheme scheme = editor.getColorsScheme();
		scheme.setColor(EditorColors.CARET_ROW_COLOR, null);
		editor.setHighlighter(createHighlighter(scheme));
	}

	private EditorHighlighter createHighlighter(EditorColorsScheme settings) {
		Language language = Language.findLanguageByID("JSON");
		if (language == null) {
			language = Language.ANY;
		}
		return new LexerEditorHighlighter(PlainTextSyntaxHighlighterFactory.getSyntaxHighlighter(language, null, null),
				settings);
	}

	public Editor getEditor() {
		return editor;
	}

	public JPanel getContent() {
		return mainPanel;
	}

	private JsonElement getSelectedDocument() {
		TreeTableTree tree = resultTreeTableView.getTree();
		JsonTreeNode treeNode = (JsonTreeNode) tree.getLastSelectedPathComponent();
		if (treeNode == null) {
			return null;
		}

		NodeDescriptor descriptor = treeNode.getDescriptor();
		if (descriptor instanceof KeyValueDescriptor) {
			KeyValueDescriptor keyValueDescriptor = (KeyValueDescriptor) descriptor;
			return (JsonElement) keyValueDescriptor.getValue();
		}

		return null;
	}

	//    public void editSelectedDocument() {
	//        Document mongoDocument = getSelectedDocument();
	//        if (mongoDocument == null) {
	//            return;
	//        }
	//
	//        EditionDialog
	//                .create(project, mongoDocumentOperations, actionCallback)
	//                .initDocument(mongoDocument)
	//                .show();
	//    }
	//
	//
	//    public void addDocument() {
	//        EditionDialog
	//                .create(project, mongoDocumentOperations, actionCallback)
	//                .initDocument(null)
	//                .show();
	//    }

	void expandAll() {
		TreeUtil.expandAll(resultTreeTableView.getTree());
	}

	void collapseAll() {
		TreeTableTree tree = resultTreeTableView.getTree();
		TreeUtil.collapseAll(tree, 1);
	}

	public String getStringifiedResult() {
		if (getCurrentViewMode() == ViewMode.INPUT) {
			return getEditorValue().toString();
		}
		JsonTreeNode rootNode = (JsonTreeNode) resultTreeTableView.getTree().getModel().getRoot();
		return stringifyResult(rootNode);
	}

	ViewMode getCurrentViewMode() {
		return currentViewMode;
	}

	void setCurrentViewMode(ViewMode viewMode) {
		this.currentViewMode = viewMode;
	}

	public JsonElement getEditorValue() {
		final Document document = editor.getDocument();
		return JsonParser.parseString(document.getText());
	}

	private String stringifyResult(DefaultMutableTreeNode selectedResultNode) {
		return IntStream.range(0, selectedResultNode.getChildCount())
		                .mapToObj(i -> getDescriptor(i, selectedResultNode).toString())
		                .collect(Collectors.joining(",", "[", "]"));
	}

	private static NodeDescriptor getDescriptor(int i, DefaultMutableTreeNode parentNode) {
		DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) parentNode.getChildAt(i);
		return (NodeDescriptor) childNode.getUserObject();
	}

	public String getSelectedNodeStringifiedValue() {
		JsonTreeNode lastSelectedResultNode = getSelectedNode();
		if (lastSelectedResultNode == null) {
			return null;
		}
		return stringifyResult(lastSelectedResultNode);
	}

	public JsonTreeNode getSelectedNode() {
		return (JsonTreeNode) resultTreeTableView.getTree().getLastSelectedPathComponent();
	}

	@Override
	public void dispose() {
		resultTreeTableView = null;
	}

	public enum ViewMode {
		TREE, TABLE, INPUT
	}

	public interface ActionCallback {

		void onOperationSuccess(String label, String message);

		void onOperationFailure(Exception exception);
	}

}
