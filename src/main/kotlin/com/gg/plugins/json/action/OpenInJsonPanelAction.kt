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
package com.gg.plugins.json.action

import com.gg.plugins.json.view.resultpanel.JsonResultPanel
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import java.awt.Toolkit
import java.awt.event.KeyEvent

class OpenInJsonPanelAction(private val resultPanel: JsonResultPanel) :
    AnAction("Open in JsonPanel", "Open selected node in sub JsonPanel", null), DumbAware {
    init {
        registerCustomShortcutSet(
            KeyEvent.VK_E,
            Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx, resultPanel
        )
    }

    override fun actionPerformed(anActionEvent: AnActionEvent) {
        resultPanel.openInSubJsonPanel()
    }
}