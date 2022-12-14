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
package com.gg.plugins.json.style

import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import java.awt.Color

object StyleAttributesProvider {
    private val LIGNT_GREEN: Color = JBColor(Color(0, 128, 0), Color(165, 194, 97))
    private val LIGHT_GRAY: Color = Gray._128

    @JvmField
    val KEY_COLOR: Color = JBColor(Color(102, 14, 122), Color(204, 120, 50))

    @JvmField
    val NUMBER_COLOR: Color = JBColor.BLUE

    val indexAttribute = SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, JBColor.BLACK)
    val keyValueAttribute = SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, KEY_COLOR)
    val numberAttribute = SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, NUMBER_COLOR)
    val booleanAttribute = SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, NUMBER_COLOR)
    val stringAttribute = SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, LIGNT_GREEN)
    val nullAttribute = SimpleTextAttributes(SimpleTextAttributes.STYLE_ITALIC, LIGHT_GRAY)
    val documentAttribute = SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, LIGHT_GRAY)
}