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

import com.gg.plugins.json.view.JsonPanel
import com.intellij.icons.AllIcons
import com.intellij.ide.actions.CloseTabToolbarAction
import com.intellij.openapi.actionSystem.AnActionEvent
import java.awt.event.KeyEvent

class CloseJsonPanelAction(jsonPanel: JsonPanel) : CloseTabToolbarAction() {
    private val jsonPanel: JsonPanel

    init {
        templatePresentation.icon = AllIcons.Actions.Close
        registerCustomShortcutSet(KeyEvent.VK_ESCAPE, 0, jsonPanel)
        this.jsonPanel = jsonPanel
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isVisible = jsonPanel.isCloseActionActive
        event.presentation.isEnabled = jsonPanel.isCloseActionActive
    }

    override fun actionPerformed(e: AnActionEvent) {
        jsonPanel.close()
    }
}