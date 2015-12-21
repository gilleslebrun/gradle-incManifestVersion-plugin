package com.gilleslebrun.gradleplugin

import org.gradle.api.Project
import org.gradle.api.Plugin

class IncManifestVersionPlugin implements Plugin<Project>
{
	static final String NAME = 'incManifestVersion'

    void apply(Project project) {
        project.extensions.create(NAME, IncManifestVersionPluginExtension)
        project.task(NAME, type: IncManifestVersionTask)
    }
}

class IncManifestVersionPluginExtension
{   String  prefixVersionName
    String  androidManifest
    boolean withVersionCodeSuffix
    boolean withDebugSuffix
    int majorVersionNumber
    int minorVersionNumber
    int patchVersionNumber
}