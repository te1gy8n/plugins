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

package pub.ihub.plugin


import org.gradle.api.Project

import static pub.ihub.plugin.Constants.GROUP_DEFAULT_DEPENDENCIES_MAPPING
import static pub.ihub.plugin.Constants.GROUP_DEPENDENCY_EXCLUDE_MAPPING
import static pub.ihub.plugin.Constants.GROUP_DEPENDENCY_VERSION_CONFIG
import static pub.ihub.plugin.Constants.GROUP_MAVEN_BOM_VERSION_CONFIG
import static pub.ihub.plugin.Constants.GROUP_MAVEN_VERSION_CONFIG



/**
 * @author henry
 */
class IHubBomPlugin implements IHubPluginAware<Project> {

	@Override
	void apply() {
		target.pluginManager.apply 'io.spring.dependency-management'

		def bomVersion = GROUP_MAVEN_BOM_VERSION_CONFIG.collect { group, module, version ->
			[group, module, findProperty(group + '.version', version)]
		}
		def dependenciesVersion = GROUP_DEPENDENCY_VERSION_CONFIG.collect { group, version, modules ->
			[group, findProperty(group + '.version', version), modules]
		}
		target.dependencyManagement {
			imports {
				bomVersion.each { group, module, version ->
					mavenBom "$group:$module:$version"
				}
			}
			printConfigContent "${target.name.toUpperCase()} Group Maven Bom Version", bomVersion,
				tap('Group', 35), tap('Module'), tap('Version', 15)

			dependencies {
				dependenciesVersion.each { group, version, modules ->
					dependencySet(group: group, version: version) {
						modules.each { entry it }
					}
				}
			}
			printConfigContent "${target.name.toUpperCase()} Group Maven Bom Version", dependenciesVersion
				.inject([]) { list, config ->
					def (group, version, modules) = config
					list + modules.collect { [group, it, version] }
				}, tap('Group', 35), tap('Module'), tap('Version', 15)
		}

		target.configurations {
			all {
				resolutionStrategy {
					eachDependency {
						def version = GROUP_MAVEN_VERSION_CONFIG[it.requested.group]
						if (it.requested.group != target.group && version) {
							it.useVersion findProperty(it.requested.group + '.version', version)
						}
					}
					// 不缓存动态版本
					cacheDynamicVersionsFor 0, 'seconds'
					// 不缓存快照模块
					cacheChangingModulesFor 0, 'seconds'
				}
			}
			all {
				GROUP_DEPENDENCY_EXCLUDE_MAPPING.each { group, modules ->
					modules.each { module ->
						exclude group: group, module: module
					}
				}
			}
			printConfigContent "${target.name.toUpperCase()} Exclude Group Modules",
				tap('Group', 40), tap('Modules'), GROUP_DEPENDENCY_EXCLUDE_MAPPING

			GROUP_DEFAULT_DEPENDENCIES_MAPPING.each { key, dependencies ->
				maybeCreate(key).getDependencies()
					.addAll(dependencies.collect { target.getDependencies().create(it) })
			}
			printConfigContent "${target.name.toUpperCase()} Config Default Dependencies",
				tap('DependencyType', 30), tap('Dependencies'), GROUP_DEFAULT_DEPENDENCIES_MAPPING
		}
	}

}