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
package com.gg.plugins.json.view

import com.gg.plugins.json.model.Pagination
import com.gg.plugins.json.model.ResultsPerPage
import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.ui.ClickListener
import com.intellij.ui.RoundedLineBorder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.event.*
import javax.swing.*
import javax.swing.border.Border

/**
 * Fork of the filter select component in VCS Tool window of Intellij
 *
 * @see com.intellij.vcs.log.ui.filter.VcsLogPopupComponent
 */
internal class PaginationPopupComponent(private val pagination: Pagination) : JPanel() {
    private lateinit var myValueLabel: JLabel

    fun initUi(): JComponent {
        myValueLabel = object : JLabel() {
            override fun getText(): String {
                return currentText
            }
        }
        setDefaultForeground()
        isFocusable = true
        border = createUnfocusedBorder()
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        add(myValueLabel)
        add(Box.createHorizontalStrut(GAP_BEFORE_ARROW))
        add(JLabel(AllIcons.Ide.Statusbar_arrows))
        installChangeListener {
            myValueLabel.revalidate()
            myValueLabel.repaint()
        }
        showPopupMenuOnClick()
        showPopupMenuFromKeyboard()
        indicateHovering()
        indicateFocusing()
        return this
    }

    private fun installChangeListener(listener: Runnable) {
        pagination.addSetPageListener(listener)
    }

    private val currentText: String
        get() {
            val resultsPerPage = pagination.resultsPerPage
            return getText(resultsPerPage)
        }

    /**
     * Create popup actions available under this filter.
     */
    private fun createActionGroup(): ActionGroup {
        val actionGroup = DefaultActionGroup()
        for (resultsPerPage in ResultsPerPage.values()) {
            actionGroup.add(ResultsPerPageAction(resultsPerPage))
        }
        return actionGroup
    }

    private fun indicateFocusing() {
        addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent) {
                border = createFocusedBorder()
            }

            override fun focusLost(e: FocusEvent) {
                border = createUnfocusedBorder()
            }
        })
    }

    private fun showPopupMenuFromKeyboard() {
        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER || e.keyCode == KeyEvent.VK_DOWN) {
                    showPopupMenu()
                }
            }
        })
    }

    private fun showPopupMenuOnClick() {
        object : ClickListener() {
            override fun onClick(event: MouseEvent, clickCount: Int): Boolean {
                showPopupMenu()
                return true
            }
        }.installOn(this)
    }

    private fun indicateHovering() {
        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) {
                setOnHoverForeground()
            }

            override fun mouseExited(e: MouseEvent) {
                setDefaultForeground()
            }
        })
    }

    private fun setDefaultForeground() {
        myValueLabel.foreground =
            if (UIUtil.isUnderDarcula()) UIUtil.getLabelForeground() else UIUtil.getInactiveTextColor()
                .darker().darker()
    }

    private fun setOnHoverForeground() {
        myValueLabel.foreground =
            if (UIUtil.isUnderDarcula()) UIUtil.getLabelForeground() else UIUtil.getTextFieldForeground()
    }

    private fun showPopupMenu() {
        val popup = createPopupMenu()
        popup.showInCenterOf(this)
    }

    private fun createPopupMenu(): ListPopup {
        return JBPopupFactory.getInstance()
            .createActionGroupPopup(
                null,
                createActionGroup(),
                DataManager.getInstance().getDataContext(this),
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                false
            )
    }

    private inner class ResultsPerPageAction(private val resultsPerPage: ResultsPerPage) :
        DumbAwareAction(
            getText(resultsPerPage)
        ) {
        override fun actionPerformed(e: AnActionEvent) {
            pagination.resultsPerPage = resultsPerPage
        }
    }

    companion object {
        private const val GAP_BEFORE_ARROW = 3
        private const val BORDER_SIZE = 2
        private fun getText(resultsPerPage: ResultsPerPage): String {
            return if (ResultsPerPage.ALL == resultsPerPage) String.format(
                "%s docs",
                ResultsPerPage.ALL.label
            ) else String.format("%s docs / page", resultsPerPage.label)
        }

        @JvmStatic
        fun createFocusedBorder(): Border {
            return BorderFactory.createCompoundBorder(
                RoundedLineBorder(
                    UIUtil.getHeaderActiveColor(), 10,
                    BORDER_SIZE
                ),
                JBUI.Borders.empty(2)
            )
        }

        @JvmStatic
        fun createUnfocusedBorder(): Border {
            return BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(
                    BORDER_SIZE,
                    BORDER_SIZE,
                    BORDER_SIZE,
                    BORDER_SIZE
                ), JBUI.Borders.empty(2)
            )
        }
    }
}