package com.gilleslebrun.gradleplugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.util.regex.Pattern

//xmlslurp, xmlparse, DOM modify your manifest
//no more comment, space between attributes, elements, new line ...
//So regular expression to do the job

class IncManifestVersionTask extends DefaultTask
{
    def msg

    @TaskAction
    def incManifestVersion()
    {   def params = [:]
        params.androidManifest       = project.incManifestVersion.androidManifest       ? project.incManifestVersion.androidManifest        : "AndroidManifest.xml"
        params.prefixVersionName     = project.incManifestVersion.prefixVersionName     ? project.incManifestVersion.prefixVersionName      : ""
        params.withVersionCodeSuffix = project.incManifestVersion.withVersionCodeSuffix
        params.withDebugSuffix       = project.incManifestVersion.withDebugSuffix

        def majorVersionNumber     = project.incManifestVersion.majorVersionNumber     ? project.incManifestVersion.majorVersionNumber      : 1
        def minorVersionNumber     = project.incManifestVersion.minorVersionNumber     ? project.incManifestVersion.minorVersionNumber      : 0
        def patchVersionNumber     = project.incManifestVersion.patchVersionNumber     ? project.incManifestVersion.patchVersionNumber      : 0

        msg = " - manifest:" + params.androidManifest
        msg = msg + ", maj:" + majorVersionNumber + ", min:" + minorVersionNumber + ", patch:" + patchVersionNumber
        if (project.incManifestVersion.prefixVersionName) { msg = msg + ", prefix:" + params.prefixVersionName}
        else                                              { msg = msg + ", prefix:none"}
        if (params.withVersionCodeSuffix)                 { msg = msg + ", with versionCode in suffix"}
        else                                              { msg = msg + ", without versionCode in suffix"}
        if (params.withDebugSuffix)                       { msg = msg + ", with _debug in suffix"}
        else                                              { msg = msg + ", without _debug in suffix"}

        def File manifest = new File("$params.androidManifest")
        if(manifest.exists())
        {   def code = incManifestVersionCode(params.androidManifest)
            if (code >0) { incManifestVersionName(code, params, majorVersionNumber, minorVersionNumber, patchVersionNumber)}
            else {   println msg}
        }
        else
        {   println "androidManifest file not found!" + msg}
    }

    def incManifestVersionCode(manifest)
    {   def resultCode
        def manifestFile =  new File(manifest)
        def manifestText = manifestFile.getText()
        def codeTag      = "android:versionCode"
        def manifestTag  = "manifest"
        def pattern      = Pattern.compile(codeTag + "(\\s*)" + "=" + "(\\s*)" + "\"(\\d+)\"")
        def matcher      = pattern.matcher(manifestText)
        def content

        if (matcher.find())
        {   resultCode = (Integer.parseInt(matcher.group(3)) ?: 0) + 1
            content = matcher.replaceFirst(codeTag + "=\"" + resultCode + "\"")
            manifestFile.write(content)
            msg = " - auto increment in manifest -> android:versionCode=" + resultCode + msg
        }
        else
        {   pattern = Pattern.compile(manifestTag)
            matcher = pattern.matcher(manifestText)
            if (matcher.find())
            {   def line    = matcher.group(0) + "\r\n" + codeTag + "=\"1\"" + "\r\n"
                content = matcher.replaceFirst(line)
                manifestFile.write(content)
                msg = " - add in manifest -> android:versionCode=1" + msg
                resultCode=1
            }
            else
            {   resultCode = 0
                msg = "unable to add android:versionCode" + msg
            }
        }
        return resultCode
    }

    def incManifestVersionName(int code, Map parameters, int maj, int min, int patch)
    {   //see semserv.org
        def manifestFile =  new File(parameters.androidManifest)
        def manifestText = manifestFile.getText()
        def versionTag   = "android:versionName"
        def manifestTag  = "manifest"
        def version = versionTag + "=\"" + parameters.prefixVersionName + maj + "." + min + "." + patch
        if (parameters.withVersionCodeSuffix) { version = version + "-" + code}
        if (parameters.withDebugSuffix)       { version = version + "_debug"}
        version = version + "\""
        def pattern = Pattern.compile(versionTag + "(\\s*)" + "=" + "(\\s*)" + "\"(\\S+)\"")
        def matcher = pattern.matcher(manifestText)
        def content

        if (matcher.find())
        {   content = matcher.replaceFirst(version)
            manifestFile.write(content)
            println ">> modify  in manifest -> " + version + msg
        }
        else
        {   pattern = Pattern.compile(manifestTag)
            matcher = pattern.matcher(manifestText)
            if (matcher.find())
            {   def line = matcher.group(0) + "\r\n" + version + "\r\n"
                content = matcher.replaceFirst(line)
                manifestFile.write(content)
                println ">> add in manifest -> " + version + msg
            }
           else
            {   println ">> unable to add android:versionName" + msg}
        }
    }
}