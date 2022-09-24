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

package com.gg.plugins.json.utils;

import java.util.LinkedList;
import java.util.List;

public class StringUtils {

	private static final String ELLIPSIS = "...";

	public static String abbreviateInCenter(String stringToAbbreviate, int length) {
		if (stringToAbbreviate.length() <= length) {
			return stringToAbbreviate;
		}
		int halfLength = length / 2;
		int firstPartLastIndex = halfLength - ELLIPSIS.length();
		int stringLength = stringToAbbreviate.length();
		return String.format("%s%s%s",
				stringToAbbreviate.substring(0, firstPartLastIndex),
				ELLIPSIS,
				stringToAbbreviate.substring(stringLength - halfLength, stringLength));
	}

	public static Number parseNumber(String number) {
		try {
			return Integer.parseInt(number);
		} catch (NumberFormatException ignored) {}
		try {
			return Long.parseLong(number);
		} catch (NumberFormatException ignored) {}
		return Double.parseDouble(number);
	}

	public static String stringifyList(List<?> list) {
		List<String> stringifiedObjects = new LinkedList<>();
		for (Object object : list) {
			if (object == null) {
				stringifiedObjects.add("null");
			} else if (object instanceof String) {
				stringifiedObjects.add("\"" + object + "\"");
			} else if (object instanceof List) {
				stringifiedObjects.add(stringifyList(((List<?>) object)));
			} else {
				stringifiedObjects.add(object.toString());
			}
		}

		return "[" + org.apache.commons.lang.StringUtils.join(stringifiedObjects, ", ") + "]";
	}
}
