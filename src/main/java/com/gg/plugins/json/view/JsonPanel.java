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

import com.gg.plugins.json.action.*;
import com.gg.plugins.json.model.Pagination;
import com.gg.plugins.json.model.ResultsPerPage;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.intellij.ide.CommonActionsManager;
import com.intellij.ide.TreeExpander;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LoadingDecorator;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.impl.StripeButton;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

public class JsonPanel extends JPanel implements Disposable {

	private final Project project;

	private final JBLabel rowCountLabel = new JBLabel();

	private final JBLabel pageNumberLabel = new JBLabel();

	private final JsonResultPanel resultPanel;

	private final Pagination pagination;

	private JsonElement jsonElement;

	private JPanel rootPanel;

	private Splitter splitter;

	private JPanel toolBar;

	private JBPopup popup;

	private JPanel errorPanel;

	private JPanel paginationPanel;

	public JsonPanel(Project project, JsonElement jsonElement) {
		this.project = project;
		this.pagination = new Pagination();
		this.jsonElement = jsonElement;

		errorPanel.setLayout(new BorderLayout());

		resultPanel = createResultPanel(project);

		LoadingDecorator loadingDecorator = new LoadingDecorator(resultPanel, this, 0);
		splitter.setOrientation(true);
		splitter.setProportion(0.2f);
		splitter.setSecondComponent(loadingDecorator.getComponent());

		setLayout(new BorderLayout());
		add(rootPanel);

		updateToolBar();
		initPaginationPanel();

		pagination.addSetPageListener(this::updateJsonElementPagination);
		pagination.addSetPageListener(() -> {
			pagination.setTotalDocuments(jsonElement.isJsonArray() ? jsonElement.getAsJsonArray().size() : 1);
			if (ResultsPerPage.ALL.equals(pagination.getResultsPerPage())) {
				pageNumberLabel.setVisible(false);
			} else {
				pageNumberLabel.setText(String.format("Page %d/%d",
						pagination.getPageNumber(),
						pagination.getTotalPageNumber()));
				pageNumberLabel.setVisible(true);
			}
		});
	}

	private JsonResultPanel createResultPanel(Project project) {
		return new JsonResultPanel(project, this.jsonElement);
	}

	private void updateToolBar() {
		toolBar.invalidate();
		toolBar.removeAll();
		toolBar.setLayout(new BorderLayout());

		JComponent actionToolBarComponent = createResultActionsComponent();
		toolBar.add(actionToolBarComponent, BorderLayout.CENTER);

		JComponent viewToolbarComponent = createSelectViewActionsComponent();
		toolBar.add(viewToolbarComponent, BorderLayout.EAST);
		toolBar.validate();
	}

	private void initPaginationPanel() {
		paginationPanel.setLayout(new BorderLayout());

		JComponent actionToolbarComponent = createPaginationActionsComponent();
		paginationPanel.add(actionToolbarComponent, BorderLayout.CENTER);

		JPanel panel = new JPanel();
		panel.add(pageNumberLabel);
		panel.add(com.intellij.ui.GuiUtils.createVerticalStrut());
		panel.add(rowCountLabel);

		paginationPanel.add(panel, BorderLayout.EAST);
	}

	private void updateJsonElementPagination() {
		JsonArray jsonArray = jsonElement.getAsJsonArray();
		JsonArray newArray = new JsonArray();

		for (int i = pagination.getCountPerPage() * (pagination.getPageNumber() - 1);
		     i < Math.min(pagination.getCountPerPage() * pagination.getPageNumber(), jsonArray.size());
		     i++) {
			newArray.add(jsonArray.get(i));
		}
		resultPanel.updateResultView(newArray, pagination);
	}

	@NotNull
	private JComponent createResultActionsComponent() {
		DefaultActionGroup actionResultGroup = new DefaultActionGroup("JsonResultGroup", true);
		if (resultPanel.getCurrentViewMode() == JsonResultPanel.ViewMode.INPUT) {
			actionResultGroup.add(new FormatJsonAction(resultPanel));
		}

		actionResultGroup.add(new CopyAllAction(resultPanel));

		if (resultPanel.getCurrentViewMode() == JsonResultPanel.ViewMode.TREE) {
			addBasicTreeActions(actionResultGroup);
		}

		return createToolbarComponent(actionResultGroup, "JsonResultGroupActions");
	}

	@NotNull
	private JComponent createSelectViewActionsComponent() {
		DefaultActionGroup viewSelectGroup = new DefaultActionGroup("JsonViewSelectGroup", false);
		viewSelectGroup.add(new ViewAsInputAction(this));
		viewSelectGroup.add(new ViewAsTreeAction(this));
		viewSelectGroup.add(new ViewAsTableAction(this));

		return createToolbarComponent(viewSelectGroup, "JsonViewSelectGroupActions");
	}

	@NotNull
	private JComponent createPaginationActionsComponent() {
		DefaultActionGroup actionResultGroup = new DefaultActionGroup("PaginationGroup", false);
		actionResultGroup.add(new ChangeResultsPerPageActionComponent(() -> new PaginationPopupComponent(pagination).initUi()));
		actionResultGroup.add(new PaginationAction.Previous(pagination));
		actionResultGroup.add(new PaginationAction.Next(pagination));

		return createToolbarComponent(actionResultGroup, "PaginationGroupActions");
	}

	private void addBasicTreeActions(DefaultActionGroup actionResultGroup) {
		final TreeExpander treeExpander = new TreeExpander() {
			@Override
			public void expandAll() {
				resultPanel.expandAll();
			}

			@Override
			public boolean canExpand() {
				return true;
			}

			@Override
			public void collapseAll() {
				resultPanel.collapseAll();
			}

			@Override
			public boolean canCollapse() {
				return true;
			}
		};

		CommonActionsManager actionsManager = CommonActionsManager.getInstance();
		final AnAction expandAllAction = actionsManager.createExpandAllAction(treeExpander, resultPanel);
		final AnAction collapseAllAction = actionsManager.createCollapseAllAction(treeExpander, resultPanel);

		Disposer.register(this, () -> {
			collapseAllAction.unregisterCustomShortcutSet(resultPanel);
			expandAllAction.unregisterCustomShortcutSet(resultPanel);
		});

		actionResultGroup.addSeparator();
		actionResultGroup.add(expandAllAction);
		actionResultGroup.add(collapseAllAction);
	}

	private JComponent createToolbarComponent(DefaultActionGroup actionGroup, String name) {
		ActionToolbar viewToolbar = ActionManager.getInstance().createActionToolbar(name, actionGroup, true);
		viewToolbar.setTargetComponent(viewToolbar.getComponent());
		viewToolbar.setLayoutPolicy(ActionToolbar.AUTO_LAYOUT_POLICY);
		JComponent viewToolbarComponent = viewToolbar.getComponent();
		viewToolbarComponent.setBorder(null);
		viewToolbarComponent.setOpaque(false);
		return viewToolbarComponent;
	}

	@Override
	public void dispose() {
		resultPanel.dispose();
		popup.dispose();
	}

	public void setViewMode(JsonResultPanel.ViewMode viewMode) {
		JsonResultPanel.ViewMode prevViewMode = resultPanel.getCurrentViewMode();
		if (resultPanel.getCurrentViewMode().equals(viewMode)) {
			return;
		}
		this.resultPanel.setCurrentViewMode(viewMode);
		UIUtil.invokeLaterIfNeeded(() -> {
			if (prevViewMode == JsonResultPanel.ViewMode.INPUT) {
				this.jsonElement = resultPanel.getEditorValue();
			}

			updateToolBar();

			resultPanel.updateResultView(jsonElement, pagination);
			rowCountLabel.setText(String.format("%s documents",
					jsonElement.isJsonArray() ? jsonElement.getAsJsonArray().size() : 1));
			initActions(resultPanel);
		});
	}

	private void initActions(JsonResultPanel jsonResultPanel) {
		if (jsonResultPanel.resultTableView != null) {
			jsonResultPanel.resultTableView.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent mouseEvent) {
					if (mouseEvent.getClickCount() == 2) {
						int selectedColumnNum = resultPanel.resultTableView.getSelectedColumn();
						String selectedColumnName = resultPanel.resultTableView.getColumnModel()
						                                                       .getColumn(selectedColumnNum)
						                                                       .getHeaderValue()
						                                                       .toString();
						JsonElement childJsonElement =
								Objects.requireNonNull(resultPanel.resultTableView.getSelectedObject())
								       .get(selectedColumnName);
						if (childJsonElement.isJsonArray() || childJsonElement.isJsonObject()) {
							popup = JBPopupFactory.getInstance()
							                      .createComponentPopupBuilder(new JsonPanel(project, childJsonElement),
									                      resultPanel)
							                      .setCancelKeyEnabled(true)
							                      .setShowBorder(false)
							                      .setCancelOnOtherWindowOpen(false)
							                      .setCancelOnWindowDeactivation(false)
							                      .setCancelOnClickOutside(false)
							                      .setCancelOnMouseOutCallback(c -> {
								                      if (c.getClickCount() == 1) {
									                      if (c.getComponent() instanceof StripeButton) {
										                      return true;
									                      }
									                      for (int i = 0; i < toolBar.getComponentCount(); i++) {
										                      if (c.getComponent().equals(toolBar.getComponent(i)))
											                      return true;
									                      }
								                      }
								                      return false;

							                      })
							                      .createPopup();
							popup.setSize(new Dimension(rootPanel.getWidth(),
									resultPanel.getHeight() + paginationPanel.getHeight()));
							popup.show(new RelativePoint(resultPanel, new Point(0, toolBar.getY())));
						}
					}
				}
			});

			DefaultActionGroup actionPopupGroup = new DefaultActionGroup("JsonPanelPopupGroup", true);
			if (ApplicationManager.getApplication() != null) {
				actionPopupGroup.add(new CopyNodeAction(resultPanel));
			}

			PopupHandler.installPopupMenu(jsonResultPanel.resultTreeTableView, actionPopupGroup, "POPUP");
		}
	}

	public JComponent getContent() {
		return rootPanel;
	}

	private static class ChangeResultsPerPageActionComponent extends DumbAwareAction implements CustomComponentAction {

		@NotNull private final Computable<JComponent> myComponentCreator;

		ChangeResultsPerPageActionComponent(@NotNull Computable<JComponent> componentCreator) {
			myComponentCreator = componentCreator;
		}

		@Override
		public @NotNull JComponent createCustomComponent(@NotNull Presentation presentation, @NotNull String place) {
			return myComponentCreator.compute();
		}

		@Override
		public void actionPerformed(@NotNull AnActionEvent e) {
		}
	}
}
