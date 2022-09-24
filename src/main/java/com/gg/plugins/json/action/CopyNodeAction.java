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

package com.gg.plugins.json.action;

import com.gg.plugins.json.view.JsonResultPanel;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;

public class CopyNodeAction extends AnAction implements DumbAware {

	private final JsonResultPanel jsonResultPanel;

	public CopyNodeAction(JsonResultPanel jsonResultPanel) {
		super("Copy...", "Copy selected node to clipboard", null);
		this.jsonResultPanel = jsonResultPanel;

		registerCustomShortcutSet(KeyEvent.VK_C,
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), jsonResultPanel);
	}

	@Override
	public void update(AnActionEvent e) {
		e.getPresentation().setVisible(jsonResultPanel.getSelectedNode() != null);
	}

	@Override
	public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
		CopyPasteManager.getInstance()
		                .setContents(new StringSelection(jsonResultPanel.getSelectedNodeStringifiedValue()));
	}
}
