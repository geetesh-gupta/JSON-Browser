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

package com.gg.plugins.json.utils

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorSettings
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.LexerEditorHighlighter
import com.intellij.openapi.editor.highlighter.EditorHighlighter
import com.intellij.openapi.fileTypes.PlainTextSyntaxHighlighterFactory
import com.intellij.openapi.project.Project

fun createEditor(data: JsonElement?, project: Project?): Editor {
    val editorFactory = EditorFactory.getInstance()
    val gson = GsonBuilder().setPrettyPrinting().create()
    val jsonOutput = gson.toJson(data)
    val editorDocument = editorFactory.createDocument(jsonOutput)
    val editor = editorFactory.createEditor(editorDocument, project)
    fillEditorSettings(editor.settings)
    val editorEx = editor as EditorEx
    attachHighlighter(editorEx)
    return editor
}

fun formatJson(data: JsonElement?): String {
    val gson = GsonBuilder().setPrettyPrinting().create()
    return gson.toJson(data)
}

private fun attachHighlighter(editor: EditorEx) {
    val scheme = editor.colorsScheme
    scheme.setColor(EditorColors.CARET_ROW_COLOR, null)
    editor.highlighter = createHighlighter(scheme)
}

private fun createHighlighter(settings: EditorColorsScheme): EditorHighlighter {
    var language = Language.findLanguageByID("JSON")
    if (language == null) {
        language = Language.ANY
    }
    return LexerEditorHighlighter(
        PlainTextSyntaxHighlighterFactory.getSyntaxHighlighter(language!!, null, null),
        settings
    )
}

private fun fillEditorSettings(editorSettings: EditorSettings) {
    editorSettings.isWhitespacesShown = true
    editorSettings.isLineMarkerAreaShown = false
    editorSettings.isIndentGuidesShown = false
    editorSettings.isLineNumbersShown = false
    editorSettings.isAllowSingleLogicalLineFolding = true
    editorSettings.additionalColumnsCount = 0
    editorSettings.additionalLinesCount = 1
    editorSettings.isUseSoftWraps = true
    editorSettings.setUseTabCharacter(false)
    editorSettings.isCaretInsideTabs = false
    editorSettings.isVirtualSpace = false
}