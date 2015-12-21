package com.gilleslebrun.gradleplugin

class GitChangelogService {
    def GIT_LOG_CMD = 'git log --grep="%s" -E --format=%s %s..%s'
    def GIT_NOTAG_LOG_CMD = 'git log --grep="%s" -E --format=%s'
    def GIT_TAG_CMD = 'git describe --tags --abbrev=0'
    def EMPTY_COMPONENT = '$$'

    def project

    static def titleTemplate = '\n## <%= title %>\n\n'
    static def headerTemplate = '<a name="<%= version %>"></a>\\n<%= versionText %> (<%= date %>)\n\n'
    static def componentTemplate = '* **<%= name %>:**'
    static def versionTemplate = '## <%= version %><%= subtitle ? (" " + subtitle) : "" %>'
    static
    def listItemTemplate = '<%= prefix %> <%= commitSubject %> (<%= commitLink %><%= closes ? (", closes " + closes) : "" %>)\n'
    static def patchVersionTemplate = '# <%= version %><%= subtitle ? (" " + subtitle) : "" %>'
    static def issueTemplate = '${repository ? "[#$issue]($repository/issues/$issue)" : "#$issue"}'
    static def commitTemplate = '${repository ? "[$commit]($repository/commits/$commit)" : "#$commit"}'

    GitChangelogService(project) {
        this.project = project
    }

    static def LinkedHashMap parseRawCommit(String raw) {
        if (raw == null || raw.empty) {
            return null
        }
        List<String> lines = raw.split("\n")
        def msg = [:], match

        msg.hash = lines.remove(0)
        msg.subject = lines.remove(0)
        msg.closes = []
        msg.breaks = []

        //closes
        if (lines.size() > 0) {
            lines.each { line -> readCloses(line,msg)
            }
        }else{
                msg.subject = msg.subject - readCloses(msg.subject,msg)
        }

        //breaks
        match = raw =~ /BREAKING CHANGE:([\s\S]*)/
        if (match) {
            msg.breaking = match[0][1];
        }

        msg.body = lines.join('\n');
        match = msg.subject =~ /^(.*)\((.*)\):\s(.*)$/

        //parse subject
        if (match.size() == 0) {
            match = msg.subject =~ /^(.*):\s(.*)$/
            if (!match) {
                println "Incorrect message: ${msg.hash} ${msg.subject}"
                return null;
            }
            msg.type = match[0][1];
            msg.subject = match[0][2];

            return msg;
        }

        msg.type = match[0][1];
        msg.component = match[0][2];
        msg.subject = match[0][3];

        return msg;
    }

    static def String readCloses(String line, msg){
        def match
        match = line =~ /(?:Closes|Fixes|Resolves|closes|fixes|resolves)\s((?:#\d+(?:\,\s)?)+)/
        if (match) {
            println("match is ${match}")
            match[0] - match[0][1]
            List<String> issues = match[0][1].split(",")
            if(issues.size()>0){
                issues.each {issue -> msg.closes.push(issue.trim())}
            }else{
                msg.closes.push(match[0][1])
            }
            println("returning ${match[0][0]}")
            return match[0][0]
        }
        return null
    }

    def ArrayList readGitLog(grep, from = null, to = null) {
        def cmd
        if (from) {
            cmd = String.format(GIT_LOG_CMD, grep, '%H%n%s%n%b%n==END==', from, to).split(" ")
        } else {
            cmd = String.format(GIT_NOTAG_LOG_CMD, grep, '%H%n%s%n%b%n==END==').split(" ")
        }
        def out = new ByteArrayOutputStream()
        println "DEBUG(cmd): ${cmd.join(" ")}"
        def res = project.exec {
            if (System.properties['os.name'].toLowerCase().contains('windows')) {
                commandLine 'cmd', '/c', cmd
            } else {
                commandLine cmd
            }

            standardOutput out
        }
        def arr = out.toString().split("\n==END==\n");

        def commits = []
        for (int i = 0; i < arr.length; i++) {
            def commit = parseRawCommit(arr[i])
            if (commit != null) {
                commits.add(commit)
            }
        }
        return commits
    }

    def getPreviousTag() {
        def cmd = GIT_TAG_CMD.split(" ")
        def out = new ByteArrayOutputStream()
        def outError = new ByteArrayOutputStream()

        project.exec {
            ignoreExitValue true
            if (System.properties['os.name'].toLowerCase().contains('windows')) {
                commandLine 'cmd', '/c', cmd
            } else {
                commandLine cmd
            }

            standardOutput out
            errorOutput outError
        }
        if (!outError.toString().replace("\n", "").isEmpty()) {
            println "Cannot get the previous tag"
        }
        return out.toString().replace("\n", "")
    }

    def writeChangelog(RandomAccessFile fw, List commits, Map opts) {
        def sections = [
                fix     : [:],
                feat    : [:],
                breaks  : [:],
                perf    : [:],
                style   : [:],
                refactor: [:],
                test    : [:],
                chore   : [:],
                docs    : [:]
        ]

        //sections.breaks[EMPTY_COMPONENT] = []

        commits.each { c ->
            def section = sections["${c.type}"]
            def component = c.component ? c.component : EMPTY_COMPONENT

            if (section != null) {
                section[component] = section[component] ? section[component] : []
                section[component].push(c);
            }

            if (c.breaking != null) {
                sections.breaks[component] = sections.breaks[component] ? sections.breaks[component] : []
                sections.breaks[component].push([
                        subject: "due to [${c.hash.substring(0, 8)}](${opts.repoUrl}/commits/${c.hash}),\n ${c.breaking}",
                        hash   : c.hash,
                        closes : []
                ]);
            }
        }
        def b = new byte[fw.length()]
        println("Appending? ${opts.append}")
        if(opts.append.toBoolean()) fw.read(b)
        fw.seek(0)
        def binding = [
                "version"    : opts.version,
                "date"       : currentDate(),
                "versionText": versionText(opts.version, opts.versionText)
        ]
        def template = engine.createTemplate(headerTemplate).make(binding)

        fw.write(template.toString().bytes)
        printSection(opts, fw, 'Documentation', sections.docs)
        printSection(opts, fw, 'Bug Fixes', sections.fix)
        printSection(opts, fw, 'Features', sections.feat)
        printSection(opts, fw, 'Performance', sections.perf)
        printSection(opts, fw, 'Refactor', sections.refactor, false)
        printSection(opts, fw, 'Style', sections.style, false)
        printSection(opts, fw, 'Test', sections.test, false)
        printSection(opts, fw, 'Chore', sections.chore, false)
        printSection(opts, fw, 'Breaking Changes', sections.breaks, false)
        printSection(opts, fw, 'Docs', sections.docs, false)

        fw.write(b)
        fw.close()
    }

    static def engine = new groovy.text.GStringTemplateEngine()

    def printSection(Map opts, RandomAccessFile fw, String title, Map section, boolean printCommitLinks = true) {

        if (section.isEmpty()) return;
        section.sort()

        def binding = ["title": title]
        def template = engine.createTemplate(titleTemplate).make(binding)

        fw.write(template.toString().bytes)

        section.each { c ->
            def prefix = '*'
            def nested = section["${c.key}"].size() > 1

            if (c.key != EMPTY_COMPONENT) {
                binding = ["name": c.key]
                def componentText = engine.createTemplate(componentTemplate).make(binding)
                if (nested) {
                    fw.write((componentText.toString() + '\n').bytes)
                    prefix = '  *'
                } else {
                    prefix = componentText.toString()
                }
            }

            section[c.key].each { commit ->
                if (printCommitLinks) {

                    binding = [
                            "prefix"       : prefix,
                            "commitSubject": commit.subject,
                            "commitLink"   : linkToCommit(commit.hash, opts),
                            "closes"       : commit.closes.collect { linkToIssue(it, opts) }.join(", ")
                    ]
                    def commitText = engine.createTemplate(listItemTemplate).make(binding)
                    fw.write(commitText.toString().bytes)
                } else {
                    fw.write(String.format("%s %s\n", prefix, commit.subject).bytes)
                }
            }
        }
    }

    static def String linkToCommit(String hash, Map opts) {
        def binding = [
                "commit"    : hash.substring(0, 8),
                "repository": opts.repoUrl
        ]
        return engine.createTemplate(commitTemplate).make(binding).toString()
    }

    static def String linkToIssue(issue, Map opts) {
        def binding = [
                "issue"     : issue.replaceAll('#',''),
                "repository": opts.trackerUrl ? opts.trackerUrl : opts.repoUrl
        ]
        return engine.createTemplate(issueTemplate).make(binding).toString()
    }

    static def String currentDate() {
        def c = new GregorianCalendar()
        return String.format('%tY/%<tm/%<td', c)
    }

    static def String versionText(String version, String subtitle) {
        def isMajor = version.tokenize('.')[2] == '0';
        def binding = [
                "version" : version,
                "subtitle": subtitle
        ]
        if (isMajor) {
            return engine.createTemplate(versionTemplate).make(binding).toString()
        } else {
            return engine.createTemplate(patchVersionTemplate).make(binding).toString()
        }
    }
}
