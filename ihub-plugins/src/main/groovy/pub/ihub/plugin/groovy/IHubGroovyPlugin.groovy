/*
 * Copyright (c) 2021 Henry 李恒 (henry.box@outlook.com).
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
package pub.ihub.plugin.groovy

import static pub.ihub.plugin.IHubPluginAware.EvaluateStage.BEFORE

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import pub.ihub.plugin.IHubPluginAware
import pub.ihub.plugin.bom.IHubBomExtension
import pub.ihub.plugin.bom.IHubBomPlugin
import pub.ihub.plugin.java.IHubJavaPlugin
import pub.ihub.plugin.verification.IHubVerificationPlugin

/**
 * Groovy插件
 * @author liheng
 */
class IHubGroovyPlugin implements IHubPluginAware<IHubGroovyExtension> {

	static TaskProvider registerGroovydocJar(Project project) {
		project.tasks.register('groovydocJar', Jar) {
			archiveClassifier.set 'groovydoc'
			Task groovydocTask = project.tasks.getByName('groovydoc').tap {
				if (JavaVersion.current().java9Compatible) {
					options.addBooleanOption 'html5', true
				}
				options.encoding = 'UTF-8'
			}
			dependsOn groovydocTask
			from groovydocTask.destinationDir
		}
	}

	@Override
	void apply(Project project) {
		project.pluginManager.apply IHubBomPlugin
		project.pluginManager.apply IHubJavaPlugin
		project.pluginManager.apply GroovyPlugin

		getExtension(project, IHubBomExtension).tap {
			String groovyGroup = 'org.codehaus.groovy'
			String groovyVersion = '3.0.8'
			importBoms {
				group groovyGroup module 'groovy-bom' version groovyVersion
			}
			dependencyVersions {
				group groovyGroup version groovyVersion modules 'groovy-all'
			}
			// 由于codenarc插件内强制指定了groovy版本，groovy3.0需要强制指定版本 TODO 判断不太准确
			if (groovyVersion.startsWith('3.')) {
				groupVersions {
					group groovyGroup version groovyVersion
				}
			}
			createExtension(project, 'iHubGroovy', IHubGroovyExtension, BEFORE) { ext ->
				dependencies {
					implementation ext.modules.unique().collect { "$groovyGroup:$it" } as String[]
				}
			}
		}

		project.pluginManager.apply IHubVerificationPlugin
	}

}