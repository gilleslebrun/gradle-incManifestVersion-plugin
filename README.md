# Gradle increment Android Manifest Version code and version name

A Gradle plugin which auto increment Android manifest version code and writes version name according to conventional version syntax, see semantic versioning http://semver.org.

## Conventional version syntax

Conventional version syntax as defined by semver.org.
For this purpose a set of simple and easy to use conventions are used:

- majorVersion.minorVersion.patchVersion

It also could add:

- version code in version name as a `suffix "-versioncodenumber"` 

- debug flag in version name as `suffix "_debug"`

- app name or any `prefix` you would like such as "myApp"

## Basic Usage

Add the plugin to your `buildscript`'s `dependencies` section:


```classpath 'com.github.gilleslebrun:git-gradleplugin:0.0.1'
```

Apply the `incManifestVersion` plugin:

```
apply plugin: 'incManifestVersion'
```

Configure your version specs and call the `incManifestVersion` Gradle task.

## Configuration

Configuration of the `incManifestVersion` has 3 mandatory integer parameters:

```
incManifestVersion {
 
 majorVersionNumber 1  
 
 minorVersionNumber 2
 
 patchVersionNumber 3
  

```
 will add or modify in Android Manifest: 
 
 
 - `android:versionCode="1"` if not defined previously.
 
 - `android:versionCode="autoincrementednumber"` if already defined previously.

 - `android:versionName="1.2.3"` whatever the previous version name.	  
  
 others string and boolean parameters are optionnals, if none is provided the defaults settings will be used. Default values are detailed below.

``` 
    androidManifest  "path_and_name_to_an_alternative_AndroidManifest.xml"
    //if not defined your default AndroidManifest.xml file.
 
 	prefixVersionName  "Myapp_"
    //if not defined no prefix, otherwise with a prefix android:versionName="Myapp_1.2.3".

    withVersionCodeSuffix  true
    //default value false, if true with the version code as suffix, android:versionName="Myapp_1.2.3-24".
    
    withDebugSuffix  true
    //default value false, if true with the debug suffix, android:versionName="Myapp_1.2.3-24_debug".
    
} 
```



## License

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

## Changelog
