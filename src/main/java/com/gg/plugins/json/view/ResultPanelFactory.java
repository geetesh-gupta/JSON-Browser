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

import com.google.gson.JsonParser;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class ResultPanelFactory implements ToolWindowFactory {
	public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

		String jsonStr =
				"{\"developer\":\"GeeteshGupta\",\"email\":\"geeteshgupta98@gmail.com\",\"github\":\"https://github" +
				".com/geetesh-gupta\",\"linkedin\":\"https://linkedin.com/in/geetesh-gupta\"," +
				"\"twitter\":\"https://twitter.com/geetesh_gupta_g\",\"currentPlugin\":\"JSONBrowser\"," +
				"\"otherPlugins\":[{\"name\":\"MongoDBBrowser\",\"urls\":[{\"github\":\"https://github" +
				".com/geetesh-gupta/Mongo-DB-Browser\"},{\"jetbrains\":\"https://plugins.jetbrains" +
				".com/plugin/20002-mongo-db-browser\"}]},{\"name\":\"JSONBrowser\"," +
				"\"urls\":[{\"github\":\"https://github.com/geetesh-gupta/JSON-Browser\"}]}]}";

		JsonPanel myToolWindow = new JsonPanel(project, JsonParser.parseString(jsonStr));

		ContentFactory contentFactory = ApplicationManager.getApplication().getService(ContentFactory.class);
		Content content = contentFactory.createContent(myToolWindow.getContent(), "", false);
		toolWindow.getContentManager().addContent(content);
	}
}
