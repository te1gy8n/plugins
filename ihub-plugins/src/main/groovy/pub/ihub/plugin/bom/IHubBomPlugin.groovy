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
package pub.ihub.plugin.bom

import static pub.ihub.plugin.IHubPluginAware.EvaluateStage.AFTER

import org.gradle.api.Project
import pub.ihub.plugin.IHubPluginAware
import pub.ihub.plugin.IHubPluginsPlugin

/**
 * BOM（Bill of Materials）组件依赖管理
 * @author henry
 */
class IHubBomPlugin implements IHubPluginAware<IHubBomExtension> {

	@Override
	void apply(Project project) {
		project.pluginManager.apply IHubPluginsPlugin
		project.pluginManager.apply 'io.spring.dependency-management'

		createExtension(project, 'iHubBom', IHubBomExtensionImpl, AFTER) { ext ->
			// 配置导入bom
			ext.importBoms {
				// TODO 由于GitHub仓库token只能个人使用，组件发布到中央仓库方可使用
//					group 'pub.ihub.lib' module 'ihub-libs' version '1.0.0-SNAPSHOT'
				group 'org.springframework.boot' module 'spring-boot-dependencies' version '2.4.5'
				group 'org.springframework.cloud' module 'spring-cloud-dependencies' version '2020.0.2'
				group 'com.alibaba.cloud' module 'spring-cloud-alibaba-dependencies' version '2021.1'
				group 'com.github.xiaoymin' module 'knife4j-dependencies' version '3.0.2'
				group 'com.sun.xml.bind' module 'jaxb-bom-ext' version '3.0.1'
				group 'de.codecentric' module 'spring-boot-admin-dependencies' version '2.4.1'
			}
			// 配置组件依赖版本
			ext.dependencyVersions {
				group 'com.alibaba' version '1.2.76' modules 'fastjson'
				group 'com.alibaba' version '1.2.6' modules 'druid', 'druid-spring-boot-starter'
				group 'com.alibaba.p3c' version '2.1.1' modules 'p3c-pmd'
				group 'com.baomidou' version '3.4.2' modules 'mybatis-plus',
					'mybatis-plus-boot-starter', 'mybatis-plus-generator'
				group 'com.github.xiaoymin' version '2.0.8' modules 'knife4j-aggregation-spring-boot-starter'
			}
			// 配置组版本策略（建议尽量使用bom）
			ext.groupVersions {
				group 'cn.hutool' version '5.6.4'
			}
			// 配置默认排除项
			ext.excludeModules {
				group 'c3p0' modules 'c3p0'
				group 'commons-logging' modules 'commons-logging'
				group 'com.zaxxer' modules 'HikariCP'
				group 'log4j' modules 'log4j'
				group 'org.apache.logging.log4j' modules 'log4j-core'
				group 'org.apache.tomcat' modules 'tomcat-jdbc'
				group 'org.slf4j' modules 'slf4j-jcl', 'slf4j-log4j12'
				group 'stax' modules 'stax-api'
			}
			// 配置默认依赖组件
			ext.dependencies {
				compileOnly 'cn.hutool:hutool-all'
				implementation 'org.slf4j:slf4j-api'
				runtimeOnly 'org.slf4j:jul-to-slf4j',
//						'org.slf4j:jcl-over-slf4j', TODO 构建原生镜像有报错
					'org.slf4j:log4j-over-slf4j'
			}

			project.dependencyManagement {
				// 导入bom配置
				imports {
					ext.bomVersions.each {
						mavenBom "$it.group:$it.module:$it.version"
					}
				}

				// 配置组件版本
				dependencies {
					ext.dependencyVersions.each { config ->
						dependencySet(group: config.group, version: config.version) {
							config.modules.each { entry it }
						}
					}
				}
			}

			project.configurations {
				all {
					resolutionStrategy {
						// 配置组件组版本（用于配置无bom组件）
						eachDependency {
							ext.groupVersions.find { s -> s.group == it.requested.group }?.version?.with { v ->
								it.useVersion v
							}
						}
						// 不缓存动态版本
						cacheDynamicVersionsFor 0, 'seconds'
						// 不缓存快照模块
						cacheChangingModulesFor 0, 'seconds'
					}
					// 排除组件依赖
					ext.excludeModules.each { group, modules ->
						modules.each { module -> exclude group: group, module: module }
					}
				}
				// 配置组件依赖
				ext.dependencies.each { type, dependencies ->
					maybeCreate(type).dependencies.addAll dependencies.collect {
						// 支持导入项目
						project.dependencies.create it.startsWith(':') ? project.project(it) : it
					}
				}
			}

			ext.printConfigContent()
		}
	}

}