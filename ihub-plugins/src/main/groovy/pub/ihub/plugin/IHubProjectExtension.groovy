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

import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import org.gradle.api.Project

import static groovy.transform.TypeCheckingMode.SKIP



/**
 * IHub项目扩展
 * @author liheng
 */
@CompileStatic
class IHubProjectExtension implements IHubExtension {

    protected Project project

    @Override
    String findProjectProperty(String key) {
        project.findProperty key
    }

    boolean findProperty(String key, boolean defaultValue) {
        findProperty(key, String.valueOf(defaultValue)).toBoolean()
    }

    String findSystemProperty(String key, String defaultValue = null) {
        findProperty(key, defaultValue) { String k -> System.getProperty(k) ?: findProjectProperty(k) }
    }

    boolean findSystemProperty(String key, boolean defaultValue) {
        findSystemProperty(key, String.valueOf(defaultValue)).toBoolean()
    }

    int findSystemProperty(String key, int defaultValue) {
        findSystemProperty(key, String.valueOf(defaultValue)).toInteger()
    }

    protected static void assertProperty(boolean condition, String message) {
        if (!condition) {
            throw new GradleException(message)
        }
    }

    Project getRootProject() {
        project.rootProject
    }

    @CompileStatic(SKIP)
    protected static void setExtProperty(Project project, String key, value) {
        project.ext.setProperty key, value
    }

    void setExtProperty(String key, value) {
        setExtProperty project, key, value
    }

    void setRootExtProperty(String key, value) {
        setExtProperty rootProject, key, value
    }

    @CompileStatic(SKIP)
    <V> V findExtProperty(Project project, String key, V defaultValue = null) {
        project.ext.with { has(key) ? getProperty(key) as V : defaultValue }
    }

    def <V> V findRootExtProperty(String key, V defaultValue = null) {
        findExtProperty rootProject, key, defaultValue
    }

}